package vn.utc.tracuu.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Phòng ban. Danh sách PHẲNG — cố ý không có cột cha.
 *
 * <p>Quyết định ở US-004: kế thừa quyền theo cây là nguồn lỗi phân quyền phổ biến nhất,
 * vì người cấu hình thấy một nhánh mà không thấy hết những gì nhánh ấy kéo theo.
 */
@Entity
@Table(name = "departments")
@Getter @Setter @NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Department(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
