package vn.utc.tracuu.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Nơi DUY NHẤT lỗi biến thành phản hồi.
 *
 * <p>Hai điều lớp này giữ, và chúng là điều dễ vi phạm nhất mà không ai nhận ra:
 * <ul>
 *   <li><strong>Không dấu vết kỹ thuật nào lọt ra ngoài</strong> (NFR-system-031). Vết gọi hàm,
 *       câu truy vấn, đường dẫn tệp máy chủ đều dừng lại ở nhật ký. Ra ngoài chỉ có mã lỗi và
 *       câu nghiệp vụ đã soạn sẵn.</li>
 *   <li><strong>Mã trạng thái không tiết lộ sự tồn tại.</strong> Tài liệu không có và tài liệu
 *       ngoài quyền đều trả <em>không tìm thấy</em>. Trả <em>không đủ quyền</em> cho vế sau là
 *       xác nhận tài liệu đó có thật.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        // Chi tiết nội bộ vào nhật ký; ra ngoài chỉ có câu nghiệp vụ.
        log.info("Lỗi nghiệp vụ {} tại {} {}: {}",
                ex.code(), req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.code().status())
                .body(ApiError.of(ex.code(), ex.field()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(ErrorCode.DU_LIEU_KHONG_HOP_LE.status())
                .body(ApiError.withFields(ErrorCode.DU_LIEU_KHONG_HOP_LE, fields));
    }

    /**
     * Thân yêu cầu không đọc được: JSON sai cú pháp, hoặc sai mã hoá ký tự.
     *
     * <p>Đây là lỗi của <strong>bên gọi</strong>, không phải sự cố máy chủ. Không bắt riêng thì
     * nó rơi vào lưới cuối và trả về "hệ thống gặp sự cố" — nói với người dùng rằng máy chủ hỏng
     * trong khi thực ra yêu cầu của họ sai. Với tiếng Việt thì đây không phải ca hiếm: một máy
     * khách gửi thân yêu cầu ở bảng mã khác UTF-8 sẽ rơi thẳng vào đây.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.info("Thân yêu cầu không đọc được: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(ErrorCode.DU_LIEU_KHONG_HOP_LE.status())
                .body(ApiError.of(ErrorCode.DU_LIEU_KHONG_HOP_LE));
    }

    /**
     * Yêu cầu <strong>sai hình dạng</strong>: thiếu tham số bắt buộc, tham số sai kiểu, thiếu
     * phần tệp trong biểu mẫu nhiều phần.
     *
     * <p>Cùng một lý do với {@link #handleUnreadableBody}, nhưng ba ngoại lệ này là lớp riêng
     * nên không bắt cùng chỗ được. Không bắt thì cả ba rơi vào lưới cuối và sinh ra hai hậu quả
     * cùng lúc: người gọi nhận mã 500 kèm câu "hệ thống gặp sự cố" cho lỗi của chính họ, và
     * nhật ký có thêm một vết ngoại lệ đầy đủ ở mức ERROR — tức là mỗi đường dẫn gõ sai đều để
     * lại một dòng đỏ, và lỗi thật thì chìm giữa chúng.
     *
     * <p>Đo ngày 30/09: {@code ?page=abc}, {@code /documents/khong-phai-so}, và thiếu tham số
     * {@code sensitivity} đều trả 500.
     */
    @ExceptionHandler({
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> handleBadRequestShape(Exception ex, HttpServletRequest req) {
        log.info("Yêu cầu sai hình dạng tại {} {}: {}",
                req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.DU_LIEU_KHONG_HOP_LE.status())
                .body(ApiError.of(ErrorCode.DU_LIEU_KHONG_HOP_LE));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(ErrorCode.TEP_QUA_LON.status())
                .body(ApiError.of(ErrorCode.TEP_QUA_LON));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        // Quyền trên CHỨC NĂNG: ở đây lộ sự tồn tại của chức năng là vô hại, nên trả đúng nghĩa.
        log.info("Chặn truy cập chức năng: {} {}", req.getMethod(), req.getRequestURI());
        return ResponseEntity.status(ErrorCode.KHONG_DU_QUYEN.status())
                .body(ApiError.of(ErrorCode.KHONG_DU_QUYEN));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc dữ liệu", ex);
        return ResponseEntity.status(ErrorCode.TRUNG_DU_LIEU.status())
                .body(ApiError.of(ErrorCode.TRUNG_DU_LIEU));
    }

    /**
     * Đường dẫn không tồn tại.
     *
     * <p>Hai ngoại lệ vì Spring ném loại khác nhau tuỳ đường đi tới: một cho bộ điều khiển
     * không khớp, một cho tài nguyên tĩnh không thấy. Thiếu loại thứ hai thì mọi địa chỉ gõ sai
     * rơi vào lưới cuối và trả về <em>lỗi hệ thống</em> — nói với người dùng rằng máy chủ hỏng
     * trong khi thực ra họ chỉ gõ sai đường.
     */
    @ExceptionHandler({NoHandlerFoundException.class,
                       org.springframework.web.servlet.resource.NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex) {
        return ResponseEntity.status(ErrorCode.KHONG_TIM_THAY.status())
                .body(ApiError.of(ErrorCode.KHONG_TIM_THAY));
    }

    /**
     * Lưới cuối. Mọi thứ chưa được phân loại rơi vào đây, và ra ngoài dưới CÙNG một câu —
     * kể cả khi bên trong là lỗi kết nối cơ sở dữ liệu hay lỗi lập trình.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req,
                                                     HttpServletResponse res) {
        // Người hỏi đóng tab giữa lúc chữ đang chảy về. Đây KHÔNG phải lỗi hệ thống: không có
        // gì hỏng và không ai cần được báo. Ghi nó ở mức ERROR làm nhật ký đầy những dòng đỏ
        // vô hại, và lỗi thật thì chìm giữa chúng.
        if (laNguoiGoiNgatKetNoi(ex)) {
            log.debug("Người gọi ngắt kết nối tại {} {}", req.getMethod(), req.getRequestURI());
            return null;
        }

        log.error("Lỗi chưa phân loại tại {} {}", req.getMethod(), req.getRequestURI(), ex);

        // Luồng sự kiện (SSE) không nhận được thân JSON: bộ chuyển đổi thấy kiểu nội dung đã
        // đặt sẵn là text/event-stream rồi ném tiếp một ngoại lệ THỨ HAI, che mất ngoại lệ
        // gốc. Với luồng đã bắt đầu chảy thì mã trạng thái cũng đã gửi đi rồi — không còn gì
        // để sửa ở tầng này, nên chỉ ghi nhận và dừng.
        //
        // Lượt hỏi đáp tự lo phần báo lỗi CỦA NÓ bằng một sự kiện `error` trong luồng
        // (AgentPipeline), nên người dùng vẫn nhận được thông báo — chỉ là không qua đường này.
        if (res.isCommitted() || MediaType.TEXT_EVENT_STREAM_VALUE.equals(res.getContentType())) {
            return null;
        }

        return ResponseEntity.status(ErrorCode.LOI_HE_THONG.status())
                .body(ApiError.of(ErrorCode.LOI_HE_THONG));
    }

    /**
     * Ngắt kết nối từ phía người gọi, nhận diện qua chuỗi thông báo.
     *
     * <p>Không nhận diện được bằng kiểu ngoại lệ: Tomcat gói nó thành {@code ClientAbortException}
     * ở chỗ này và {@code IOException} trần ở chỗ kia, tuỳ lúc đứt là trong hay ngoài phần
     * bất đồng bộ. Bắt theo kiểu sẽ sót một nửa số ca.
     */
    private boolean laNguoiGoiNgatKetNoi(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String ten = t.getClass().getSimpleName();
            if (ten.equals("ClientAbortException") || ten.equals("AsyncRequestNotUsableException")) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && (m.contains("Broken pipe")
                    || m.contains("Connection reset by peer")
                    || m.contains("An established connection was aborted"))) {
                return true;
            }
            if (t.getCause() == t) break;
        }
        return false;
    }
}
