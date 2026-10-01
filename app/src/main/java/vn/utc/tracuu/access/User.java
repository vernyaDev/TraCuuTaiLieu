package vn.utc.tracuu.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tài khoản người dùng.
 *
 * <p>{@code clearanceLevel} là SỐ THỨ TỰ chứ không phải chuỗi liệt kê. Lý do ghi ở US-007:
 * thêm mức mật thứ tư về sau là thay đổi rẻ nếu thang là số, và đắt nếu thang là chuỗi —
 * đắt vì phải sửa mọi phép so sánh rải khắp mã nguồn.
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Column(name = "clearance_level", nullable = false)
    private int clearanceLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role = Role.NHAN_VIEN;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_departments",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id"))
    private Set<Department> departments = new LinkedHashSet<>();

    /** Khoá tạm thời sau nhiều lần đăng nhập sai. */
    public boolean isLockedNow() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
