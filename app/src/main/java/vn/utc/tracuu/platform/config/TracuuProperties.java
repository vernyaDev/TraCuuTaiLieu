package vn.utc.tracuu.platform.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Toàn bộ tham số hiệu chỉnh được của hệ thống, ràng buộc thành bản ghi bất biến.
 *
 * <p>Hai điều ràng buộc hình dạng của lớp này:
 * <ul>
 *   <li>{@code NFR-system-047} — mọi ngưỡng sửa được bằng tệp cấu hình, không dịch lại mã.
 *       Hằng số ngưỡng nằm rải trong mã là vi phạm, kể cả khi giá trị đúng.</li>
 *   <li>{@code NFR-system-046} — kết quả đo mang theo điều kiện sinh ra nó. Bản ghi bất biến
 *       tuần tự hoá được nguyên vẹn vào {@code eval_runs.config}; một đối tượng đổi được
 *       giữa chừng thì cấu hình ghi lại không còn là cấu hình đã chạy.</li>
 * </ul>
 */
@Validated
@ConfigurationProperties(prefix = "tracuu")
public record TracuuProperties(
        @NotNull Storage storage,
        @NotNull Security security,
        @NotNull Embedding embedding,
        @NotNull Llm llm,
        @NotNull Segmentation segmentation,
        @NotNull Chunking chunking,
        @NotNull Retrieval retrieval,
        @NotNull Context context,
        @NotNull Agent agent,
        @NotNull Ingestion ingestion,
        @NotNull Qa qa,
        @NotNull Gap gap,
        @NotNull Audit audit
) {

    public record Storage(
            @NotBlank String root,
            @Min(1) int maxFileSizeMb,
            @NotNull List<String> acceptedExtensions
    ) {
        public long maxFileSizeBytes() {
            return (long) maxFileSizeMb * 1024L * 1024L;
        }

        public boolean accepts(String extension) {
            return extension != null && acceptedExtensions.contains(extension.toLowerCase());
        }
    }

    public record Security(
            @Min(4) int bcryptStrength,
            @Min(1) int maxFailedLogins,
            @Min(1) int lockoutMinutes,
            @NotNull Map<Integer, String> clearanceLabels
    ) {
        /** Nhãn hiển thị của một mức mật. Thang là SỐ nên thêm mức thứ tư là sửa cấu hình. */
        public String labelOf(int level) {
            return clearanceLabels.getOrDefault(level, "Mức " + level);
        }

        public int maxClearance() {
            return clearanceLabels.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
        }
    }

    public record Embedding(
            @NotBlank String modelPath,
            @NotBlank String modelFile,
            @NotBlank String tokenizerFile,
            @Min(1) int dimension,
            @Min(16) int maxSequenceLength,
            @Min(1) int batchSize,
            @Min(1) int threads
    ) {}

    public record Llm(
            @NotBlank String baseUrl,
            @NotBlank String model,
            @NotNull Duration connectTimeout,
            @NotNull Duration readTimeout,
            double temperature,
            double topP,
            long seed,
            @Min(1) int numPredict,
            /**
             * Tỉ lệ tối thiểu của mô hình phải nằm trên GPU thì phép kiểm khởi động số 4 mới cho
             * qua. Đặt thành cấu hình chứ không phải hằng số vì nó là một ĐÁNH ĐỔI, không phải
             * một hằng vật lý: nơi nào chấp nhận chậm hơn để chạy mô hình lớn hơn thì hạ sàn.
             */
            double minGpuRatio
    ) {}

    public record Segmentation(
            @NotBlank String strategy,
            @NotBlank String dictionary
    ) {}

    public record Chunking(
            @NotBlank String defaultStrategy,
            @NotNull Map<String, ChunkStrategy> strategies
    ) {
        public ChunkStrategy strategy(String name) {
            ChunkStrategy s = strategies.get(name);
            if (s == null) {
                throw new IllegalArgumentException(
                        "Chiến lược tách đoạn không có trong cấu hình: " + name
                        + ". Đang khai: " + strategies.keySet());
            }
            return s;
        }

        public ChunkStrategy defaults() {
            return strategy(defaultStrategy);
        }
    }

    public record ChunkStrategy(
            @Min(32) int maxTokens,
            @DefaultValue("0") int overlapTokens,
            @DefaultValue("0") int minTokens,
            @DefaultValue("0") int breakpointPercentile
    ) {}

    public record Retrieval(
            @Min(1) int topK,
            @Min(1) int candidateK,
            @NotBlank String mode,
            @Min(1) int rrfK,
            @NotNull Bm25 bm25,
            double allowListRatioThreshold,
            boolean iterativeScan,
            /**
             * Phạm vi quyền dưới tỉ lệ này thì bật quét chỉ mục lặp.
             *
             * <p>Phải đặt bằng hoặc cao hơn {@code allowListRatioThreshold}, nếu không sinh ra
             * một DẢI MÙ: người có phạm vi nằm giữa hai ngưỡng vừa không được quét lặp, vừa
             * không được lợi thế của danh sách cho-phép — và họ mất độ phủ mà không dấu hiệu nào
             * báo. Bản trước đặt cứng 0.25 trong mã trong khi ngưỡng kia là 0.5 cấu hình được.
             */
            double narrowScopeRatio,
            /**
             * Trần số tuple mà quét chỉ mục lặp được xem qua.
             *
             * <p>Đây là chỗ đánh đổi độ trễ lấy độ phủ, nên nó phải là cấu hình chứ không phải
             * hằng số. Trần NHỎ HƠN số đoạn của tập đang hoạt động nghĩa là quét có thể dừng
             * giữa chừng — đúng kiểu tụt độ phủ không có dấu hiệu mà quét lặp sinh ra để chống.
             * Hệ thống cảnh báo khi rơi vào tình huống đó thay vì im lặng.
             */
            @Min(1) int maxScanTuples,
            double minScore
    ) {
        public boolean usesVector() { return !"bm25".equals(mode); }
        public boolean usesKeyword() { return !"vector".equals(mode); }
    }

    public record Bm25(double k1, double b) {}

    public record Context(
            @Min(256) int maxTokens,
            @Min(1) int maxChunksPerDocument
    ) {}

    public record Agent(
            @Min(1) int maxSubqueries,
            @Min(0) int historyTurns,
            @Min(1) int summarizeAfterTurns,
            @NotNull Refusal refusal,
            @NotNull Grounding grounding
    ) {}

    public record Refusal(double minTopScore, @Min(1) int minSupportingChunks) {}

    public record Grounding(boolean enabled, double minSentenceSupport) {}

    public record Ingestion(
            @Min(1) int threadPoolSize,
            @Min(1) int queueCapacity,
            /**
             * Bắt quản trị xác nhận metadata TRƯỚC khi tài liệu vào chỉ mục.
             *
             * <p>Mặc định tắt. Lý do: nhãn mật mặc định là mức thấp nhất và quyền đọc là
             * từ-chối-mặc-định, nên một tài liệu chưa xác nhận vẫn không ai đọc được cho tới
             * khi có người cấp quyền — cái gate thật đã nằm ở tầng quyền. Bật cờ này thêm một
             * bước thủ công cho mọi tài liệu để đổi lấy một bảo đảm mà tầng quyền đã cho.
             *
             * <p>Tổ chức nào muốn ngày hiệu lực được người kiểm trước khi bất kỳ câu trả lời
             * nào dựa vào nó thì bật lên.
             */
            boolean requireMetadataConfirmation,
            @NotNull Overlap overlap,
            @NotNull Cluster cluster
    ) {}

    public record Overlap(boolean enabled, double similarityThreshold) {}

    public record Cluster(double similarityThreshold) {}

    public record Qa(
            @Min(1) int maxConcurrentQuestions,
            @Min(1) int queueCapacity,
            @NotNull Duration queueTimeout
    ) {}

    public record Gap(
            @Min(1) int retentionMonths,
            double clusterSimilarityThreshold,
            @Min(1) int minClusterSize
    ) {}

    public record Audit(@Min(1) int retentionMonths) {}
}
