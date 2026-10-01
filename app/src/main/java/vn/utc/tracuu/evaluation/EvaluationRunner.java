package vn.utc.tracuu.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.AgentEvent;
import vn.utc.tracuu.agent.AgentPipeline;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.knowledge.ChunkSetRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.time.Instant;
import java.util.*;

/**
 * Bộ chạy đo — <strong>chạy bằng LỆNH, cố ý không có đường mạng</strong>.
 *
 * <p>Lý do không mở ra thành đường mạng không phải là tiết kiệm công. Bộ đo là công cụ của tác
 * giả, không phải tính năng của người dùng; mở nó ra nghĩa là phải phân quyền cho nó, phải
 * giới hạn tần suất, và phải xử lý một lượt chạy kéo dài hàng giờ qua một kết nối HTTP. Chạy
 * bằng lệnh không có vấn đề nào trong số đó.
 *
 * <p><strong>Nó gọi thẳng bộ điều phối agent, đúng như một người dùng gọi</strong> (luật R6).
 * Cùng đường mã, chỉ khác đầu vào và cách ghi kết quả. Bộ đo đi đường riêng thì nó đo một hệ
 * thống khác với hệ thống được bàn giao, và mọi con số trong chương thực nghiệm sẽ nói về một
 * hệ thống không tồn tại.
 *
 * <h2>Cách dùng</h2>
 * <pre>
 *   java -jar tracuu.jar --eval \
 *        --questions=v1 --user=quantri --mode=hybrid --label="lai-cau-truc"
 *
 *   --resume=&lt;runId&gt;   chạy tiếp một lượt dở dang
 *   --limit=N           chỉ chạy N câu đầu, để thử nhanh
 * </pre>
 */
