package vn.utc.tracuu.gap;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một lượt bị từ chối.
 *
 * <p>{@code departmentSnapshot} chụp lại phòng ban TẠI THỜI ĐIỂM HỎI. Người dùng có thể đổi
 * phòng ban sau đó, mà báo cáo khoảng trống cần biết phòng ban <em>lúc hỏi</em> để trả lời đúng
 * câu "nên cấp quyền cho phòng nào". Tra phòng ban hiện tại khi xem báo cáo là trả lời một câu
 * hỏi khác với câu đang được hỏi.
 */
@Entity
@Table(name = "refusal_log")
@Getter @Setter @NoArgsConstructor
public class RefusalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn_id")
    private Long turnId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(columnDefinition = "text")
    private String rewritten;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(name = "department_snapshot", length = 600)
    private String departmentSnapshot;

    @Column(name = "clearance_snapshot")
    private Integer clearanceSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
