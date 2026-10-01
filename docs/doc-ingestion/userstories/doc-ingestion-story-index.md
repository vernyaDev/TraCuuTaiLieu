---
type: userstory-index
feature: doc-ingestion
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Nạp và quản lý vòng đời tài liệu — Story Index

## Stories

| ID | Tiêu đề | Persona | Năng lực | Ưu tiên | Trạng thái | Cập nhật |
|---|---|---|---|---|---|---|
| US-009 | Nạp tệp docx, pdf, txt, Markdown và theo dõi tiến độ | Quản trị tri thức | `CAP-doc-ingestion-01` | Must | draft | 2026-09-09 |
| US-010 | Biết tệp nào nạp hỏng và hỏng vì gì | Quản trị tri thức | `CAP-doc-ingestion-01` | Must | draft | 2026-09-09 |
| US-011 | Tách đoạn theo cấu trúc, giữ đường dẫn phân cấp để trích dẫn | Quản trị tri thức | `CAP-doc-ingestion-02` | Must | draft | 2026-09-09 |
| US-012 | Bảng biểu không bị cắt giữa chừng | Quản trị tri thức | `CAP-doc-ingestion-02` | Must | draft | 2026-09-09 |
| US-013 | Xác nhận metadata vòng đời hệ thống đã điền sẵn | Quản trị tri thức | `CAP-doc-ingestion-03` | Must | draft | 2026-09-09 |
| US-014 | Được cảnh báo tài liệu mới chồng lấn chủ đề | Quản trị tri thức | `CAP-doc-ingestion-04` | Should | draft | 2026-09-09 |
| US-015 | Gỡ tài liệu hoặc thay bằng bản mới, giữ chuỗi thay thế | Quản trị tri thức | `CAP-doc-ingestion-05` | Must | draft | 2026-09-09 |
| US-016 | Xem danh sách tài liệu, lọc theo trạng thái hiệu lực | Quản trị tri thức | `CAP-doc-ingestion-05` | Should | draft | 2026-09-09 |

## Ghi chú phạm vi

Chỉ nạp tài liệu văn bản số hoá. Tài liệu scan hoặc ảnh, audio, video nằm ngoài phạm vi. Không có
cơ chế đồng bộ tự động từ thư mục mạng — mọi tài liệu vào kho qua thao tác tải lên có người xác nhận.

Kích thước tệp tối đa 50MB; quy mô kho mục tiêu khoảng 1.000 tài liệu, trong đó khoảng 30% là tài
liệu nhiều bảng biểu.
