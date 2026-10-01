package vn.utc.tracuu.platform.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sổ đăng ký các lượt hỏi đang chạy, để dừng được chúng từ một yêu cầu khác.
 *
 * <p>Cần một sổ ở giữa vì hai đường đi khác nhau: lượt hỏi sống trong một luồng đang giữ kết
 * nối sự kiện, còn lệnh dừng đến qua một yêu cầu HTTP hoàn toàn khác. Không có chỗ gặp nhau
 * thì nút <em>Dừng</em> chỉ đóng được kết nối ở phía trình duyệt, còn máy chủ vẫn sinh chữ cho
 * hết — và trên một mô hình dùng chung bộ nhớ đồ hoạ, một lượt bị bỏ mà vẫn chạy là lấy mất
 * lượt của người đang xếp hàng.
 *
 * <p>Sổ này giữ thêm <strong>mã người sở hữu</strong>. Thiếu nó thì bất kỳ ai biết mã lượt đều
 * dừng được lượt của người khác — một lỗ nhỏ nhưng rất dễ mở, vì mã lượt là số tăng dần và
 * chính người dùng thấy nó trong sự kiện kết thúc.
 */
@Component
public class RunningTurns {

    private static final Logger log = LoggerFactory.getLogger(RunningTurns.class);

    private record Entry(long ownerUserId, AtomicBoolean cancelled) {}

    private final Map<Long, Entry> running = new ConcurrentHashMap<>();

    /** Ghi một lượt vào sổ. Cờ truyền vào chính là cờ mà bộ điều phối đang hỏi. */
    public void register(long turnId, long ownerUserId, AtomicBoolean cancelled) {
        running.put(turnId, new Entry(ownerUserId, cancelled));
    }

    public void unregister(long turnId) {
        running.remove(turnId);
    }

    /**
     * Dừng một lượt.
     *
     * <p>Lượt của người khác, hoặc lượt đã kết thúc, đều trả về <em>không tìm thấy</em> — cùng
     * một lối ra, cố ý. Phân biệt hai ca ấy là nói cho người gọi biết lượt đó có tồn tại.
     */
    public void stop(long turnId, long requesterUserId) {
        Entry entry = running.get(turnId);
        if (entry == null || entry.ownerUserId() != requesterUserId) {
            throw BusinessException.khongTimThay();
        }
        entry.cancelled().set(true);
        log.debug("Người dùng {} dừng lượt {}", requesterUserId, turnId);
    }

    /** Số lượt đang chạy — cho màn theo dõi và cho phép đo. */
    public int count() {
        return running.size();
    }
}
