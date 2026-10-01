package vn.utc.tracuu.ingestion;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tách từ tiếng Việt, chạy hoàn toàn trong máy ảo Java.
 *
 * <p><strong>Vì sao khâu này tồn tại.</strong> Tiếng Việt viết rời từng âm tiết: <em>"bảo hiểm
 * xã hội"</em> là <em>một</em> khái niệm nhưng là <em>bốn</em> token nếu cắt theo khoảng trắng.
 * Chỉ mục toàn văn cắt theo khoảng trắng sẽ cho <em>"xã"</em> và <em>"hội"</em> cùng trọng số
 * với những từ mang nghĩa, và câu hỏi về bảo hiểm xã hội sẽ khớp nhầm mọi văn bản có chữ
 * <em>"hội"</em>. Nối âm tiết thành từ ghép là thứ sửa đúng chuyện đó.
 *
 * <p><strong>Vì sao không dùng thư viện gốc.</strong> Các bộ tách từ tiếng Việt phổ biến đều
 * cần thư viện biên dịch sẵn theo nền tảng. Trên máy dựng không ra được mạng, hoặc trên máy
 * dùng nền tảng khác, chúng hỏng theo kiểu khó chẩn đoán. Bản khớp-dài-nhất ở đây chỉ cần một
 * tệp từ điển văn bản nên luôn chạy — đổi lại, chất lượng phụ thuộc độ phủ của từ điển, và đó
 * là một đánh đổi nói thẳng chứ không giấu.
 *
 * <p>Đây chính là ẩn số mà kế hoạch xếp vào nhóm <em>phải đo sớm</em>. Cách đo: so độ chính xác
 * truy hồi giữa {@code whitespace} và {@code longest-match} trên cùng bộ câu hỏi chuẩn — hai
 * lượt chạy của bộ đo, cùng một tập đoạn.
 */
@Component
public class VietnameseSegmenter {

    private static final Logger log = LoggerFactory.getLogger(VietnameseSegmenter.class);
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    /** Từ ghép dài nhất tính theo âm tiết — quá ngưỡng này thì khớp dài nhất mất nhiều hơn được. */
    private static final int MAX_SYLLABLES = 4;

    private final TracuuProperties props;
    private final Set<String> dictionary = new HashSet<>();
    private boolean longestMatch;

    public VietnameseSegmenter(TracuuProperties props) {
        this.props = props;
    }

    @PostConstruct
    void load() {
        longestMatch = "longest-match".equalsIgnoreCase(props.segmentation().strategy());
        if (!longestMatch) {
            log.info("Tách từ: cắt theo khoảng trắng (bản nền để đối chiếu).");
            return;
        }
        String location = props.segmentation().dictionary();
        try {
            Resource res = new DefaultResourceLoader().getResource(location);
            if (!res.exists()) {
                log.warn("Không thấy từ điển tách từ tại {} — lùi về cắt theo khoảng trắng. "
                        + "Nhánh từ khoá sẽ kém chính xác với từ ghép tiếng Việt.", location);
                longestMatch = false;
                return;
            }
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String w = line.strip().toLowerCase(VI);
                    if (w.isEmpty() || w.charAt(0) == '#') continue;
                    if (w.indexOf(' ') > 0) dictionary.add(w);
                }
            }
            log.info("Tách từ: khớp dài nhất, từ điển {} từ ghép.", dictionary.size());
        } catch (Exception e) {
            log.warn("Đọc từ điển thất bại ({}) — lùi về cắt theo khoảng trắng.", e.getMessage());
            longestMatch = false;
        }
    }

    /**
     * Tách văn bản thành chuỗi token, từ ghép nối bằng dấu gạch dưới.
     *
     * <p>Nối bằng gạch dưới thay vì giữ khoảng trắng là có chủ đích: chỉ mục toàn văn của cơ sở
     * dữ liệu cắt theo khoảng trắng, nên chỉ cách này mới giữ được từ ghép thành <em>một</em>
     * token khi xuống tới tầng chỉ mục.
     */
    public String segment(String text) {
        if (text == null || text.isBlank()) return "";
        List<String> tokens = tokenize(text);
        return String.join(" ", tokens);
    }

    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> syllables = splitSyllables(text.toLowerCase(VI));
        if (!longestMatch) return syllables;

        List<String> out = new ArrayList<>(syllables.size());
        int i = 0;
        while (i < syllables.size()) {
            int matched = 1;
            // Khớp DÀI NHẤT: thử cụm dài trước, lùi dần. Thử ngắn trước sẽ cắt
            // "bảo hiểm xã hội" thành "bảo hiểm" + "xã hội" và mất khái niệm gốc.
            for (int len = Math.min(MAX_SYLLABLES, syllables.size() - i); len >= 2; len--) {
                String candidate = String.join(" ", syllables.subList(i, i + len));
                if (dictionary.contains(candidate)) {
                    out.add(candidate.replace(' ', '_'));
                    matched = len;
                    break;
                }
            }
            if (matched == 1) out.add(syllables.get(i));
            i += matched;
        }
        return out;
    }

    /**
     * Cắt thành âm tiết, bỏ dấu câu, giữ số và ký tự có dấu tiếng Việt.
     *
     * <p>Chuẩn hoá về dạng tổ hợp sẵn trước khi cắt: cùng một chữ "ệ" có hai cách mã hoá Unicode,
     * và hai cách đó cho hai token khác nhau ở tầng chỉ mục — một lỗi không nhìn thấy được bằng
     * mắt mà làm hỏng mọi phép so khớp.
     */
    private List<String> splitSyllables(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else if (c == '_' || c == '/' || c == '-') {
                // Giữ nguyên trong mã hiệu văn bản: "15/2020/NĐ-CP" là MỘT định danh.
                if (!current.isEmpty()) current.append(c);
            } else {
                flush(current, out);
            }
        }
        flush(current, out);
        return out;
    }

    private void flush(StringBuilder current, List<String> out) {
        if (current.isEmpty()) return;
        String token = current.toString();
        while (!token.isEmpty() && !Character.isLetterOrDigit(token.charAt(token.length() - 1))) {
            token = token.substring(0, token.length() - 1);
        }
        if (!token.isEmpty()) out.add(token);
        current.setLength(0);
    }

    /** Có đang dùng khớp dài nhất không — bộ đo ghi giá trị này vào cấu hình của lượt chạy. */
    public boolean isLongestMatch() {
        return longestMatch;
    }

    public int dictionarySize() {
        return dictionary.size();
    }
}
