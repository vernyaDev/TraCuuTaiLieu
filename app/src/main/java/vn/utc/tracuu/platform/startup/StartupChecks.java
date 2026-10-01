package vn.utc.tracuu.platform.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sáu phép kiểm điều kiện khởi động, chạy <strong>theo thứ tự</strong> trước khi nhận yêu cầu đầu tiên.
 *
 * <p>Thất bại ở bất kỳ phép nào thì <strong>dừng hẳn</strong> kèm thông báo nêu rõ thiếu gì và làm
 * gì tiếp — không khởi động ở chế độ suy giảm. Lý do: hai trong sáu phép bắt đúng loại lỗi mà nếu
 * không kiểm thì hệ thống <em>vẫn chạy</em>, chỉ là chạy sai hoặc chạy chậm, và người vận hành
 * không có cách nào biết.
 *
 * <p>Đặt ở {@link Order} 10 để chạy TRƯỚC {@code AdminBootstrap} — vô nghĩa khi tạo tài khoản
 * quản trị trên một cơ sở dữ liệu chưa sẵn sàng.
 */
@Component
@Order(10)
public class StartupChecks implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    /** Chế độ quét chỉ mục lặp có từ phiên bản này của phần mở rộng vector. */
    private static final String VECTOR_MIN_VERSION = "0.8.0";

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final OllamaClient llm;
    private final TracuuProperties props;

    public StartupChecks(JdbcTemplate jdbc, StringRedisTemplate redis,
                         OllamaClient llm, TracuuProperties props) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.llm = llm;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Kiểm điều kiện khởi động…");
        kiemCoSoDuLieu();          // 1
        kiemPhanMoRongVector();    // 2
        kiemMoHinhNgonNgu();       // 3
        kiemMoHinhTrenGpu();       // 4
        kiemMoHinhBieuDien();      // 5
        kiemBoNhoDem();            // 6
        log.info("Sáu phép kiểm điều kiện khởi động đều đạt.");
    }

    /** 1 — Không có gì chạy được nếu thiếu cơ sở dữ liệu. */
    private void kiemCoSoDuLieu() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            log.info("  [1/6] Cơ sở dữ liệu: kết nối được.");
        } catch (RuntimeException e) {
            throw dung("""
                    Không kết nối được cơ sở dữ liệu.
                      Kiểm tra dịch vụ 'db' đang chạy và thông tin kết nối trong cấu hình.
                      Chi tiết: %s""".formatted(e.getMessage()));
        }
    }

    /**
     * 2 — Phần mở rộng vector có mặt VÀ đủ mới để quét chỉ mục lặp.
     *
     * <p>Đây là một trong hai phép kiểm đáng giá nhất. Chỉ mục xấp xỉ tìm láng giềng gần nhất
     * <em>trước</em> rồi mới áp bộ lọc; nếu phần lớn láng giềng nằm ngoài phạm vi quyền thì kết
     * quả trả về ít hơn số cần, và <strong>độ phủ tụt mà không có dấu hiệu gì</strong>. Chế độ
     * quét lặp giải đúng chuyện đó. Thiếu nó thì {@code AC-006} của US-008 không đạt được, mà
     * hệ thống vẫn trông như đang chạy bình thường.
     */
    private void kiemPhanMoRongVector() {
        String version;
        try {
            version = jdbc.queryForObject(
                    "SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        } catch (RuntimeException e) {
            version = null;
        }
        if (version == null) {
            throw dung("""
                    Cơ sở dữ liệu chưa cài phần mở rộng vector.
                      Dùng ảnh 'pgvector/pgvector:pg16' hoặc chạy: CREATE EXTENSION vector;""");
        }
        if (compareVersions(version, VECTOR_MIN_VERSION) < 0) {
            throw dung("""
                    Phần mở rộng vector quá cũ: đang có %s, cần tối thiểu %s.
                      Bản cũ hơn không có chế độ quét chỉ mục lặp. Thiếu nó thì lọc quyền trước
                      xếp hạng làm TỤT ĐỘ PHỦ một cách âm thầm — hệ thống vẫn trả lời, chỉ là bỏ
                      sót tài liệu đúng, và không có dấu hiệu nào để nhận ra.
                      Nâng ảnh cơ sở dữ liệu lên bản có vector >= %s."""
                    .formatted(version, VECTOR_MIN_VERSION, VECTOR_MIN_VERSION));
        }
        log.info("  [2/6] Phần mở rộng vector {}: có quét chỉ mục lặp.", version);
    }

    /** 3 — Máy chủ mô hình kết nối được và mô hình đã tải về. */
    private void kiemMoHinhNgonNgu() {
        if (!llm.reachable()) {
            throw dung("""
                    Không kết nối được máy chủ mô hình tại %s.
                      Kiểm tra dịch vụ 'llm' đang chạy."""
                    .formatted(props.llm().baseUrl()));
        }
        List<String> models = llm.availableModels();
        String wanted = props.llm().model();
        boolean has = models.stream().anyMatch(m -> m.equals(wanted) || m.startsWith(wanted + ":"));
        if (!has) {
            throw dung("""
                    Máy chủ mô hình chạy nhưng chưa có mô hình '%s'.
                      Tải về bằng: docker exec tracuu-llm ollama pull %s
                      Mô hình hiện có: %s"""
                    .formatted(wanted, wanted, models.isEmpty() ? "(chưa có mô hình nào)" : String.join(", ", models)));
        }
        log.info("  [3/6] Mô hình ngôn ngữ '{}': đã có.", wanted);
    }

    /**
     * 4 — Mô hình chạy trên GPU chứ không phải CPU.
     *
     * <p>Phép kiểm đáng giá thứ hai. NFR-system-020 và mục 12 của bộ yêu cầu phi chức năng nói
     * thẳng: <em>không cam kết chạy khi không có GPU, không có chế độ suy giảm dùng CPU</em>.
     * Thà không chạy còn hơn chạy chậm — vì chạy chậm thì người dùng vẫn gõ câu hỏi, vẫn chờ,
     * và mọi con số độ trễ đo được đều vô nghĩa.
     *
     * <p>Chỉ cảnh báo khi mô hình <em>chưa nạp</em>: Ollama nạp mô hình lười, lần gọi đầu mới nạp,
     * nên "chưa nạp" lúc khởi động là bình thường và không phải lỗi.
     */
    private void kiemMoHinhTrenGpu() {
        OllamaClient.VramStatus st = llm.vramStatus();
        if (!st.loaded()) {
            log.warn("  [4/6] Mô hình chưa nạp nên chưa kiểm được GPU. Sẽ kiểm lại ở câu hỏi đầu tiên.");
            return;
        }
        if (!st.onGpu()) {
            throw dung("""
                    Mô hình đang chạy trên CPU chứ không phải GPU.
                      Hệ thống vẫn trả lời được nhưng chậm tới mức không dùng nổi, và mọi ngưỡng
                      độ trễ đã cam kết đều không đạt.
                      Kiểm tra trình điều khiển GPU và phần khai báo thiết bị của dịch vụ 'llm'.""");
        }

        // "Có byte nào trên GPU" KHÔNG phải điều phép kiểm này cần biết.
        //
        // Bản trước chỉ hỏi vramBytes > 0, nên một mô hình nạp MỘT PHẦN vẫn qua cửa — đúng cái
        // ca mà phép kiểm sinh ra để chặn. Đo ngày 30/09: qwen2.5:7b trên GPU 6 GB với ngữ cảnh
        // 3072 token chỉ nằm 84% trên GPU, 16% còn lại chạy CPU; phép kiểm in ra "84% mô hình"
        // rồi báo ĐẠT. Cái giá của 16% đó đo được: chữ đầu ở 5 câu đồng thời tăng từ 3,8 giây
        // (mô hình nằm trọn GPU) lên 12,0 giây — ăn 80% ngân sách của ngưỡng 15 giây.
        double tyLe = st.gpuRatio();
        double san = props.llm().minGpuRatio();
        if (tyLe < san) {
            throw dung("""
                    Mô hình chỉ nằm %d%% trên GPU, phần còn lại chạy trên CPU (sàn đang đặt: %d%%).
                      Đây KHÔNG phải cảnh báo hiệu năng nhỏ: phần tràn sang CPU chậm hơn hàng
                      chục lần, và nó kéo theo mọi ngưỡng độ trễ đã cam kết.
                      Cách xử lý, theo thứ tự nên thử:
                        · dùng mô hình nhỏ hơn (tracuu.llm.model),
                        · giảm ngữ cảnh tối đa (tracuu.retrieval.max-tokens),
                        · hoặc hạ sàn tracuu.llm.min-gpu-ratio nếu đã chấp nhận cái giá đó."""
                    .formatted(Math.round(tyLe * 100), Math.round(san * 100)));
        }
        log.info("  [4/6] Mô hình trên GPU: {} MB bộ nhớ đồ hoạ ({}% mô hình).",
                st.vramBytes() / (1024 * 1024), Math.round(tyLe * 100));
    }

    /** 5 — Mô hình sinh biểu diễn ngữ nghĩa nạp được; thiếu thì việc nạp tài liệu sẽ hỏng. */
    private void kiemMoHinhBieuDien() {
        Path dir = Path.of(props.embedding().modelPath());
        Path model = dir.resolve(props.embedding().modelFile());
        Path tokenizer = dir.resolve(props.embedding().tokenizerFile());

        if (Files.isReadable(model) && Files.isReadable(tokenizer)) {
            log.info("  [5/6] Mô hình sinh biểu diễn: có tại {}.", dir.toAbsolutePath());
            return;
        }
        // Đường dự phòng: sinh biểu diễn qua máy chủ mô hình. Chấp nhận được, nhưng KHÔNG phải
        // hình dạng đã thiết kế — nói thẳng ra thay vì im lặng chuyển đường.
        log.warn("""
                  [5/6] Chưa có mô hình sinh biểu diễn trong {} — chuyển sang đường DỰ PHÒNG
                        (sinh biểu diễn qua máy chủ mô hình). Hệ quả: việc nạp tài liệu tranh
                        GPU với việc trả lời câu hỏi, trái với thiết kế ở kiến trúc Mục 1.
                        Đặt {} và {} vào thư mục trên để dùng đường chính.""",
                dir.toAbsolutePath(), props.embedding().modelFile(), props.embedding().tokenizerFile());
    }

    /** 6 — Bộ nhớ đệm: phiên đăng nhập và hàng đợi câu hỏi đứng trên nó. */
    private void kiemBoNhoDem() {
        try {
            redis.opsForValue().set("tracuu:khoi-dong", String.valueOf(System.currentTimeMillis()));
            redis.delete("tracuu:khoi-dong");
            log.info("  [6/6] Bộ nhớ đệm: kết nối được.");
        } catch (RuntimeException e) {
            throw dung("""
                    Không kết nối được bộ nhớ đệm.
                      Phiên đăng nhập và hàng đợi câu hỏi đứng trên nó nên không chạy thiếu được.
                      Kiểm tra dịch vụ 'cache'. Chi tiết: %s""".formatted(e.getMessage()));
        }
    }

    private IllegalStateException dung(String message) {
        return new IllegalStateException("\n\n══ Không khởi động được ══\n" + message + "\n");
    }

    /** So sánh dạng {@code 0.8.0}. Trả số âm khi a cũ hơn b. */
    static int compareVersions(String a, String b) {
        String[] x = a.split("\\.");
        String[] y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? parse(x[i]) : 0;
            int yi = i < y.length ? parse(y[i]) : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        return 0;
    }

    private static int parse(String s) {
        StringBuilder digits = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) digits.append(c); else break;
        }
        return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
    }
}
