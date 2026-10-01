package vn.utc.tracuu.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Nhật ký kiểm toán quyền. NFR-system-029 đòi giữ tối thiểu 12 tháng.
 *
 * <p>Đây là thứ trả lời được câu "ai đã mở quyền tài liệu này" khi có sự cố — câu mà không
 * bảng nào khác trong lược đồ trả lời nổi, vì trạng thái hiện tại không mang lịch sử.
 */
@Entity
@Table(name = "permission_audit")
@Getter @Setter @NoArgsConstructor
public class PermissionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    /**
     * Khai kiểu JSON cho tầng ánh xạ.
     *
     * <p>Thiếu khai báo này thì Hibernate gửi giá trị đi dưới dạng chuỗi ký tự, và PostgreSQL
     * từ chối vì nó KHÔNG tự ép chuỗi sang jsonb. Lỗi chỉ xuất hiện ở lần ghi thật đầu tiên —
     * biên dịch sạch, kiểm thử đơn vị sạch, rồi đổ ở môi trường chạy.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb")
    private String beforeJson;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb")
    private String afterJson;

    @Column(name = "at", nullable = false)
    private Instant at = Instant.now();

    public static PermissionAudit of(Long actorId, String action, String targetType,
                                     Long targetId, String before, String after) {
        PermissionAudit a = new PermissionAudit();
        a.actorId = actorId;
        a.action = action;
        a.targetType = targetType;
        a.targetId = targetId;
        a.beforeJson = before;
        a.afterJson = after;
        return a;
    }
}
