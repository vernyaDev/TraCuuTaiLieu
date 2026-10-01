---
type: usecase-index
feature: doc-ingestion
status: draft
updated: 2026-09-09
links:
  - docs/doc-ingestion/userstories/doc-ingestion-story-index.md
  - docs/_product/prd.md
---

# Nạp và quản lý vòng đời tài liệu — Use Case Index

## Use cases

| UC | Actor chính | Mức | Story | Năng lực | Màn hình | Trạng thái |
|---|---|---|---|---|---|---|
| `UC-ingest-document` | Quản trị tri thức | sea-level | US-009, US-010, US-011, US-012, US-013, US-014, US-015, US-007 | `CAP-doc-ingestion-01` đến `CAP-doc-ingestion-05` | Quản trị tài liệu | draft |

Mã màn hình và mã lỗi được cấp ở bước danh sách màn hình.

## Ba cổng chặn trong luồng nạp

Luồng nạp có ba chỗ tài liệu **không** được đi tiếp. Ghi tách bạch vì đây là nơi quyết định chất
lượng kho, và bỏ sót một cổng nào cũng dẫn tới hậu quả khác nhau:

| Cổng | Ở bước | Không qua thì sao | Hậu quả nếu bỏ cổng này |
|---|---|---|---|
| Nhãn mật | 2 | Không hoàn tất được việc nạp | Tài liệu mật vào kho không nhãn, tầng chặn theo mức mật vô hiệu |
| Xác nhận metadata | 8 | Dừng ở trạng thái chờ, không vào phạm vi truy hồi | Hệ thống trích dẫn văn bản đã hết hiệu lực mà không cảnh báo |
| Gán quyền | 12 | Ở trạng thái sẵn sàng nhưng không ai đọc được | Nếu mặc định mở thay vì đóng thì mọi tài liệu mới lộ cho toàn công ty |

Cổng thứ ba là mặc-định-đóng có chủ đích: tài liệu chưa gán quyền thì không ai đọc được. Đây là
lựa chọn an toàn, đổi lại quản trị phải nhớ gán — và đó là lý do màn hình quản trị phải hiện số
việc đang chờ (US-016).

## Bốn nhóm lỗi nạp

| Nhóm | Ví dụ | Người quản trị làm gì |
|---|---|---|
| Tệp không đọc được | pdf bản scan, tệp khoá, tệp hỏng | Tìm bản văn bản gốc, gỡ khoá, hoặc lưu lại bằng phần mềm soạn thảo |
| Nội dung không đủ | tệp rỗng, quá ngắn | Kiểm lại có đúng tệp cần nạp không |
| Mâu thuẫn metadata | ngày hết hiệu lực sớm hơn ngày hiệu lực, vòng lặp thay thế | Sửa lại thông tin rồi xác nhận |
| Suy giảm chất lượng | bảng không bóc được cấu trúc, tài liệu không có cấu trúc rõ | Vẫn nạp được nhưng phải biết tài liệu này có nguy cơ trả lời kém |

Nhóm cuối là nhóm dễ bỏ qua nhất: tài liệu **nạp thành công** nhưng chất lượng kém. Hệ thống phải
cảnh báo thay vì âm thầm nạp — nếu không, tài liệu tra bảng hỏng sẽ chỉ lộ ra khi người dùng nhận
câu trả lời sai.

## Quan hệ với use case khác

- Bước 12 chuyển sang `UC-grant-document-access`.
- Nhánh 1c chuyển sang luồng thay thế tài liệu của US-015.
- Chất lượng bước 5 quyết định chất lượng trích dẫn ở `UC-single-turn-qa` bước 9.
- Kết quả bước 6 và bước 9 là nguồn dữ liệu cho cảnh báo ở `UC-single-turn-qa` bước 6.
