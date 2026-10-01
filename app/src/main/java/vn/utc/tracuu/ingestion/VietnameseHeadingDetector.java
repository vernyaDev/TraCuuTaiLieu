package vn.utc.tracuu.ingestion;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhận diện tiêu đề phân cấp trong văn bản hành chính và pháp quy tiếng Việt.
 *
 * <p>Đây là thứ làm nên đường dẫn phân cấp <em>Chương VII › Điều 113 › Khoản 2</em> trong trích
 * dẫn. Không có nó thì trích dẫn chỉ chỉ được tới tên tài liệu, và người đọc phải tự mở tệp gốc
 * dò tay — tức trích dẫn mất gần hết giá trị.
 *
 * <p>Thứ bậc bám cấu trúc văn bản quy phạm Việt Nam: <strong>Phần › Chương › Mục › Điều ›
 * Khoản › Điểm</strong>. Tài liệu không theo cấu trúc ấy (quy trình nội bộ, hướng dẫn) rơi về
 * đánh số thường {@code 1.} / {@code 1.1.} — vẫn có phân cấp, chỉ là nông hơn.
 */
@Component
public class VietnameseHeadingDetector {

    /**
     * Độ dài tối đa của một dòng còn được coi là tiêu đề.
     *
     * <p>Con số này cố ý <strong>chặt</strong>, vì hai hướng sai ở đây không cân nhau:
     * <ul>
     *   <li>Bỏ sót một tiêu đề dài làm mất <em>một</em> bậc trong đường dẫn phân cấp của các
     *       đoạn thuộc mục đó. Khó chịu, nhưng cục bộ.</li>
     *   <li>Nhận nhầm một câu văn xuôi ("Điều 5 của quy chế này quy định rằng…") làm tiêu đề
     *       thì câu ấy <em>cướp</em> đường dẫn phân cấp, và <strong>mọi đoạn phía sau nó</strong>
     *       mang sai đường dẫn cho tới đầu mục tiếp theo. Trích dẫn khi đó chỉ sai chỗ một cách
     *       thuyết phục — tệ hơn hẳn trích dẫn thiếu chi tiết.</li>
     * </ul>
     * Tiêu đề pháp quy thật gần như luôn dưới ngưỡng này.
     */
    private static final int MAX_HEADING_LENGTH = 150;

    private record Rule(Pattern pattern, int level, String label) {}

    /**
     * Cờ so khớp không phân biệt hoa thường, <strong>có hiệu lực cả với ký tự tiếng Việt</strong>.
     *
     * <p>{@link Pattern#CASE_INSENSITIVE} một mình chỉ áp cho chữ cái ASCII. Thiếu
     * {@link Pattern#UNICODE_CASE} thì {@code "Chương VII"} viết thường không khớp mẫu
     * {@code CHƯƠNG} — vì {@code Ư} và {@code ư} nằm ngoài ASCII. Hệ quả: mọi tiêu đề viết
     * thường hoặc viết hoa đầu từ đều tụt xuống luật dự phòng và mang sai cấp, kéo theo toàn
     * bộ đường dẫn phân cấp lệch — mà không có lỗi nào báo ra.
     */
    private static final int VI_IGNORE_CASE = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    /**
     * Số đếm và số thứ tự viết bằng chữ.
     *
     * <p>Phải có cả hai dạng: văn bản pháp quy Việt Nam viết cả <em>"Phần thứ nhất"</em> (số
     * thứ tự) lẫn <em>"Phần một"</em> (số đếm), và chúng xuất hiện lẫn lộn ngay trong cùng một
     * bộ tài liệu. Thiếu một dạng thì tiêu đề cấp cao nhất rơi xuống luật dự phòng và mang sai
     * cấp, kéo theo toàn bộ đường dẫn phân cấp phía dưới lệch một bậc.
     */
    private static final String SO_VIET_CHU =
            "NHẤT|HAI|BA|TƯ|BỐN|NĂM|SÁU|BẢY|TÁM|CHÍN|MƯỜI|MỘT";

