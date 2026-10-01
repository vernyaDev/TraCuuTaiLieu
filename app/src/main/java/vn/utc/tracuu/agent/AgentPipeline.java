package vn.utc.tracuu.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.action.AnswerGenerator;
import vn.utc.tracuu.agent.action.ContextBuilder;
import vn.utc.tracuu.agent.action.GroundingChecker;
import vn.utc.tracuu.agent.action.RefusalDecider;
import vn.utc.tracuu.agent.memory.FollowUpRewriter;
import vn.utc.tracuu.agent.planner.QueryPlanner;
import vn.utc.tracuu.agent.prompt.PromptNormalizer;
import vn.utc.tracuu.agent.reasoning.ComplexityClassifier;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.agent.retriever.Retriever;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Bộ điều phối bảy bước — đường mã mà <strong>cả người dùng lẫn bộ đo</strong> đều đi qua.
 *
 * <pre>
 *   1 prompt     chuẩn hoá, nhận diện ý định
 *   2 reasoning  đơn giản hay phức tạp
 *   3 memory     câu rút gọn thành câu độc lập
 *   4 planner    phức tạp thì tách truy vấn con
 *   5 retriever  lọc quyền TRƯỚC, rồi BM25 + vector, hợp nhất thứ hạng
 *   6 tool       hiệu lực, chuỗi thay thế, chồng lấn
 *   7 action     đủ căn cứ thì sinh chữ và kiểm gắn nguồn, không thì từ chối
 * </pre>
 *
 * <p>Lớp này <strong>không biết gì về giao thức web</strong> (luật R2): nó nhận một ngữ cảnh và
 * đẩy ra các sự kiện nghiệp vụ. Nhờ vậy bộ đo gọi thẳng vào đây như một người dùng gọi (luật
 * R6) — cùng đường mã, chỉ khác đầu vào và cách ghi kết quả. Bộ đo đi đường riêng thì nó đo một
 * hệ thống khác với hệ thống được bàn giao, và mọi con số trong chương thực nghiệm sẽ nói về
 * một hệ thống không tồn tại.
 *
 * <p><strong>Bộ điều phối là tường minh, không phải một vòng lặp tự quyết.</strong> Bảy bước
 * chạy theo thứ tự cố định, mỗi bước là một lời gọi nhìn thấy được. Một vòng lặp để mô hình tự
 * chọn bước kế tiếp sẽ cho quỹ đạo khác nhau giữa hai lần chạy cùng một câu hỏi — và khi đó
 * không lượt chạy thí nghiệm nào so được với lượt nào.
 */
@Component
public class AgentPipeline {

    private static final Logger log = LoggerFactory.getLogger(AgentPipeline.class);

    private final PromptNormalizer normalizer;
    private final ComplexityClassifier classifier;
    private final FollowUpRewriter rewriter;
    private final QueryPlanner planner;
    private final Retriever retriever;
    private final vn.utc.tracuu.agent.tool.DocumentTools tools;
    private final RefusalDecider refusalDecider;
    private final ContextBuilder contextBuilder;
    private final AnswerGenerator generator;
    private final GroundingChecker grounding;
    private final TracuuProperties props;

    public AgentPipeline(PromptNormalizer normalizer, ComplexityClassifier classifier,
                         FollowUpRewriter rewriter, QueryPlanner planner, Retriever retriever,
                         vn.utc.tracuu.agent.tool.DocumentTools tools, RefusalDecider refusalDecider,
                         ContextBuilder contextBuilder, AnswerGenerator generator,
                         GroundingChecker grounding, TracuuProperties props) {
        this.normalizer = normalizer;
        this.classifier = classifier;
        this.rewriter = rewriter;
        this.planner = planner;
        this.retriever = retriever;
        this.tools = tools;
        this.refusalDecider = refusalDecider;
        this.contextBuilder = contextBuilder;
        this.generator = generator;
        this.grounding = grounding;
        this.props = props;
    }

    /** Kết quả một lượt: ngữ cảnh cuối cùng cộng hai con số độ trễ đo được. */
    public record Outcome(AgentContext context, List<RetrievedChunk> citations,
                          long firstTokenMs, long totalMs) {}

