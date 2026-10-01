package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.knowledge.*;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Năm bước biến một tệp thành các đoạn tra cứu được.
 *
 * <pre>
 *   bóc chữ  →  tách đoạn  →  tách từ  →  sinh biểu diễn  →  ghi + thống kê
 * </pre>
 *
 * <p>Lớp này cố ý <strong>không biết gì về HTTP, về hàng đợi hay về giao dịch dài</strong>: nó
 * nhận một tệp và trả về kết quả. Việc chạy nền, ghi tiến độ và gom đợt thuộc về
 * {@code IngestionService}. Tách như vậy để bộ đo gọi lại được đúng đường này khi cần nạp kho
 * thử nghiệm theo nhiều chiến lược tách đoạn khác nhau.
 *
 * <p>Luật R5: khâu nạp <strong>không gọi</strong> gói agent. Hai bên gặp nhau qua các bảng của
 * gói knowledge, không gọi thẳng nhau — nạp là việc theo đợt, trả lời là việc theo yêu cầu, và
 * trộn hai vòng đời đó là cách nhanh nhất để việc nạp làm treo việc trả lời.
 */
@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final List<TextExtractor> extractors;
    private final Map<String, Chunker> chunkers;
    private final VietnameseSegmenter segmenter;
    private final EmbeddingService embeddings;
    private final ChunkWriter writer;
    private final Bm25StatsService bm25;
    private final TracuuProperties props;
    private final vn.utc.tracuu.knowledge.ChunkSetRepository chunkSets;

    public IngestionPipeline(List<TextExtractor> extractors,
                             List<Chunker> chunkerList,
                             VietnameseSegmenter segmenter,
                             EmbeddingService embeddings,
                             ChunkWriter writer,
                             Bm25StatsService bm25,
                             TracuuProperties props,
                             vn.utc.tracuu.knowledge.ChunkSetRepository chunkSets) {
        this.extractors = extractors;
        this.chunkers = chunkerList.stream()
                .collect(Collectors.toMap(Chunker::name, c -> c));
        this.segmenter = segmenter;
        this.embeddings = embeddings;
        this.writer = writer;
        this.chunkSets = chunkSets;
        this.bm25 = bm25;
        this.props = props;
    }

    /**
     * Kết quả nạp một tệp.
     *
     * <p>Mang theo cả {@code content} đã bóc: bước gợi ý metadata cần đọc văn bản gốc, và bóc
     * lại lần nữa chỉ để lấy đúng văn bản vừa bóc là trả giá hai lần cho một việc — với tệp
     * pdf lớn thì lần bóc thứ hai không rẻ.
     */
    public record Result(int chunkCount, String detectedTitle, int characterCount,
                         String modelSignature, ExtractedContent content) {}

    /**
     * Nạp một tệp vào một tập đoạn.
     *
     * @param onStage nhận tên bước đang chạy, bằng lời nghiệp vụ — nguồn cho chỉ báo tiến trình
     *                của màn nạp. Giao diện hiện "đang tách đoạn" dễ chịu hơn hẳn một thanh chạy
     *                không nói gì trong bốn phút.
     */
    public Result run(Path file, String originalFilename, long documentId,
                      ChunkSet chunkSet, Consumer<String> onStage) {

        String ext = FileStorage.extensionOf(originalFilename);

        onStage.accept("Đang bóc chữ");
        TextExtractor extractor = extractors.stream()
                .filter(e -> e.supports(ext))
                .findFirst()
                .orElseThrow(() -> new ExtractionException(ErrorCode.DINH_DANG_KHONG_HO_TRO,
                        "Không có bộ bóc chữ cho đuôi " + ext));
        ExtractedContent content = extractor.extract(file);

        onStage.accept("Đang tách đoạn");
        Chunker chunker = Optional.ofNullable(chunkers.get(chunkSet.getStrategyName()))
                .orElseThrow(() -> new IllegalStateException(
                        "Chiến lược tách đoạn '" + chunkSet.getStrategyName()
                        + "' không có bản cài. Đang có: " + chunkers.keySet()));
        TracuuProperties.ChunkStrategy params = props.chunking().strategy(chunkSet.getStrategyName());
        List<ChunkCandidate> candidates = chunker.chunk(content, params);

        if (candidates.isEmpty()) {
            throw new ExtractionException(ErrorCode.TEP_KHONG_CO_VAN_BAN,
                    "Bóc được chữ nhưng không tách được đoạn nào");
        }

        onStage.accept("Đang tách từ tiếng Việt");
        List<String> segmented = candidates.stream()
                .map(c -> segmenter.segment(enriched(c)))
                .toList();
        List<Integer> tokenCounts = segmented.stream()
                .map(s -> s.isEmpty() ? 0 : s.split(" ").length)
                .toList();

        // Đóng dấu / đối chiếu chữ ký TRƯỚC khi sinh vector: phát hiện sau khi đã ghi nửa kho
        // thì việc sửa là nạp lại từ đầu.
        chotChuKyBieuDien(chunkSet);

        onStage.accept("Đang sinh biểu diễn ngữ nghĩa");
        // Gắn đường dẫn phân cấp vào văn bản đem đi sinh vector: đoạn "Mức hưởng là 75%" tự nó
        // không nói về chủ đề gì, nhưng "Chế độ ốm đau › Mức hưởng là 75%" thì có.
        List<float[]> vectors = embeddings.embedAll(
                candidates.stream().map(this::enriched).toList());

        onStage.accept("Đang ghi vào chỉ mục");
        int written = writer.write(chunkSet.getId(), documentId, candidates, segmented, vectors, tokenCounts);

        onStage.accept("Đang cập nhật thống kê tìm kiếm");
        bm25.recompute(chunkSet.getId());

        log.info("Nạp xong tài liệu {}: {} đoạn, {} ký tự, mô hình {}",
                documentId, written, content.length(), embeddings.signature());
        return new Result(written, content.detectedTitle(), content.length(),
                embeddings.signature(), content);
    }

    /**
     * Đóng dấu chữ ký mô hình vào tập đoạn, hoặc từ chối nếu chữ ký đã khác.
     *
     * <p>Tập đoạn chưa có chữ ký thì nhận chữ ký của mô hình đang chạy. Tập đã có chữ ký khác
     * thì <strong>dừng lại</strong>: ghi tiếp là trộn hai không gian vector vào một chỉ mục, và
     * hậu quả không hiện ra dưới dạng lỗi mà dưới dạng kết quả tìm kiếm kém đi trên một phần
     * kho — thứ gần như không thể chẩn đoán về sau.
     *
     * <p>Thà hỏng một lần nạp còn hơn hỏng cả chỉ mục một cách im lặng. Lối ra đúng khi thật sự
     * muốn đổi mô hình là tạo tập đoạn mới và nạp lại, không phải ghi đè lên tập cũ.
     */
    private void chotChuKyBieuDien(ChunkSet chunkSet) {
        String dangChay = embeddings.signature();
        int soChieu = embeddings.dimension();
        String daCo = chunkSet.getEmbeddingSignature();

        if (daCo == null || daCo.isBlank()) {
            chunkSet.setEmbeddingSignature(dangChay);
            chunkSet.setEmbeddingDimension(soChieu);
            chunkSets.save(chunkSet);
            log.info("Đóng dấu chữ ký mô hình '{}' ({} chiều) cho tập đoạn {}.",
                    dangChay, soChieu, chunkSet.getId());
            return;
        }

        boolean khopSoChieu = chunkSet.getEmbeddingDimension() == null
                || chunkSet.getEmbeddingDimension() == soChieu;
        if (!daCo.equals(dangChay) || !khopSoChieu) {
            throw new ExtractionException(ErrorCode.MO_HINH_BIEU_DIEN_LECH, """
                    Tập đoạn đang hoạt động được nạp bằng mô hình sinh biểu diễn '%s' (%s chiều),                     còn mô hình đang chạy là '%s' (%d chiều). Ghi tiếp sẽ trộn hai không gian                     vector vào cùng một chỉ mục, làm kết quả tìm kiếm sai trên một phần kho mà                     không có lỗi nào báo ra. Muốn đổi mô hình thì tạo tập đoạn mới và nạp lại                     toàn bộ kho."""
                    .formatted(daCo, String.valueOf(chunkSet.getEmbeddingDimension()),
                            dangChay, soChieu));
        }
    }

    /** Văn bản đem đi sinh vector và tách từ: đường dẫn phân cấp cộng nội dung. */
    private String enriched(ChunkCandidate c) {
        if (c.breadcrumb() == null || c.breadcrumb().isBlank()) return c.content();
        return c.breadcrumb() + System.lineSeparator() + c.content();
    }

    /** Đánh dấu một tài liệu đã lập chỉ mục xong. */
    public void markIndexed(Document doc) {
        doc.setStatus(DocumentStatus.DA_LAP_CHI_MUC);
        doc.setIndexedAt(Instant.now());
        doc.setErrorReason(null);
    }
}
