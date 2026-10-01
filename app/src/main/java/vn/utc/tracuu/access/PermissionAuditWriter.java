package vn.utc.tracuu.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhật ký kiểm toán trong một giao dịch RIÊNG.
 *
 * <p><strong>Vì sao phải tách giao dịch — phát hiện từ kiểm thử tích hợp 29/09.</strong> Truy
 * vấn truy hồi chạy trong giao dịch <em>chỉ-đọc</em>, nhưng đường đọc-không-lọc-quyền lại phải
 * ghi một dòng kiểm toán. Ghi trong giao dịch chỉ-đọc làm PostgreSQL bỏ dở cả giao dịch, và
 * mọi câu lệnh sau đó trong cùng giao dịch đều đổ với trạng thái {@code 25P02}. Hệ quả kép, và
 * cả hai đều tệ:
 *
 * <ul>
 *   <li>Dòng kiểm toán <strong>không bao giờ được ghi</strong> — tức đường đặc quyền mà luật R1
 *       cho phép trở thành đường không ai theo dõi, đúng điều luật ấy sinh ra để chặn.</li>
 *   <li>Bản thân việc truy hồi không lọc <strong>đổ hoàn toàn</strong>, nên khâu phân loại lý do
 *       khoảng trống luôn rơi về nhánh dự phòng. Báo cáo vẫn hiện ra, chỉ là mọi lý do
 *       <em>thiếu quyền</em> bị xếp nhầm — sai âm thầm, không lỗi nào báo.</li>
 * </ul>
 *
 * <p>Giao dịch riêng còn giải một việc nữa, đúng về nguyên tắc: <strong>bản ghi kiểm toán phải
 * sống sót ngay cả khi nghiệp vụ xung quanh bị hoàn tác.</strong> Một lần đọc đã xảy ra thì nó
 * đã xảy ra, bất kể giao dịch gọi nó có thành công hay không.
 */
@Component
public class PermissionAuditWriter {

    private static final Logger log = LoggerFactory.getLogger(PermissionAuditWriter.class);
    private static final Logger audit = LoggerFactory.getLogger("KIEM_TOAN_QUYEN");

    private final PermissionAuditRepository repository;

    public PermissionAuditWriter(PermissionAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Ghi một lần đọc không lọc quyền.
     *
     * <p>{@link Propagation#REQUIRES_NEW} là phần cốt yếu: nó mở giao dịch mới thay vì tham gia
     * giao dịch chỉ-đọc đang chạy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordUnfilteredRead(String reason) {
        audit.warn("Đọc KHÔNG lọc quyền — lý do: {}", reason);
        try {
            repository.save(PermissionAudit.of(
                    null, "DOC_KHONG_LOC_QUYEN", "HE_THONG", null, null,
                    "{\"lyDo\":\"" + reason.replace("\"", "'") + "\"}"));
        } catch (RuntimeException e) {
            // Không được để việc ghi nhật ký làm hỏng nghiệp vụ, nhưng cũng KHÔNG được im lặng:
            // mất dòng kiểm toán là mất bằng chứng về một lần đọc đặc quyền.
            log.error("KHÔNG ghi được nhật ký kiểm toán cho lần đọc không lọc quyền: {}",
                    e.getMessage(), e);
        }
    }
}
