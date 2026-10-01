package vn.utc.tracuu.ingestion;

import java.util.List;

/**
 * Một bảng bóc được, giữ nguyên quan hệ hàng–cột.
 *
 * <p>{@code header} tách khỏi {@code rows} vì US-012 đòi khi một bảng dài phải chia thành nhiều
 * đoạn thì <strong>dòng tiêu đề được lặp lại</strong> ở từng mảnh. Không tách thì mảnh thứ hai
 * trở đi là một khối số không ai đọc được nghĩa.
 */
public record ExtractedTable(List<String> header, List<List<String>> rows, String caption) {

    public boolean hasHeader() {
        return header != null && !header.isEmpty();
    }

    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }

    /** Kết xuất dạng văn bản có dấu phân cách, giữ được cột khi đưa vào ngữ cảnh mô hình. */
    public String render(List<List<String>> subsetRows) {
        StringBuilder sb = new StringBuilder();
        if (caption != null && !caption.isBlank()) sb.append(caption).append('\n');
        if (hasHeader()) sb.append(String.join(" | ", header)).append('\n');
        for (List<String> row : subsetRows) sb.append(String.join(" | ", row)).append('\n');
        return sb.toString().stripTrailing();
    }

    public String renderAll() {
        return render(rows == null ? List.of() : rows);
    }
}
