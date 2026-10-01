package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Một đợt nạp tài liệu.
 *
 * <p>Nạp là thao tác KHÔNG ĐỒNG BỘ: gọi xong trả về {@code trackingId} ngay, tiến độ đọc qua
 * đường riêng. {@code AC-007} của US-009 đòi việc xử lý không phụ thuộc người dùng còn mở màn
 * hình hay không — đóng tab giữa chừng thì việc nạp vẫn chạy tiếp.
 */
@Entity
@Table(name = "ingestion_batches")
@Getter @Setter @NoArgsConstructor
public class IngestionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id", nullable = false, unique = true)
    private UUID trackingId = UUID.randomUUID();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(nullable = false, length = 20)
    private String status = "DANG_CHAY";

    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;
}
