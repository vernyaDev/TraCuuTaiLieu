package vn.utc.tracuu.qa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.User;
import vn.utc.tracuu.access.UserRepository;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.AgentEvent;
import vn.utc.tracuu.agent.AgentPipeline;
import vn.utc.tracuu.agent.memory.ConversationSummarizer;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.gap.GapService;
import vn.utc.tracuu.gap.RefusalClassifier;
import vn.utc.tracuu.knowledge.ChunkSet;
import vn.utc.tracuu.knowledge.ChunkSetRepository;
import vn.utc.tracuu.platform.security.AppUserDetails;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;
import vn.utc.tracuu.platform.web.QuestionQueue;
import vn.utc.tracuu.platform.web.RunningTurns;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Đường hỏi đáp đầy đủ: hàng đợi, bộ điều phối, lưu lượt, ghi khoảng trống.
 *
 * <p>Lớp này là chỗ những thứ <em>ngoài</em> nghiệp vụ agent được gắn vào — hàng đợi, giao dịch,
 * nhật ký. Bộ điều phối giữ nguyên sự thuần khiết của nó (luật R2), nên bộ đo gọi thẳng
 * {@code AgentPipeline} mà không kéo theo hàng đợi hay bảng lượt.
 */
@Service
public class QaService {

    private static final Logger log = LoggerFactory.getLogger(QaService.class);

    private final AgentPipeline pipeline;
    private final QuestionQueue queue;
    private final ConversationRepository conversations;
    private final TurnRepository turns;
    private final TurnSubqueryRepository subqueries;
    /**
     * Tham chiếu tới CHÍNH bean này qua bộ chứa, để lời gọi đi qua proxy.
     *
     * <p>Gọi thẳng {@code persist(...)}, {@code recordGap(...)}, {@code closeTurn(...)} trong
     * cùng lớp là lời gọi Java thường: nó KHÔNG đi qua proxy, nên cả {@code @Transactional} lẫn
     * {@code @Async} trên ba phương thức đó đều không có tác dụng nào. Ba lời hứa hỏng cùng lúc
     * — việc ghi không nguyên tử, và quan trọng hơn: việc phân loại khoảng trống chạy ĐỒNG BỘ
     * trong luồng của người hỏi.
     *
     * <p>Đo ngày 30/09: máy chủ báo lượt xong sau 1,4–2,4 giây, nhưng người hỏi còn chờ thêm
     * tới 1,9 giây nữa — đúng phần việc đáng lẽ chạy nền. Con số độ trễ báo cáo vì thế nhỏ hơn
     * thời gian người dùng thật sự chờ.
     */
    private final org.springframework.beans.factory.ObjectProvider<QaService> chinhNo;

    private final TurnCitationRepository citations;

    private final TurnWarningRepository warnings;
    private final ChunkSetRepository chunkSets;
    private final UserRepository users;
    private final GapService gaps;
    private final RefusalClassifier refusalClassifier;
    private final ConversationSummarizer summarizer;
    private final RunningTurns runningTurns;

    public QaService(AgentPipeline pipeline, QuestionQueue queue,
                     ConversationRepository conversations, TurnRepository turns,
                     TurnSubqueryRepository subqueries, TurnCitationRepository citations,
                     org.springframework.beans.factory.ObjectProvider<QaService> chinhNo,
                     TurnWarningRepository warnings, ChunkSetRepository chunkSets,
                     UserRepository users, GapService gaps, RefusalClassifier refusalClassifier,
                     ConversationSummarizer summarizer, RunningTurns runningTurns) {
        this.pipeline = pipeline;
        this.queue = queue;
        this.conversations = conversations;
        this.turns = turns;
        this.subqueries = subqueries;
        this.citations = citations;
        this.chinhNo = chinhNo;
        this.warnings = warnings;
        this.chunkSets = chunkSets;
        this.users = users;
        this.gaps = gaps;
        this.refusalClassifier = refusalClassifier;
        this.summarizer = summarizer;
        this.runningTurns = runningTurns;
    }

