---
type: usecase-index
feature: qa-chat
status: draft
updated: 2026-09-09
links:
  - docs/qa-chat/userstories/qa-chat-story-index.md
  - docs/_product/prd.md
---

# Hỏi đáp có trích dẫn — Use Case Index

## Use cases

| UC | Actor chính | Mức | Story | Năng lực | Màn hình | Trạng thái |
|---|---|---|---|---|---|---|
| `UC-single-turn-qa` | Nhân viên | sea-level | US-017, US-018, US-019, US-008, US-023, US-024, US-025 | `CAP-qa-chat-01`, `CAP-qa-chat-06` | Hỏi đáp, Xem tài liệu | draft |
| `UC-follow-up-qa` | Nhân viên | sea-level | US-020, US-021, US-026, US-017 | `CAP-qa-chat-02` | Hỏi đáp | draft |
| `UC-multi-step-qa` | Nhân viên | sea-level | US-022, US-017, US-023, US-024, US-031 | `CAP-qa-chat-03` | Hỏi đáp | draft |
| `UC-refuse-answer` | Nhân viên | sea-level | US-023, US-024, US-027, US-008, US-031 | `CAP-qa-chat-04`, `CAP-knowledge-gap-01` | Hỏi đáp | draft |

Mã màn hình và mã lỗi được cấp ở bước danh sách màn hình; cột "Màn hình" hiện ghi bằng tên gọi,
sẽ thay bằng mã khi có.

## Quan hệ giữa các use case

`UC-single-turn-qa` là luồng nền. Ba use case còn lại đều rẽ ra từ nó hoặc quay về nó:

- `UC-follow-up-qa` xử lý phần viết lại câu hỏi rồi **gọi lại** `UC-single-turn-qa` từ bước 3.
- `UC-multi-step-qa` thay bước truy hồi đơn bằng chuỗi truy vấn con có thứ tự; khi bước phân rã bị
  tắt trong cấu hình thì nó **trở thành** `UC-single-turn-qa`.
- `UC-refuse-answer` là kết cục hợp lệ mà cả ba use case kia đều có thể dẫn tới, ở hai thời điểm
  khác nhau: trước khi gọi mô hình ngôn ngữ (tầng một) và sau khi đã sinh câu trả lời (tầng hai).

## Ba đường vào của việc từ chối

Đây là chỗ dễ đặc tả thiếu nhất, nên ghi tách bạch ở đây:

| Đường vào | Xảy ra ở bước nào | Đã gọi mô hình chưa | Ghi khoảng trống |
|---|---|---|---|
| Không đoạn nào vượt ngưỡng | Sau truy hồi | chưa | có |
| Phạm vi quyền không chứa tài liệu liên quan | Trước truy hồi | chưa | có, lý do thiếu quyền |
| Mọi ý không gắn được về nguồn | Sau khi sinh xong | rồi | có, lý do riêng |

Câu hỏi ngoài phạm vi kho tri thức cũng dẫn tới thông báo từ chối nhưng **không** tạo bản ghi
khoảng trống — nó không phải tín hiệu về kho tri thức.

## Bảy module agent hiện ra ở use case nào

| Module | Use case | Bước |
|---|---|---|
| Prompt | `UC-single-turn-qa` | 1–2 |
| Reasoning | `UC-single-turn-qa`, `UC-multi-step-qa` | 2 |
| Memory | `UC-follow-up-qa` | 3, 3a |
| Planner | `UC-multi-step-qa` | 3 |
| Retriever | `UC-single-turn-qa` | 3–5 |
| Tool | `UC-single-turn-qa` | 6 |
| Action | `UC-single-turn-qa` | 7–9 |
