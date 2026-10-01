---
type: userstory-index
feature: access-control
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Tài khoản và phân quyền — Story Index

> Mã story đánh duy nhất toàn dự án (US-001…US-036), không đánh lại từ 001 theo từng thư mục.

## Stories

| ID | Tiêu đề | Persona | Năng lực | Ưu tiên | Trạng thái | Cập nhật |
|---|---|---|---|---|---|---|
| US-001 | Đăng nhập và đăng xuất | Nhân viên | `CAP-access-control-01` | Must | draft | 2026-09-09 |
| US-002 | Tạo tài khoản bằng tay | Quản trị tri thức | `CAP-access-control-01` | Must | draft | 2026-09-09 |
| US-003 | Nhập hàng loạt tài khoản từ tệp danh sách | Quản trị tri thức | `CAP-access-control-01` | Should | draft | 2026-09-09 |
| US-004 | Quản lý phòng ban và thành viên | Quản trị tri thức | `CAP-access-control-02` | Must | draft | 2026-09-09 |
| US-005 | Gán quyền đọc tài liệu cho người dùng hoặc phòng ban | Quản trị tri thức | `CAP-access-control-03` | Must | draft | 2026-09-09 |
| US-006 | Đặt quyền từ chối, từ chối thắng mọi cho phép | Quản trị tri thức | `CAP-access-control-03` | Must | draft | 2026-09-09 |
| US-007 | Nhãn mật tài liệu và mức mật người dùng | Quản trị tri thức | `CAP-access-control-05` | Must | draft | 2026-09-09 |
| US-008 | Truy hồi chỉ trả về đoạn nằm trong quyền của người hỏi | Nhân viên | `CAP-access-control-04` | Must | draft | 2026-09-09 |

## Ghi chú phạm vi

Tài khoản là tài khoản cục bộ của bản triển khai. Đăng nhập một lần qua SSO hoặc đồng bộ danh bạ
người dùng doanh nghiệp nằm ngoài phạm vi (xem mục 5 của PRD).

Mã lỗi và mã màn hình chưa được cấp ở tầng story — chúng được đặt ở bước đặc tả màn hình và yêu
cầu phi chức năng. Acceptance criteria dưới đây mô tả hành vi lỗi bằng lời, chưa gắn mã.
