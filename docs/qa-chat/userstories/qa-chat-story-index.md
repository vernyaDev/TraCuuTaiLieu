---
type: userstory-index
feature: qa-chat
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Hỏi đáp có trích dẫn — Story Index

## Stories

| ID | Tiêu đề | Persona | Năng lực | Ưu tiên | Trạng thái | Cập nhật |
|---|---|---|---|---|---|---|
| US-017 | Hỏi một lượt và nhận câu trả lời kèm trích dẫn | Nhân viên | `CAP-qa-chat-01` | Must | draft | 2026-09-09 |
| US-018 | Câu trả lời hiện dần từng chữ | Nhân viên | `CAP-qa-chat-01` | Must | draft | 2026-09-09 |
| US-019 | Mở tài liệu gốc từ trích dẫn | Nhân viên | `CAP-qa-chat-06` | Should | draft | 2026-09-09 |
| US-020 | Hỏi tiếp nối, hệ thống hiểu câu rút gọn | Nhân viên | `CAP-qa-chat-02` | Must | draft | 2026-09-09 |
| US-021 | Hội thoại dài vẫn giữ được mạch | Nhân viên | `CAP-qa-chat-02` | Should | draft | 2026-09-09 |
| US-022 | Câu hỏi nhiều vế được phân rã, trả lời đủ mọi vế | Nhân viên | `CAP-qa-chat-03` | Must | draft | 2026-09-09 |
| US-023 | Không có căn cứ thì nói không biết, không đoán | Nhân viên | `CAP-qa-chat-04` | Must | draft | 2026-09-09 |
| US-024 | Ý không gắn được về nguồn thì bị loại khỏi câu trả lời | Nhân viên | `CAP-qa-chat-04` | Must | draft | 2026-09-09 |
| US-025 | Cảnh báo nguồn hết hiệu lực hoặc có văn bản chồng lấn | Nhân viên | `CAP-qa-chat-05` | Should | draft | 2026-09-09 |
| US-026 | Xem lại các lượt hỏi đáp trước trong phiên | Nhân viên | `CAP-qa-chat-02` | Must | draft | 2026-09-09 |

## Bảy module agent hiện ra ở story nào

| Module | Story chính | Story liên quan |
|---|---|---|
| Prompt | US-017 | US-020 |
| Reasoning | US-022 | US-017 |
| Memory | US-020, US-021 | US-026 |
| Planner | US-022 | — |
| Retriever | US-017 | US-008, US-011, US-012 |
| Tool | US-025 | US-014, US-015 |
| Action | US-017, US-024 | US-018, US-023 |

Bảng này để chương "Phân tích thiết kế" của báo cáo chỉ được đúng story nào chứng minh module nào,
tránh tình trạng module có trong sơ đồ nhưng không có gì kiểm chứng.

## Ghi chú phạm vi

Không có đánh giá câu trả lời bằng phản hồi người dùng, không chia sẻ hội thoại, không xuất câu
trả lời ra tệp. Hội thoại chỉ sống trong phiên; hồ sơ người dùng bền vững qua nhiều phiên nằm
ngoài phạm vi theo ADR-0002.
