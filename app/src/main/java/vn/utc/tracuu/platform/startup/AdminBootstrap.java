package vn.utc.tracuu.platform.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.Role;
import vn.utc.tracuu.access.User;
import vn.utc.tracuu.access.UserRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tạo tài khoản quản trị đầu tiên nếu chưa có tài khoản quản trị nào.
 *
 * <p>Mật khẩu KHÔNG nằm trong tệp lược đồ. Lý do: một bản băm ghi sẵn trong mã nguồn là mật khẩu
 * mặc định dùng chung cho mọi bản cài, và nó sẽ sống sót tới lúc vận hành thật. Ở đây mật khẩu
 * lấy từ biến môi trường, hoặc sinh ngẫu nhiên và in ra nhật ký <em>đúng một lần</em>, kèm cờ
 * bắt đổi ngay lần đăng nhập đầu.
 */
@Component
@Order(20)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TracuuProperties props;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, TracuuProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.countByRole(Role.QUAN_TRI) > 0) {
            return;
        }

        String username = envOr("TRACUU_ADMIN_USERNAME", "quantri");
        String fromEnv = System.getenv("TRACUU_ADMIN_PASSWORD");
        String password = (fromEnv == null || fromEnv.isBlank()) ? randomPassword() : fromEnv;

        User admin = new User();
        admin.setUsername(username);
        admin.setFullName("Quản trị tri thức");
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.QUAN_TRI);
        admin.setClearanceLevel(props.security().maxClearance());
        admin.setActive(true);
        admin.setMustChangePassword(true);
        users.save(admin);

        if (fromEnv == null || fromEnv.isBlank()) {
            log.warn("""

                    ════════════════════════════════════════════════════════════════
                     Đã tạo tài khoản quản trị đầu tiên.
                       Tên đăng nhập : {}
                       Mật khẩu tạm  : {}
                     Mật khẩu này chỉ hiện MỘT LẦN và phải đổi ngay lần đăng nhập đầu.
                     Đặt biến môi trường TRACUU_ADMIN_PASSWORD để tự chọn mật khẩu.
                    ════════════════════════════════════════════════════════════════
                    """, username, password);
        } else {
            log.info("Đã tạo tài khoản quản trị đầu tiên '{}' với mật khẩu từ biến môi trường.", username);
        }
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String randomPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return "Tc" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "9";
    }
}
