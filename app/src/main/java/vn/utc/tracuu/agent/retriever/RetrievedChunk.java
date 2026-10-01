package vn.utc.tracuu.agent.retriever;

/**
 * Một đoạn được truy hồi, kèm <strong>cả ba điểm số</strong>.
 *
 * <p>Ba điểm lưu riêng chứ không chỉ lưu điểm cuối. Lý do thực dụng: không tách ba thì không
 * phân tích được vì sao nhánh lai thắng hay thua từng nhánh riêng lẻ — mà đó chính là câu hỏi
 * trung tâm của chương thực nghiệm. Một bảng chỉ có điểm cuối trả lời được "cái nào tốt hơn"
 * nhưng không trả lời được "vì sao", và người phản biện sẽ hỏi câu thứ hai.
 *
 * <p>{@code scoreBm25} hoặc {@code scoreVector} bằng {@code null} nghĩa là nhánh ấy
 * <em>không tìm thấy</em> đoạn này — khác hẳn với tìm thấy nhưng điểm 0.
 */
public record RetrievedChunk(
        long chunkId,
        long documentId,
        String documentTitle,
        String breadcrumb,
        String content,
        int ordinal,
        boolean fromTable,
        Integer tablePart,
        Integer tablePartsTotal,
        Double scoreBm25,
        Double scoreVector,
        double scoreRrf,
        int rank
) {
    public RetrievedChunk withRank(int newRank) {
        return new RetrievedChunk(chunkId, documentId, documentTitle, breadcrumb, content, ordinal,
                fromTable, tablePart, tablePartsTotal, scoreBm25, scoreVector, scoreRrf, newRank);
    }

    public RetrievedChunk withRrf(double rrf) {
        return new RetrievedChunk(chunkId, documentId, documentTitle, breadcrumb, content, ordinal,
                fromTable, tablePart, tablePartsTotal, scoreBm25, scoreVector, rrf, rank);
    }

    /** Trích dẫn dạng người đọc: tên tài liệu kèm đường dẫn phân cấp. */
    public String citationLabel() {
        if (breadcrumb == null || breadcrumb.isBlank()) return documentTitle;
        return documentTitle + " › " + breadcrumb;
    }

    /** Cả hai nhánh cùng tìm thấy — tín hiệu mạnh hơn hẳn chỉ một nhánh tìm thấy. */
    public boolean foundByBothBranches() {
        return scoreBm25 != null && scoreVector != null;
    }
}
