package vn.utc.tracuu.knowledge;

/** Bốn hướng xử lý một cặp tài liệu bị nghi chồng lấn. */
public enum OverlapDecision {
    CHO,
    THAY_THE,
    /** Xác nhận cùng tồn tại HỢP LỆ — giá trị này phải đọc được ngay trong đường sinh câu trả lời. */
    CUNG_TON_TAI,
    BAO_DONG_GIA
}
