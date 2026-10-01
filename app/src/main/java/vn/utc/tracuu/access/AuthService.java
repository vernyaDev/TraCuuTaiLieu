package vn.utc.tracuu.access;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.security.AppUserDetails;
import vn.utc.tracuu.platform.web.BusinessException;

import java.util.Set;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Đăng nhập, đăng xuất, đổi mật khẩu.
 *
 * <p>Một điểm dễ làm sai và khó thấy: <strong>sai tên đăng nhập và sai mật khẩu phải trả về
 * cùng MỘT thông báo</strong>. Phân biệt hai trường hợp là xác nhận tài khoản có tồn tại, tức
 * biến màn đăng nhập thành công cụ dò danh sách nhân sự.
 *
 * <p>Tài khoản bị vô hiệu hoá là ngoại lệ CÓ CHỦ ĐÍCH và trả thông báo riêng — lý do ghi ở
 * {@code SCR-access-control-01}: người bị vô hiệu hoá cần biết để đi hỏi quản trị, và bản thân
 * việc tài khoản từng tồn tại thì người đó đã biết rồi.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PermissionResolver permissions;
    private final TracuuProperties props;
    private final SecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public AuthService(UserRepository users, PasswordEncoder encoder,
                       PermissionResolver permissions, TracuuProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.permissions = permissions;
        this.props = props;
    }

    @Transactional
    public AppUserDetails login(String username, String rawPassword,
                                HttpServletRequest req, HttpServletResponse res) {

        User user = users.findByUsernameWithDepartments(username == null ? "" : username.trim())
                .orElse(null);

        if (user == null) {
            // Vẫn tốn một lần băm để thời gian phản hồi của "sai tên" không khác "sai mật khẩu";
            // chênh lệch thời gian cũng là một kênh lộ sự tồn tại.
            encoder.matches(rawPassword == null ? "" : rawPassword,
                    "$2a$12$0000000000000000000000000000000000000000000000000000");
            throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP);
        }

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.TAI_KHOAN_BI_VO_HIEU);
        }
        if (user.isLockedNow()) {
            throw new BusinessException(ErrorCode.TAI_KHOAN_BI_KHOA_TAM);
        }

        if (!encoder.matches(rawPassword == null ? "" : rawPassword, user.getPasswordHash())) {
            registerFailure(user);
            throw new BusinessException(ErrorCode.SAI_THONG_TIN_DANG_NHAP);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        users.save(user);

        AppUserDetails details = AppUserDetails.from(user);
        establishSession(details, req, res);
        log.info("Đăng nhập thành công: {}", user.getUsername());
        return details;
    }

    private void registerFailure(User user) {
        int failures = user.getFailedLoginCount() + 1;
        user.setFailedLoginCount(failures);
        if (failures >= props.security().maxFailedLogins()) {
            user.setLockedUntil(Instant.now().plus(props.security().lockoutMinutes(), ChronoUnit.MINUTES));
            user.setFailedLoginCount(0);
            log.warn("Tạm khoá tài khoản {} sau {} lần sai", user.getUsername(), failures);
        }
        users.save(user);
    }

    /**
     * Đổi mật khẩu. Thành công thì <strong>vào thẳng màn hỏi đáp, không bắt đăng nhập lại</strong>
     * ({@code AC-007} của US-002) — nên phiên được dựng lại ngay tại đây với cờ mật khẩu tạm đã tắt.
     */
    @Transactional
    public AppUserDetails changePassword(long userId, String currentPassword, String newPassword,
                                         HttpServletRequest req, HttpServletResponse res) {
        User user = users.findByIdWithDepartments(userId)
                .orElseThrow(BusinessException::khongTimThay);

        if (!encoder.matches(currentPassword == null ? "" : currentPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.MAT_KHAU_CU_KHONG_DUNG, "currentPassword");
        }
        validateStrength(newPassword);

        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        users.save(user);

        AppUserDetails details = AppUserDetails.from(user);
        establishSession(details, req, res);
        return details;
    }

    /** Quản trị đặt lại mật khẩu cho người khác: luôn bật lại cờ mật khẩu tạm. */
    @Transactional
    public void resetPassword(long targetUserId, String newPassword, long actorId) {
        validateStrength(newPassword);
        User user = users.findById(targetUserId).orElseThrow(BusinessException::khongTimThay);
        user.setPasswordHash(encoder.encode(newPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        users.save(user);
        log.info("Quản trị {} đặt lại mật khẩu cho {}", actorId, user.getUsername());
    }

    /** Độ dài tối thiểu theo {@code NFR-system-026}. */
    private static final int DO_DAI_TOI_THIEU = 12;

    /**
     * Danh sách mật khẩu phổ biến, nạp một lần lúc khởi động từ tài nguyên đóng gói kèm.
     *
     * <p>Nạp lười và giữ lại: đọc tệp ở mỗi lần đổi mật khẩu là phí, còn đọc lúc dựng bean thì
     * một tệp hỏng sẽ làm cả ứng dụng không khởi động được — trong khi hậu quả đúng của việc
     * thiếu danh sách là "kiểm yếu đi", không phải "không chạy".
     */
    private static volatile Set<String> matKhauPhoBien;

    /**
     * Kiểm độ mạnh mật khẩu theo đúng {@code NFR-system-026}.
     *
     * <p>Bản trước đòi ≥ 8 ký tự và <strong>bắt buộc trộn chữ với số</strong>. Cả ba điểm đều
     * lệch đặc tả: đặc tả đòi ≥ 12 ký tự, đòi chặn danh sách mật khẩu phổ biến, và nói rõ
     * <em>không</em> bắt buộc trộn loại ký tự — với lý do đã ghi ngay trong đặc tả: quy tắc trộn
     * ký tự chỉ đẩy người dùng tới những biến thể dễ đoán ("Matkhau@1"), còn độ dài mới là thứ
     * thật sự làm chậm một bộ dò.
     *
     * <p>Chuẩn hoá trước khi so danh sách: "P@ssw0rd" và "password" là cùng một mật khẩu trước
     * một bộ dò tự động, nên chặn cái này mà tha cái kia là chặn hình thức.
     */
    private void validateStrength(String password) {
        if (password == null || password.length() < DO_DAI_TOI_THIEU
                || laMatKhauPhoBien(password)) {
            throw new BusinessException(ErrorCode.MAT_KHAU_KHONG_DAT, "newPassword");
        }
    }

    private boolean laMatKhauPhoBien(String password) {
        Set<String> ds = matKhauPhoBien;
        if (ds == null) {
            ds = napDanhSach();
            matKhauPhoBien = ds;
        }
        String thuong = password.toLowerCase(java.util.Locale.ROOT);
        return ds.contains(thuong) || ds.contains(boBienThe(thuong));
    }

    /** Đưa các phép thay chữ-bằng-số quen thuộc về dạng gốc. */
    private static String boBienThe(String s) {
        return s.replace('@', 'a').replace('0', 'o').replace('1', 'i')
                .replace('3', 'e').replace('4', 'a').replace('5', 's')
                .replace('$', 's').replace('!', 'i');
    }

    private static Set<String> napDanhSach() {
        try (var in = AuthService.class.getResourceAsStream("/mat-khau-pho-bien.txt")) {
            if (in == null) {
                log.warn("Không tìm thấy danh sách mật khẩu phổ biến — bỏ qua phép kiểm này.");
                return Set.of();
            }
            try (var r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                return r.lines()
                        .map(String::strip)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                        .map(l -> l.toLowerCase(java.util.Locale.ROOT))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }
        } catch (java.io.IOException e) {
            log.warn("Không đọc được danh sách mật khẩu phổ biến", e);
            return Set.of();
        }
    }

    private void establishSession(AppUserDetails details, HttpServletRequest req, HttpServletResponse res) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        contextRepo.saveContext(ctx, req, res);
    }
}
