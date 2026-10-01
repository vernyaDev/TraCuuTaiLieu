---
type: test-strategy
feature: access-control
status: draft
updated: 2026-09-24
links:
  - docs/_shared/nfr.md
  - docs/qa-chat/test/test-strategy.md
  - docs/access-control/userstories/access-control-story-index.md
---

# Tài khoản và phân quyền — Chiến lược kiểm thử

**Trường bền dùng chung toàn sản phẩm** — môi trường đo, thiết bị, ngôn ngữ, nguồn dữ liệu thử, cách
xử lý khi thiếu nguồn — khai một lần ở `../../qa-chat/test/test-strategy.md`. Điều kiện đo và ngưỡng
ở Mục 0 và Mục 1 của `../../_shared/nfr.md`. Không chép lại ở đây.

**Đợt chạy này (2026-09-24):** dựng lưới nghiệm thu trước khi viết dòng code đầu tiên · chưa có mốc
so sánh · mức nền, chưa bật hiệu năng định lượng và tiếp cận sâu.

## Điều riêng của tính năng này

**Phần lớn hành vi không hiện ra trên giao diện.** Quyền có áp đúng không, lọc có diễn ra trước bước
xếp hạng không, mục từ chối có thắng quyền cho phép không — không màn hình nào nói cho biết. Hệ quả:
**42 trên 118 mục phải chạy tay**, cao hơn hẳn ba tính năng kia.

Cách chạy chuẩn cho nhóm mục ấy: **dựng sẵn hai tài khoản khác phạm vi quyền**, hỏi cùng một câu
bằng cả hai, rồi so kết quả. Đây là cách duy nhất quan sát được luật phân quyền từ bên ngoài mà
không phải soi cơ sở dữ liệu.

**Dữ liệu thử tối thiểu phải có:** một tài liệu chỉ một phòng ban đọc được · một tài liệu có mục từ
chối cá nhân đè lên quyền phòng ban · một tài liệu nhãn mật cao hơn mức mật của người thử · một tài
khoản chưa được gán quyền trên tài liệu nào. Thiếu bộ này thì mười mục của nhóm 6 và nhóm 8 không
chạy được.

## Điều cố ý không phủ

Kiểm thử thâm nhập, thử tiêm mã, dò lỗ hổng. Mục bảo mật ở đây là **kiểm nghiệm thu**: có chặn đúng
vai không, có lộ sự tồn tại tài khoản không.
