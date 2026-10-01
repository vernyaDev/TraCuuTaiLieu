package vn.utc.tracuu.platform.web;

/**
 * Lỗi nghiệp vụ có mã. Thông điệp gửi ra ngoài lấy từ {@link ErrorCode} chứ không lấy từ
 * {@code getMessage()} — để không một dấu vết kỹ thuật nào lọt ra ngoài (NFR-system-031).
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final String field;

    public BusinessException(ErrorCode code) {
        this(code, null, null);
    }

    public BusinessException(ErrorCode code, String field) {
        this(code, field, null);
    }

    public BusinessException(ErrorCode code, String field, String internalDetail) {
        super(internalDetail == null ? code.name() : code.name() + ": " + internalDetail);
        this.code = code;
        this.field = field;
    }

    public ErrorCode code() { return code; }
    public String field() { return field; }

    /** Dữ liệu không tồn tại HOẶC ngoài quyền — dùng CHUNG một lối ra, cố ý. */
    public static BusinessException khongTimThay() {
        return new BusinessException(ErrorCode.KHONG_TIM_THAY);
    }
}
