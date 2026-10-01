package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.List;

/**
 * Chọn đường sinh biểu diễn ngữ nghĩa và <strong>nói rõ đang đi đường nào</strong>.
 *
 * <p>Lớp này tồn tại vì hai đường không tương đương: ONNX trên CPU là thiết kế, còn máy chủ mô
 * hình là đường vá tạm dùng chung GPU với việc sinh chữ. Ẩn việc chuyển đường đi là cách tạo ra
 * một hệ thống chạy được nhưng không ai giải thích nổi vì sao độ trễ nạp tài liệu lại làm chậm
 * việc trả lời.
 *
 * <p>Chữ ký mô hình được ghi kèm mỗi tập đoạn. Nạp nửa kho bằng đường này rồi nửa kia bằng
 * đường kia thì hai nửa nằm ở hai không gian vector khác nhau, và truy hồi trở nên vô nghĩa
 * trên một nửa — mà không có lỗi nào báo ra.
 */
@Service
public class EmbeddingService implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final OnnxEmbeddingModel onnx;
    private final OllamaClient llm;
    private final TracuuProperties props;
    private final String fallbackModel;

    public EmbeddingService(OnnxEmbeddingModel onnx, OllamaClient llm, TracuuProperties props,
                            @Value("${tracuu.embedding.fallback-model:nomic-embed-text}") String fallbackModel) {
        this.onnx = onnx;
        this.llm = llm;
        this.props = props;
        this.fallbackModel = fallbackModel;
    }

    @Override
    public boolean ready() {
        return onnx.ready() || llm.reachable();
    }

    @Override
    public int dimension() {
        return props.embedding().dimension();
    }

    @Override
    public String signature() {
        return onnx.ready() ? onnx.signature() : "ollama:" + fallbackModel;
    }

    /** Đang chạy đường chính hay đường dự phòng — bộ đo ghi giá trị này vào cấu hình lượt chạy. */
    public boolean usingDesignedPath() {
        return onnx.ready();
    }

    @Override
    public float[] embed(String text) {
        if (onnx.ready()) return onnx.embed(text);
        return fit(OnnxEmbeddingModel.normalize(llm.embed(safe(text), fallbackModel)));
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (onnx.ready()) return onnx.embedAll(texts);
        return texts.stream().map(this::embed).toList();
    }

    /**
     * Ép vector về đúng số chiều cột lược đồ khai báo.
     *
     * <p>Mô hình dự phòng có thể trả số chiều khác. Cắt bớt hay đệm thêm đều làm méo khoảng
     * cách, nên đây là chỗ <strong>phải kêu to</strong> chứ không phải chỗ sửa lặng lẽ: kho nạp
     * bằng vector đã bị ép sẽ cho kết quả truy hồi kém mà không lỗi nào chỉ ra nguyên nhân.
     */
    private float[] fit(float[] v) {
        int want = dimension();
        if (v.length == want) return v;
        log.warn("Mô hình dự phòng trả {} chiều nhưng lược đồ khai {} chiều. Vector bị ép cho vừa, "
                + "chất lượng truy hồi sẽ giảm. Sửa tracuu.embedding.dimension cho khớp mô hình, "
                + "rồi nạp lại kho.", v.length, want);
        float[] out = new float[want];
        System.arraycopy(v, 0, out, 0, Math.min(v.length, want));
        return OnnxEmbeddingModel.normalize(out);
    }

    private String safe(String text) {
        if (text == null) return "";
        // Cắt theo ký tự để tránh gửi cả một bảng dài sang máy chủ mô hình.
        int max = props.embedding().maxSequenceLength() * 8;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
