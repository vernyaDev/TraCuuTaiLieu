---
type: test-strategy
feature: doc-ingestion
status: draft
updated: 2026-09-24
links:
  - docs/_shared/nfr.md
  - docs/qa-chat/test/test-strategy.md
  - docs/doc-ingestion/userstories/doc-ingestion-story-index.md
---

# Nạp và quản lý vòng đời tài liệu — Chiến lược kiểm thử

**Trường bền dùng chung toàn sản phẩm** khai ở `../../qa-chat/test/test-strategy.md`; điều kiện đo
và ngưỡng ở Mục 0 và Mục 1 của `../../_shared/nfr.md`. Không chép lại.

**Đợt chạy này (2026-09-24):** dựng lưới nghiệm thu trước khi viết code · mức nền · chưa bật ngưỡng
thời gian nạp.

## Điều riêng của tính năng này — bộ tệp thử là thứ quyết định

Ba mươi hai mục chạy tay tập trung ở chất lượng tách đoạn và bảng biểu, và chúng **chỉ chạy được
nếu có đúng tệp**. Bộ tệp thử tối thiểu:

| Tệp cần có | Phục vụ mục |
|---|---|
| Văn bản pháp quy có chương, điều, khoản rõ ràng | Tách bám cấu trúc, đường dẫn phân cấp |
| Tài liệu không có tiêu đề phân cấp | Cách tách dự phòng, cảnh báo suy giảm |
| Bảng nhỏ nằm trọn một trang | Bảng không bị chia |
| Bảng dài trải qua nhiều trang pdf | Chia theo hàng, lặp dòng tiêu đề, ghép qua trang |
| Bảng có ô gộp | Quan hệ hàng cột sau khi tách |
| Cùng một nội dung ở cả bốn định dạng | Bốn định dạng cho cấu trúc tương đương |
| Pdf bản scan không có lớp văn bản | Nhánh lỗi bóc |
| Tệp đặt mật khẩu · tệp hỏng cấu trúc · tệp rỗng | Ba nhánh lỗi còn lại |
| Tệp vượt năm mươi megabyte | Chặn trước khi tải lên |
| Hai tài liệu cùng chủ đề, cùng phân loại | Phát hiện chồng lấn |
| Một cặp văn bản cũ và bản thay thế | Chuỗi thay thế, ưu tiên bản mới |

**Dựng bộ tệp này trước, không dựng song song lúc chạy.** Thiếu một tệp thì mục tương ứng không phải
là *trượt*, mà là *không chạy được* — hai trạng thái khác nhau và phải ghi khác nhau.

## Điều cố ý không phủ

Ngưỡng thời gian nạp (`NFR-system-016` đến `018`) đo trên kho thật, không đo trong đợt nghiệm thu
giao diện. So sánh ba chiến lược tách bằng độ chính xác và độ phủ thuộc bộ đo của `evaluation`.
