package vn.utc.tracuu;

import vn.utc.tracuu.platform.config.TracuuProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Dựng bản ghi cấu hình cho kiểm thử.
 *
 * <p>Giá trị ở đây <strong>trùng với {@code application.yml}</strong>. Đó là chủ đích: một bộ
 * kiểm thử chạy trên cấu hình khác cấu hình thật sẽ xanh trong khi sản phẩm hỏng, và loại lỗi
 * ấy chỉ lộ ra khi đã lên môi trường chạy thật.
 */
public final class TestProperties {

    private TestProperties() {}

    public static TracuuProperties defaults() {
        return new TracuuProperties(
                new TracuuProperties.Storage("./var/documents", 50,
                        List.of("docx", "doc", "pdf", "xlsx", "txt", "md")),
                new TracuuProperties.Security(12, 5, 15,
                        Map.of(1, "Công khai", 2, "Nội bộ", 3, "Mật")),
                new TracuuProperties.Embedding("./models", "model.onnx", "tokenizer.json",
                        768, 256, 16, 4),
                new TracuuProperties.Llm("http://localhost:11434", "qwen2.5:7b-instruct-q4_K_M",
                        Duration.ofSeconds(10), Duration.ofSeconds(180), 0.1, 0.9, 42, 1024,
                        1.0),
                new TracuuProperties.Segmentation("longest-match", "classpath:vi/vi-words.txt"),
                new TracuuProperties.Chunking("structural", Map.of(
                        "fixed", new TracuuProperties.ChunkStrategy(512, 64, 0, 0),
                        "structural", new TracuuProperties.ChunkStrategy(512, 64, 80, 0))),
                new TracuuProperties.Retrieval(8, 50, "hybrid", 60,
                        new TracuuProperties.Bm25(1.2, 0.75), 0.5, true, 0.5, 60000, 0.0),
                new TracuuProperties.Context(3072, 3),
                new TracuuProperties.Agent(4, 6, 10,
                        new TracuuProperties.Refusal(0.35, 1),
                        new TracuuProperties.Grounding(true, 0.30)),
                new TracuuProperties.Ingestion(2, 500, false,
                        new TracuuProperties.Overlap(true, 0.85),
                        new TracuuProperties.Cluster(0.75)),
                new TracuuProperties.Qa(5, 50, Duration.ofMinutes(5)),
                new TracuuProperties.Gap(12, 0.80, 3),
                new TracuuProperties.Audit(12));
    }
}
