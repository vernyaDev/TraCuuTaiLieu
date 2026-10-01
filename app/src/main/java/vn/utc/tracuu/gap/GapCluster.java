package vn.utc.tracuu.gap;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một nhóm khoảng trống — nhiều câu hỏi cùng chủ đề đều bị từ chối.
 *
 * <p>Gom nhóm là thứ biến một danh sách hàng trăm câu hỏi lẻ thành một danh sách vài việc phải
 * làm. Trạng thái đã xử lý tồn tại để nhóm ấy không hiện lại lần nữa sau khi quản trị đã bổ
 * sung tài liệu — không có nó thì danh sách chỉ dài thêm và sẽ bị bỏ qua.
 */
@Entity
@Table(name = "gap_clusters")
@Getter @Setter @NoArgsConstructor
public class GapCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String label;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 0;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen = Instant.now();

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen = Instant.now();

    @Column(nullable = false, length = 20)
    private String status = "CHO";

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolve_note", length = 1000)
    private String resolveNote;
}
