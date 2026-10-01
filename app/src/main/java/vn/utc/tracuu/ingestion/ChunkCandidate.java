package vn.utc.tracuu.ingestion;

/**
 * Một đoạn đã tách, chưa ghi xuống cơ sở dữ liệu.
 *
 * <p>{@code tablePart}/{@code tablePartsTotal} cho phép hiển thị <em>"mảnh 2/4, đã lặp dòng
 * tiêu đề"</em> ở màn xem trước, và cho phép kiểm chất lượng tách bảng mà không phải mở tài
 * liệu gốc — thứ mà nếu thiếu thì người kiểm phải tin vào cảm giác.
 */
public record ChunkCandidate(
        String content,
        String breadcrumb,
        int charStart,
        int charEnd,
        boolean table,
        Integer tablePart,
        Integer tablePartsTotal
) {
    public static ChunkCandidate text(String content, String breadcrumb, int start, int end) {
        return new ChunkCandidate(content, breadcrumb, start, end, false, null, null);
    }

    public static ChunkCandidate tablePiece(String content, String breadcrumb,
                                            int start, int end, int part, int total) {
        return new ChunkCandidate(content, breadcrumb, start, end, true, part, total);
    }
}
