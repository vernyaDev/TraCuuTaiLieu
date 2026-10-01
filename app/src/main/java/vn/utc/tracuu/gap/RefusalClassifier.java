package vn.utc.tracuu.gap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.agent.retriever.Retriever;
import vn.utc.tracuu.platform.config.TracuuProperties;

/**
 * Phân loại lý do thật của một lần từ chối — <strong>nơi DUY NHẤT trong hệ thống được đọc
 * không lọc quyền</strong>.
 *
 * <p>Vì sao buộc phải đọc không lọc: muốn biết người hỏi bị chặn vì <em>thiếu quyền</em> chứ
 * không phải vì <em>kho trống</em> thì phải so kết quả có lọc với kết quả không lọc. Không có
 * phép so ấy thì tab <em>thiếu quyền</em> của báo cáo khoảng trống không có dữ liệu, và quản
 * trị mất hẳn khả năng trả lời câu hỏi quan trọng nhất của họ: <em>nên cấp quyền cho phòng
 * nào</em>.
 *
 * <p><strong>Kết quả của lớp này KHÔNG BAO GIỜ đi ra màn hình người hỏi.</strong> Nó chỉ vào
 * nhật ký khoảng trống cho quản trị. Người hỏi luôn nhận đúng một câu từ chối như nhau, bất kể
 * lý do thật là gì — nếu câu từ chối đổi theo lý do thì người hỏi chỉ cần so hai lần từ chối
 * là suy ra được tài liệu nào tồn tại ngoài quyền của mình.
 *
 * <p>Mọi lần đọc không lọc đều ghi nhật ký kiểm toán, do chính {@code PermissionResolver} làm
 * khi nhận phạm vi {@code unfiltered}. Đây là điều biến một ngoại lệ thành một đường hợp pháp
 * và đếm được.
 */
@Component
public class RefusalClassifier {

    private static final Logger log = LoggerFactory.getLogger(RefusalClassifier.class);

    private final Retriever retriever;
    private final TracuuProperties props;

    public RefusalClassifier(Retriever retriever, TracuuProperties props) {
        this.retriever = retriever;
        this.props = props;
    }

    /** Bốn lý do khoảng trống, dùng cho nhật ký và cho báo cáo — không dùng cho thông báo. */
    public static final String THIEU_TAI_LIEU = "THIEU_TAI_LIEU";
    public static final String THIEU_QUYEN = "THIEU_QUYEN";
    public static final String CAN_CU_MONG = "CAN_CU_MONG";
    public static final String NGOAI_PHAM_VI = "NGOAI_PHAM_VI";

    /**
     * Lý do thật của một lần từ chối.
     *
     * @param reportedReason lý do mà bộ điều phối đã ghi nhận
     * @param question       câu hỏi đã viết lại, tức câu thật sự đem đi truy hồi
     */
    @Transactional(readOnly = true)
    public String classify(String reportedReason, String question, long chunkSetId, long userId) {

        // Ngoài phạm vi thì không cần đọc gì thêm — và cũng không được đọc, vì đọc không lọc là
        // đặc quyền, chỉ dùng khi thật sự cần phân biệt.
        if (NGOAI_PHAM_VI.equals(reportedReason)) {
            return NGOAI_PHAM_VI;
        }

        try {
            Retriever.Outcome unfiltered = retriever.retrieve(
                    question,
                    new PermissionScope.Unfiltered(
                            "phan-loai-ly-do-khoang-trong cho nguoi dung " + userId),
                    chunkSetId,
                    props.retrieval().topK());

            if (unfiltered.isEmpty()) {
                // Không lọc quyền mà vẫn không có gì: kho thật sự thiếu tài liệu.
                return THIEU_TAI_LIEU;
            }

            double best = unfiltered.chunks().stream()
                    .mapToDouble(c -> c.scoreVector() != null ? c.scoreVector() : 0)
                    .max().orElse(0);

            if (best < props.agent().refusal().minTopScore()) {
                // Có tài liệu nhưng liên quan hời hợt — kho thiếu tài liệu ĐÚNG về chủ đề này.
                return CAN_CU_MONG;
            }

            // Có tài liệu tốt mà người hỏi không thấy: đây là vấn đề phân quyền, không phải
            // vấn đề nội dung. Đúng thứ mà quản trị cần biết để đi cấp quyền.
            log.debug("Khoảng trống do thiếu quyền: người dùng {} hỏi về chủ đề CÓ tài liệu", userId);
            return THIEU_QUYEN;

        } catch (RuntimeException e) {
            // Phân loại hỏng thì ghi lý do bộ điều phối đã báo, không chặn việc ghi nhật ký.
            log.warn("Không phân loại được lý do khoảng trống: {}", e.getMessage());
            return reportedReason == null ? CAN_CU_MONG : reportedReason;
        }
    }
}
