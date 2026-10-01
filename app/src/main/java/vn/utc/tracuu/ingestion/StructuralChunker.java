package vn.utc.tracuu.ingestion;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Tách đoạn <strong>bám cấu trúc tài liệu</strong>: không cắt ngang một đầu mục, và mỗi đoạn
 * mang theo đường dẫn phân cấp của chỗ nó nằm.
 *
 * <p>Ba điều chiến lược này làm khác hẳn cách cắt theo độ dài cố định:
 *
 * <ol>
 *   <li><strong>Ranh giới đoạn trùng ranh giới đầu mục.</strong> Cắt giữa Điều 113 thì nửa sau
 *       mất chủ ngữ — nó nói "trong trường hợp này" mà không còn "trường hợp" nào ở gần.</li>
 *   <li><strong>Đường dẫn phân cấp đi theo từng đoạn.</strong> Đây là thứ làm trích dẫn chỉ
 *       được tới <em>Chương VII › Điều 113 › Khoản 2</em> thay vì chỉ tới tên tài liệu.</li>
 *   <li><strong>Bảng không bị chia giữa hàng, và mảnh nào cũng có dòng tiêu đề.</strong> Chia
 *       giữa hàng làm mất quan hệ cột; thiếu dòng tiêu đề làm mảnh thứ hai trở đi thành một
 *       khối số vô nghĩa.</li>
 * </ol>
 *
 * <p>Đoạn quá ngắn được gộp với đoạn kế tiếp cùng đầu mục ({@code minTokens}) — một đoạn hai
 * dòng thì vector của nó không mang đủ ngữ cảnh để khớp với câu hỏi nào.
 */
@Component
public class StructuralChunker implements Chunker {

    private static final String SEP = " › ";

    private final VietnameseSegmenter segmenter;

    public StructuralChunker(VietnameseSegmenter segmenter) {
        this.segmenter = segmenter;
    }

    @Override
    public String name() {
        return "structural";
    }

    @Override
    public List<ChunkCandidate> chunk(ExtractedContent content, TracuuProperties.ChunkStrategy params) {
        List<ChunkCandidate> out = new ArrayList<>();
        String[] stack = new String[7];

        StringBuilder buffer = new StringBuilder();
        int bufferStart = -1;
        int bufferEnd = -1;
        String bufferCrumb = "";

        for (TextBlock block : content.blocks()) {

            if (block.isHeading()) {
                // Đầu mục mới: chốt những gì đang gom, rồi cập nhật đường dẫn.
                bufferStart = flush(out, buffer, bufferCrumb, bufferStart, bufferEnd, params);
                int level = Math.max(1, Math.min(6, block.level()));
                stack[level] = block.text().strip();
                for (int deeper = level + 1; deeper < stack.length; deeper++) stack[deeper] = null;
                bufferCrumb = crumb(stack);
                continue;
            }

            String crumb = crumb(stack);

            if (block.isTable()) {
                bufferStart = flush(out, buffer, bufferCrumb, bufferStart, bufferEnd, params);
                out.addAll(splitTable(block, crumb, params));
                bufferCrumb = crumb;
                continue;
            }

            String text = block.text().strip();
            if (text.isEmpty()) continue;

            int wouldBe = countTokens(buffer.length() == 0 ? text : buffer + " " + text);
            if (!buffer.isEmpty() && wouldBe > params.maxTokens()) {
                bufferStart = flush(out, buffer, bufferCrumb, bufferStart, bufferEnd, params);
            }

            if (buffer.isEmpty()) {
                bufferStart = block.charStart();
                bufferCrumb = crumb;
            } else {
                buffer.append(System.lineSeparator());
            }
            buffer.append(text);
            bufferEnd = block.charEnd();
        }

        flush(out, buffer, bufferCrumb, bufferStart, bufferEnd, params);
        return mergeTooShort(out, params);
    }

    /**
     * Chia một bảng thành các mảnh vừa ngưỡng, <strong>lặp dòng tiêu đề ở mọi mảnh</strong> và
     * luôn cắt ở ranh giới hàng.
     */
    private List<ChunkCandidate> splitTable(TextBlock block, String crumb,
                                            TracuuProperties.ChunkStrategy params) {
        ExtractedTable table = block.table();
        List<ChunkCandidate> pieces = new ArrayList<>();

        if (table == null || table.rowCount() == 0) {
            pieces.add(ChunkCandidate.text(block.text(), crumb, block.charStart(), block.charEnd()));
            return pieces;
        }

        int headerTokens = table.hasHeader() ? countTokens(String.join(" ", table.header())) : 0;
        List<List<String>> current = new ArrayList<>();
        List<List<List<String>>> groups = new ArrayList<>();
        int tokens = headerTokens;

        for (List<String> row : table.rows()) {
            int rowTokens = countTokens(String.join(" ", row));
            if (!current.isEmpty() && tokens + rowTokens > params.maxTokens()) {
                groups.add(current);
                current = new ArrayList<>();
                tokens = headerTokens;     // dòng tiêu đề lặp lại nên tính lại từ đầu
            }
            current.add(row);
            tokens += rowTokens;
        }
        if (!current.isEmpty()) groups.add(current);

        int total = groups.size();
        for (int i = 0; i < total; i++) {
            String rendered = table.render(groups.get(i));
            pieces.add(ChunkCandidate.tablePiece(
                    rendered, crumb, block.charStart(), block.charEnd(), i + 1, total));
        }
        return pieces;
    }

    /**
     * Gộp đoạn quá ngắn với đoạn liền sau <strong>cùng đường dẫn phân cấp</strong>.
     *
     * <p>Điều kiện cùng đường dẫn là quan trọng: gộp hai đoạn khác đầu mục thì đoạn kết quả
     * mang một đường dẫn sai cho một nửa nội dung của chính nó.
     */
    private List<ChunkCandidate> mergeTooShort(List<ChunkCandidate> input,
                                               TracuuProperties.ChunkStrategy params) {
        if (params.minTokens() <= 0 || input.size() < 2) return input;

        List<ChunkCandidate> out = new ArrayList<>();
        int i = 0;
        while (i < input.size()) {
            ChunkCandidate c = input.get(i);
            if (c.table() || countTokens(c.content()) >= params.minTokens() || i == input.size() - 1) {
                out.add(c);
                i++;
                continue;
            }
            ChunkCandidate next = input.get(i + 1);
            boolean sameSection = java.util.Objects.equals(c.breadcrumb(), next.breadcrumb());
            if (next.table() || !sameSection
                    || countTokens(c.content() + " " + next.content()) > params.maxTokens()) {
                out.add(c);
                i++;
                continue;
            }
            input.set(i + 1, ChunkCandidate.text(
                    c.content() + System.lineSeparator() + next.content(),
                    c.breadcrumb(), c.charStart(), next.charEnd()));
            i++;
        }
        return out;
    }

    private int flush(List<ChunkCandidate> out, StringBuilder buffer, String crumb,
                      int start, int end, TracuuProperties.ChunkStrategy params) {
        if (buffer.isEmpty()) return -1;
        out.add(ChunkCandidate.text(buffer.toString(), crumb, Math.max(0, start), Math.max(0, end)));
        buffer.setLength(0);
        return -1;
    }

    private String crumb(String[] stack) {
        StringBuilder sb = new StringBuilder();
        for (String s : stack) {
            if (s == null || s.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(SEP);
            sb.append(s.length() > 80 ? s.substring(0, 77) + "…" : s);
        }
        return sb.toString();
    }

    private int countTokens(String text) {
        return segmenter.tokenize(text).size();
    }
}
