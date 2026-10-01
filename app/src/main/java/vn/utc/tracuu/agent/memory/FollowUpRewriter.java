package vn.utc.tracuu.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.List;
import java.util.Locale;

/**
 * Bước 3 — viết câu hỏi rút gọn thành câu đứng một mình được.
 *
 * <p>Người dùng hỏi tiếp bằng câu cụt: <em>"thế còn nghỉ ốm thì sao?"</em>. Câu ấy đem đi truy
 * hồi thẳng sẽ không khớp gì, vì nó thiếu chủ đề. Bước này ghép ngữ cảnh lượt trước vào để câu
 * hỏi tự đứng được.
 *
 * <p>Hai điều bước này bắt buộc phải làm đúng:
 * <ul>
 *   <li><strong>Chỉ viết lại khi câu thật sự rút gọn.</strong> Viết lại một câu vốn đã đầy đủ
 *       là thêm một lượt gọi mô hình vào mọi lượt hỏi, và là cơ hội để mô hình bóp méo câu hỏi
 *       gốc. Nhận diện rút gọn làm bằng luật nên không tốn gì.</li>
 *   <li><strong>Giữ cả hai bản.</strong> Câu gốc và câu đã viết lại lưu riêng trong cơ sở dữ
 *       liệu: người dùng phải xem được bản đã viết lại ({@code AC-002} của US-020), và khi một
 *       câu bị từ chối thì phải biết việc viết lại có phải nguyên nhân thật không
 *       ({@code AC-004} của US-027). Ghi đè bản gốc là xoá mất bằng chứng đúng lúc cần nó.</li>
 * </ul>
 */
@Component
public class FollowUpRewriter {

    private static final Logger log = LoggerFactory.getLogger(FollowUpRewriter.class);
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private static final String SYSTEM = """
            Bạn viết lại câu hỏi tiếp nối thành câu hỏi đứng một mình được, dựa vào đoạn hội thoại trước.
            Quy tắc bắt buộc:
            - Chỉ bổ sung chủ đề còn thiếu. KHÔNG thêm thông tin mới, KHÔNG trả lời câu hỏi.
            - Giữ nguyên ý định và mức độ cụ thể của câu gốc.
            - Trả về DUY NHẤT câu hỏi đã viết lại, bằng tiếng Việt, không giải thích, không thêm dấu ngoặc.
            """;

    /** Dấu hiệu câu đang tham chiếu ngược về lượt trước. */
    private static final List<String> ELLIPTICAL = List.of(
            "thế còn", "còn ", "vậy còn", "thì sao", "như thế nào", "cái đó", "cái này",
            "nó ", "họ ", "trường hợp này", "trường hợp đó", "ở trên", "vừa nói",
            "tương tự", "ngược lại", "bao lâu", "bao nhiêu", "khi nào", "ai duyệt");

    private final OllamaClient llm;
    private final TracuuProperties props;

    public FollowUpRewriter(OllamaClient llm, TracuuProperties props) {
        this.llm = llm;
        this.props = props;
    }

    public AgentContext apply(AgentContext ctx) {
        if (ctx.history().isEmpty() || !looksElliptical(ctx.normalizedQuestion())) {
            return ctx.withRewritten(ctx.normalizedQuestion(), false);
        }

        try {
            String rewritten = llm.generate(SYSTEM, buildPrompt(ctx)).strip();
            rewritten = cleanup(rewritten);

            if (rewritten.isBlank() || rewritten.length() > 500) {
                return ctx.withRewritten(ctx.normalizedQuestion(), false);
            }
            log.debug("Viết lại câu hỏi: [{}] thành [{}]", ctx.normalizedQuestion(), rewritten);
            return ctx.withRewritten(rewritten, true);

        } catch (RuntimeException e) {
            // Không viết lại được thì dùng câu gốc. Mất một chút chất lượng truy hồi còn hơn
            // hỏng cả lượt vì một bước phụ trợ.
            log.warn("Không viết lại được câu tiếp nối, dùng câu gốc: {}", e.getMessage());
            return ctx.withRewritten(ctx.normalizedQuestion(), false);
        }
    }

    /**
     * Câu có vẻ rút gọn không.
     *
     * <p>Câu ngắn là dấu hiệu mạnh nhất: một câu dưới sáu chữ trong hội thoại đang diễn ra gần
     * như luôn dựa vào lượt trước.
     */
    public boolean looksElliptical(String question) {
        if (question == null || question.isBlank()) return false;
        String q = question.toLowerCase(VI);
        int words = q.split("\\s+").length;
        if (words <= 6) return true;
        return words <= 20 && ELLIPTICAL.stream().anyMatch(q::contains);
    }

    private String buildPrompt(AgentContext ctx) {
        StringBuilder sb = new StringBuilder("Đoạn hội thoại trước:\n");
        List<AgentContext.PreviousTurn> history = ctx.history();
        int from = Math.max(0, history.size() - props.agent().historyTurns());
        for (int i = from; i < history.size(); i++) {
            AgentContext.PreviousTurn t = history.get(i);
            sb.append("Người hỏi: ").append(t.question()).append('\n');
            if (t.answer() != null && !t.answer().isBlank()) {
                sb.append("Trả lời: ").append(shorten(t.answer())).append('\n');
            }
        }
        sb.append("\nCâu hỏi tiếp nối cần viết lại: ").append(ctx.normalizedQuestion());
        sb.append("\n\nCâu hỏi đã viết lại:");
        return sb.toString();
    }

    /** Cắt câu trả lời cũ khi đưa vào lời nhắc — chỉ cần đủ để biết chủ đề, không cần cả bài. */
    private String shorten(String answer) {
        String a = answer.replaceAll("\\s+", " ").strip();
        return a.length() <= 300 ? a : a.substring(0, 300) + "…";
    }

    /** Mô hình hay trả kèm ngoặc kép hoặc tiền tố; gỡ ra trước khi dùng. */
    private String cleanup(String s) {
        String out = s.replaceAll("^(Câu hỏi đã viết lại|Câu hỏi|Viết lại)\\s*:\\s*", "").strip();
        if (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
            out = out.substring(1, out.length() - 1).strip();
        }
        int newline = out.indexOf('\n');
        return newline > 0 ? out.substring(0, newline).strip() : out;
    }
}
