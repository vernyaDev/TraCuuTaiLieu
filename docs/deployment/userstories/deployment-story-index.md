---
type: userstory-index
feature: deployment
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Đóng gói on-premise — Story Index

## Stories

| ID | Tiêu đề | Persona | Năng lực | Ưu tiên | Trạng thái | Cập nhật |
|---|---|---|---|---|---|---|
| US-034 | Khởi động toàn hệ thống bằng một lệnh | Quản trị hệ thống | `CAP-deployment-01` | Must | draft | 2026-09-09 |
| US-035 | Thiếu GPU thì dừng hẳn kèm thông báo rõ nguyên nhân | Quản trị hệ thống | `CAP-deployment-02` | Must | draft | 2026-09-09 |
| US-036 | Sao lưu và phục hồi kho tri thức | Quản trị hệ thống | `CAP-deployment-03` | Could | draft | 2026-09-09 |

## Ghi chú phạm vi

Bốn dịch vụ trong một tệp cấu hình: ứng dụng, cơ sở dữ liệu có hỗ trợ tìm kiếm ngữ nghĩa, dịch vụ
mô hình ngôn ngữ cục bộ, và bộ nhớ đệm.

Bản giao **bắt buộc có GPU** — không hỗ trợ chế độ chỉ dùng CPU. Đây là quyết định đã chốt, và
US-035 là chỗ nó được thi hành thay vì chỉ ghi trong tài liệu hướng dẫn.

Nhóm này cũng là phần chứng minh luận điểm cốt lõi của đề tài: toàn bộ hệ thống, kể cả mô hình ngôn
ngữ, chạy trong mạng nội bộ và dữ liệu không rời khỏi tổ chức. Một cấu hình nào đó gọi ra ngoài là
phá vỡ luận điểm ấy, nên đó là điều kiện phải kiểm được chứ không phải điều để tin.
