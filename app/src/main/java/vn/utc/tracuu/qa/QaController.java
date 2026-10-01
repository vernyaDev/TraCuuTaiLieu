package vn.utc.tracuu.qa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.utc.tracuu.agent.AgentEvent;
import vn.utc.tracuu.platform.security.AppUserDetails;
import vn.utc.tracuu.platform.security.CurrentUser;
import vn.utc.tracuu.platform.web.BusinessException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Điểm vào hỏi đáp — luồng sự kiện một chiều.
 *
 * <p>Dùng luồng sự kiện thay vì một lần gọi trả về trọn câu trả lời vì độ trễ: một câu trả lời
 * đầy đủ mất tới hai mươi giây, và hai mươi giây nhìn màn hình trắng là trải nghiệm khác hẳn
 * với việc thấy chữ chạy dần từ giây thứ năm. Ngưỡng <em>thời gian tới chữ đầu tiên</em> chỉ có
 * nghĩa khi có cơ chế đẩy dần.
 *
 * <p>Lớp này chỉ dịch sự kiện nghiệp vụ sang khung truyền — mọi quyết định đều đã nằm ở tầng
 * dưới. Đây là ranh giới giữ cho gói agent không biết gì về web (luật R2).
 */
@RestController
@RequestMapping("/api/qa")
public class QaController {

    private static final Logger log = LoggerFactory.getLogger(QaController.class);

    /** Dài hơn ngưỡng độ trễ của câu phức tạp khá nhiều, để không cắt nhầm lượt đang chạy tốt. */
    private static final long SSE_TIMEOUT_MS = 300_000;

    private final QaService qa;
    private final ConversationRepository conversations;
    private final TurnRepository turns;
    private final TurnCitationRepository citations;
    private final TurnSubqueryRepository subqueries;
    private final ThreadPoolTaskExecutor executor;
    private final vn.utc.tracuu.platform.web.RunningTurns runningTurns;

    public QaController(QaService qa, ConversationRepository conversations, TurnRepository turns,
                        TurnCitationRepository citations, TurnSubqueryRepository subqueries,
                        vn.utc.tracuu.platform.web.RunningTurns runningTurns,
                        @Qualifier("qaExecutor") ThreadPoolTaskExecutor executor) {
        this.qa = qa;
        this.conversations = conversations;
        this.turns = turns;
        this.citations = citations;
        this.subqueries = subqueries;
        this.runningTurns = runningTurns;
        this.executor = executor;
    }

    public record AskRequest(
            @NotBlank @Size(max = 2000) String question,
            Long conversationId) {}

    /**
     * Gửi câu hỏi, nhận luồng sự kiện.
     *
     * <p>Cờ ngắt được nối thẳng vào các sự kiện đóng kết nối. Người gọi đóng tab thì máy chủ
     * <strong>dừng sinh chữ</strong> — trên một mô hình dùng chung GPU, chạy tiếp một lượt đã bị
     * bỏ là lấy mất lượt của người đang chờ trong hàng đợi.
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@Valid @RequestBody AskRequest body) {
        AppUserDetails user = CurrentUser.require();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancelled = new AtomicBoolean(false);

        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(e -> cancelled.set(true));

        executor.execute(() -> {
            try {
                qa.ask(user, body.conversationId(), body.question(),
                        event -> send(emitter, event, cancelled),
                        cancelled);
            } catch (BusinessException e) {
                send(emitter, new AgentEvent.Error(e.code().name(), e.code().message()), cancelled);
            } catch (RuntimeException e) {
                log.error("Lỗi ngoài dự kiến trong lượt hỏi", e);
                send(emitter, new AgentEvent.Error("LOI_HE_THONG",
                        "Hệ thống gặp sự cố khi xử lý câu hỏi. Thử lại sau ít phút."), cancelled);
            } finally {
                try { emitter.complete(); } catch (RuntimeException ignored) { }
            }
        });

        return emitter;
    }

    private void send(SseEmitter emitter, AgentEvent event, AtomicBoolean cancelled) {
        if (cancelled.get()) return;
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event));
        } catch (IOException | IllegalStateException e) {
            // Người gọi đã đóng kết nối. Không phải lỗi — đánh dấu để tầng dưới dừng sinh chữ.
            cancelled.set(true);
        }
    }

    // ── Lịch sử hội thoại ─────────────────────────────────────────────────────────────────

    /**
     * Một lượt trong lịch sử hội thoại.
     *
     * <p>KHÔNG có trường lý do từ chối, cùng lý do với {@link vn.utc.tracuu.agent.AgentEvent.Refusal}:
     * lý do nội bộ phân biệt hai ca mà câu chữ cố ý không phân biệt, nên đưa nó ra ngoài là mở
     * lại đúng khe hở vừa bịt — chỉ khác là qua một đường khác.
     */
    public record TurnView(Long id, int ordinal, String question, String rewrittenQuestion,
                           String answer, String status,
                           List<CitationView> citations, List<StepView> steps,
                           Integer firstTokenMs, Integer totalMs) {}

    public record CitationView(int rank, Long chunkId, Long documentId, Float scoreRrf) {}

    public record StepView(int ordinal, String text, Integer dependsOn, int hits) {}

    @GetMapping("/conversations/{id}/turns")
    public List<TurnView> history(@PathVariable Long id) {
        long userId = CurrentUser.requireId();
        Conversation conversation = conversations.findById(id)
                .orElseThrow(BusinessException::khongTimThay);
        if (!conversation.getUserId().equals(userId)) {
            throw BusinessException.khongTimThay();
        }

        return turns.findByConversationIdOrderByOrdinal(id).stream()
                .map(t -> new TurnView(
                        t.getId(), t.getOrdinal(), t.getRawQuestion(), t.getRewrittenQuestion(),
                        t.getAnswer(), t.getStatus(),
                        citations.findByTurnIdOrderByRank(t.getId()).stream()
                                .map(c -> new CitationView(c.getRank(), c.getChunkId(),
                                        null, c.getScoreRrf()))
                                .toList(),
                        subqueries.findByTurnIdOrderByOrdinal(t.getId()).stream()
                                .map(s -> new StepView(s.getOrdinal(), s.getText(),
                                        s.getDependsOnOrdinal(), s.getHitCount()))
                                .toList(),
                        t.getLatencyFirstTokenMs(), t.getLatencyTotalMs()))
                .toList();
    }

    /**
     * Dừng một lượt đang chạy.
     *
     * <p>Đóng kết nối ở trình duyệt là chưa đủ: máy chủ phải BIẾT để ngừng sinh chữ. Đường này
     * tồn tại cho cả trường hợp người dùng bấm Dừng ở một tab khác, hoặc kết nối sự kiện đã
     * đứt mà lượt vẫn còn chạy.
     */
    @PostMapping("/turns/{turnId}/stop")
    public void stopTurn(@PathVariable Long turnId) {
        runningTurns.stop(turnId, CurrentUser.requireId());
    }

    /** Bắt đầu phiên hội thoại mới. */
    @PostMapping("/conversations")
    public Conversation startConversation() {
        return conversations.save(new Conversation(CurrentUser.requireId()));
    }

    @GetMapping("/conversations")
    public List<Conversation> myConversations() {
        return conversations.findByUserIdOrderByLastActivityAtDesc(CurrentUser.requireId());
    }
}
