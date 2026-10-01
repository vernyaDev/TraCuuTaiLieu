package vn.utc.tracuu.evaluation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.reasoning.ComplexityClassifier;
import vn.utc.tracuu.agent.AgentPipeline;
import vn.utc.tracuu.knowledge.ChunkSetRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Đo độ trễ trên phần cứng thật — trả lời trực tiếp giả định về việc mô hình có chạy nổi trong
 * ngưỡng đã cam kết hay không.
 *
 * <p><strong>Vì sao lệnh này tồn tại riêng thay vì nằm trong bộ đo chất lượng.</strong> Bộ đo
 * chất lượng trả lời <em>"câu trả lời có đúng không"</em>; lệnh này trả lời <em>"hệ thống có
 * nhanh đủ dùng không"</em>. Hai câu hỏi ấy cần hai điều kiện chạy khác nhau: đo chất lượng chạy
 * tuần tự cho tất định, còn đo độ trễ phải chạy <strong>đồng thời</strong> để thấy chỗ suy giảm.
 * Trộn hai việc là cho ra một bảng số không diễn giải được theo chiều nào.
 *
 * <p><strong>Ba điều kiện đo mà thiếu một là số vô nghĩa</strong>, và cả ba đều được in ra cùng
 * kết quả:
 * <ol>
 *   <li><strong>Mô hình phải nằm trên bộ nhớ đồ hoạ.</strong> Cùng một mô hình chạy CPU chậm hơn
 *       hàng chục lần; một bảng số không nói rõ điều này thì vô dụng.</li>
 *   <li><strong>Ngữ cảnh phải dài đúng bằng giá trị cấu hình.</strong> Thời gian xử lý lời nhắc
 *       tỉ lệ thuận với số token, nên đo với ngữ cảnh ngắn cho một con số đẹp mà vận hành thật
 *       không bao giờ đạt.</li>
 *   <li><strong>Phạm vi quyền phải nói rõ.</strong> Truy vấn bật chế độ quét lặp khi phạm vi hẹp,
 *       và quét lặp đổi độ trễ lấy độ phủ — đo ở phạm vi toàn kho là đo ở điều kiện thuận lợi
 *       nhất.</li>
 * </ol>
 *
 * <h2>Cách dùng</h2>
 * <pre>
 *   java -jar tracuu.jar --measure --user=quantri
 *   java -jar tracuu.jar --measure --user=quantri --concurrency=1,2,5 --repeat=10
 * </pre>
 */