    public Outcome run(AgentContext initial, Consumer<AgentEvent> emit, BooleanSupplier cancelled) {
        long start = System.currentTimeMillis();
        long[] firstToken = { -1 };

        try {
            // ── 1. Chuẩn hoá và nhận diện ý định ──────────────────────────────────────────
            emit.accept(new AgentEvent.Status("Đang hiểu câu hỏi"));
            AgentContext ctx = normalizer.apply(initial);

            if (ctx.intent() == AgentContext.Intent.CHAO_HOI) {
                // Chào hỏi không đi vào truy hồi. Trả lời luôn thay vì từ chối: từ chối một lời
                // chào là thô, và cũng không phải sự thật.
                emit.accept(new AgentEvent.Token(RefusalDecider.THONG_DIEP_CHAO_HOI));
                ctx = ctx.withAnswer(RefusalDecider.THONG_DIEP_CHAO_HOI);
                firstToken[0] = System.currentTimeMillis() - start;
                return new Outcome(ctx, List.of(), firstToken[0], System.currentTimeMillis() - start);
            }

            // ── 2. Phân loại độ phức tạp ──────────────────────────────────────────────────
            ctx = classifier.apply(ctx);

            // ── 3. Viết lại câu tiếp nối ──────────────────────────────────────────────────
            if (!ctx.history().isEmpty()) {
                emit.accept(new AgentEvent.Status("Đang đọc lại ngữ cảnh hội thoại"));
                ctx = rewriter.apply(ctx);
                if (ctx.rewritten()) {
                    emit.accept(new AgentEvent.Rewritten(ctx.rewrittenQuestion()));
                }
            } else {
                ctx = ctx.withRewritten(ctx.normalizedQuestion(), false);
            }

            // ── 4. Phân rã truy vấn con ───────────────────────────────────────────────────
            if (ctx.complexity() == AgentContext.Complexity.PHUC_TAP) {
                emit.accept(new AgentEvent.Status("Đang chia câu hỏi thành các phần"));
            }
            ctx = planner.apply(ctx);
            if (ctx.subQueries().size() > 1) {
                emit.accept(new AgentEvent.Plan(ctx.subQueries().stream()
                        .map(sq -> new AgentEvent.PlanItem(sq.ordinal(), sq.text(), sq.dependsOnOrdinal()))
                        .toList()));
            }

            if (cancelled.getAsBoolean()) return cancelledOutcome(ctx, start);

            // ── 5. Truy hồi ───────────────────────────────────────────────────────────────
            emit.accept(new AgentEvent.Status("Đang tìm trong tài liệu"));
            ctx = retrieveAll(ctx);

            // ── 6. Tra hiệu lực, thay thế, chồng lấn ──────────────────────────────────────
            ctx = tools.apply(ctx);

            // ── 7a. Tầng chặn thứ nhất ────────────────────────────────────────────────────
            Optional<AgentContext> refused = refusalDecider.decide(ctx);
            if (refused.isPresent()) {
                ctx = refused.get();
                emit.accept(new AgentEvent.Refusal(
                        ctx.refusal().stage(), ctx.refusal().message()));
                long total = System.currentTimeMillis() - start;
                log.debug("Từ chối tầng một sau {} ms, lý do {}", total, ctx.refusal().reason());
                return new Outcome(ctx, List.of(), total, total);
            }

            // Cảnh báo phát TRƯỚC câu trả lời: người đọc cần biết nguồn có vấn đề ngay từ đầu,
            // không phải sau khi đã đọc xong và tin vào nó.
            for (AgentContext.Warning w : ctx.warnings()) {
                emit.accept(new AgentEvent.Warning(w.type(), w.documentId(), w.documentTitle(), w.detail()));
            }

            // ── 7b. Sinh câu trả lời ──────────────────────────────────────────────────────
            emit.accept(new AgentEvent.Status("Đang soạn câu trả lời"));
            ContextBuilder.Context context = contextBuilder.build(ctx.chunks());

            if (context.included().isEmpty()) {
                ctx = refusalDecider.refuseUngrounded(ctx);
                emit.accept(new AgentEvent.Refusal(
                        ctx.refusal().stage(), ctx.refusal().message()));
                long total = System.currentTimeMillis() - start;
                return new Outcome(ctx, List.of(), total, total);
            }

            String answer = generator.generate(ctx, context, piece -> {
                if (firstToken[0] < 0) firstToken[0] = System.currentTimeMillis() - start;
                emit.accept(new AgentEvent.Token(piece));
            }, cancelled);

            if (cancelled.getAsBoolean()) return cancelledOutcome(ctx.withAnswer(answer), start);

            // ── 7c. Mô hình TỰ khai là không đủ căn cứ ────────────────────────────────────
            //
            // Quy tắc 3 của lời nhắc yêu cầu đúng điều này: "nguồn không đủ thì nói thẳng là
            // không đủ căn cứ". Hệ thống đã HỎI tín hiệu đó, nên phải xử lý nó — bản trước để
            // nguyên câu ấy chảy ra màn hình như một câu trả lời bình thường, kèm cả danh sách
            // trích dẫn. Người đọc nhận tám thẻ nguồn gắn vào một câu không trả lời gì, và
            // hiểu nhầm rằng tám nguồn đó có liên quan. Một lời từ chối sạch tốt hơn hẳn.
            if (moHinhKhaiKhongDuCanCu(answer)) {
                ctx = refusalDecider.refuseUngrounded(ctx);
                emit.accept(new AgentEvent.Refusal(
                        ctx.refusal().stage(), ctx.refusal().message()));
                long total = System.currentTimeMillis() - start;
                log.info("Mô hình tự khai không đủ căn cứ — chuyển thành từ chối.");
                return new Outcome(ctx, List.of(), firstToken[0], total);
            }

            // ── 7d. Tầng chặn thứ hai ─────────────────────────────────────────────────────
            GroundingChecker.Result check = grounding.check(answer, context.included());
            if (!check.grounded()) {
                // Chữ đã hiện trên màn hình. Sự kiện từ chối là tín hiệu để giao diện XOÁ phần
                // đã hiện — một lượt hoặc trả lời hoặc từ chối, không có lượt nửa vời.
                ctx = refusalDecider.refuseUngrounded(ctx);
                emit.accept(new AgentEvent.Refusal(
                        ctx.refusal().stage(), ctx.refusal().message()));
                long total = System.currentTimeMillis() - start;
                log.info("Thu hồi câu trả lời: chỉ {}/{} câu gắn được về nguồn",
                        Math.round(check.supportedRatio() * check.checkedSentences()),
                        check.checkedSentences());
                return new Outcome(ctx, List.of(), firstToken[0], total);
            }

            // ── Trích dẫn: LUÔN đến sau mẩu chữ cuối ──────────────────────────────────────
            List<RetrievedChunk> cited = generator.citedOnly(answer, context.included());
            emit.accept(new AgentEvent.Citations(toCitations(cited)));

            ctx = ctx.withAnswer(answer);
            long total = System.currentTimeMillis() - start;
            return new Outcome(ctx, cited, Math.max(0, firstToken[0]), total);

        } catch (OllamaClient.LlmUnavailableException e) {
            log.error("Máy chủ mô hình không dùng được", e);
            emit.accept(new AgentEvent.Error("MO_HINH_KHONG_SAN_SANG",
                    "Dịch vụ sinh câu trả lời hiện chưa sẵn sàng. "
                    + "Báo quản trị hệ thống kiểm tra máy chủ mô hình."));
            long total = System.currentTimeMillis() - start;
            return new Outcome(initial, List.of(), Math.max(0, firstToken[0]), total);

        } catch (RuntimeException e) {
            log.error("Lượt hỏi đáp lỗi", e);
            emit.accept(new AgentEvent.Error("LOI_HE_THONG",
                    "Hệ thống gặp sự cố khi xử lý câu hỏi. Thử lại sau ít phút."));
            long total = System.currentTimeMillis() - start;
            return new Outcome(initial, List.of(), Math.max(0, firstToken[0]), total);
        }
    }

