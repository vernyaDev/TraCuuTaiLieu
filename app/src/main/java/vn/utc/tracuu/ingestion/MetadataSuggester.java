package vn.utc.tracuu.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.knowledge.Category;
import vn.utc.tracuu.knowledge.CategoryRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.llm.OllamaClient;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đề xuất metadata cho tài liệu vừa nạp, mỗi giá trị kèm trích đoạn làm căn cứ.
 *
 * <h2>Điều quan trọng nhất ở lớp này: đối chiếu căn cứ</h2>
 *
 * <p>Mô hình ngôn ngữ trả về căn cứ cực kỳ thuyết phục cho những câu <em>không có trong tài
 * liệu</em>. Nếu tin thẳng, khâu xác nhận metadata trở thành thứ tệ hơn cả không có: quản trị
 * đọc một câu trích nghe rất đúng, bấm đồng ý, và một ngày hiệu lực sai đi vào kho — rồi mọi
 * cảnh báo hết hiệu lực về sau đều tính trên con số sai đó.
 *
 * <p>Vì vậy mọi trích đoạn đều được <strong>tìm lại trong văn bản gốc</strong>. Không tìm thấy
 * thì giá trị vẫn giữ nhưng bị đánh dấu chưa đối chiếu, và giao diện phải hiện khác đi. So khớp
 * có chuẩn hoá dấu và khoảng trắng, vì mô hình hay trả lại câu trích với dấu câu hơi khác.
 *
 * <p>Ngày tháng thì <strong>không hỏi mô hình trước</strong> — dò bằng biểu thức trên chính văn
 * bản, vì định dạng ngày tiếng Việt hữu hạn và dò trực tiếp thì không bao giờ bịa. Chỉ nhờ mô
 * hình cho những thứ cần đọc hiểu: tên tài liệu và danh mục.
 *
 * <p>Luật R7: việc gợi ý metadata và việc sinh câu trả lời dùng chung dịch vụ mô hình nhưng
 * khác hoàn toàn đường mã trả kết quả — đây là đầu dây bên gợi ý, nó không biết gì về
 * {@code agent/}.
 */
@Component
public class MetadataSuggester {

    private static final Logger log = LoggerFactory.getLogger(MetadataSuggester.class);
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    /** Số ký tự đầu tài liệu đưa cho mô hình. Metadata luôn nằm ở phần mở đầu. */
    private static final int HEAD_CHARS = 3000;

    private static final String SYSTEM = """
            Bạn đọc phần đầu một văn bản nội bộ và rút ra metadata.
            Trả về DUY NHẤT một đối tượng JSON, không giải thích, không bọc trong khối mã:
            {
              "title": "tên văn bản",
              "titleEvidence": "câu trích NGUYÊN VĂN từ văn bản chứa tên đó",
              "category": "một trong các danh mục được liệt kê",
              "categoryEvidence": "câu trích NGUYÊN VĂN làm căn cứ chọn danh mục"
            }
            Quy tắc bắt buộc:
            - Trường evidence phải là câu COPY NGUYÊN VĂN từ văn bản, không viết lại, không tóm tắt.
            - Không suy đoán. Không tìm thấy căn cứ cho trường nào thì để trường đó là null.
            - category phải chọn đúng một giá trị trong danh sách, không tự tạo giá trị mới.
            """;

    private final OllamaClient llm;
    private final CategoryRepository categories;
    private final ObjectMapper json;
    private final TracuuProperties props;

    public MetadataSuggester(OllamaClient llm, CategoryRepository categories,
                             ObjectMapper json, TracuuProperties props) {
        this.llm = llm;
        this.categories = categories;
        this.json = json;
        this.props = props;
    }

