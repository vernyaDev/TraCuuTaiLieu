package vn.utc.tracuu.ingestion;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata hệ thống đề xuất cho một tài liệu, <strong>kèm trích đoạn làm căn cứ từng giá trị</strong>.
 *
 * <p>Trích đoạn không phải phần trang trí. Quản trị phải xác nhận metadata cho cả nghìn tài
 * liệu; nếu mỗi giá trị chỉ hiện ra một con số không rõ từ đâu thì họ chỉ có hai lựa chọn —
 * bấm đồng ý hết, hoặc mở từng tệp gốc ra dò. Cả hai đều làm khâu xác nhận trở nên vô nghĩa.
 * Có trích đoạn thì họ đọc một dòng là quyết được.
 *
 * <p>Mỗi trường mang thêm cờ {@code verified}: trích đoạn đã được đối chiếu là <em>có thật
 * trong tài liệu</em> hay chưa. Mô hình bịa ra một câu trích dẫn nghe rất thuyết phục là
 * chuyện thường, và một căn cứ bịa còn tệ hơn không có căn cứ — vì nó làm quản trị tin.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataSuggestion(
        Suggested<String> title,
        Suggested<String> category,
        Suggested<LocalDate> effectiveFrom,
        Suggested<LocalDate> effectiveTo,
        Suggested<Boolean> hasNoExpiry,
        Suggested<Integer> sensitivity,
        List<String> notes
) {

    /**
     * Một giá trị đề xuất.
     *
     * @param value    giá trị hệ thống đoán
     * @param evidence câu trích nguyên văn từ tài liệu làm căn cứ
     * @param verified trích đoạn đã đối chiếu thấy trong tài liệu chưa
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Suggested<T>(T value, String evidence, boolean verified) {

        public static <T> Suggested<T> of(T value, String evidence, boolean verified) {
            return value == null ? null : new Suggested<>(value, evidence, verified);
        }

        /** Đề xuất dùng được: có giá trị VÀ căn cứ đã đối chiếu. */
        public boolean trustworthy() {
            return value != null && verified;
        }
    }

    public static MetadataSuggestion empty(String reason) {
        return new MetadataSuggestion(null, null, null, null, null, null,
                List.of(reason));
    }

    /** Số trường có căn cứ đã đối chiếu — con số giao diện dùng để xếp việc cần xem trước. */
    public int verifiedCount() {
        int count = 0;
        if (title != null && title.verified()) count++;
        if (category != null && category.verified()) count++;
        if (effectiveFrom != null && effectiveFrom.verified()) count++;
        if (effectiveTo != null && effectiveTo.verified()) count++;
        if (sensitivity != null && sensitivity.verified()) count++;
        return count;
    }

    /** Những trường có giá trị nhưng căn cứ KHÔNG đối chiếu được — cần quản trị soi kỹ. */
    public List<String> unverifiedFields() {
        List<String> out = new ArrayList<>();
        if (title != null && !title.verified()) out.add("tên tài liệu");
        if (category != null && !category.verified()) out.add("danh mục");
        if (effectiveFrom != null && !effectiveFrom.verified()) out.add("ngày hiệu lực");
        if (effectiveTo != null && !effectiveTo.verified()) out.add("ngày hết hiệu lực");
        if (sensitivity != null && !sensitivity.verified()) out.add("nhãn mật");
        return out;
    }
}