    /**
     * Hỏi một câu và đẩy sự kiện ra dần.
     *
     * <p>Chạy đồng bộ trong luồng của người gọi tầng web — luồng ấy đang giữ một kết nối mở,
     * nên nó phải sống tới khi lượt kết thúc.
     */
    public void ask(AppUserDetails user, Long conversationId, String question,
                    Consumer<AgentEvent> emit, AtomicBoolean cancelled) {

        ChunkSet activeSet = chunkSets.findByActiveTrue().orElseThrow(() ->
                new BusinessException(ErrorCode.LOI_HE_THONG, null, "Chưa có tập đoạn hoạt động"));

        Conversation conversation = resolveConversation(user.userId(), conversationId);
        Turn turn = openTurn(conversation, question, activeSet.getId());

        // Ghi vào sổ NGAY sau khi có mã lượt: người dùng bấm Dừng lúc còn đang xếp hàng cũng
        // phải dừng được, không chỉ lúc đang sinh chữ.
        runningTurns.register(turn.getId(), user.userId(), cancelled);

        QuestionQueue.Ticket ticket = null;
        try {
            ticket = queue.acquire(position ->
                    emit.accept(new AgentEvent.Queued(position, Math.max(0, position - 1))));

            AgentContext initial = AgentContext.start(
                    question, user.scope(), activeSet.getId(), conversation.getId(),
                    recentHistory(conversation.getId()));

            AgentPipeline.Outcome outcome = pipeline.run(initial, emit, cancelled::get);
            chinhNo.getObject().persist(turn, outcome, user);

            emit.accept(new AgentEvent.Done(turn.getId(),
                    Math.max(0, outcome.firstTokenMs()), outcome.totalMs()));

        } catch (QuestionQueue.QueueFullException e) {
            chinhNo.getObject().closeTurn(turn, "LOI");
            emit.accept(new AgentEvent.Error(ErrorCode.HE_THONG_BAN.name(),
                    ErrorCode.HE_THONG_BAN.message()));

        } catch (QuestionQueue.QueueTimeoutException e) {
            chinhNo.getObject().closeTurn(turn, "LOI");
            emit.accept(new AgentEvent.Error(ErrorCode.QUA_THOI_GIAN_CHO.name(),
                    ErrorCode.QUA_THOI_GIAN_CHO.message()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            chinhNo.getObject().closeTurn(turn, "DA_DUNG");

        } catch (RuntimeException e) {
            log.error("Lượt hỏi {} lỗi", turn.getId(), e);
            chinhNo.getObject().closeTurn(turn, "LOI");
            emit.accept(new AgentEvent.Error(ErrorCode.LOI_HE_THONG.name(),
                    ErrorCode.LOI_HE_THONG.message()));

        } finally {
            queue.release(ticket);
            runningTurns.unregister(turn.getId());
        }
    }

    // ── Lưu trữ ────────────────────────────────────────────────────────────────────────────

    @Transactional
    protected Conversation resolveConversation(long userId, Long conversationId) {
        if (conversationId != null) {
            Conversation existing = conversations.findById(conversationId)
                    .orElseThrow(BusinessException::khongTimThay);
            // Hội thoại của người khác trả "không tìm thấy", không trả "không đủ quyền" —
            // mã trạng thái không được tiết lộ sự tồn tại.
            if (!existing.getUserId().equals(userId)) {
                throw BusinessException.khongTimThay();
            }
            return existing;
        }
        return conversations.save(new Conversation(userId));
    }

    @Transactional
    protected Turn openTurn(Conversation conversation, String question, long chunkSetId) {
        Turn turn = new Turn();
        turn.setConversationId(conversation.getId());
        turn.setOrdinal(turns.maxOrdinal(conversation.getId()) + 1);
        turn.setRawQuestion(question);
        turn.setChunkSetId(chunkSetId);
        turn.setStatus("DANG_CHAY");
        return turns.save(turn);
    }

    @Transactional
    public void persist(Turn turn, AgentPipeline.Outcome outcome, AppUserDetails user) {
        AgentContext ctx = outcome.context();

        turn.setRewrittenQuestion(ctx.rewritten() ? ctx.rewrittenQuestion() : null);
        turn.setComplexity(ctx.complexity() == null ? null : ctx.complexity().name());
        turn.setLatencyFirstTokenMs(outcome.firstTokenMs() < 0 ? null : (int) outcome.firstTokenMs());
        turn.setLatencyTotalMs((int) outcome.totalMs());
        turn.setLatencyRetrievalMs((int) ctx.retrievalMs());

        if (ctx.refused()) {
            turn.setStatus("TU_CHOI");
            turn.setRefusalReason(ctx.refusal().reason());
            turn.setRefusalStage(ctx.refusal().stage());
            turn.setAnswer(null);
        } else {
            turn.setStatus("XONG");
            turn.setAnswer(ctx.answer());
        }
        turns.save(turn);

        saveSubqueries(turn.getId(), ctx);
        saveCitations(turn.getId(), outcome.citations());
        saveWarnings(turn.getId(), ctx);

        Conversation conversation = conversations.findById(turn.getConversationId()).orElseThrow();
        conversation.setTurnCount(turn.getOrdinal());
        conversation.setLastActivityAt(java.time.Instant.now());

        // Hội thoại dài thì cuộn phần cũ thành tóm tắt thay vì cắt bỏ — người dùng vẫn hỏi
        // tiếp được về chủ đề "lúc đầu".
        if (summarizer.shouldSummarize(turn.getOrdinal())) {
            conversation.setSummary(summarizer.summarize(
                    conversation.getSummary(), recentHistory(conversation.getId())));
        }
        conversations.save(conversation);

        if (ctx.refused()) {
            chinhNo.getObject().recordGap(turn, ctx, user);
        }
    }

    private void saveSubqueries(long turnId, AgentContext ctx) {
        if (ctx.subQueries().size() <= 1) return;
        for (AgentContext.SubQuery sq : ctx.subQueries()) {
            TurnSubquery row = new TurnSubquery();
            row.setTurnId(turnId);
            row.setOrdinal(sq.ordinal());
            row.setDependsOnOrdinal(sq.dependsOnOrdinal());
            row.setText(sq.text());
            row.setHitCount(sq.hitCount());
            subqueries.save(row);
        }
    }

    private void saveCitations(long turnId, List<RetrievedChunk> cited) {
        int rank = 1;
        for (RetrievedChunk c : cited) {
            TurnCitation row = new TurnCitation();
            row.setTurnId(turnId);
            row.setChunkId(c.chunkId());
            row.setRank(rank++);
            row.setScoreBm25(c.scoreBm25() == null ? null : c.scoreBm25().floatValue());
            row.setScoreVector(c.scoreVector() == null ? null : c.scoreVector().floatValue());
            row.setScoreRrf((float) c.scoreRrf());
            citations.save(row);
        }
    }

    private void saveWarnings(long turnId, AgentContext ctx) {
        for (AgentContext.Warning w : ctx.warnings()) {
            TurnWarning row = new TurnWarning();
            row.setTurnId(turnId);
            row.setType(w.type());
            row.setDocumentId(w.documentId());
            row.setDetail(w.detail());
            warnings.save(row);
        }
    }

    /**
     * Ghi khoảng trống, sau khi phân loại lý do THẬT.
     *
     * <p>Chạy không đồng bộ: người hỏi đã nhận thông báo từ chối rồi, và việc phân loại phải
     * chạy thêm một lượt truy hồi không lọc quyền — không được bắt họ chờ việc ấy.
     */
    @Async("khoangTrongExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordGap(Turn turn, AgentContext ctx, AppUserDetails user) {
        try {
            String realReason = refusalClassifier.classify(
                    ctx.refusal().reason(), ctx.effectiveQuestion(),
                    ctx.chunkSetId(), user.userId());

            String departments = users.findByIdWithDepartments(user.userId())
                    .map(User::getDepartments)
                    .map(set -> set.stream()
                            .map(d -> d.getName())
                            .collect(Collectors.joining(", ")))
                    .orElse("");

            gaps.record(turn.getId(), user.userId(), turn.getRawQuestion(),
                    turn.getRewrittenQuestion(), realReason,
                    departments, user.clearanceLevel());

        } catch (RuntimeException e) {
            log.warn("Không ghi được nhật ký khoảng trống cho lượt {}: {}",
                    turn.getId(), e.getMessage());
        }
    }

    @Transactional
    public void closeTurn(Turn turn, String status) {
        turn.setStatus(status);
        turns.save(turn);
    }

    /** Các lượt gần nhất, theo thứ tự cũ trước — nguồn để viết lại câu tiếp nối. */
    @Transactional(readOnly = true)
    protected List<AgentContext.PreviousTurn> recentHistory(long conversationId) {
        List<Turn> recent = turns.findRecent(conversationId, PageRequest.of(0, 6));
        List<AgentContext.PreviousTurn> out = new ArrayList<>(recent.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            Turn t = recent.get(i);
            out.add(new AgentContext.PreviousTurn(
                    t.getRewrittenQuestion() != null ? t.getRewrittenQuestion() : t.getRawQuestion(),
                    t.getAnswer()));
        }
        return out;
    }
}