    public MetadataSuggestion suggest(ExtractedContent content, String filename) {
        if (content == null || content.plainText() == null || content.plainText().isBlank()) {
            return MetadataSuggestion.empty("Tài liệu không có văn bản để rút metadata.");
        }
        String fullText = content.plainText();
        String head = fullText.length() <= HEAD_CHARS ? fullText : fullText.substring(0, HEAD_CHARS);

        // Ngày tháng dò bằng biểu thức trên văn bản — chắc chắn không bịa.
        MetadataSuggestion.Suggested<LocalDate> from = findEffectiveFrom(fullText);
        MetadataSuggestion.Suggested<LocalDate> to = findEffectiveTo(fullText);
        MetadataSuggestion.Suggested<Boolean> noExpiry = findNoExpiry(fullText);
        MetadataSuggestion.Suggested<Integer> sensitivity = findSensitivity(head);

        MetadataSuggestion.Suggested<String> title = null;
        MetadataSuggestion.Suggested<String> category = null;
        List<String> notes = new ArrayList<>();

        try {
            List<String> names = categories.findAll().stream().map(Category::getName).toList();
            JsonNode parsed = json.readTree(stripFences(llm.generate(SYSTEM, buildPrompt(head, names))));

            title = verified(text(parsed, "title"), text(parsed, "titleEvidence"), fullText);
            String suggestedCategory = text(parsed, "category");
            // Mô hình tự tạo danh mục mới thì bỏ: danh mục là danh sách đóng do quản trị đặt.
            if (suggestedCategory != null && names.stream().noneMatch(suggestedCategory::equalsIgnoreCase)) {
                notes.add("Bỏ danh mục \"" + suggestedCategory + "\" vì không có trong danh sách.");
                suggestedCategory = null;
            }
            category = verified(suggestedCategory, text(parsed, "categoryEvidence"), fullText);

        } catch (OllamaClient.LlmUnavailableException e) {
            notes.add("Chưa gợi ý được tên và danh mục vì dịch vụ mô hình không sẵn sàng.");
            log.info("Bỏ bước gợi ý metadata cho {}: {}", filename, e.getMessage());
        } catch (Exception e) {
            notes.add("Chưa gợi ý được tên và danh mục vì kết quả mô hình không đọc được.");
            log.info("Kết quả gợi ý metadata cho {} không đọc được: {}", filename, e.getMessage());
        }

        // Tiêu đề bóc được từ cấu trúc tài liệu đáng tin hơn tiêu đề mô hình đoán — nó là chữ
        // thật trong tệp, không phải suy luận.
        if (content.detectedTitle() != null && !content.detectedTitle().isBlank()
                && (title == null || !title.verified())) {
            title = MetadataSuggestion.Suggested.of(
                    content.detectedTitle(), content.detectedTitle(), true);
        }

        MetadataSuggestion suggestion = new MetadataSuggestion(
                title, category, from, to, noExpiry, sensitivity,
                notes.isEmpty() ? List.of() : notes);

        List<String> unverified = suggestion.unverifiedFields();
        if (!unverified.isEmpty()) {
            log.debug("Tài liệu {}: {} trường không đối chiếu được căn cứ ({})",
                    filename, unverified.size(), String.join(", ", unverified));
        }
        return suggestion;
    }

    // ── Đối chiếu căn cứ ───────────────────────────────────────────────────────────────────

    private <T> MetadataSuggestion.Suggested<T> verified(T value, String evidence, String fullText) {
        if (value == null) return null;
        boolean ok = evidence != null && !evidence.isBlank() && containsLoosely(fullText, evidence);
        return MetadataSuggestion.Suggested.of(value, evidence, ok);
    }

    /**
     * Trích đoạn có nằm trong văn bản không, so khớp nới lỏng.
     *
     * <p>Nới lỏng ở ba chỗ: bỏ phân biệt hoa thường, gộp khoảng trắng, và bỏ dấu câu. Mô hình
     * thường trả lại câu trích đúng nội dung nhưng khác dấu phẩy hoặc thừa khoảng trắng — so
     * khớp tuyệt đối sẽ đánh dấu "chưa đối chiếu" cho những căn cứ hoàn toàn thật, và quản trị
     * sẽ nhanh chóng học cách bỏ qua cảnh báo đó.
     */
    static boolean containsLoosely(String haystack, String needle) {
        String h = canonical(haystack);
        String n = canonical(needle);
        if (n.length() < 12) return false;   // câu quá ngắn khớp bừa được, không tính là căn cứ
        return h.contains(n);
    }

