package vn.utc.tracuu.ingestion;

/**
 * Một khối văn bản kèm vị trí trong tài liệu gốc.
 *
 * <p>{@code charStart}/{@code charEnd} là cầu nối cho {@code AC-010} của US-029: đáp án chuẩn
 * khai theo <strong>vị trí trong tài liệu</strong>, nên cùng một bộ câu hỏi ánh xạ được sang
 * mọi tập đoạn mà không phải soạn lại đáp án cho từng chiến lược tách.
 *
 * <p>{@code level} chỉ có nghĩa với khối tiêu đề: 1 là cấp cao nhất.
 */
public record TextBlock(
        BlockKind kind,
        int level,
        String text,
        int charStart,
        int charEnd,
        ExtractedTable table
) {
    public static TextBlock heading(int level, String text, int start, int end) {
        return new TextBlock(BlockKind.TIEU_DE, level, text, start, end, null);
    }

    public static TextBlock paragraph(String text, int start, int end) {
        return new TextBlock(BlockKind.DOAN_VAN, 0, text, start, end, null);
    }

    public static TextBlock table(ExtractedTable t, String rendered, int start, int end) {
        return new TextBlock(BlockKind.BANG, 0, rendered, start, end, t);
    }

    public boolean isHeading() { return kind == BlockKind.TIEU_DE; }
    public boolean isTable()   { return kind == BlockKind.BANG; }
}
