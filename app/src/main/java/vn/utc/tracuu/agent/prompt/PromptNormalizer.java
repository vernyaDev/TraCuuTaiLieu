package vn.utc.tracuu.agent.prompt;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.AgentContext;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Bước 1 — chuẩn hoá câu hỏi và nhận diện ý định.
 *
 * <p>Bước này <strong>không gọi mô hình ngôn ngữ</strong>, và đó là điều kiện để
 * {@code NFR-system-004} đạt được: từ chối ở tầng một phải nhanh hơn hẳn đường trả lời. Một câu
 * chào hỏi mà phải chờ mô hình mới biết là câu chào hỏi thì đã mất mấy giây vô ích.
 *
 * <p>Chuẩn hoá Unicode về dạng tổ hợp sẵn là bước âm thầm nhưng cần: cùng một chữ tiếng Việt có
 * hai cách mã hoá, và hai cách đó cho hai chuỗi khác nhau ở mọi phép so khớp phía sau — một lỗi
 * không nhìn thấy bằng mắt.
 */
@Component
public class PromptNormalizer {

    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final int MAX_LENGTH = 2000;

    private static final List<String> GREETINGS = List.of(
            "xin chào", "chào bạn", "chào em", "hello", "hi ", "alo",
            "cảm ơn", "cám ơn", "thanks", "tạm biệt", "bye");

    /** Dấu hiệu câu hỏi nằm ngoài phạm vi kho tài liệu nội bộ. */
    private static final List<String> OUT_OF_SCOPE = List.of(
            "thời tiết", "tỷ giá", "giá vàng", "bóng đá", "kết quả xổ số",
            "dịch câu", "viết giúp tôi bài thơ", "làm bài tập", "nấu ăn");

    public AgentContext apply(AgentContext ctx) {
        String normalized = normalize(ctx.rawQuestion());
        AgentContext.Intent intent = detectIntent(normalized);
        return ctx.withNormalized(normalized, intent);
    }

    public String normalize(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFC)
                // Ký tự vô hình lọt vào khi dán từ trình duyệt hoặc tài liệu Word.
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .replaceAll("\\s+", " ")
                .strip();
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH) : s;
    }

    /**
     * Nhận diện ý định bằng luật, không bằng mô hình.
     *
     * <p>Chỉ nhận diện những ca <strong>rõ ràng</strong>. Nghi ngờ thì trả về tra cứu và để
     * truy hồi quyết định — chặn nhầm một câu hỏi thật tệ hơn nhiều so với việc chạy thừa một
     * lượt truy hồi cho một câu chào hỏi.
     */
    public AgentContext.Intent detectIntent(String normalized) {
        String lower = normalized.toLowerCase(VI);

        boolean shortEnoughToBeGreeting = lower.length() <= 40;
        if (shortEnoughToBeGreeting && GREETINGS.stream().anyMatch(lower::startsWith)) {
            return AgentContext.Intent.CHAO_HOI;
        }
        if (OUT_OF_SCOPE.stream().anyMatch(lower::contains)) {
            return AgentContext.Intent.NGOAI_PHAM_VI;
        }
        return AgentContext.Intent.TRA_CUU;
    }
}
