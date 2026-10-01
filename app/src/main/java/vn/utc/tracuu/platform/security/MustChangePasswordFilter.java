package vn.utc.tracuu.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.utc.tracuu.platform.web.ApiError;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.IOException;
import java.util.Set;

/**
 * Chặn mọi chức năng khác khi tài khoản còn đang dùng mật khẩu tạm.
 *
 * <p>Hợp đồng giao diện Mục 2 nói rõ: đăng nhập thành công mà cờ đổi mật khẩu còn bật thì
 * <strong>chưa</strong> cho vào màn hỏi đáp. Đặt luật này ở bộ lọc thay vì ở từng bộ điều khiển
 * là có chủ đích — một chỗ quên kiểm là một lối đi vòng, và số bộ điều khiển thì còn tăng.
 */
@Component
@Order(60)
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> DUOC_PHEP = Set.of(
            "/api/auth/me", "/api/auth/change-password", "/api/auth/logout", "/api/auth/login");

    private final ObjectMapper json;

    public MustChangePasswordFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        boolean chanDuoc = path.startsWith("/api/") && !DUOC_PHEP.contains(path);

        if (chanDuoc && CurrentUser.find().map(AppUserDetails::mustChangePassword).orElse(false)) {
            res.setStatus(ErrorCode.PHAI_DOI_MAT_KHAU.status().value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            json.writeValue(res.getWriter(), ApiError.of(ErrorCode.PHAI_DOI_MAT_KHAU));
            return;
        }
        chain.doFilter(req, res);
    }
}
