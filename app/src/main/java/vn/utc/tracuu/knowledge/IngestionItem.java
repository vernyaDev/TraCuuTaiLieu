package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Một tệp trong một đợt nạp. Lỗi của tệp này không làm hỏng các tệp khác cùng đợt. */
@Entity
@Table(name = "ingestion_items")
@Getter @Setter @NoArgsConstructor
public class IngestionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(nullable = false, length = 400)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionStatus status = IngestionStatus.CHO;

    /** Bước đang chạy, bằng lời nghiệp vụ — nguồn cho chỉ báo tiến trình của màn nạp. */
    @Column(length = 40)
    private String stage;

    @Column(name = "error_code", length = 40)
    private String errorCode;

    @Column(name = "error_reason", length = 1000)
    private String errorReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