    private static final Rule[] RULES = {
            new Rule(Pattern.compile(
                    "^PHẦN\\s+(THỨ\\s+)?([IVXLCDM]+|\\d+|[A-ZĐ]|" + SO_VIET_CHU + ")\\b.*",
                    VI_IGNORE_CASE), 1, "Phần"),
            new Rule(Pattern.compile("^CHƯƠNG\\s+([IVXLCDM]+|\\d+)\\b.*",
                    VI_IGNORE_CASE), 2, "Chương"),
            new Rule(Pattern.compile("^MỤC\\s+(\\d+|[IVXLCDM]+)\\b.*",
                    VI_IGNORE_CASE), 3, "Mục"),
            new Rule(Pattern.compile("^ĐIỀU\\s+(\\d+)\\b.*",
                    VI_IGNORE_CASE), 4, "Điều"),
            new Rule(Pattern.compile("^KHOẢN\\s+(\\d+)\\b.*",
                    VI_IGNORE_CASE), 5, "Khoản"),
            new Rule(Pattern.compile("^ĐIỂM\\s+([a-zđ])\\b.*",
                    VI_IGNORE_CASE), 6, "Điểm"),
    };

    /** Đánh số thường: {@code 1.} · {@code 1.2.} · {@code 1.2.3.} — cấp theo số dấu chấm. */
    private static final Pattern NUMBERED = Pattern.compile("^(\\d+(?:\\.\\d+){0,3})\\.?\\s+\\S.*");

    /** Đề mục chữ cái: {@code a)} · {@code b.} — luôn là cấp sâu nhất. */
    private static final Pattern LETTERED = Pattern.compile("^([a-zđ])[).]\\s+\\S.*");

    /**
     * Cấp của một dòng nếu nó là tiêu đề, hoặc 0 nếu không phải.
     *
     * <p>Chặn theo độ dài là có chủ đích: một câu dài bắt đầu bằng "Điều 5 quy định rằng…" là
     * văn xuôi nhắc tới điều luật, không phải đầu mục. Thiếu chặn này thì đường dẫn phân cấp bị
     * một câu giữa bài cướp mất, và mọi đoạn phía sau mang sai đường dẫn.
     */
    public int levelOf(String rawLine) {
        if (rawLine == null) return 0;
        String line = rawLine.strip();
        if (line.isEmpty() || line.length() > MAX_HEADING_LENGTH) return 0;

        for (Rule r : RULES) {
            if (r.pattern().matcher(line).matches()) return r.level();
        }

        Matcher numbered = NUMBERED.matcher(line);
        if (numbered.matches() && line.length() <= 150) {
            long dots = numbered.group(1).chars().filter(c -> c == '.').count();
            return (int) Math.min(6, 3 + dots);
        }

        Matcher lettered = LETTERED.matcher(line);
        if (lettered.matches() && line.length() <= 150) return 6;

        // Dòng viết HOA toàn bộ, ngắn, không kết thúc bằng dấu câu: đầu mục của tài liệu
        // không theo chuẩn pháp quy.
        if (line.length() <= 90 && line.equals(line.toUpperCase(new Locale("vi", "VN")))
                && line.chars().anyMatch(Character::isLetter)
                && !line.endsWith(".") && !line.endsWith(",") && !line.endsWith(":")) {
            return 2;
        }
        return 0;
    }

    public boolean isHeading(String line) {
        return levelOf(line) > 0;
    }

    /**
     * Rút gọn tiêu đề để đưa vào đường dẫn phân cấp.
     *
     * <p>Giữ phần định danh (<em>Điều 113</em>) và cắt phần mô tả dài phía sau — đường dẫn phân
     * cấp nằm trên mỗi đoạn nên nó phải ngắn, còn nội dung đầy đủ thì đã nằm trong chính đoạn.
     */
    public String shorten(String heading) {
        String h = heading.strip().replaceAll("\\s+", " ");
        int cut = Math.min(indexOrMax(h, ". "), Math.min(indexOrMax(h, " – "), indexOrMax(h, " - ")));
        if (cut < h.length() && cut > 3) h = h.substring(0, cut);
        return h.length() > 80 ? h.substring(0, 77) + "…" : h;
    }

    private static int indexOrMax(String s, String needle) {
        int i = s.indexOf(needle);
        return i < 0 ? s.length() : i;
    }
}
