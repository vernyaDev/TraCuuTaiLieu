package vn.utc.tracuu.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    /**
     * Bể luồng riêng cho khâu nạp tài liệu.
     *
     * <p>Kích thước để ở cấu hình vì {@code NFR-system-047} gọi đích danh nó, nhưng lý do sâu
     * hơn nằm ở {@code NFR-system-011}: sinh biểu diễn ngữ nghĩa chạy trên CPU, nên mỗi luồng
     * nạp thêm là một phần CPU bị lấy khỏi việc trả lời câu hỏi. Đặt số này bằng số nhân là
     * cách chắc chắn làm hỏng ngưỡng <em>"nạp tài liệu không làm thời gian trả lời quá 1,5
     * lần"</em>.
     *
     * <p>Hàng đợi đầy thì <strong>người gọi tự chạy việc đó</strong> thay vì ném lỗi: một đợt
     * nạp lớn sẽ chậm lại chứ không mất tệp. Đây là khác biệt giữa "chậm" và "im lặng đánh rơi".
     */
    @Bean("ingestionExecutor")
    public ThreadPoolTaskExecutor ingestionExecutor(TracuuProperties props) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.ingestion().threadPoolSize());
        executor.setMaxPoolSize(props.ingestion().threadPoolSize());
        executor.setQueueCapacity(props.ingestion().queueCapacity());
        executor.setThreadNamePrefix("nap-tai-lieu-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Luồng chạy các lượt hỏi đáp phía sau kết nối sự kiện một chiều.
     *
     * <p>Bể này <strong>rộng hơn</strong> số câu xử lý cùng lúc, và đó là chủ đích: phần lớn
     * thời gian một lượt là ngồi chờ trong hàng đợi, không phải tính toán. Thắt cổ chai thật
     * nằm ở {@code QuestionQueue}; bể luồng chỉ cần đủ rộng để người chờ vẫn giữ được kết nối
     * và vẫn nhận được vị trí của mình.
     */
    @Bean("qaExecutor")
    public ThreadPoolTaskExecutor qaExecutor(TracuuProperties props) {
        int capacity = props.qa().maxConcurrentQuestions() + props.qa().queueCapacity();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.qa().maxConcurrentQuestions());
        executor.setMaxPoolSize(Math.max(capacity, props.qa().maxConcurrentQuestions() * 2));
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("hoi-dap-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Bể luồng riêng cho việc phân loại khoảng trống tri thức.
     *
     * <p>Việc này chạy một lượt truy hồi <strong>không lọc quyền trên toàn kho</strong> để trả
     * lời câu "vì sao lượt này bị từ chối" cho quản trị. Nó đắt, và nó không phải việc người
     * hỏi đang chờ — nên nó không được dùng chung bể với việc phục vụ câu hỏi, nếu không nó
     * cạnh tranh với đúng thứ nó phục vụ.
     *
     * <p>Bể nhỏ và hàng đợi có trần: bản ghi khoảng trống là tín hiệu để cải thiện kho, không
     * phải dữ liệu bắt buộc đúng. Lúc hệ thống bận thì bỏ một bản ghi tốt hơn là làm chậm một
     * câu trả lời.
     */
    @Bean("khoangTrongExecutor")
    public ThreadPoolTaskExecutor khoangTrongExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("khoang-trong-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}
