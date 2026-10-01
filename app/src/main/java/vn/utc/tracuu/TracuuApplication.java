package vn.utc.tracuu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import vn.utc.tracuu.platform.config.TracuuProperties;

/**
 * Hệ thống tra cứu tri thức nội bộ.
 *
 * <p>Một tiến trình phục vụ cả giao diện lẫn nghiệp vụ: Vite dựng tệp tĩnh lúc dựng ảnh,
 * lớp này phục vụ chúng. Ba tiến trình còn lại — cơ sở dữ liệu, mô hình ngôn ngữ, bộ nhớ đệm —
 * nằm ngoài, theo sơ đồ ở {@code docs/_shared/architecture.md} Mục 1.
 */
@SpringBootApplication
@EnableConfigurationProperties(TracuuProperties.class)
@EnableAsync
@EnableScheduling
public class TracuuApplication {

    public static void main(String[] args) {
        SpringApplication.run(TracuuApplication.class, args);
    }
}
