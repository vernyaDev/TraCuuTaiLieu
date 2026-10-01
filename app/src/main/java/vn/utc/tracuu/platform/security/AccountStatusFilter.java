package vn.utc.tracuu.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.utc.tracuu.platform.web.ApiError;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.io.IOException;
import java.util.Set;

/**
 * Cắt phiên đang mở của tài khoản đã bị <strong>vô hiệu hoá</strong> hoặc đã bị <strong>đổi
 * vai</strong>.
 *
 * <p><strong>Lỗi mà lớp này sửa, đo thật ngày 30/09.</strong> Quản trị bấm "vô hiệu hoá" một tài
 * khoản; cột {@code is_active} trong cơ sở dữ liệu đổi thành {@code false}; nhưng phiên đang mở
 * của người đó <em>vẫn dùng được</em> — cả {@code /api/auth/me} lẫn {@code /api/qa/ask} đều trả
 * mã 200. Lý do: danh tính người dùng ({@link AppUserDetails}) được chụp <em>một lần lúc đăng
 * nhập</em> rồi nằm trong phiên; không có gì đọc lại trạng thái tài khoản ở các yêu cầu sau. Với
 * thời hạn phiên 8 giờ, một người vừa nghỉ việc vẫn đọc được tài liệu nội bộ suốt phần còn lại
 * của ngày làm việc — trong khi màn quản trị đã hiện "đã vô hiệu hoá".
 *
 * <p>Việc đổi vai có cùng gốc bệnh: hạ một người từ quản trị xuống nhân viên không lấy lại được
 * quyền quản trị của phiên đang mở, vì danh sách quyền cũng nằm trong bản chụp đó.
 *
 * <p><strong>Vì sao đọc thẳng cơ sở dữ liệu mỗi yêu cầu, không đệm.</strong> Đây là truy vấn
 * theo khoá chính, dưới một phần nghìn giây, và nó chỉ chạy cho các đường {@code /api/}. Đệm lại
 * sẽ tạo một cửa sổ mà người đã bị vô hiệu vẫn vào được — đúng cái lỗi đang sửa, chỉ ngắn hơn.
 * Một cơ chế thu hồi mà vẫn để lọt trong mười lăm giây thì không phải cơ chế thu hồi.
 *
 * <p>Đặt ở bộ lọc chứ không ở từng bộ điều khiển, cùng lý do với {@link MustChangePasswordFilter}:
 * một chỗ quên kiểm là một lối đi vòng, và số bộ điều khiển thì còn tăng.
 */
@Component
@Order(55)
public class AccountStatusFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusFilter.class);

    /** Đăng xuất phải đi qua được, nếu không người bị vô hiệu không dọn nổi phiên của chính họ. */
    private static final Set<String> DUOC_PHEP = Set.of("/api/auth/logout", "/api/auth/login");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public AccountStatusFilter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        if (!path.startsWith("/api/") || DUOC_PHEP.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        var dangNhap = CurrentUser.find();
        if (dangNhap.isEmpty()) {
            chain.doFilter(req, res);
            return;
        }
        AppUserDetails phien = dangNhap.get();

        TrangThai hienTai;
        try {
            hienTai = jdbc.query(
                    "SELECT is_active, role FROM users WHERE id = ?",
                    rs -> rs.next() ? new TrangThai(rs.getBoolean(1), rs.getString(2)) : null,
                    phien.userId());
        } catch (RuntimeException e) {
            // Không đọc được trạng thái thì KHÔNG cho qua. Đây là lớp thu hồi quyền; hỏng mà vẫn
            // cho đi tiếp là biến một sự cố cơ sở dữ liệu thành một lỗ hổng.
            log.error("Không đọc được trạng thái tài khoản {}", phien.userId(), e);
            tuChoi(res, ErrorCode.LOI_HE_THONG, req);
            return;
        }

        boolean conHieuLuc = hienTai != null
                && hienTai.active()
                && hienTai.role().equals(phien.role().name());

        if (!conHieuLuc) {
            log.info("Cắt phiên của tài khoản {}: trạng thái đã đổi kể từ lúc đăng nhập.",
                    phien.username());
            huyPhien(req);
            tuChoi(res, hienTai != null && !hienTai.active()
                    ? ErrorCode.TAI_KHOAN_BI_VO_HIEU : ErrorCode.CHUA_DANG_NHAP, req);
            return;
        }

        chain.doFilter(req, res);
    }

    private record TrangThai(boolean active, String role) {}

    private void huyPhien(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) s.invalidate();
        SecurityContextHolder.clearContext();
    }

    private void tuChoi(HttpServletResponse res, ErrorCode ma, HttpServletRequest req) throws IOException {
        if (res.isCommitted()) return;
        res.setStatus(ma.status().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(), ApiError.of(ma));
    }
}
