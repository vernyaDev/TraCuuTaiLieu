package vn.utc.tracuu.platform.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.util.Optional;

/** Lấy danh tính của phiên hiện tại. Đường duy nhất để mã nghiệp vụ biết "ai đang hỏi". */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<AppUserDetails> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            return Optional.empty();
        }
        return Optional.of(details);
    }

    public static AppUserDetails require() {
        return find().orElseThrow(() -> new BusinessException(ErrorCode.CHUA_DANG_NHAP));
    }

    public static long requireId() {
        return require().userId();
    }

    /** Phạm vi quyền của người đang đăng nhập — tham số bắt buộc của mọi truy vấn chạm đoạn. */
    public static PermissionScope requireScope() {
        return require().scope();
    }

    public static void requireAdmin() {
        if (!require().role().isAdmin()) {
            throw new BusinessException(ErrorCode.KHONG_DU_QUYEN);
        }
    }
}
