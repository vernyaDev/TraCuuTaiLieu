package vn.utc.tracuu.platform.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kết thúc tiến trình sau khi một lệnh chạy-bằng-lệnh làm xong việc.
 *
 * <p>Không có lớp này thì {@code --eval} chạy xong bảng số rồi tiến trình <strong>vẫn ngồi đó
 * phục vụ HTTP</strong>, giữ nguyên kết nối cơ sở dữ liệu và mô hình trong bộ nhớ đồ hoạ. Với
 * một lượt đo kéo dài hàng giờ trên cùng chiếc máy đang dùng để lập trình, đó không phải điều
 * bất tiện nhỏ: người chạy phải nhớ tự dừng, và quên thì mô hình chiếm chỗ bộ nhớ đồ hoạ cho
 * tới lần khởi động lại sau.
 *
 * <p>Chạy sau MỌI bộ chạy khác ({@link Order} lớn nhất) nên nó không bao giờ cắt ngang việc của
 * lệnh nào.
 */
@Component
@Order(Integer.MAX_VALUE)
public class CliExitRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CliExitRunner.class);

    /** Các cờ biến tiến trình thành công cụ dòng lệnh thay vì máy chủ. */
    private static final List<String> CLI_FLAGS =
            List.of("eval", "measure", "import-questions", "import-grades");

    private final ApplicationContext context;

    public CliExitRunner(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        String flag = CLI_FLAGS.stream().filter(args::containsOption).findFirst().orElse(null);
        if (flag == null) return;

        log.info("Lệnh --{} đã xong, kết thúc tiến trình.", flag);
        // Đóng có trật tự: trả kết nối cơ sở dữ liệu và chờ các việc nền kết thúc, thay vì
        // dừng thô. Một đợt nạp tài liệu đang dở sẽ được chạy hết chứ không bị cắt giữa.
        SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
    }
}
