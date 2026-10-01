package vn.utc.tracuu.knowledge;

/** Vòng đời một tài liệu trong kho. */
public enum DocumentStatus {
    /** Vừa tải lên, đang bóc chữ và tách đoạn. */
    DANG_XU_LY,
    /** Đã tách đoạn xong, chờ quản trị xác nhận metadata hệ thống đề xuất. */
    CHO_XAC_NHAN,
    /** Đã lập chỉ mục — trạng thái DUY NHẤT được đưa vào truy hồi. */
    DA_LAP_CHI_MUC,
    /** Nạp thất bại, lý do ghi ở {@code error_reason}. */
    LOI,
    /**
     * Đã gỡ khỏi kho. Văn bản KHÔNG biến mất — chúng bị thay thế (US-015).
     * Không tham gia truy hồi nhưng trích dẫn cũ vẫn tra ngược được.
     */
    DA_GO;

    public boolean searchable() {
        return this == DA_LAP_CHI_MUC;
    }
}
