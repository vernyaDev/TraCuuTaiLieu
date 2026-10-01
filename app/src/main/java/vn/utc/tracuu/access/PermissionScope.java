package vn.utc.tracuu.access;

/**
 * Phạm vi quyền của một truy vấn chạm bảng đoạn — luật R1 của kiến trúc.
 *
 * <p>Luật cũ phát biểu tuyệt đối: <em>"không truy vấn nào chạm bảng đoạn mà không đi qua bộ
 * giải quyết quyền"</em>. Nhưng hệ thống có <strong>ba đường thật sự cần đọc không lọc</strong>,
 * và một luật tuyệt đối không xoá được chúng — nó chỉ đẩy chúng xuống thành ngoại lệ không ai
 * theo dõi, nằm im trong bản bàn giao. Ba giá trị dưới đây biến cả ba đường thành hợp pháp và
 * <strong>đếm được</strong>.
 *
 * <p>Giao diện này {@code sealed}: danh sách giá trị hợp lệ là đóng, và trình biên dịch — chứ
 * không phải một dòng ghi chú — là thứ chặn người sau thêm giá trị thứ tư. Đây là chủ đích:
 * luật đi ngược thói quen ("cứ đọc thẳng cho nhanh") mà chỉ tồn tại dạng văn xuôi thì sẽ thua.
 */
public sealed interface PermissionScope {

    /** Mô tả ngắn để ghi nhật ký. Không chứa nội dung tài liệu (NFR-system-048). */
    String describe();

    /**
     * Mọi đường đi từ giao diện: hỏi đáp, xem tài liệu gốc, tải tệp.
     * Không ghi nhật ký kiểm toán — đây là đường thường.
     */
    record ForUser(long userId) implements PermissionScope {
        @Override public String describe() {
            return "user(" + userId + ")";
        }
    }

    /**
     * Bộ đo, chạy dưới danh tính một tài khoản thật có phạm vi quyền xác định.
     *
     * <p>Tồn tại vì {@code AC-004} của US-030 đòi chạy được bộ đo với một tài khoản có phạm vi
     * quyền hẹp — trước đây không tài liệu nào nói bộ đo chạy dưới danh tính gì.
     */
    record ForEvaluation(long principalUserId, String runLabel) implements PermissionScope {
        @Override public String describe() {
            return "evaluation(" + principalUserId + ", " + runLabel + ")";
        }
    }

    /**
     * Đọc KHÔNG lọc quyền. Chỉ khâu phân loại lý do khoảng trống được sinh giá trị này,
     * và chỉ từ gói {@code gap/}.
     *
     * <p>Vì sao cần: muốn biết người hỏi bị chặn vì <em>thiếu quyền</em> chứ không phải vì
     * <em>kho trống</em> thì buộc phải so kết quả có lọc với kết quả không lọc ({@code AC-002}
     * của US-027). Cả tab <em>thiếu quyền</em> của báo cáo khoảng trống đứng trên năng lực đó.
     *
     * <p><strong>Ghi nhật ký kiểm toán MỌI LẦN</strong> — xem {@code PermissionResolver}.
     * Không có đường HTTP nào sinh được giá trị này.
     */
    record Unfiltered(String reason) implements PermissionScope {
        public Unfiltered {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Đọc không lọc quyền phải nêu lý do — lý do trống là thứ khiến "
                        + "nhật ký kiểm toán trở nên vô dụng đúng lúc cần đến nó.");
            }
        }
        @Override public String describe() {
            return "unfiltered(" + reason + ")";
        }
    }

    /** Danh tính dùng để tính tập tài liệu được phép; rỗng khi không lọc. */
    default Long effectiveUserId() {
        return switch (this) {
            case ForUser u -> u.userId();
            case ForEvaluation e -> e.principalUserId();
            case Unfiltered ignored -> null;
        };
    }

    default boolean requiresAudit() {
        return this instanceof Unfiltered;
    }
}
