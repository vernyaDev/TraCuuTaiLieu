package vn.utc.tracuu.ingestion;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sinh biểu diễn ngữ nghĩa bằng mô hình ONNX, chạy trên CPU trong máy ảo Java.
 *
 * <p>Đây là đường chính theo kiến trúc Mục 1. Chạy trên CPU là <strong>quyết định có chủ
 * đích</strong>, không phải hạn chế: bộ nhớ đồ hoạ 6 GB chỉ đủ cho mô hình sinh chữ, và nếu
 * hai việc cùng tranh GPU thì việc nạp tài liệu sẽ làm treo việc trả lời — đúng thứ mà
 * {@code NFR-system-011} đặt ngưỡng để đo.
 *
 * <p>Gộp các đoạn lại thành lô trước khi chạy: trên CPU, phần lớn thời gian một lượt suy luận
 * là chi phí cố định, nên chạy 16 đoạn một lô nhanh hơn hẳn 16 lần chạy lẻ.
 */
@Component
public class OnnxEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(OnnxEmbeddingModel.class);

    private final TracuuProperties props;

    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;
    private boolean ready;
    private String signature = "onnx:chua-nap";

    public OnnxEmbeddingModel(TracuuProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        Path dir = Path.of(props.embedding().modelPath());
        Path modelFile = dir.resolve(props.embedding().modelFile());
        Path tokenizerFile = dir.resolve(props.embedding().tokenizerFile());

        if (!Files.isReadable(modelFile) || !Files.isReadable(tokenizerFile)) {
            log.info("Chưa có mô hình ONNX tại {} — khâu nạp sẽ dùng đường dự phòng.",
                    dir.toAbsolutePath());
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setIntraOpNumThreads(props.embedding().threads());
            session = env.createSession(modelFile.toString(), options);

            tokenizer = HuggingFaceTokenizer.builder()
                    .optTokenizerPath(tokenizerFile)
                    .optMaxLength(props.embedding().maxSequenceLength())
                    .optTruncation(true)
                    .optPadding(true)
                    .build();

            ready = true;
            signature = "onnx:" + modelFile.getFileName() + ":" + props.embedding().dimension()
                      + ":" + props.embedding().maxSequenceLength();
            log.info("Mô hình sinh biểu diễn ONNX đã nạp: {} chiều, {} luồng.",
                    props.embedding().dimension(), props.embedding().threads());

        } catch (Exception e) {
            log.error("Không nạp được mô hình ONNX, chuyển đường dự phòng: {}", e.getMessage());
            ready = false;
        }
    }

    @Override public boolean ready() { return ready; }
    @Override public int dimension() { return props.embedding().dimension(); }
    @Override public String signature() { return signature; }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (!ready) throw new IllegalStateException("Mô hình ONNX chưa sẵn sàng");

        List<float[]> out = new ArrayList<>(texts.size());
        int batch = props.embedding().batchSize();
        for (int i = 0; i < texts.size(); i += batch) {
            out.addAll(runBatch(texts.subList(i, Math.min(texts.size(), i + batch))));
        }
        return out;
    }

    private List<float[]> runBatch(List<String> texts) {
        int n = texts.size();
        Encoding[] encodings = tokenizer.batchEncode(texts.toArray(new String[0]));
        int seq = encodings[0].getIds().length;

        long[] ids = new long[n * seq];
        long[] mask = new long[n * seq];
        long[] types = new long[n * seq];

        for (int i = 0; i < n; i++) {
            System.arraycopy(encodings[i].getIds(), 0, ids, i * seq, seq);
            System.arraycopy(encodings[i].getAttentionMask(), 0, mask, i * seq, seq);
        }

        try (OnnxTensor idTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(ids), new long[]{n, seq});
             OnnxTensor maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(mask), new long[]{n, seq});
             OnnxTensor typeTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(types), new long[]{n, seq})) {

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", idTensor);
            inputs.put("attention_mask", maskTensor);
            // Một số mô hình không nhận đầu vào này; chỉ truyền khi phiên khai báo nó.
            if (session.getInputNames().contains("token_type_ids")) {
                inputs.put("token_type_ids", typeTensor);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                float[][][] hidden = (float[][][]) result.get(0).getValue();
                List<float[]> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    out.add(normalize(meanPool(hidden[i], encodings[i].getAttentionMask())));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Lỗi khi sinh biểu diễn ngữ nghĩa: " + e.getMessage(), e);
        }
    }

    /**
     * Gộp trung bình theo mặt nạ chú ý.
     *
     * <p>Phải loại các vị trí đệm ra khỏi phép trung bình. Gộp cả phần đệm thì đoạn ngắn bị kéo
     * về một vector chung, và mọi đoạn ngắn trông như giống nhau — một lỗi làm tụt chất lượng
     * truy hồi mà không hề báo lỗi.
     */
    private float[] meanPool(float[][] tokenVectors, long[] attentionMask) {
        int dim = tokenVectors[0].length;
        float[] sum = new float[dim];
        int count = 0;
        for (int t = 0; t < tokenVectors.length && t < attentionMask.length; t++) {
            if (attentionMask[t] == 0) continue;
            for (int d = 0; d < dim; d++) sum[d] += tokenVectors[t][d];
            count++;
        }
        if (count > 0) {
            for (int d = 0; d < dim; d++) sum[d] /= count;
        }
        return sum;
    }

    /** Chuẩn hoá độ dài 1 để khoảng cách cosin quy về tích vô hướng. */
    static float[] normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += (double) x * x;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
        }
        return v;
    }

    @PreDestroy
    void close() {
        try { if (session != null) session.close(); } catch (Exception ignored) { }
        try { if (tokenizer != null) tokenizer.close(); } catch (Exception ignored) { }
    }
}
