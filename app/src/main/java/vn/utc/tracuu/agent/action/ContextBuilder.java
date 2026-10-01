package vn.utc.tracuu.agent.action;

import org.springframework.stereotype.Component;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dựng phần ngữ cảnh đưa vào mô hình từ các đoạn đã truy hồi.
 *
 * <p><strong>{@code context.max-tokens} là biến mà mọi ngưỡng độ trễ treo lên.</strong> Thời
 * gian xử lý lời nhắc tỉ lệ thuận với số token đưa vào, nên đo độ trễ với ngữ cảnh ngắn hơn giá
 * trị cấu hình là đo ở một điều kiện không tồn tại trong vận hành. Đây là lý do con số ấy được
 * khai là tham số hạng nhất chứ không nằm lẫn trong mã.
 *
 * <p>Giới hạn số đoạn mỗi tài liệu cũng có lý do cụ thể: một tài liệu dài có thể chiếm trọn
 * ngữ cảnh bằng năm đoạn liên tiếp, đẩy hết các nguồn khác ra ngoài. Câu trả lời khi đó chỉ
 * dựa trên một nguồn duy nhất mà vẫn trông như đã tổng hợp nhiều nguồn.
 */
@Component
public class ContextBuilder {

    /** Ước lượng token từ ký tự cho tiếng Việt. Dùng để CẮT, không dùng để báo cáo. */
    private static final double CHARS_PER_TOKEN = 3.2;

    private final TracuuProperties props;

    public ContextBuilder(TracuuProperties props) {
        this.props = props;
    }

    /** Ngữ cảnh đã dựng, kèm đúng những đoạn thật sự lọt vào. */
    public record Context(String text, List<RetrievedChunk> included, int estimatedTokens) {}

    /**
     * Dựng ngữ cảnh, giữ thứ tự xếp hạng.
     *
     * <p>Chỉ những đoạn <strong>thật sự lọt vào</strong> mới được trả về. Đây là điều quan
     * trọng: trích dẫn phải khớp với thứ mô hình đã đọc. Trích dẫn một đoạn bị cắt khỏi ngữ
     * cảnh là trích dẫn một nguồn mà câu trả lời không hề dựa vào — sai nghiêm trọng hơn là
     * thiếu trích dẫn, vì nó trông đúng.
     */
    public Context build(List<RetrievedChunk> chunks) {
        int budget = props.context().maxTokens();
        int perDocument = props.context().maxChunksPerDocument();

        StringBuilder sb = new StringBuilder();
        List<RetrievedChunk> included = new ArrayList<>();
        Map<Long, Integer> usedPerDocument = new HashMap<>();
        int used = 0;

        for (RetrievedChunk c : chunks) {
            int already = usedPerDocument.getOrDefault(c.documentId(), 0);
            if (already >= perDocument) continue;

            String block = render(included.size() + 1, c);
            int cost = estimateTokens(block);
            if (used + cost > budget) {
                // Hết chỗ. Dừng hẳn thay vì bỏ qua đoạn này để thử đoạn sau — thứ tự xếp hạng
                // có nghĩa, và lấy đoạn hạng 8 sau khi bỏ hạng 5 là phá thứ tự đó.
                break;
            }
            sb.append(block).append(System.lineSeparator()).append(System.lineSeparator());
            included.add(c);
            usedPerDocument.put(c.documentId(), already + 1);
            used += cost;
        }

        return new Context(sb.toString().stripTrailing(), included, used);
    }

    /**
     * Một đoạn trong ngữ cảnh, đánh số để mô hình trích dẫn được.
     *
     * <p>Số hiệu là số thứ tự <em>trong ngữ cảnh</em>, không phải mã đoạn trong cơ sở dữ liệu.
     * Mô hình đánh số nhỏ đáng tin hơn hẳn việc chép lại một mã dài, và tầng gắn trích dẫn sẽ
     * ánh xạ ngược về mã thật.
     */
    private String render(int index, RetrievedChunk c) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Nguồn ").append(index).append("] ").append(c.documentTitle());
        if (c.breadcrumb() != null && !c.breadcrumb().isBlank()) {
            sb.append(" — ").append(c.breadcrumb());
        }
        if (c.fromTable() && c.tablePart() != null && c.tablePartsTotal() != null) {
            sb.append(" (bảng, mảnh ").append(c.tablePart())
              .append('/').append(c.tablePartsTotal()).append(')');
        }
        sb.append(System.lineSeparator()).append(c.content().strip());
        return sb.toString();
    }

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
