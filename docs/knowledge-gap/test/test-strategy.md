---
type: test-strategy
feature: knowledge-gap
status: draft
updated: 2026-09-24
links:
  - docs/_shared/nfr.md
  - docs/qa-chat/test/test-strategy.md
  - docs/knowledge-gap/userstories/knowledge-gap-story-index.md
---

# Báo cáo khoảng trống tri thức — Chiến lược kiểm thử

**Trường bền dùng chung toàn sản phẩm** khai ở `../../qa-chat/test/test-strategy.md`; điều kiện đo ở
Mục 0 của `../../_shared/nfr.md`. Không chép lại.

**Đợt chạy này (2026-09-24):** dựng lưới nghiệm thu trước khi viết code · mức nền.

## Điều riêng của tính năng này

**Dữ liệu của màn này không nhập tay được — nó sinh ra từ `qa-chat`.** Muốn có nhóm khoảng trống để
kiểm thì phải hỏi những câu bị từ chối. Hệ quả: **danh mục này không chạy độc lập**, nó chạy sau khi
`qa-chat` đã chạy được tối thiểu luồng từ chối.

Kịch bản dựng dữ liệu tối thiểu, chạy bằng chính màn hỏi đáp:

| Cần dựng | Cách dựng |
|---|---|
| Nhóm *thiếu tài liệu* | Hỏi ba lần cùng một chủ đề kho không có |
| Nhóm *thiếu quyền* | Dùng tài khoản hạn chế, hỏi việc mà tài liệu liên quan nằm ngoài quyền |
| Nhóm *căn cứ mỏng* | Hỏi việc kho chỉ có một đoạn liên quan thấp |
| Nhóm *đã xử lý nhưng chưa hết* | Đánh dấu một nhóm đã xử lý, rồi hỏi lại đúng chủ đề ấy và vẫn bị từ chối |
| Bản ghi câu tiếp nối | Hỏi một câu rút gọn nối tiếp, bị từ chối |

## Nghĩa vụ mà bằng chứng nằm ở sự vắng mặt

`AC-005` của US-028 — không dùng để theo dõi cá nhân — được phủ bằng bốn mục kiểm, nhiều hơn hẳn các
nghĩa vụ khác. Lý do: **không có gì để bấm vào mà kiểm**. Phải kiểm bốn chỗ vắng mặt riêng biệt: ô
lọc theo người dùng, đường mạng trả về mọi câu của một người, định danh cá nhân khi mở rộng nhóm, và
tham số theo người hỏi trong liên kết khắc phục. Kiểm một chỗ rồi kết luận là bỏ sót ba cửa còn lại.

Hai trong bốn mục ấy phải rà mã nguồn hoặc rà danh sách đường mạng, không quan sát được qua giao diện.

## Điều cố ý không phủ

Ngưỡng gom nhóm câu hỏi chưa hiệu chỉnh nên chỉ kiểm *có gom hay không*, không kiểm *gom đúng mức
nào*. Ngưỡng gộp phòng ban nhỏ chưa chốt nên chưa có mục kiểm.
