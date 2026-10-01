package vn.utc.tracuu.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.access.Role;
import vn.utc.tracuu.access.User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Danh tính người đang đăng nhập, gắn vào phiên.
 *
 * <p>Giữ {@code clearanceLevel} và danh sách phòng ban ở đây là để GIAO DIỆN biết hiển thị gì,
 * <strong>không phải</strong> để quyết định quyền. Quyền luôn tính lại từ cơ sở dữ liệu qua
 * {@code PermissionResolver} — nếu quyết định quyền bằng dữ liệu chụp lúc đăng nhập thì
 * {@code AC-005} của US-008 (quyền đổi có hiệu lực ngay ở câu kế tiếp) không thể đạt.
 */
public record AppUserDetails(
        Long userId,
        String username,
        String fullName,
        String passwordHash,
        Role role,
        int clearanceLevel,
        boolean active,
        boolean mustChangePassword,
        Set<Long> departmentIds
) implements UserDetails, Serializable {

    public static AppUserDetails from(User u) {
        return new AppUserDetails(
                u.getId(), u.getUsername(), u.getFullName(), u.getPasswordHash(),
                u.getRole(), u.getClearanceLevel(), u.isActive(), u.isMustChangePassword(),
                u.getDepartments().stream().map(d -> d.getId()).collect(Collectors.toSet()));
    }

    /** Phạm vi quyền của mọi truy vấn phát sinh từ phiên này. Luôn là {@code user(id)}. */
    public PermissionScope scope() {
        return new PermissionScope.ForUser(userId);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
