package vn.utc.tracuu.access;

import java.util.Set;

/**
 * Tập tài liệu một người được đọc, biểu diễn theo <strong>chiều lọc có lợi hơn</strong>.
 *
 * <p>Mô hình dữ liệu Mục 7 bước 2: truyền một mảng vài trăm mã vào truy vấn thì nhanh; truyền
 * một mảng chín trăm mã thì chậm hơn cả việc loại trừ vài chục mã còn lại. Nên cùng một tập
 * được biểu diễn hai cách, và bên gọi không cần biết cách nào đang dùng — nó chỉ ghép mệnh đề
 * mà {@link #sqlPredicate} trả về.
 */
public sealed interface ReadableDocuments {

    /** Không đọc được gì. Tài khoản mới chưa gán quyền rơi vào đây (AC-004 của US-008). */
    record None() implements ReadableDocuments {
        @Override public boolean permits(long documentId) { return false; }
        @Override public String sqlPredicate(String alias) { return "false"; }
        @Override public Set<Long> parameterIds() { return Set.of(); }
        @Override public int estimatedSize() { return 0; }
    }

    /** Đọc được tất cả. CHỈ sinh ra từ {@link PermissionScope.Unfiltered}. */
    record All() implements ReadableDocuments {
        @Override public boolean permits(long documentId) { return true; }
        @Override public String sqlPredicate(String alias) { return "true"; }
        @Override public Set<Long> parameterIds() { return Set.of(); }
        @Override public int estimatedSize() { return Integer.MAX_VALUE; }
    }

    /** Phạm vi hẹp: liệt kê tài liệu ĐƯỢC PHÉP. */
    record AllowList(Set<Long> ids) implements ReadableDocuments {
        @Override public boolean permits(long documentId) { return ids.contains(documentId); }
        @Override public String sqlPredicate(String alias) { return alias + ".document_id = ANY(?)"; }
        @Override public Set<Long> parameterIds() { return ids; }
        @Override public int estimatedSize() { return ids.size(); }
    }

    /** Phạm vi rộng: liệt kê tài liệu BỊ CHẶN, rẻ hơn hẳn khi người dùng đọc được gần hết kho. */
    /**
     * "Đọc được mọi thứ TRỪ danh sách này" — dạng gọn khi người dùng được cấp phần lớn kho.
     *
     * <p><strong>{@code maxDocumentId} là mốc kho lúc danh sách chặn được tính, và nó là phần
     * bắt buộc của tính đúng đắn, không phải một tối ưu.</strong> Danh sách chặn là một ảnh
     * chụp. Tài liệu nạp SAU ảnh chụp đó không nằm trong danh sách, nên nếu chỉ hỏi "có bị chặn
     * không" thì câu trả lời là "không" — và từ-chối-mặc-định lặng lẽ biến thành cho-phép-mặc-định
     * cho đúng những tài liệu mới nhất, thường là những tài liệu nhạy cảm nhất.
     *
     * <p>Đo thật ngày 30/09: một tài liệu vừa nạp, <em>không có một dòng cấp quyền nào</em>, được
     * một tài khoản thường đọc trọn nội dung qua {@code GET /api/documents/{id}/chunks} với mã
     * 200. Bản ghi quyền đúng, truy vấn đúng, cache đúng — chỉ cách biểu diễn là sai, và nó sai
     * theo chiều mở.
     *
     * <p>Với mốc này, mọi id vượt mốc bị từ chối cho tới khi phạm vi được tính lại. Đó là hành vi
     * đúng của một hệ từ-chối-mặc-định: cái gì chưa biết thì không cho.
     */
    record DenyList(Set<Long> blockedIds, int corpusSize, long maxDocumentId)
            implements ReadableDocuments {
        @Override public boolean permits(long documentId) {
            return documentId <= maxDocumentId && !blockedIds.contains(documentId);
        }
        @Override public String sqlPredicate(String alias) {
            // Mốc là số nguyên do hệ thống tự tính, không phải dữ liệu người dùng, nên ghép
            // thẳng vào câu lệnh là an toàn — và giữ được đúng MỘT dấu hỏi như các nhánh khác,
            // nên nơi gọi không phải đổi cách truyền tham số.
            return "NOT (" + alias + ".document_id = ANY(?)) AND "
                    + alias + ".document_id <= " + maxDocumentId;
        }
        @Override public Set<Long> parameterIds() { return blockedIds; }
        @Override public int estimatedSize() { return corpusSize - blockedIds.size(); }
    }

    boolean permits(long documentId);

    /**
     * Mệnh đề SQL ghép vào truy vấn truy hồi. Tham số duy nhất là mảng mã ở
     * {@link #parameterIds()}; mệnh đề {@code true}/{@code false} không dùng tham số nào.
     */
    String sqlPredicate(String alias);

    Set<Long> parameterIds();

    /** Số tài liệu ước tính trong phạm vi — dùng để quyết có bật chế độ quét lặp hay không. */
    int estimatedSize();

    default boolean isEmpty() { return this instanceof None; }

    default boolean needsIdParameter() {
        return this instanceof AllowList || this instanceof DenyList;
    }
}
