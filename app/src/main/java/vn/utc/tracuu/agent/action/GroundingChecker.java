package vn.utc.tracuu.agent.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.ingestion.VietnameseSegmenter;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tầng chặn thứ hai — kiểm mọi ý trong câu trả lời có gắn được về đoạn nguồn không.
 *
 * <p>Tầng một chặn khi <em>chưa có</em> vật liệu. Tầng này chặn khi <em>có vật liệu nhưng câu
 * trả lời không dùng nó</em> — tức mô hình đã tự bịa. Đây là hai loại lỗi khác nhau và cần hai
 * chỗ chặn khác nhau; gộp làm một thì loại thứ hai không ai bắt.
 *
 * <p><strong>Cách kiểm: chồng lấn từ vựng theo từng câu.</strong> Không gọi mô hình để tự chấm
 * mình — một mô hình đánh giá chính đầu ra của nó là phép kiểm yếu nhất có thể, và nó lại tốn
 * thêm một lượt chờ nữa vào đúng chỗ độ trễ đang chật. Cách ở đây tất định, chạy trong vài
 * mili giây, và tái lập được y hệt giữa hai lượt chạy thí nghiệm.
 *
 * <p>Điều phép kiểm này <strong>không</strong> làm được, nói thẳng ra: nó bắt được câu bịa hoàn
 * toàn, nhưng không bắt được câu dùng đúng từ của nguồn mà <em>đảo nghĩa</em> — ví dụ bỏ mất
 * chữ "không". Loại lỗi ấy cần người đọc đối chiếu, và đó là lý do trích dẫn phải chỉ tới đúng
 * đoạn chứ không chỉ tới tên tài liệu.
 */
@Component
public class GroundingChecker {

    private static final Logger log = LoggerFactory.getLogger(GroundingChecker.class);

    /** Câu ngắn hơn ngưỡng này thường là câu nối, không mang mệnh đề cần kiểm. */
    private static final int MIN_WORDS_TO_CHECK = 6;

    private final VietnameseSegmenter segmenter;
    private final TracuuProperties props;

    public GroundingChecker(VietnameseSegmenter segmenter, TracuuProperties props) {
        this.segmenter = segmenter;
        this.props = props;
    }

    /** Kết quả kiểm: tỉ lệ câu gắn được, và những câu không gắn được. */
    public record Result(boolean grounded, double supportedRatio,
                         int checkedSentences, List<String> ungrounded) {}

    public Result check(String answer, List<RetrievedChunk> sources) {
        if (!props.agent().grounding().enabled()) {
            return new Result(true, 1.0, 0, List.of());
        }
        if (answer == null || answer.isBlank() || sources.isEmpty()) {
            return new Result(false, 0, 0, List.of());
        }

        Set<String> sourceTokens = new HashSet<>();
        for (RetrievedChunk c : sources) {
            sourceTokens.addAll(segmenter.tokenize(c.content()));
            if (c.breadcrumb() != null) sourceTokens.addAll(segmenter.tokenize(c.breadcrumb()));
            sourceTokens.addAll(segmenter.tokenize(c.documentTitle()));
        }

        List<String> sentences = splitSentences(answer);
        List<String> ungrounded = new java.util.ArrayList<>();
        int checked = 0;
        int supported = 0;

        for (String sentence : sentences) {
            List<String> tokens = segmenter.tokenize(sentence).stream()
                    .filter(t -> t.length() > 1)
                    .toList();
            if (tokens.size() < MIN_WORDS_TO_CHECK) continue;

            checked++;
            long hits = tokens.stream().filter(sourceTokens::contains).count();
            double ratio = (double) hits / tokens.size();

            if (ratio >= props.agent().grounding().minSentenceSupport()) {
                supported++;
            } else {
                ungrounded.add(sentence.strip());
            }
        }

        if (checked == 0) {
            // Câu trả lời toàn câu ngắn. Bản trước trả về "đã gắn nguồn" ở đây, tức là mặc
            // định TIN khi không có gì để kiểm — và đó chính là lỗ hổng: một câu trả lời đủ
            // ngắn thì đi thẳng qua tầng chặn thứ hai mà không bị kiểm dòng nào.
            //
            // Đo ngày 30/09: hỏi "Cách nấu phở bò ngon nhất là gì?" trên kho quy chế nội bộ,
            // mô hình trả "Không đủ căn cứ" — bốn từ, dưới ngưỡng nên không kiểm — và hệ thống
            // phát nó ra như một CÂU TRẢ LỜI kèm tám trích dẫn. Người đọc thấy tám thẻ nguồn
            // gắn vào một câu không trả lời gì, tưởng tám nguồn đó có liên quan.
            //
            // Cách sửa không phải là đảo thành "không gắn được" — như thế sẽ chối cả những câu
            // trả lời ngắn HỢP LỆ ("Mười hai ngày làm việc."). Thay vào đó bỏ ngưỡng độ dài và
            // kiểm TOÀN BỘ câu trả lời như một khối: câu ngắn hợp lệ vẫn trùng từ với nguồn,
            // còn câu "không đủ căn cứ" thì không.
            List<String> toanBo = segmenter.tokenize(answer).stream()
                    .filter(t -> t.length() > 1)
                    .toList();
            if (toanBo.isEmpty()) return new Result(false, 0, 0, List.of(answer.strip()));

            long trung = toanBo.stream().filter(sourceTokens::contains).count();
            double tyLe = (double) trung / toanBo.size();
            boolean dat = tyLe >= props.agent().grounding().minSentenceSupport();
            if (!dat) {
                log.info("Câu trả lời ngắn không gắn được về nguồn: {}/{} từ trùng nguồn",
                        trung, toanBo.size());
            }
            return new Result(dat, tyLe, 1, dat ? List.of() : List.of(answer.strip()));
        }

        double ratio = (double) supported / checked;
        boolean grounded = supported > 0 && ratio >= 0.5;

        if (!grounded) {
            log.info("Câu trả lời KHÔNG gắn được về nguồn: {}/{} câu có căn cứ", supported, checked);
        }
        return new Result(grounded, ratio, checked, ungrounded);
    }

    private List<String> splitSentences(String text) {
        return List.of(text.split("(?<=[.!?;])\\s+|\\R+")).stream()
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