    private static String canonical(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFC)
                .toLowerCase(VI)
                .replaceAll("[\\p{Punct}…“”„«»]", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    // ── Dò ngày tháng bằng biểu thức ──────────────────────────────────────────────────────

    private static final Pattern DATE = Pattern.compile(
            "(?<d>\\d{1,2})\\s*[/\\-.]\\s*(?<m>\\d{1,2})\\s*[/\\-.]\\s*(?<y>\\d{4})"
          + "|ngày\\s+(?<d2>\\d{1,2})\\s+tháng\\s+(?<m2>\\d{1,2})\\s+năm\\s+(?<y2>\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern FROM_CUE = Pattern.compile(
            "(có hiệu lực (kể )?từ|hiệu lực từ|áp dụng từ|thi hành (kể )?từ|kể từ ngày)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern TO_CUE = Pattern.compile(
            "(hết hiệu lực|có hiệu lực đến|hiệu lực đến|áp dụng đến|thời hạn đến)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern NO_EXPIRY_CUE = Pattern.compile(
            "(không (có )?thời hạn|vô thời hạn|cho đến khi có văn bản thay thế"
          + "|đến khi có quy định mới|hiệu lực vô thời hạn)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private MetadataSuggestion.Suggested<LocalDate> findEffectiveFrom(String text) {
        return findDateNear(text, FROM_CUE);
    }

    private MetadataSuggestion.Suggested<LocalDate> findEffectiveTo(String text) {
        return findDateNear(text, TO_CUE);
    }

    /**
     * Ngày nằm gần một cụm chỉ dấu.
     *
     * <p>Phải tìm theo cụm chỉ dấu chứ không lấy ngày đầu tiên gặp được: văn bản hành chính
     * luôn mở đầu bằng ngày ban hành, và ngày ban hành khác ngày có hiệu lực. Lấy bừa ngày đầu
     * tiên là cách chắc chắn gán sai cho gần như mọi văn bản.
     */
    private MetadataSuggestion.Suggested<LocalDate> findDateNear(String text, Pattern cue) {
        Matcher cueMatcher = cue.matcher(text);
        while (cueMatcher.find()) {
            int windowEnd = Math.min(text.length(), cueMatcher.end() + 120);
            String window = text.substring(cueMatcher.start(), windowEnd);

            Matcher dateMatcher = DATE.matcher(window);
            if (dateMatcher.find()) {
                LocalDate date = parse(dateMatcher);
                if (date != null) {
                    String evidence = window.replaceAll("\\s+", " ").strip();
                    if (evidence.length() > 200) evidence = evidence.substring(0, 200);
                    return MetadataSuggestion.Suggested.of(date, evidence, true);
                }
            }
        }
        return null;
    }

    private MetadataSuggestion.Suggested<Boolean> findNoExpiry(String text) {
        Matcher m = NO_EXPIRY_CUE.matcher(text);
        if (!m.find()) return null;
        int start = Math.max(0, m.start() - 60);
        int end = Math.min(text.length(), m.end() + 60);
        return MetadataSuggestion.Suggested.of(
                true, text.substring(start, end).replaceAll("\\s+", " ").strip(), true);
    }

    /**
     * Nhãn mật dò theo từ khoá đóng dấu trên văn bản.
     *
     * <p>Chỉ nâng lên mức cao hơn khi có dấu hiệu tường minh. Đoán nhãn mật quá tay sẽ khoá
     * tài liệu khỏi người đáng được đọc; đoán quá nhẹ thì lộ. Nên ở đây chỉ nhận dấu hiệu rõ,
     * còn lại để quản trị quyết — và mặc định là mức thấp nhất.
     */
    private MetadataSuggestion.Suggested<Integer> findSensitivity(String head) {
        String lower = head.toLowerCase(VI);
        int max = props.security().maxClearance();

        for (var entry : props.security().clearanceLabels().entrySet()) {
            String label = entry.getValue().toLowerCase(VI);
            int index = lower.indexOf(label);
            if (index >= 0 && entry.getKey() > 1) {
                int start = Math.max(0, index - 40);
                int end = Math.min(head.length(), index + label.length() + 40);
                return MetadataSuggestion.Suggested.of(
                        Math.min(entry.getKey(), max),
                        head.substring(start, end).replaceAll("\\s+", " ").strip(), true);
            }
        }
        return null;
    }

    private static LocalDate parse(Matcher m) {
        try {
            String d = m.group("d") != null ? m.group("d") : m.group("d2");
            String mo = m.group("m") != null ? m.group("m") : m.group("m2");
            String y = m.group("y") != null ? m.group("y") : m.group("y2");
            if (d == null || mo == null || y == null) return null;
            return LocalDate.parse(
                    String.format("%04d-%02d-%02d",
                            Integer.parseInt(y), Integer.parseInt(mo), Integer.parseInt(d)),
                    DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (RuntimeException e) {
            return null;   // ngày không hợp lệ kiểu 31/02 — bỏ qua thay vì đoán
        }
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────────────────

    private String buildPrompt(String head, List<String> categoryNames) {
        return "Danh mục được phép chọn: " + String.join(" | ", categoryNames)
             + System.lineSeparator() + System.lineSeparator()
             + "Phần đầu văn bản:" + System.lineSeparator()
             + head + System.lineSeparator() + System.lineSeparator()
             + "JSON:";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String s = value.asText().strip();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    /** Mô hình hay bọc JSON trong khối mã dù đã được yêu cầu không làm vậy. */
    static String stripFences(String raw) {
        if (raw == null) return "{}";
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            int fence = s.lastIndexOf("```");
            if (fence > 0) s = s.substring(0, fence);
        }
        int open = s.indexOf('{');
        int close = s.lastIndexOf('}');
        return open >= 0 && close > open ? s.substring(open, close + 1) : "{}";
    }
}