@Component
@Order(30)
public class LatencyMeasurementRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LatencyMeasurementRunner.class);

    /** Câu hỏi mặc định khi chưa nạp bộ câu hỏi chuẩn. Đủ đa dạng để không đo trên một ca. */
    private static final List<String> DEFAULT_QUESTIONS = List.of(
            "Người lao động được nghỉ hằng năm bao nhiêu ngày?",
            "Thủ tục xin nghỉ phép dài ngày gồm những bước nào?",
            "Ai có thẩm quyền phê duyệt đơn nghỉ phép?",
            "Quy định về làm thêm giờ áp dụng cho những ai?",
            "Chế độ nghỉ ốm được hưởng bao nhiêu phần trăm lương?");

    private final AgentPipeline pipeline;
    private final GroundTruthQuestionRepository questions;
    private final ChunkSetRepository chunkSets;
    private final OllamaClient llm;
    private final JdbcTemplate jdbc;
    private final TracuuProperties props;
    /** Cùng bộ phân loại mà đường chạy thật dùng — đo bằng luật khác luật đang chạy là vô nghĩa. */
    private final ComplexityClassifier classifier;

    public LatencyMeasurementRunner(AgentPipeline pipeline,
                                    GroundTruthQuestionRepository questions,
                                    ChunkSetRepository chunkSets, OllamaClient llm,
                                    JdbcTemplate jdbc, TracuuProperties props,
                                    ComplexityClassifier classifier) {
        this.pipeline = pipeline;
        this.questions = questions;
        this.chunkSets = chunkSets;
        this.llm = llm;
        this.jdbc = jdbc;
        this.props = props;
        this.classifier = classifier;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("measure")) return;

        String username = option(args, "user", null);
        if (username == null) {
            log.error("""

                    Thiếu --user. Phạm vi quyền của người đo QUYẾT ĐỊNH con số đo được: truy vấn
                    bật chế độ quét lặp khi phạm vi hẹp, và quét lặp đổi độ trễ lấy độ phủ.
                    Một bảng độ trễ không nói rõ đo dưới quyền ai là một bảng không diễn giải được.

                    Ví dụ: --measure --user=quantri
                    """);
            return;
        }

        Long userId = jdbc.query("SELECT id FROM users WHERE lower(username) = lower(?)",
                rs -> rs.next() ? rs.getLong(1) : null, username);
        if (userId == null) {
            log.error("Không có tài khoản '{}'.", username);
            return;
        }

        Long chunkSetId = chunkSets.findByActiveTrue().map(cs -> cs.getId()).orElse(null);
        if (chunkSetId == null) {
            log.error("Chưa có tập đoạn hoạt động. Nạp ít nhất một tài liệu trước khi đo.");
            return;
        }

        List<Integer> levels = parseLevels(option(args, "concurrency", "1,2,5"));
        int repeat = Integer.parseInt(option(args, "repeat", "5"));
        List<String> questionSet = resolveQuestions(option(args, "questions", null));

        printConditions(userId, chunkSetId, questionSet.size(), repeat);

        // Làm nóng: lượt đầu tiên gánh cả việc nạp mô hình vào bộ nhớ đồ hoạ. Đưa nó vào phép
        // đo là trộn một chi phí chỉ xảy ra một lần vào con số của mọi lượt.
        log.info("Đang làm nóng (nạp mô hình vào bộ nhớ)…");
        measureOne(questionSet.get(0), userId, chunkSetId);
        reportVram("sau khi làm nóng");

        Map<Boolean, List<String>> nhom = tachTheoDoPhucTap(questionSet);
        List<String> donGian = nhom.get(false);
        List<String> phucTap = nhom.get(true);
        log.info("Bộ câu hỏi tách theo độ phức tạp: {} câu đơn giản, {} câu phức tạp.",
                donGian.size(), phucTap.size());

        Map<Integer, Stats> byLevel = new LinkedHashMap<>();
        if (!donGian.isEmpty()) {
            for (int level : levels) {
                log.info("");
                log.info("── Câu ĐƠN GIẢN, mức {} câu hỏi đồng thời ─────────────────────", level);
                byLevel.put(level, measureAtConcurrency(donGian, userId, chunkSetId, level, repeat));
                reportVram("ở mức " + level + " câu đồng thời");
            }
        }

        Stats statsPhucTap = null;
        if (!phucTap.isEmpty()) {
            log.info("");
            log.info("── Câu PHỨC TẠP, một người hỏi ────────────────────────────────");
            statsPhucTap = measureAtConcurrency(phucTap, userId, chunkSetId, 1, repeat);
        }

        printVerdict(byLevel, statsPhucTap, donGian.size(), phucTap.size());
    }

    // ── Đo ────────────────────────────────────────────────────────────────────────────────

    /** Kết quả một lượt: thời gian tới chữ đầu và tổng thời gian, hoặc từ chối. */
    private record Sample(long firstTokenMs, long totalMs, boolean refused, boolean failed) {}

    /**
     * Tách bộ câu hỏi thành hai nhóm theo đúng bộ phân loại mà đường chạy thật dùng.
     *
     * <p>Không có bước này thì phép đo sai ở chỗ khó thấy nhất: {@code nfr.md} khai
     * {@code NFR-system-001} là ngưỡng của <strong>câu hỏi đơn giản</strong> (5 giây), còn câu
     * phức tạp có ngưỡng riêng {@code NFR-system-003} (60 giây) vì nó cố ý trả thêm một lượt
     * gọi mô hình để phân rã truy vấn con. Trộn hai nhóm vào một rổ rồi chấm tất cả bằng
     * ngưỡng 5 giây cho ra một kết luận "KHÔNG ĐẠT" hoàn toàn sai — đo được ngày 30/09: bốn
     * câu đơn giản đều dưới 2,1 giây, một câu phức tạp mất 8,2 giây, và cái p95 gộp lại báo
     * hỏng một ngưỡng mà sản phẩm không hề vi phạm.
     *
     * <p>Sai theo chiều ngược lại còn tệ hơn: một bộ toàn câu phức tạp sẽ được chấm bằng
     * ngưỡng dễ của câu đơn giản và báo ĐẠT trong khi chưa ai kiểm nó bằng ngưỡng đúng.
     */
    private Map<Boolean, List<String>> tachTheoDoPhucTap(List<String> questionSet) {
        Map<Boolean, List<String>> out = new LinkedHashMap<>();
        out.put(false, new ArrayList<>());
        out.put(true, new ArrayList<>());
        for (String q : questionSet) {
            boolean phucTap = classifier.classify(q) == AgentContext.Complexity.PHUC_TAP;
            out.get(phucTap).add(q);
        }
        return out;
    }

    private Sample measureOne(String question, long userId, long chunkSetId) {
        AgentContext ctx = AgentContext.start(question,
                new PermissionScope.ForEvaluation(userId, "do-do-tre"), chunkSetId, null, List.of());
        try {
            AgentPipeline.Outcome outcome = pipeline.run(ctx, e -> { }, () -> false);
            return new Sample(outcome.firstTokenMs(), outcome.totalMs(),
                    outcome.context().refused(), false);
        } catch (RuntimeException e) {
            log.warn("  một lượt đo lỗi: {}", e.getMessage());
            return new Sample(-1, -1, false, true);
        }
    }

    private Stats measureAtConcurrency(List<String> questionSet, long userId, long chunkSetId,
                                       int level, int repeat) {
        List<Sample> samples = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(level);
        AtomicLong index = new AtomicLong();

        long wallStart = System.currentTimeMillis();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int round = 0; round < repeat; round++) {
                for (int slot = 0; slot < level; slot++) {
                    futures.add(pool.submit(() -> {
                        String q = questionSet.get(
                                (int) (index.getAndIncrement() % questionSet.size()));
                        samples.add(measureOne(q, userId, chunkSetId));
                    }));
                }
                // Chờ hết đợt trước khi sang đợt sau: mục đích là đo ĐÚNG mức đồng thời đã khai,
                // không phải đo một hàng đợi trôi tự do.
                for (Future<?> f : futures) {
                    try { f.get(10, TimeUnit.MINUTES); } catch (Exception ignored) { }
                }
                futures.clear();
            }
        } finally {
            pool.shutdownNow();
        }
        long wallMs = System.currentTimeMillis() - wallStart;

        Stats stats = Stats.from(samples, wallMs);
        log.info("  {} lượt · chữ đầu p50 {} / p95 {} · đầy đủ p50 {} / p95 {} · từ chối {} · lỗi {}",
                stats.count, ms(stats.firstTokenP50), ms(stats.firstTokenP95),
                ms(stats.totalP50), ms(stats.totalP95), stats.refused, stats.failed);
        return stats;
    }

    private record Stats(int count, long firstTokenP50, long firstTokenP95,
                         long totalP50, long totalP95, int refused, int failed, long wallMs) {

        static Stats from(List<Sample> samples, long wallMs) {
            List<Long> first = samples.stream()
                    .filter(s -> !s.failed() && !s.refused() && s.firstTokenMs() >= 0)
                    .map(Sample::firstTokenMs).sorted().toList();
            List<Long> total = samples.stream()
                    .filter(s -> !s.failed() && !s.refused())
                    .map(Sample::totalMs).sorted().toList();

            return new Stats(samples.size(),
                    percentile(first, 50), percentile(first, 95),
                    percentile(total, 50), percentile(total, 95),
                    (int) samples.stream().filter(Sample::refused).count(),
                    (int) samples.stream().filter(Sample::failed).count(),
                    wallMs);
        }

        /** p95 nghĩa là 95 lần trong 100 phải đạt — dùng p95 vì trung bình che mất đúng những lần chậm. */
        private static long percentile(List<Long> sorted, int p) {
            if (sorted.isEmpty()) return -1;
            int index = (int) Math.ceil(sorted.size() * p / 100.0) - 1;
            return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
        }
    }

    // ── In điều kiện và kết luận ───────────────────────────────────────────────────────────

    private void printConditions(long userId, long chunkSetId, int questionCount, int repeat) {
        OllamaClient.VramStatus vram = llm.vramStatus();
        Integer permitted = jdbc.query("""
                WITH my_depts AS (SELECT department_id FROM user_departments WHERE user_id = ?)
                SELECT count(DISTINCT d.id) FROM documents d
                JOIN document_acl a ON a.document_id = d.id AND a.effect = 'CHO_PHEP'
                WHERE d.status = 'DA_LAP_CHI_MUC'
                  AND ( (a.subject_type = 'NGUOI_DUNG' AND a.subject_id = ?)
                     OR (a.subject_type = 'PHONG_BAN' AND a.subject_id IN
                         (SELECT department_id FROM my_depts)) )
                """, rs -> rs.next() ? rs.getInt(1) : 0, userId, userId);
        Integer corpus = jdbc.queryForObject(
                "SELECT count(*) FROM documents WHERE status = 'DA_LAP_CHI_MUC'", Integer.class);
        Integer chunkCount = jdbc.queryForObject(
                "SELECT count(*) FROM chunks WHERE chunk_set_id = ?", Integer.class, chunkSetId);

        double scope = (corpus == null || corpus == 0) ? 0 : 100.0 * permitted / corpus;

        log.info("""

                ══ ĐIỀU KIỆN ĐO ═══════════════════════════════════════════════
                  Mô hình              : {}
                  Nằm trên GPU         : {}
                  Ngữ cảnh tối đa      : {} token   ← mọi ngưỡng độ trễ treo lên con số này
                  Số đoạn truy hồi     : {}
                  Kho                  : {} tài liệu, {} đoạn
                  Phạm vi quyền        : {} tài liệu ({}% kho)
                  Quét chỉ mục lặp     : {}
                  Bộ câu hỏi           : {} câu, lặp {} lượt mỗi mức
                ═══════════════════════════════════════════════════════════════
                """,
                props.llm().model(),
                vram.loaded() ? (vram.onGpu()
                        ? "CÓ — " + (vram.vramBytes() / (1024 * 1024)) + " MB, "
                          + Math.round(vram.gpuRatio() * 100) + "% mô hình"
                        : "KHÔNG — đang chạy CPU")
                        : "chưa nạp (sẽ nạp ở lượt làm nóng)",
                props.context().maxTokens(), props.retrieval().topK(),
                corpus, chunkCount, permitted, Math.round(scope),
                props.retrieval().iterativeScan() ? "bật khi phạm vi hẹp" : "tắt",
                questionCount, repeat);
    }

    private void reportVram(String when) {
        OllamaClient.VramStatus vram = llm.vramStatus();
        if (!vram.loaded()) {
            log.info("  bộ nhớ đồ hoạ {}: mô hình không còn nạp", when);
            return;
        }
        log.info("  bộ nhớ đồ hoạ {}: {} MB ({}% mô hình nằm trên GPU)",
                when, vram.vramBytes() / (1024 * 1024), Math.round(vram.gpuRatio() * 100));
    }

    /**
     * So kết quả với ngưỡng đã cam kết.
     *
     * <p>Chỉ đối chiếu, không tự kết luận đạt hay không đạt toàn cục: ngưỡng ở tài liệu khai
     * kèm điều kiện đo chuẩn (kho 1.000 tài liệu, khoảng 50.000 đoạn), và một lần đo trên kho
     * nhỏ hơn thì <em>không</em> chứng minh được ngưỡng đạt ở kho đầy. Nói rõ điều đó ra thay
     * vì để người đọc tưởng đã xong.
     */
    private void printVerdict(Map<Integer, Stats> byLevel, Stats phucTap,
                              int soDonGian, int soPhucTap) {
        Stats single = byLevel.get(1);
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append(System.lineSeparator())
          .append("══ ĐỐI CHIẾU NGƯỠNG ═══════════════════════════════════════════")
          .append(System.lineSeparator());

        // NFR-001/002/009 là ngưỡng của CÂU ĐƠN GIẢN (nfr.md). Không có câu đơn giản nào
        // trong bộ thì phải nói thẳng là chưa kiểm, tuyệt đối không im lặng bỏ qua — một
        // ngưỡng không được kiểm mà không ai biết thì tệ hơn một ngưỡng báo hỏng.
        sb.append(String.format("  Bộ câu hỏi: %d câu đơn giản · %d câu phức tạp%n",
                soDonGian, soPhucTap));
        if (soDonGian == 0) {
            sb.append("  NFR-001/002/009: CHƯA KIỂM — bộ câu hỏi không có câu đơn giản nào.")
              .append(System.lineSeparator());
        }
        if (single != null) {
            sb.append(line("NFR-001 chữ đầu, câu đơn giản", single.firstTokenP95, 5_000));
            sb.append(line("NFR-002 đầy đủ, câu đơn giản", single.totalP95, 20_000));
        }
        Stats five = byLevel.get(5);
        if (five != null) {
            sb.append(line("NFR-009 chữ đầu, 5 câu đồng thời", five.firstTokenP95, 15_000));
            sb.append(line("NFR-009 đầy đủ, 5 câu đồng thời", five.totalP95, 60_000));
        }
        if (phucTap != null) {
            sb.append(line("NFR-003 đầy đủ, câu phức tạp", phucTap.totalP95, 60_000));
            sb.append(String.format("  %-38s p95 %-10s (không có ngưỡng riêng)%n",
                    "  — chữ đầu, câu phức tạp", ms(phucTap.firstTokenP95)));
        } else {
            sb.append("  NFR-003: CHƯA KIỂM — bộ câu hỏi không có câu phức tạp nào.")
              .append(System.lineSeparator());
        }

        sb.append(System.lineSeparator())
          .append("  Suy giảm khi tăng số câu đồng thời:").append(System.lineSeparator());
        byLevel.forEach((level, s) -> sb
                .append(String.format("    %d câu: chữ đầu p95 %-10s đầy đủ p95 %-10s%n",
                        level, ms(s.firstTokenP95), ms(s.totalP95))));

        sb.append(System.lineSeparator())
          .append("""
                    ĐỌC KẾT QUẢ NÀY THẾ NÀO — ba điều phải nói rõ:

                    1. Ngưỡng ở tài liệu khai kèm điều kiện "kho 1.000 tài liệu, khoảng 50.000
                       đoạn". Đo trên kho nhỏ hơn mà đạt thì CHƯA chứng minh được sẽ đạt ở kho
                       đầy — thời gian truy hồi tăng theo số đoạn. Xem dòng "Kho" ở phần điều kiện.

                    2. Phạm vi quyền đổi kết quả rất mạnh, và ĐO ĐƯỢC ngày 30/09 là ngược với
                       phỏng đoán ban đầu: trên kho 996 tài liệu, chữ đầu p95 là 933 ms ở phạm
                       vi 1 tài liệu, 1,4 giây ở 4%, và 8,2 giây ở 100%. Phạm vi RỘNG mới là ca
                       chậm, không phải phạm vi hẹp — càng nhiều đoạn được phép đọc thì càng
                       nhiều ứng viên phải chấm điểm. Ghi rõ phạm vi khi trích con số ra ngoài.

                    3. Câu đơn giản và câu phức tạp được đo TÁCH nhau vì chúng có ngưỡng khác
                       nhau (NFR-001 so với NFR-003). Câu phức tạp trả thêm một lượt gọi mô hình
                       để phân rã truy vấn con — riêng lượt đó tốn khoảng 6 giây trên máy này.
                       Gộp hai nhóm rồi lấy p95 chung là cách tạo ra một kết luận "KHÔNG ĐẠT"
                       cho một ngưỡng mà sản phẩm không hề vi phạm.
                  """);
        sb.append("═══════════════════════════════════════════════════════════════");
        log.info(sb.toString());
    }

    private String line(String label, long actual, long threshold) {
        String verdict = actual < 0 ? "không đo được"
                : actual <= threshold ? "ĐẠT" : "KHÔNG ĐẠT";
        return String.format("  %-38s p95 %-10s ngưỡng %-10s %s%n",
                label, ms(actual), ms(threshold), verdict);
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────────────────

    private List<String> resolveQuestions(String version) {
        if (version != null) {
            List<String> fromSet = questions.findByQuestionSetVersionOrderByOrdinal(version)
                    .stream().map(GroundTruthQuestion::getText).toList();
            if (!fromSet.isEmpty()) return fromSet;
            log.warn("Bộ câu hỏi '{}' rỗng, dùng bộ câu hỏi mặc định.", version);
        }
        return DEFAULT_QUESTIONS;
    }

    private static List<Integer> parseLevels(String raw) {
        List<Integer> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                int v = Integer.parseInt(part.strip());
                if (v > 0) out.add(v);
            } catch (NumberFormatException ignored) { }
        }
        return out.isEmpty() ? List.of(1) : out;
    }

    private static String ms(long value) {
        if (value < 0) return "—";
        return value < 1000 ? value + " ms" : String.format(Locale.US, "%.1f s", value / 1000.0);
    }

    private static String option(ApplicationArguments args, String name, String fallback) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }
}
