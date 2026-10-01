package vn.utc.tracuu.agent.reasoning;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;

import java.util.List;
import java.util.Locale;

/**
 * Bước 2 — câu hỏi này đơn giản hay phức tạp.
 *
 * <p>Quyết định ở đây điều khiển hai thứ: có chạy bước phân rã truy vấn con hay không, và
 * ngưỡng độ trễ nào áp cho lượt này ({@code NFR-system-002} cho câu đơn giản so với
 * {@code NFR-system-003} cho câu phức tạp).
 *
 * <p><strong>Phân loại bằng luật, không bằng mô hình.</strong> Ba lý do, theo thứ tự quan trọng:
 * <ol>
 *   <li>Gọi mô hình để phân loại là thêm một lượt chờ vào <em>mọi</em> câu hỏi, kể cả câu đơn
 *       giản nhất — đúng chỗ mà ngưỡng thời gian tới chữ đầu đang chật.</li>
 *   <li>Luật thì tất định. {@code NFR-system-044} đòi cố định được yếu tố ngẫu nhiên; một bộ
 *       phân loại bằng mô hình có thể cho hai kết quả khác nhau trên cùng một câu, và khi đó
 *       hai lượt chạy thí nghiệm không còn so được với nhau.</li>
 *   <li>Sai ở đây rẻ. Đánh nhầm câu phức tạp thành đơn giản thì vẫn có câu trả lời, chỉ là
 *       không phân rã; đánh nhầm chiều ngược lại thì chậm hơn một chút.</li>
 * </ol>
 */
@Component
public class ComplexityClassifier {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    /** Từ nối cho thấy câu hỏi đang gộp nhiều việc. */
    private static final List<String> MULTI_PART = List.of(
            " và ", " đồng thời ", " ngoài ra ", " bên cạnh đó ", " cũng như ");

    /** Từ cho thấy câu hỏi đòi đối chiếu — luôn cần ít nhất hai lần truy hồi. */
    private static final List<String> COMPARISON = List.of(
            "so sánh", "khác nhau", "khác gì", "giống nhau", "đối chiếu",
            "hơn hay", "nên chọn", "ưu nhược", "thay đổi gì", "trước và sau");

    /** Từ cho thấy câu hỏi đòi tổng hợp nhiều nguồn. */
    private static final List<String> AGGREGATION = List.of(
            "tất cả", "toàn bộ", "liệt kê", "danh sách", "những ai", "bao nhiêu trường hợp",
            "quy trình đầy đủ", "các bước", "tổng hợp");

    /** Câu dài hơn ngưỡng này gần như luôn gộp nhiều ý. */
    private static final int LONG_QUESTION_WORDS = 28;

    public AgentContext apply(AgentContext ctx) {
        return ctx.withComplexity(classify(ctx.effectiveQuestion()));
    }

    public AgentContext.Complexity classify(String question) {
        if (question == null || question.isBlank()) return AgentContext.Complexity.DON_GIAN;
        String q = question.toLowerCase(VI);

        if (COMPARISON.stream().anyMatch(q::contains)) return AgentContext.Complexity.PHUC_TAP;
        if (AGGREGATION.stream().anyMatch(q::contains)) return AgentContext.Complexity.PHUC_TAP;

        // Nhiều dấu hỏi là nhiều câu hỏi gộp làm một.
        if (q.chars().filter(c -> c == '?').count() >= 2) return AgentContext.Complexity.PHUC_TAP;

        boolean multiPart = MULTI_PART.stream().anyMatch(q::contains);
        int words = q.split("\\s+").length;

        // Chỉ riêng chữ "và" thì chưa đủ — "chế độ thai sản và ốm đau" là một chủ đề.
        // Phải là câu DÀI mà lại có từ nối thì mới coi là gộp nhiều việc.
        if (multiPart && words >= 16) return AgentContext.Complexity.PHUC_TAP;
        if (words >= LONG_QUESTION_WORDS) return AgentContext.Complexity.PHUC_TAP;

        return AgentContext.Complexity.DON_GIAN;
    }
}
