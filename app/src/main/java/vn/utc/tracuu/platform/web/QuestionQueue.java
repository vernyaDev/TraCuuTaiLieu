package vn.utc.tracuu.platform.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

/**
 * Hàng đợi câu hỏi — {@code NFR-system-008}.
 *
 * <p>Quá năm câu đang xử lý thì câu thứ sáu <strong>vào hàng đợi và người hỏi thấy vị trí của
 * mình</strong>, chứ không phải chờ trắng màn hình.
 *
 * <p><strong>Vì sao xếp hàng thay vì cứ nhận hết.</strong> Mô hình sinh chữ dùng chung một GPU;
 * các câu hỏi đồng thời chia nhau băng thông bộ nhớ đồ hoạ chứ không chạy song song thật. Quá
 * ngưỡng thì thời gian của <em>mọi người</em> cùng xấu đi, thay vì người đến sau phải chờ. Một
 * hàng đợi có thông báo trung thực hơn là để tất cả cùng chậm mà không ai biết vì sao.
 *
 * <p>Vị trí trong hàng chỉ là <strong>ước lượng</strong>: nó đếm số người đang chờ tại thời
 * điểm hỏi. Người phía trước có thể ngắt kết nối, nên vị trí thật có thể tốt hơn. Nói "khoảng"
 * ở giao diện là trung thực hơn nói một con số chính xác giả.
 */
@Component
public class QuestionQueue {

    private static final Logger log = LoggerFactory.getLogger(QuestionQueue.class);

    private final Semaphore slots;
    private final ConcurrentLinkedQueue<Long> waiting = new ConcurrentLinkedQueue<>();
    private final AtomicLong ticketSeq = new AtomicLong();
    private final TracuuProperties props;

    public QuestionQueue(TracuuProperties props) {
        this.props = props;
        // Công bằng: ai vào trước được phục vụ trước. Không công bằng thì một người có thể chờ
        // rất lâu trong khi người khác liên tục chen vào, và đó là loại chậm không giải thích nổi.
        this.slots = new Semaphore(props.qa().maxConcurrentQuestions(), true);
    }

    /** Không còn chỗ chờ. Khác hẳn "đang chờ" — ca này phải từ chối nhận. */
    public static class QueueFullException extends RuntimeException {
        public QueueFullException() {
            super("Hàng đợi câu hỏi đã đầy");
        }
    }

    /**
     * Xin một chỗ xử lý.
     *
     * @param onQueued gọi MỘT LẦN kèm vị trí, chỉ khi phải chờ thật. Không phải chờ thì không
     *                 gọi — bắn một sự kiện "bạn ở vị trí 1" cho người không hề chờ chỉ làm
     *                 giao diện nhấp nháy vô nghĩa.
     * @return thẻ để trả chỗ lại. Luôn trả trong khối {@code finally}.
     */
    public Ticket acquire(IntConsumer onQueued) throws InterruptedException {
        long id = ticketSeq.incrementAndGet();

        if (slots.tryAcquire()) {
            return new Ticket(id, false);
        }

        if (waiting.size() >= props.qa().queueCapacity()) {
            throw new QueueFullException();
        }

        waiting.add(id);
        int position = waiting.size();
        onQueued.accept(position);
        log.debug("Câu hỏi {} vào hàng đợi, vị trí {}", id, position);

        try {
            boolean got = slots.tryAcquire(props.qa().queueTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!got) {
                throw new QueueTimeoutException();
            }
            return new Ticket(id, true);
        } finally {
            waiting.remove(id);
        }
    }

    /** Chờ quá lâu. Trả lời trung thực còn hơn để kết nối treo vô hạn. */
    public static class QueueTimeoutException extends RuntimeException {
        public QueueTimeoutException() {
            super("Chờ quá lâu trong hàng đợi");
        }
    }

    public void release(Ticket ticket) {
        if (ticket != null && !ticket.released) {
            ticket.released = true;
            slots.release();
        }
    }

    /** Số câu đang xử lý và số câu đang chờ — cho màn quản trị và cho phép đo. */
    public Snapshot snapshot() {
        return new Snapshot(
                props.qa().maxConcurrentQuestions() - slots.availablePermits(),
                props.qa().maxConcurrentQuestions(),
                waiting.size());
    }

    public record Snapshot(int running, int capacity, int waiting) {}

    public static final class Ticket {
        final long id;
        final boolean wasQueued;
        boolean released;

        Ticket(long id, boolean wasQueued) {
            this.id = id;
            this.wasQueued = wasQueued;
        }

        public boolean wasQueued() { return wasQueued; }
    }
}
