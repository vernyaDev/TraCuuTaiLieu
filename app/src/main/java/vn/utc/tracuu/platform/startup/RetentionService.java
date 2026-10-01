package vn.utc.tracuu.platform.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.PermissionAuditRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Dọn dữ liệu quá thời hạn giữ.
 *
 * <p>Hai loại dữ liệu có thời hạn, và <strong>hai loại ấy có lý do trái nhau</strong>:
 * <ul>
 *   <li><em>Nhật ký kiểm toán quyền</em> giữ tối thiểu 12 tháng vì nó là thứ duy nhất trả lời
 *       được câu "ai đã mở quyền tài liệu này" khi có sự cố. Ở đây thời hạn là <strong>sàn
 *       tối thiểu</strong>, không phải trần.</li>
 *   <li><em>Nhật ký khoảng trống</em> giữ tối đa 12 tháng vì nó chứa câu hỏi của người dùng —
 *       dữ liệu nhạy cảm, và giữ vô thời hạn không có lý do chính đáng. Ở đây thời hạn là
 *       <strong>trần</strong>. Việc dọn nó nằm ở {@code GapService}.</li>
 * </ul>
 *
 * <p>Vì nhật ký kiểm toán có sàn chứ không có trần, lớp này chỉ dọn phần <em>vượt xa</em> sàn:
 * mặc định giữ gấp đôi thời hạn khai báo. Xoá đúng lúc vừa hết 12 tháng là tự tay bỏ bằng chứng
 * ở đúng tháng thứ mười ba, khi một tranh chấp thường mới nổ ra.
 */
@Component
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    /** Hệ số an toàn trên thời hạn tối thiểu của nhật ký kiểm toán. */
    private static final int AUDIT_SAFETY_FACTOR = 2;

    private final PermissionAuditRepository audits;
    private final TracuuProperties props;

    public RetentionService(PermissionAuditRepository audits, TracuuProperties props) {
        this.audits = audits;
        this.props = props;
    }

    /** Chạy lúc 3 giờ sáng, lệch giờ với việc dọn nhật ký khoảng trống để không tranh khoá. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeAudit() {
        int months = props.audit().retentionMonths() * AUDIT_SAFETY_FACTOR;
        Instant cutoff = Instant.now().minus(months * 30L, ChronoUnit.DAYS);

        int removed = audits.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("Dọn {} bản ghi kiểm toán quyền cũ hơn {} tháng (thời hạn tối thiểu {} tháng "
                   + "nhân hệ số an toàn {}).",
                    removed, months, props.audit().retentionMonths(), AUDIT_SAFETY_FACTOR);
        }
    }
}
