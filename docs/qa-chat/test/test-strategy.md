---
type: test-strategy
feature: qa-chat
status: draft
updated: 2026-09-24
links:
  - docs/_shared/nfr.md
  - docs/qa-chat/userstories/qa-chat-story-index.md
  - docs/qa-chat/srs/qa-chat-userflow.md
---

# Hỏi đáp có trích dẫn — Chiến lược kiểm thử

## Điều kiện dùng chung toàn sản phẩm

**Không chép lại ở đây.** Môi trường đo, quy mô kho, phần cứng, ngưỡng độ trễ và điều kiện đo chuẩn
sống ở Mục 0 và Mục 1 của `docs/_shared/nfr.md` — đó là nguồn duy nhất. Chép sang đây là tạo bản thứ
hai sẽ lệch.

Hai điều kiện đáng nhắc vì chúng đổi kết quả đo: **ngữ cảnh đưa vào mô hình** phải bằng giá trị mặc
định của `context.max_tokens`, và **phạm vi quyền của người hỏi** phải khai rõ — đo ở phạm vi toàn
kho cho kết quả thuận lợi hơn hẳn phạm vi hẹp.

## Trường bền — hỏi một lần, dùng lại

| Trường | Giá trị |
|---|---|
| Ngôn ngữ danh mục | Tiếng Việt; giữ nguyên nhãn nút và tên trường như xuất hiện trên màn |
| Thiết bị | Máy bàn 1024 điểm ảnh, theo `NFR-system-042` |
| Nguồn dữ liệu thử | Bộ 100 câu tầng truy hồi và bộ 50 câu tầng agent của `evaluation`; kho văn bản pháp quy công khai đã tuyển chọn |
| Xử lý khi thiếu nguồn | Giả định **chỉ** cho phần dựng cảnh không đổi hành vi. Wording, ngưỡng, hệ quả phụ mà tài liệu chưa chốt thì ghi câu hỏi mở và **chặn mục đó**, không đoán |
| Người chạy | Một người, vừa là tác giả vừa là người kiểm |

## Trường theo đợt chạy

| Trường | Đợt này (2026-09-24) |
|---|---|
| Mục đích | Dựng lưới nghiệm thu **trước khi viết dòng code đầu tiên**, không phải kiểm hồi quy |
| Mốc so sánh | Chưa có bản chạy nào — đây là đợt đầu |
| Mức phủ | **Mức nền**: luồng chính, luồng thay thế, kiểm đầu vào và biên, lỗi, bảo mật cơ bản, trạng thái tải, tiếp cận cơ bản, ca biên |
| Chưa bật | Ngưỡng hiệu năng định lượng và tiếp cận sâu — phần lớn phụ thuộc phép đo chưa chạy trước 05/10, bật bây giờ chỉ sinh ra mục chờ |

## Điều chiến lược này cố ý không phủ

- **Kiểm thử thâm nhập.** Mục bảo mật ở đây là kiểm nghiệm thu: có bắt đăng nhập không, có chặn
  đúng vai không, phiên hết hạn có đẩy ra không. Không dò lỗ hổng.
- **Đo hiệu năng có ngưỡng.** Chờ phép đo mô hình chạy trước 05/10.
- **Kiểm tự động toàn bộ.** Mục đánh dấu `[No]` là mục người phải chạy tay: soi cơ sở dữ liệu, đua
  tranh đồng thời, cảm nhận bố cục, phán đoán trải nghiệm.
