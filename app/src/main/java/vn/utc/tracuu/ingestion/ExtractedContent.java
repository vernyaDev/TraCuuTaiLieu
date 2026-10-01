package vn.utc.tracuu.ingestion;

import java.util.List;

/**
 * Kết quả bóc một tài liệu.
 *
 * <p>{@code hasTextLayer} sai nghĩa là tệp không chứa lớp văn bản nào — thường gặp ở bản chụp
 * quét. Đây là một nhánh lỗi <strong>riêng</strong>, không gộp vào "tệp hỏng": người dùng cần
 * biết tệp của họ vẫn tốt, chỉ là hệ thống chưa nhận dạng chữ trong ảnh, để họ đi tìm bản có
 * lớp văn bản thay vì tải lại đúng tệp ấy.
 */
public record ExtractedContent(
        String plainText,
        List<TextBlock> blocks,
        boolean hasTextLayer,
        String detectedTitle
) {
    public static ExtractedContent empty(String reason) {
        return new ExtractedContent("", List.of(), false, null);
    }

    public int length() {
        return plainText == null ? 0 : plainText.length();
    }
}
