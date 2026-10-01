package vn.utc.tracuu.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.security.AppUserDetails;
import vn.utc.tracuu.platform.security.CurrentUser;

import java.util.List;

/** Xác thực — hợp đồng giao diện Mục 2. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final UserRepository users;
    private final TracuuProperties props;

    public AuthController(AuthService auth, UserRepository users, TracuuProperties props) {
        this.auth = auth;
        this.users = users;
        this.props = props;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank String newPassword) {}

    /**
     * Thông tin người đang đăng nhập.
     *
     * <p>{@code mustChangePassword} là cờ làm giao diện chuyển sang màn đổi mật khẩu và
     * <strong>chưa</strong> cho vào màn hỏi đáp. {@code admin} là thứ giao diện dùng để quyết
     * hiện khu quản trị hay không — lộ sự tồn tại của chức năng là vô hại.
     */
    public record Me(
            Long id, String username, String fullName, String role,
            int clearanceLevel, String clearanceLabel,
            List<String> departments, boolean mustChangePassword, boolean admin) {}

    @PostMapping("/login")
    public Me login(@Valid @RequestBody LoginRequest body,
                    HttpServletRequest req, HttpServletResponse res) {
        AppUserDetails d = auth.login(body.username(), body.password(), req, res);
        return toMe(d);
    }

    @GetMapping("/me")
    public Me me() {
        return toMe(CurrentUser.require());
    }

    @PostMapping("/change-password")
    public Me changePassword(@Valid @RequestBody ChangePasswordRequest body,
                             HttpServletRequest req, HttpServletResponse res) {
        AppUserDetails d = auth.changePassword(
                CurrentUser.requireId(), body.currentPassword(), body.newPassword(), req, res);
        return toMe(d);
    }

    private Me toMe(AppUserDetails d) {
        List<String> deptNames = users.findByIdWithDepartments(d.userId())
                .map(u -> u.getDepartments().stream().map(Department::getName).toList())
                .orElse(List.of());
        return new Me(
                d.userId(), d.username(), d.fullName(), d.role().name(),
                d.clearanceLevel(), props.security().labelOf(d.clearanceLevel()),
                deptNames, d.mustChangePassword(), d.role().isAdmin());
    }
}