@Component
public class EvaluationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRunner.class);

    /** Các mức k báo cáo. Ba mức đủ để thấy xu hướng mà không làm bảng rối. */
    private static final int[] K_VALUES = { 1, 5, 10 };

    private final AgentPipeline pipeline;
    private final GroundTruthQuestionRepository questions;
    private final GroundTruthPositionRepository positions;
    private final EvalRunRepository runs;
    private final EvalResultRepository results;
    private final EvalMetricRepository metrics;
    private final HumanGradeRepository humanGrades;
    private final ChunkSetRepository chunkSets;
    private final JdbcTemplate jdbc;
    private final TracuuProperties props;
    private final ObjectMapper json;

    public EvaluationRunner(AgentPipeline pipeline,
                            GroundTruthQuestionRepository questions,
                            GroundTruthPositionRepository positions,
                            EvalRunRepository runs,
                            EvalResultRepository results,
                            EvalMetricRepository metrics,
                            HumanGradeRepository humanGrades,
                            ChunkSetRepository chunkSets, JdbcTemplate jdbc,
                            TracuuProperties props, ObjectMapper json) {
        this.pipeline = pipeline;
        this.questions = questions;
        this.positions = positions;
        this.runs = runs;
        this.results = results;
        this.metrics = metrics;
        this.humanGrades = humanGrades;
        this.chunkSets = chunkSets;
        this.jdbc = jdbc;
        this.props = props;
        this.json = json;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("import-questions")) {
            importQuestions(option(args, "import-questions", null),
                            option(args, "questions", "v1"));
            return;
        }
        if (args.containsOption("import-grades")) {
            importHumanGrades(option(args, "import-grades", null),
                              option(args, "grader", null),
                              args.containsOption("run")
                                      ? Long.parseLong(option(args, "run", "0")) : null);
            return;
        }
        if (!args.containsOption("eval")) return;

        String version = option(args, "questions", "v1");
        String username = option(args, "user", null);
        String label = option(args, "label", null);
        Integer limit = args.containsOption("limit")
                ? Integer.parseInt(option(args, "limit", "0")) : null;
        Long resumeId = args.containsOption("resume")
                ? Long.parseLong(option(args, "resume", "0")) : null;

        if (username == null) {
            log.error("""

                    Thiếu tham số --user.

                    Bộ đo chạy DƯỚI DANH TÍNH một tài khoản thật, có phạm vi quyền xác định.
                    Đây không phải thủ tục: phạm vi quyền quyết định tập tài liệu truy hồi nhìn
                    thấy, nên một lượt đo không nói rõ chạy dưới quyền ai là một con số không
                    diễn giải được.

                    Ví dụ: --eval --user=quantri --questions=v1 --mode=hybrid
                    """);
            return;
        }

        Long userId = jdbc.query("SELECT id FROM users WHERE lower(username) = lower(?)",
                rs -> rs.next() ? rs.getLong(1) : null, username);
        if (userId == null) {
            log.error("Không có tài khoản '{}'. Bộ đo cần một tài khoản thật để lấy phạm vi quyền.",
                    username);
            return;
        }

        try {
            EvalRun run = resumeId != null
                    ? runs.findById(resumeId).orElseThrow(
                        () -> new IllegalArgumentException("Không có lượt chạy " + resumeId))
                    : createRun(version, label, args);

            execute(run, version, userId, username, limit);

        } catch (RuntimeException e) {
            log.error("Lượt đo thất bại: {}", e.getMessage(), e);
        }
    }

    /**
     * Nạp bộ câu hỏi chuẩn từ một tệp JSON.
     *
     * <p>Mỗi mục gồm câu hỏi, độ phức tạp, và <strong>danh sách vị trí đáp án</strong> khai
     * bằng mã tài liệu cộng khoảng ký tự. Khai theo khoảng ký tự chứ không theo mã đoạn là
     * quyết định đã chốt: cùng một bộ câu hỏi ánh xạ được sang mọi chiến lược tách đoạn mà
     * không phải soạn lại đáp án lần nào.
     *
     * <pre>
     * [
     *   { "ordinal": 1,
     *     "text": "Nghỉ phép năm được bao nhiêu ngày?",
     *     "complexity": "DON_GIAN",
     *     "expectedAnswer": "12 ngày làm việc",
     *     "positions": [ { "documentId": 3, "charStart": 1200, "charEnd": 1480 } ] }
     * ]
     * </pre>
     */
    private void importQuestions(String path, String version) {
        if (path == null || path.isBlank()) {
            log.error("Thiếu đường dẫn tệp. Ví dụ: --import-questions=bo-cau-hoi.json --questions=v1");
            return;
        }
        try {
            List<Map<String, Object>> items = json.readValue(
                    java.nio.file.Files.readString(java.nio.file.Path.of(path),
                            java.nio.charset.StandardCharsets.UTF_8),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});

            int created = 0;
            int positionsCreated = 0;

            for (Map<String, Object> item : items) {
                GroundTruthQuestion q = new GroundTruthQuestion();
                q.setQuestionSetVersion(version);
                q.setOrdinal(((Number) item.getOrDefault("ordinal", created + 1)).intValue());
                q.setText(String.valueOf(item.get("text")));
                q.setComplexity(String.valueOf(item.getOrDefault("complexity", "DON_GIAN")));
                Object expected = item.get("expectedAnswer");
                q.setExpectedAnswer(expected == null ? null : String.valueOf(expected));
                q.setNote(item.get("note") == null ? null : String.valueOf(item.get("note")));
                questions.save(q);
                created++;

                Object raw = item.get("positions");
                if (raw instanceof List<?> list) {
                    for (Object entry : list) {
                        if (!(entry instanceof Map<?, ?> p)) continue;
                        GroundTruthPosition position = new GroundTruthPosition();
                        position.setQuestionId(q.getId());
                        position.setDocumentId(((Number) p.get("documentId")).longValue());
                        position.setCharStart(((Number) p.get("charStart")).intValue());
                        position.setCharEnd(((Number) p.get("charEnd")).intValue());
                        positions.save(position);
                        positionsCreated++;
                    }
                }
            }

            log.info("""

                    Đã nạp bộ câu hỏi '{}': {} câu, {} vị trí đáp án.
                      Câu không có vị trí đáp án nào sẽ luôn cho độ phủ 0 — kiểm lại tệp nguồn
                      nếu con số vị trí thấp bất thường so với số câu.
                    """, version, created, positionsCreated);

        } catch (Exception e) {
            log.error("Không nạp được bộ câu hỏi từ {}: {}", path, e.getMessage());
        }
    }

    /**
     * Nhập điểm do NGƯỜI chấm và đối chiếu với điểm máy.
     *
     * <p>Điểm người ghi vào bảng riêng, <strong>không ghi đè điểm máy</strong>. Thứ có giá trị
     * không phải điểm cuối cùng mà là <em>độ lệch giữa hai bên</em>: một bộ chấm tự động lệch xa
     * người chấm thì mọi con số nó sinh ra đều phải đọc kèm cảnh báo, và ghi đè là xoá mất đúng
     * phép so ấy.
     *
     * <pre>
     *   java -jar tracuu.jar --import-grades=diem.json --grader="Nguyen Van A" --run=3
     *
     *   [ { "questionOrdinal": 1, "score": 0.8, "note": "thieu can cu cho muc 14 ngay" } ]
     * </pre>
     */
    private void importHumanGrades(String path, String grader, Long runId) {
        if (path == null || grader == null || runId == null) {
            log.error("""

                    Thiếu tham số. Cả ba đều bắt buộc:
                      --import-grades=<tệp json>   danh sách điểm
                      --grader="<tên người chấm>"  ai chấm — cần để so giữa nhiều người chấm
                      --run=<mã lượt chạy>         điểm thuộc lượt đo nào

                    Thiếu tên người chấm thì không phân biệt được "máy lệch người" với
                    "hai người chấm lệch nhau", mà hai kết luận ấy dẫn tới hai việc khác nhau.
                    """);
            return;
        }

        EvalRun run = runs.findById(runId).orElse(null);
        if (run == null) {
            log.error("Không có lượt chạy {}.", runId);
            return;
        }

        try {
            List<Map<String, Object>> items = json.readValue(
                    java.nio.file.Files.readString(java.nio.file.Path.of(path),
                            java.nio.charset.StandardCharsets.UTF_8),
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});

            List<GroundTruthQuestion> questionList =
                    questions.findByQuestionSetVersionOrderByOrdinal(run.getQuestionSetVersion());
            Map<Integer, Long> byOrdinal = new HashMap<>();
            questionList.forEach(q -> byOrdinal.put(q.getOrdinal(), q.getId()));

            int saved = 0;
            int skipped = 0;
            for (Map<String, Object> item : items) {
                Object ordinalValue = item.get("questionOrdinal");
                if (ordinalValue == null) { skipped++; continue; }
                Long questionId = byOrdinal.get(((Number) ordinalValue).intValue());
                if (questionId == null) { skipped++; continue; }

                EvalResult result = results.findByRunIdAndQuestionId(runId, questionId).orElse(null);
                if (result == null) { skipped++; continue; }

                HumanGrade grade = humanGrades
                        .findByResultIdAndGrader(result.getId(), grader)
                        .orElseGet(HumanGrade::new);
                grade.setResultId(result.getId());
                grade.setGrader(grader);
                grade.setScore(((Number) item.getOrDefault("score", 0)).floatValue());
                Object note = item.get("note");
                grade.setNote(note == null ? null : String.valueOf(note));
                humanGrades.save(grade);
                saved++;
            }

            log.info("Đã nhận {} điểm của {} cho lượt {} ({} dòng bỏ qua vì không khớp câu hỏi "
                   + "hoặc câu chưa có kết quả máy).", saved, grader, runId, skipped);
            reportAgreement(runId);

        } catch (Exception e) {
            log.error("Không nhập được điểm người chấm từ {}: {}", path, e.getMessage());
        }
    }

    /**
     * So điểm máy với điểm người.
     *
     * <p>Báo ba con số vì chúng nói ba điều khác nhau: <em>độ lệch trung bình</em> cho biết máy
     * chấm chặt hay lỏng hơn người, <em>độ lệch tuyệt đối trung bình</em> cho biết sai số điển
     * hình, và <em>độ lệch lớn nhất</em> chỉ ra ca tệ nhất — mà ca tệ nhất thường mới là ca đáng
     * đi xem.
     */
    private void reportAgreement(long runId) {
        List<Object[]> pairs = humanGrades.autoVersusHuman(runId);
        if (pairs.isEmpty()) {
            log.info("Chưa có cặp (điểm máy, điểm người) nào để so — bộ chấm tự động chưa chạy "
                   + "cho lượt này, hoặc chạy mà không sinh điểm.");
            return;
        }

        double sumDiff = 0, sumAbs = 0, maxAbs = 0;
        for (Object[] pair : pairs) {
            double auto = ((Number) pair[0]).doubleValue();
            double human = ((Number) pair[1]).doubleValue();
            double diff = auto - human;
            sumDiff += diff;
            sumAbs += Math.abs(diff);
            maxAbs = Math.max(maxAbs, Math.abs(diff));
        }
        int n = pairs.size();

        log.info("""

                ── Máy chấm so với người chấm, lượt {} ───────────────
                  Số câu so được          : {}
                  Độ lệch trung bình      : {}  (dương = máy chấm CAO hơn người)
                  Độ lệch tuyệt đối TB    : {}
                  Độ lệch lớn nhất        : {}

                  Độ lệch tuyệt đối lớn thì mọi con số của bộ chấm tự động phải đọc kèm cảnh báo.
                """, runId, n, fmt(sumDiff / n), fmt(sumAbs / n), fmt(maxAbs));

        metrics.save(new EvalMetric(runId, "lech_may_nguoi_tb", null, (float) (sumDiff / n)));
        metrics.save(new EvalMetric(runId, "lech_may_nguoi_tuyetdoi_tb", null, (float) (sumAbs / n)));
        metrics.save(new EvalMetric(runId, "lech_may_nguoi_lonnhat", null, (float) maxAbs));
    }

    private EvalRun createRun(String version, String label, ApplicationArguments args) {
        Long chunkSetId = chunkSets.findByActiveTrue()
                .map(cs -> cs.getId())
                .orElseThrow(() -> new IllegalStateException("Chưa có tập đoạn hoạt động"));

        EvalRun run = new EvalRun();
        run.setLabel(label);
        run.setQuestionSetVersion(version);
        run.setChunkSetId(chunkSetId);
        run.setRetrievalMode(option(args, "mode", props.retrieval().mode()));
        run.setModelName(props.llm().model());
        run.setTemperature((float) props.llm().temperature());
        run.setSeed(props.llm().seed());
        run.setTopK(props.retrieval().topK());
        run.setContextMaxTokens(props.context().maxTokens());
        run.setConfig(snapshotConfig());
        return runs.save(run);
    }

    /**
     * Chụp toàn bộ cấu hình vào kết quả.
     *
     * <p>Không chụp thì hai lượt chạy cách nhau một tuần, với một tham số đã đổi ở giữa, trông
     * vẫn như so được với nhau — và kết luận rút ra từ phép so ấy là sai mà không ai phát hiện.
     */
    private String snapshotConfig() {
        try {
            return json.writeValueAsString(Map.of(
                    "retrieval", props.retrieval(),
                    "context", props.context(),
                    "chunking", props.chunking(),
                    "agent", props.agent(),
                    "segmentation", props.segmentation(),
                    "embedding", Map.of("dimension", props.embedding().dimension(),
                                        "maxSequenceLength", props.embedding().maxSequenceLength()),
                    "llm", Map.of("model", props.llm().model(),
                                  "temperature", props.llm().temperature(),
                                  "topP", props.llm().topP(),
                                  "seed", props.llm().seed())));
        } catch (Exception e) {
            log.warn("Không chụp được cấu hình vào kết quả đo: {}", e.getMessage());
            return "{}";
        }
    }

    private void execute(EvalRun run, String version, long userId, String username, Integer limit) {
        List<GroundTruthQuestion> pending = results.findPending(run.getId(), version);
        if (limit != null && limit > 0 && pending.size() > limit) {
            pending = pending.subList(0, limit);
        }

        if (pending.isEmpty()) {
            log.info("Lượt chạy {} đã có đủ kết quả cho bộ câu hỏi {}.", run.getId(), version);
            summarise(run);
            return;
        }

        log.info("""

                ── Bắt đầu lượt đo {} ──────────────────────────────────
                  Bộ câu hỏi : {} ({} câu còn lại)
                  Tập đoạn   : {}
                  Cách tìm   : {}
                  Chạy dưới  : {} (phạm vi quyền của tài khoản này)
                  Mô hình    : {} (nhiệt độ {}, hạt giống {})
                """, run.getId(), version, pending.size(), run.getChunkSetId(),
                run.getRetrievalMode(), username, run.getModelName(),
                run.getTemperature(), run.getSeed());

        int done = 0;
        for (GroundTruthQuestion q : pending) {
            try {
                evaluateOne(run, q, userId);
            } catch (RuntimeException e) {
                EvalResult failed = new EvalResult();
                failed.setRunId(run.getId());
                failed.setQuestionId(q.getId());
                failed.setErrorReason(truncate(e.getMessage()));
                results.save(failed);
                log.warn("Câu {} lỗi: {}", q.getOrdinal(), e.getMessage());
            }
            if (++done % 10 == 0) {
                log.info("  … {}/{} câu", done, pending.size());
            }
        }

        run.setStatus("XONG");
        run.setFinishedAt(Instant.now());
        runs.save(run);
        summarise(run);
    }

    private void evaluateOne(EvalRun run, GroundTruthQuestion q, long userId) {
        List<GroundTruthPosition> expected = positions.findByQuestionId(q.getId());

        // Cùng đường mã người dùng đi. Phạm vi quyền là `evaluation` chứ không phải `user` —
        // giá trị ấy tồn tại để một lượt đo phân biệt được với một lượt hỏi thật trong nhật ký.
        AgentContext ctx = AgentContext.start(
                q.getText(),
                new PermissionScope.ForEvaluation(userId, "run-" + run.getId()),
                run.getChunkSetId(), null, List.of());

        List<RetrievedChunk> retrieved = new ArrayList<>();
        long start = System.currentTimeMillis();

        AgentPipeline.Outcome outcome = pipeline.run(ctx, event -> {
            if (event instanceof AgentEvent.Citations c) {
                // chỉ để biết lượt này có ra trích dẫn không; thứ hạng lấy từ ngữ cảnh bên dưới
                log.trace("Câu {} có {} trích dẫn", q.getOrdinal(), c.items().size());
            }
        }, () -> false);

        retrieved.addAll(outcome.context().chunks());
        int latency = (int) (System.currentTimeMillis() - start);

        Metrics m = score(retrieved, expected);

        EvalResult result = new EvalResult();
        result.setRunId(run.getId());
        result.setQuestionId(q.getId());
        result.setAnswer(outcome.context().answer());
        result.setRefused(outcome.context().refused());
        result.setRefusalReason(outcome.context().refused()
                ? outcome.context().refusal().reason() : null);
        result.setPrecisionAtK((float) m.precision);
        result.setRecallAtK((float) m.recall);
        result.setReciprocalRank((float) m.reciprocalRank);
        result.setLatencyMs(latency);
        results.save(result);
    }

    /**
     * Chấm điểm truy hồi bằng cách đối chiếu <strong>khoảng ký tự</strong>.
     *
     * <p>Một đoạn được tính là đúng khi khoảng ký tự của nó <em>giao nhau</em> với một vị trí
     * đáp án trong cùng tài liệu. Đối chiếu theo khoảng chứ không theo mã đoạn là điều làm bộ
     * câu hỏi dùng lại được cho mọi chiến lược tách — mà đó chính là lý do giữ nhiều tập đoạn
     * song song ngay từ đầu.
     */
    private Metrics score(List<RetrievedChunk> retrieved, List<GroundTruthPosition> expected) {
        if (expected.isEmpty()) return new Metrics(0, 0, 0);

        Set<Long> matchedPositions = new HashSet<>();
        int hits = 0;
        double reciprocalRank = 0;

        for (int i = 0; i < retrieved.size(); i++) {
            RetrievedChunk c = retrieved.get(i);
            Integer[] range = chunkRange(c.chunkId());
            if (range == null) continue;

            boolean matched = false;
            for (GroundTruthPosition p : expected) {
                if (p.getDocumentId() != c.documentId()) continue;
                if (overlaps(range[0], range[1], p.getCharStart(), p.getCharEnd())) {
                    matchedPositions.add(p.getId());
                    matched = true;
                }
            }
            if (matched) {
                hits++;
                if (reciprocalRank == 0) reciprocalRank = 1.0 / (i + 1);
            }
        }

        double precision = retrieved.isEmpty() ? 0 : (double) hits / retrieved.size();
        double recall = (double) matchedPositions.size() / expected.size();
        return new Metrics(precision, recall, reciprocalRank);
    }

    private record Metrics(double precision, double recall, double reciprocalRank) {}

    private Integer[] chunkRange(long chunkId) {
        return jdbc.query("SELECT char_start, char_end FROM chunks WHERE id = ?",
                rs -> rs.next()
                        ? new Integer[]{ rs.getInt(1), rs.getInt(2) }
                        : null, chunkId);
    }

    private static boolean overlaps(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    /** Tính chỉ số tổng hợp và in bảng. */
    private void summarise(EvalRun run) {
        List<EvalResult> all = results.findByRunId(run.getId());
        if (all.isEmpty()) return;

        double avgPrecision = all.stream().filter(r -> r.getPrecisionAtK() != null)
                .mapToDouble(EvalResult::getPrecisionAtK).average().orElse(0);
        double avgRecall = all.stream().filter(r -> r.getRecallAtK() != null)
                .mapToDouble(EvalResult::getRecallAtK).average().orElse(0);
        double mrr = all.stream().filter(r -> r.getReciprocalRank() != null)
                .mapToDouble(EvalResult::getReciprocalRank).average().orElse(0);
        long refused = all.stream().filter(EvalResult::isRefused).count();

        List<Integer> latencies = all.stream()
                .map(EvalResult::getLatencyMs).filter(Objects::nonNull).sorted().toList();
        int p95 = latencies.isEmpty() ? 0
                : latencies.get(Math.min(latencies.size() - 1,
                    (int) Math.ceil(latencies.size() * 0.95) - 1));

        int k = run.getTopK() == null ? props.retrieval().topK() : run.getTopK();
        metrics.save(new EvalMetric(run.getId(), "do_chinh_xac", k, (float) avgPrecision));
        metrics.save(new EvalMetric(run.getId(), "do_phu", k, (float) avgRecall));
        metrics.save(new EvalMetric(run.getId(), "thu_hang_dung_trung_binh", null, (float) mrr));
        metrics.save(new EvalMetric(run.getId(), "ty_le_tu_choi", null, (float) refused / all.size()));
        metrics.save(new EvalMetric(run.getId(), "do_tre_p95_ms", null, p95));

        log.info("""

                ── Kết quả lượt đo {} ──────────────────────────────────
                  Số câu chấm được       : {}
                  Độ chính xác@{}        : {}
                  Độ phủ@{}              : {}
                  Thứ hạng đúng trung bình: {}
                  Tỉ lệ từ chối          : {} ({}/{})
                  Độ trễ p95             : {} ms

                  Điều kiện: cách tìm {} · tập đoạn {} · mô hình {} · ngữ cảnh {} token
                  Mọi con số trên CHỈ so sánh được với lượt chạy có cùng các điều kiện này.
                """,
                run.getId(), all.size(),
                k, fmt(avgPrecision), k, fmt(avgRecall), fmt(mrr),
                fmt((double) refused / all.size()), refused, all.size(), p95,
                run.getRetrievalMode(), run.getChunkSetId(), run.getModelName(),
                run.getContextMaxTokens());

        // Các mức k khác chỉ có nghĩa khi truy hồi lấy đủ số đoạn; nêu ra để người đọc bảng
        // biết con số trên ứng với đúng một mức k, không phải cả ba.
        log.info("  Ghi chú: báo cáo ở mức k = {}. Các mức {} cần chạy lại với top-k tương ứng.",
                k, Arrays.toString(K_VALUES));
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 497) + "…";
    }

    private static String option(ApplicationArguments args, String name, String fallback) {
        List<String> values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }
}
