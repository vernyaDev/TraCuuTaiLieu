package vn.utc.tracuu.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.util.List;

/**
 * Cuộn phần cũ của hội thoại dài thành một đoạn tóm tắt.
 *
 * <p><strong>Vì sao không cắt bỏ.</strong> Hội thoại tới lượt thứ mười lăm thì không thể đưa
 * hết vào lời nhắc — số token ăn thẳng vào ngưỡng độ trễ. Cách rẻ nhất là cắt phần cũ đi, và
 * nó hỏng theo một kiểu khó chịu: người dùng hỏi <em>"thế còn trường hợp lúc đầu tôi nói thì
 * sao?"</em> và hệ thống không còn biết "lúc đầu" là gì. Tóm tắt giữ được chủ đề đã đi qua với
 * chi phí một phần nhỏ số token.
 *
 * <p><strong>Tóm tắt chỉ giữ CHỦ ĐỀ, không giữ nội dung câu trả lời.</strong> Đây là điều dễ
 * làm sai nhất: nếu tóm tắt chép lại các con số và quy định đã trả lời, thì lượt sau mô hình
 * có thể lấy chúng ra dùng lại <em>mà không truy hồi tài liệu nào</em> — và câu trả lời ấy
 * không có trích dẫn, không qua tầng kiểm gắn nguồn, không kiểm lại quyền đọc. Một quy định
 * đúng ở lượt ba có thể đã hết hiệu lực, hoặc quyền của người hỏi đã bị thu hồi giữa chừng.
 * Nên tóm tắt nói <em>"đã hỏi về mức hưởng chế độ ốm đau"</em>, không nói mức hưởng là bao nhiêu.
 */
@Component
public class ConversationSummarizer {

    private static final Logger log = LoggerFactory.getLogger(ConversationSummarizer.class);

    private static final String SYSTEM = """
            Bạn viết một đoạn tóm tắt ngắn về CHỦ ĐỀ của đoạn hội thoại tra cứu tài liệu.
            Quy tắc bắt buộc:
            - Chỉ nêu người dùng đã hỏi về những CHỦ ĐỀ gì, theo thứ tự.
            - TUYỆT ĐỐI không chép lại số liệu, thời hạn, mã văn bản hay nội dung quy định
              trong các câu trả lời. Chỉ nêu chủ đề.
            - Viết liền một đoạn, tối đa 4 câu, bằng tiếng Việt.
            - Không mở đầu bằng "Đoạn hội thoại này", đi thẳng vào nội dung.
            """;

    private final OllamaClient llm;
    private final TracuuProperties props;

    public ConversationSummarizer(OllamaClient llm, TracuuProperties props) {
        this.llm = llm;
        this.props = props;
    }

    /** Đã tới lúc cuộn chưa. */
    public boolean shouldSummarize(int turnCount) {
        int threshold = props.agent().summarizeAfterTurns();
        return turnCount >= threshold && turnCount % threshold == 0;
    }

    /**
     * Tóm tắt các lượt cũ.
     *
     * @param existingSummary tóm tắt lần trước, nếu có — tóm tắt được cuộn tiếp chứ không
     *                        dựng lại từ đầu, vì các lượt cũ nhất đã không còn trong danh sách
     * @return tóm tắt mới, hoặc {@code existingSummary} nếu không tóm tắt được
     */
    public String summarize(String existingSummary, List<AgentContext.PreviousTurn> turns) {
        if (turns == null || turns.isEmpty()) return existingSummary;

        try {
            String result = llm.generate(SYSTEM, buildPrompt(existingSummary, turns)).strip();
            if (result.isBlank()) return existingSummary;
            if (result.length() > 1200) result = result.substring(0, 1200);
            log.debug("Đã cuộn {} lượt thành tóm tắt {} ký tự", turns.size(), result.length());
            return result;

        } catch (RuntimeException e) {
            // Không tóm tắt được thì giữ tóm tắt cũ. Mất một chút ngữ cảnh còn hơn hỏng lượt hỏi.
            log.info("Không cuộn được tóm tắt hội thoại: {}", e.getMessage());
            return existingSummary;
        }
    }

    private String buildPrompt(String existingSummary, List<AgentContext.PreviousTurn> turns) {
        StringBuilder sb = new StringBuilder();
        if (existingSummary != null && !existingSummary.isBlank()) {
            sb.append("Tóm tắt phần trước đó:").append(System.lineSeparator())
              .append(existingSummary).append(System.lineSeparator()).append(System.lineSeparator());
        }
        sb.append("Các lượt hỏi cần gộp vào tóm tắt:").append(System.lineSeparator());
        for (AgentContext.PreviousTurn t : turns) {
            sb.append("- ").append(t.question()).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator()).append("Tóm tắt:");
        return sb.toString();
    }
}