    /**
     * Mô hình có đang nói "tôi không trả lời được" thay vì trả lời không?
     *
     * <p>Đây KHÔNG phải dò một tật riêng của một mô hình. Lời nhắc hệ thống yêu cầu đúng câu
     * này ở quy tắc 3, nên nó là một phần của giao ước giữa hệ thống và mô hình — việc đọc nó
     * cũng chính đáng như đọc dấu trích dẫn {@code [1]} mà quy tắc 2 yêu cầu.
     *
     * <p>Đòi câu trả lời phải NGẮN mới coi là lời khai: một câu trả lời dài có thể nhắc cụm
     * "không đủ căn cứ" khi nói về một phần nhỏ ("riêng mức phụ cấp thì không đủ căn cứ"), và
     * chối cả câu trả lời đó là vứt đi phần thực sự có nguồn.
     */
    private static boolean moHinhKhaiKhongDuCanCu(String answer) {
        if (answer == null) return false;
        String s = answer.strip();
        if (s.length() > 200) return false;
        String thuong = s.toLowerCase(java.util.Locale.forLanguageTag("vi-VN"));
        return thuong.contains("không đủ căn cứ")
                || thuong.contains("không có đủ căn cứ")
                || thuong.contains("không tìm thấy thông tin");
    }

    /**
     * Truy hồi cho từng truy vấn con rồi gộp.
     *
     * <p>Gộp theo mã đoạn và <strong>giữ thứ hạng tốt nhất</strong>: một đoạn trả lời được nhiều
     * phần của câu hỏi thì nó quan trọng hơn, không phải trùng lặp cần loại.
     */
    private AgentContext retrieveAll(AgentContext ctx) {
        int topK = props.retrieval().topK();
        List<AgentContext.SubQuery> subQueries = ctx.subQueries();

        if (subQueries.size() == 1) {
            Retriever.Outcome outcome = retriever.retrieve(
                    subQueries.get(0).text(), ctx.scope(), ctx.chunkSetId(), topK);
            return ctx
                    .withSubQueries(List.of(subQueries.get(0).withHits(outcome.chunks().size())))
                    .withChunks(outcome.chunks(), outcome.permittedDocumentCount(), outcome.elapsedMs());
        }

        // Câu phức tạp: chia hạn mức cho các phần, nhưng mỗi phần vẫn lấy tối thiểu hai đoạn —
        // một đoạn cho mỗi phần là không đủ để tổng hợp một câu so sánh.
        int perQuery = Math.max(2, topK / subQueries.size());
        Map<Long, RetrievedChunk> merged = new LinkedHashMap<>();
        List<AgentContext.SubQuery> withHits = new ArrayList<>();
        int permitted = 0;
        long elapsed = 0;

        for (AgentContext.SubQuery sq : subQueries) {
            Retriever.Outcome outcome = retriever.retrieve(
                    sq.text(), ctx.scope(), ctx.chunkSetId(), perQuery);
            permitted = Math.max(permitted, outcome.permittedDocumentCount());
            elapsed += outcome.elapsedMs();
            withHits.add(sq.withHits(outcome.chunks().size()));

            for (RetrievedChunk c : outcome.chunks()) {
                merged.merge(c.chunkId(), c,
                        (existing, incoming) -> existing.rank() <= incoming.rank() ? existing : incoming);
            }
        }

        List<RetrievedChunk> all = new ArrayList<>(merged.values());
        all.sort(Comparator.comparingInt(RetrievedChunk::rank));
        List<RetrievedChunk> capped = all.size() > topK ? all.subList(0, topK) : all;

        return ctx.withSubQueries(withHits).withChunks(capped, permitted, elapsed);
    }

    private List<AgentEvent.Citation> toCitations(List<RetrievedChunk> chunks) {
        List<AgentEvent.Citation> out = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk c = chunks.get(i);
            out.add(new AgentEvent.Citation(i + 1, c.chunkId(), c.documentId(), c.documentTitle(),
                    c.breadcrumb(), excerpt(c.content()),
                    c.scoreBm25(), c.scoreVector(), c.scoreRrf()));
        }
        return out;
    }

    private String excerpt(String content) {
        String s = content.replaceAll("\\s+", " ").strip();
        return s.length() <= 240 ? s : s.substring(0, 237) + "…";
    }

    private Outcome cancelledOutcome(AgentContext ctx, long start) {
        long total = System.currentTimeMillis() - start;
        log.debug("Lượt hỏi bị người gọi ngắt sau {} ms", total);
        return new Outcome(ctx, List.of(), -1, total);
    }
}
