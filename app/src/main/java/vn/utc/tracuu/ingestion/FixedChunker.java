package vn.utc.tracuu.ingestion;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Tách đoạn theo độ dài cố định có phần chồng lấn — <strong>bản nền để đối chiếu</strong>.
 *
 * <p>Chiến lược này tồn tại không phải vì nó tốt, mà vì US-030 đòi so sánh bằng số liệu. Không
 * có bản nền thì câu <em>"tách bám cấu trúc cho kết quả tốt hơn"</em> là một khẳng định không
 * có gì chống lưng. Đây là cách tách phổ biến nhất trong các hệ thống cùng loại, nên nó cũng
 * là mốc so sánh mà người phản biện sẽ nghĩ tới đầu tiên.
 *
 * <p>Phần chồng lấn giữa hai đoạn liền nhau là thứ bù lại điểm yếu lớn nhất của cách này: một
 * ý bị cắt ngang vẫn còn nguyên vẹn ở một trong hai đoạn.
 */
@Component
public class FixedChunker implements Chunker {

    private final VietnameseSegmenter segmenter;

    public FixedChunker(VietnameseSegmenter segmenter) {
        this.segmenter = segmenter;
    }

    @Override
    public String name() {
        return "fixed";
    }

    @Override
    public List<ChunkCandidate> chunk(ExtractedContent content, TracuuProperties.ChunkStrategy params) {
        List<ChunkCandidate> out = new ArrayList<>();
        String text = content.plainText();
        if (text == null || text.isBlank()) return out;

        // Cắt ở ranh giới câu gần nhất thay vì cắt giữa chữ: cùng một ngưỡng độ dài nhưng
        // đoạn đọc được, và vector của nó không bị nhiễu bởi nửa câu cụt.
        List<int[]> sentences = sentenceBounds(text);
        if (sentences.isEmpty()) return out;

        int i = 0;
        while (i < sentences.size()) {
            int start = sentences.get(i)[0];
            int end = start;
            int tokens = 0;
            int j = i;

            while (j < sentences.size()) {
                int[] s = sentences.get(j);
                int t = segmenter.tokenize(text.substring(s[0], s[1])).size();
                if (tokens > 0 && tokens + t > params.maxTokens()) break;
                tokens += t;
                end = s[1];
                j++;
            }
            if (j == i) {  // một câu dài hơn cả ngưỡng: vẫn phải lấy, không bỏ
                end = sentences.get(i)[1];
                j = i + 1;
            }

            out.add(ChunkCandidate.text(text.substring(start, end).strip(), "", start, end));

            if (j >= sentences.size()) break;
            i = backtrackForOverlap(text, sentences, i, j, params.overlapTokens());
        }
        return out;
    }

    /** Lùi lại vài câu để hai đoạn liền nhau chồng lấn khoảng {@code overlapTokens} token. */
    private int backtrackForOverlap(String text, List<int[]> sentences, int from, int next, int overlapTokens) {
        if (overlapTokens <= 0) return next;
        int tokens = 0;
        int k = next - 1;
        while (k > from && tokens < overlapTokens) {
            int[] s = sentences.get(k);
            tokens += segmenter.tokenize(text.substring(s[0], s[1])).size();
            k--;
        }
        return Math.max(from + 1, k + 1);
    }

    /** Ranh giới câu theo dấu chấm câu và xuống dòng. */
    private List<int[]> sentenceBounds(String text) {
        List<int[]> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean terminal = c == '.' || c == '!' || c == '?' || c == '\n' || c == ';';
            if (!terminal) continue;
            int end = i + 1;
            if (end - start > 1) out.add(new int[]{start, end});
            start = end;
        }
        if (start < text.length()) out.add(new int[]{start, text.length()});
        return out;
    }
}
