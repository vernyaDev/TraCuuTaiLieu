package vn.utc.tracuu.platform.web;

import org.springframework.http.HttpStatus;

/**
 * Danh mục mã lỗi máy đọc được, kèm câu nghiệp vụ cho người đọc.
 *
 * <p>Câu cho người đọc trả lời đủ ba vế: <strong>chuyện gì xảy ra · vì sao · giờ làm gì</strong>.
 * Đây là yêu cầu của hợp đồng giao diện Mục 1 nguyên tắc hai, không phải gợi ý về văn phong —
 * một câu lỗi thiếu vế thứ ba để người dùng đứng im giữa màn hình.
 */
public enum ErrorCode {

    // ── Xác thực ──────────────────────────────────────────────────────────────────────────
    /** Cố ý KHÔNG phân biệt sai tên với sai mật khẩu — phân biệt là xác nhận tài khoản có thật. */
    SAI_THONG_TIN_DANG_NHAP(HttpStatus.UNAUTHORIZED,
            "Tên đăng nhập hoặc mật khẩu không đúng. Kiểm tra lại, hoặc liên hệ quản trị nếu bạn quên mật khẩu."),
    TAI_KHOAN_BI_VO_HIEU(HttpStatus.UNAUTHORIZED,
            "Tài khoản đã bị vô hiệu hoá nên không đăng nhập được. Liên hệ quản trị hệ thống để mở lại."),
    TAI_KHOAN_BI_KHOA_TAM(HttpStatus.UNAUTHORIZED,
            "Tài khoản tạm khoá do đăng nhập sai nhiều lần. Chờ hết thời gian khoá rồi thử lại, hoặc nhờ quản trị đặt lại mật khẩu."),
    CHUA_DANG_NHAP(HttpStatus.UNAUTHORIZED,
            "Phiên làm việc đã kết thúc. Đăng nhập lại để tiếp tục."),
    PHAI_DOI_MAT_KHAU(HttpStatus.FORBIDDEN,
            "Tài khoản đang dùng mật khẩu tạm. Đổi mật khẩu trước khi dùng các chức năng khác."),
    MAT_KHAU_CU_KHONG_DUNG(HttpStatus.BAD_REQUEST,
            "Mật khẩu hiện tại không đúng nên chưa đổi được. Nhập lại mật khẩu đang dùng."),
    MAT_KHAU_KHONG_DAT(HttpStatus.BAD_REQUEST,
            "Mật khẩu mới chưa đạt yêu cầu tối thiểu. Đặt mật khẩu dài ít nhất 8 ký tự, có cả chữ và số."),

    // ── Quyền ─────────────────────────────────────────────────────────────────────────────
    /** Quyền trên CHỨC NĂNG. Ở đây lộ sự tồn tại của chức năng là vô hại. */
    KHONG_DU_QUYEN(HttpStatus.FORBIDDEN,
            "Bạn không có quyền dùng chức năng này vì nó dành cho quản trị tri thức. Liên hệ quản trị nếu bạn cần quyền đó."),
    /** Dữ liệu không tồn tại HOẶC người gọi không được đọc — CÙNG một mã, cố ý. */
    KHONG_TIM_THAY(HttpStatus.NOT_FOUND,
            "Không tìm thấy nội dung này. Có thể nó đã bị gỡ, hoặc bạn chưa được cấp quyền xem. Liên hệ quản trị tri thức để được hỗ trợ."),

