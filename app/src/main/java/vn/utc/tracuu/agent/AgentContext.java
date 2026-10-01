package vn.utc.tracuu.agent;

import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;

import java.util.List;

/**
 * Ngữ cảnh đi qua cả bảy bước của bộ điều phối — <strong>bất biến</strong>.
 *
 * <p>Luật R3: mỗi bước trả về một bản MỚI, không sửa tại chỗ. Lý do không phải là sở thích
 * lập trình hàm: {@code AC-005} của US-031 đòi ghi lại được toàn bộ quá trình xử lý từng câu —
 * câu đã viết lại, các truy vấn con, các đoạn đã truy hồi. Với ngữ cảnh bất biến,
 * <strong>chuỗi các bản ghi chính là bản ghi quá trình</strong>, không cần rải mã ghi nhật ký
 * khắp nơi. Đây cũng là thứ làm phần phân tích lỗi trong báo cáo viết được.
 *
 * <p>Sửa tại chỗ thì tới bước bảy không còn biết câu hỏi ban đầu trông ra sao, và khi một lượt
 * cho kết quả lạ thì không có cách nào dựng lại đường đi của nó.
 */
public record AgentContext(

        // ── Đầu vào, không đổi suốt lượt ──────────────────────────────────────────────────
        String rawQuestion,
        PermissionScope scope,
        long chunkSetId,
        Long conversationId,
        List<PreviousTurn> history,

        // ── Bước 1: chuẩn hoá và nhận diện ý định ─────────────────────────────────────────
        String normalizedQuestion,
        Intent intent,

        // ── Bước 2: phân loại độ phức tạp ─────────────────────────────────────────────────
        Complexity complexity,

        // ── Bước 3: viết lại câu tiếp nối thành câu độc lập ───────────────────────────────
        String rewrittenQuestion,
        boolean rewritten,

        // ── Bước 4: phân rã thành truy vấn con ────────────────────────────────────────────
        List<SubQuery> subQueries,

        // ── Bước 5: truy hồi ──────────────────────────────────────────────────────────────
        List<RetrievedChunk> chunks,
        int permittedDocumentCount,
        long retrievalMs,

        // ── Bước 6: tra công cụ (hiệu lực, thay thế, chồng lấn) ───────────────────────────
        List<Warning> warnings,

        // ── Bước 7: tổng hợp hoặc từ chối ─────────────────────────────────────────────────
        String answer,
        Refusal refusal
) {

    /** Ý định của câu hỏi. Chỉ ý định tra cứu mới đi tiếp vào truy hồi. */
    public enum Intent { TRA_CUU, CHAO_HOI, NGOAI_PHAM_VI }

    public enum Complexity { DON_GIAN, PHUC_TAP }

    /** Một lượt trước trong hội thoại — nguồn để viết lại câu rút gọn. */
    public record PreviousTurn(String question, String answer) {}

    /** Một truy vấn con, có thể phụ thuộc kết quả của truy vấn con trước. */
    public record SubQuery(int ordinal, String text, Integer dependsOnOrdinal, int hitCount) {
        public SubQuery withHits(int hits) {
            return new SubQuery(ordinal, text, dependsOnOrdinal, hits);
        }
    }

    /** Cảnh báo kèm câu trả lời: tài liệu hết hiệu lực, đã bị thay thế, hoặc chồng lấn. */
    public record Warning(String type, Long documentId, String documentTitle, String detail) {}

    /**
     * Từ chối, kèm lý do thuộc một trong bốn loại.
     *
     * <p>{@code stage} phân biệt từ chối ở tầng một (trước khi gọi mô hình, vì không đủ căn cứ)
     * với tầng hai (sau khi sinh chữ, vì không ý nào gắn được về nguồn). Hai tầng này khác nhau
     * về thời gian phản hồi và về việc có phải thu hồi chữ đã hiện hay không.
     */
    public record Refusal(String reason, String stage, String message) {}

    /** Ngữ cảnh ban đầu của một lượt. */
    public static AgentContext start(String rawQuestion, PermissionScope scope, long chunkSetId,
                                     Long conversationId, List<PreviousTurn> history) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId,
                history == null ? List.of() : history,
                rawQuestion, Intent.TRA_CUU, Complexity.DON_GIAN,
                rawQuestion, false, List.of(), List.of(), 0, 0, List.of(), null, null);
    }

    // ── Bản mới sau mỗi bước ──────────────────────────────────────────────────────────────

    public AgentContext withNormalized(String normalized, Intent intent) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalized, intent, complexity, rewrittenQuestion, rewritten, subQueries,
                chunks, permittedDocumentCount, retrievalMs, warnings, answer, refusal);
    }

    public AgentContext withComplexity(Complexity c) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, c, rewrittenQuestion, rewritten, subQueries,
                chunks, permittedDocumentCount, retrievalMs, warnings, answer, refusal);
    }

    public AgentContext withRewritten(String question, boolean changed) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, question, changed, subQueries,
                chunks, permittedDocumentCount, retrievalMs, warnings, answer, refusal);
    }

    public AgentContext withSubQueries(List<SubQuery> queries) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, rewrittenQuestion, rewritten, queries,
                chunks, permittedDocumentCount, retrievalMs, warnings, answer, refusal);
    }

    public AgentContext withChunks(List<RetrievedChunk> retrieved, int permitted, long ms) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, rewrittenQuestion, rewritten, subQueries,
                retrieved, permitted, ms, warnings, answer, refusal);
    }

    public AgentContext withWarnings(List<Warning> w) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, rewrittenQuestion, rewritten, subQueries,
                chunks, permittedDocumentCount, retrievalMs, w, answer, refusal);
    }

    public AgentContext withAnswer(String text) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, rewrittenQuestion, rewritten, subQueries,
                chunks, permittedDocumentCount, retrievalMs, warnings, text, null);
    }

    /**
     * Từ chối. Xoá luôn phần câu trả lời đã có, nếu có.
     *
     * <p>Hợp đồng giao diện Mục 3: từ chối và chữ trả lời <strong>loại trừ nhau</strong>. Một
     * lượt hoặc trả lời hoặc từ chối, không có lượt trả lời nửa vời rồi từ chối. Giữ lại phần
     * chữ đã sinh ở đây là mở đường cho nó rò ra ngoài qua một đường khác.
     */
    public AgentContext withRefusal(String reason, String stage, String message) {
        return new AgentContext(rawQuestion, scope, chunkSetId, conversationId, history,
                normalizedQuestion, intent, complexity, rewrittenQuestion, rewritten, subQueries,
                chunks, permittedDocumentCount, retrievalMs, warnings, null,
                new Refusal(reason, stage, message));
    }

    /** Câu hỏi thực sự đem đi truy hồi: bản đã viết lại nếu có, không thì bản đã chuẩn hoá. */
    public String effectiveQuestion() {
        return rewritten && rewrittenQuestion != null ? rewrittenQuestion : normalizedQuestion;
    }

    public boolean refused() {
        return refusal != null;
    }
}