    // ── Nạp tài liệu ──────────────────────────────────────────────────────────────────────
    TEP_QUA_LON(HttpStatus.PAYLOAD_TOO_LARGE,
            "Tệp vượt quá dung lượng cho phép nên không tải lên được. Tách nhỏ tài liệu rồi tải lại."),
    DINH_DANG_KHONG_HO_TRO(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "Định dạng tệp này chưa được hỗ trợ. Chuyển sang một trong các định dạng được nhận rồi tải lại."),
    TEP_HONG(HttpStatus.UNPROCESSABLE_ENTITY,
            "Không đọc được nội dung tệp vì tệp hỏng hoặc đã đặt mật khẩu. Mở thử bằng ứng dụng soạn thảo để kiểm tra, rồi tải lại bản đọc được."),
    TEP_KHONG_CO_VAN_BAN(HttpStatus.UNPROCESSABLE_ENTITY,
            "Tệp không chứa lớp văn bản nào, thường gặp ở bản chụp quét. Hệ thống chưa nhận dạng được chữ trong ảnh, hãy dùng bản có lớp văn bản."),
    /**
     * Mã riêng cho việc lệch mô hình sinh biểu diễn.
     *
     * <p>Không dùng lại một mã chung chung: câu chữ phải trỏ đúng nguyên nhân. Trước khi có mã
     * này, lệch mô hình báo ra câu "Thao tác không thực hiện được ở trạng thái hiện tại của tài
     * liệu. Làm mới danh sách" — người vận hành sẽ đi làm mới danh sách và thấy không có gì đổi,
     * trong khi việc cần làm nằm ở cấu hình mô hình. Một thông báo lạc đề còn tệ hơn không có
     * thông báo, vì nó dẫn người ta đi sai hướng một cách tự tin.
     */
    MO_HINH_BIEU_DIEN_LECH(HttpStatus.CONFLICT,
            "Kho đang dùng một mô hình sinh biểu diễn khác với mô hình đã nạp dữ liệu. Nạp tiếp sẽ làm kết quả tìm kiếm sai trên một phần kho. Chạy lại đúng mô hình cũ, hoặc nạp lại toàn bộ kho bằng một tập đoạn mới."),

    // ── Vòng đời tài liệu ─────────────────────────────────────────────────────────────────
    VONG_THAY_THE(HttpStatus.CONFLICT,
            "Khai báo này tạo thành vòng lặp thay thế nên bị từ chối. Kiểm tra lại chuỗi thay thế của hai tài liệu."),
    TRANG_THAI_KHONG_HOP_LE(HttpStatus.CONFLICT,
            "Thao tác không thực hiện được ở trạng thái hiện tại của tài liệu. Làm mới danh sách để xem trạng thái mới nhất."),

    // ── Hỏi đáp ───────────────────────────────────────────────────────────────────────────
    HE_THONG_BAN(HttpStatus.SERVICE_UNAVAILABLE,
            "Hệ thống đang xử lý quá nhiều câu hỏi nên chưa nhận thêm được. Chờ một lát rồi gửi lại câu hỏi."),
    QUA_THOI_GIAN_CHO(HttpStatus.GATEWAY_TIMEOUT,
            "Câu hỏi xử lý quá lâu nên đã dừng. Thử rút gọn câu hỏi hoặc chia thành các câu nhỏ hơn."),
    MO_HINH_KHONG_SAN_SANG(HttpStatus.SERVICE_UNAVAILABLE,
            "Dịch vụ sinh câu trả lời hiện chưa sẵn sàng. Báo quản trị hệ thống kiểm tra máy chủ mô hình."),

    // ── Chung ─────────────────────────────────────────────────────────────────────────────
    DU_LIEU_KHONG_HOP_LE(HttpStatus.BAD_REQUEST,
            "Dữ liệu gửi lên chưa hợp lệ. Sửa các ô được đánh dấu rồi gửi lại."),
    TRUNG_DU_LIEU(HttpStatus.CONFLICT,
            "Giá trị này đã tồn tại trong hệ thống. Chọn một giá trị khác."),
    LOI_HE_THONG(HttpStatus.INTERNAL_SERVER_ERROR,
            "Hệ thống gặp sự cố khi xử lý yêu cầu. Thử lại sau ít phút; nếu vẫn lỗi, báo quản trị kèm thời điểm bạn gặp.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() { return status; }
    public String message() { return message; }
}
