---
type: project-profile
status: draft
updated: 2026-09-09
links: []
---

# Project Profile

> Kho thông tin cấp dự án tích lũy dần — skill thiếu thông tin sẽ hỏi rồi ghi vào đây, skill sau
> đọc lại không hỏi nữa (xem `.claude/rules/project-profile.md`). Chỉ tạo section khi có nội dung
> thật; mỗi mục kèm ngày ghi.

## Domain

Hệ thống tra cứu tri thức nội bộ doanh nghiệp: nhân viên đặt câu hỏi tiếng Việt tự nhiên trên kho
tài liệu nội bộ (quy định, quy trình, hợp đồng, tài liệu kỹ thuật dạng docx/pdf/txt/Markdown), hệ
thống trả lời kèm trích dẫn nguồn kiểm chứng được và từ chối trả lời khi không đủ căn cứ. Kiến trúc
AI Agent 7 module theo workflow cố định (Prompt → Reasoning → Memory → Planner → Retriever → Tool →
Action) trên nền LLM chạy cục bộ.

**Luận điểm cốt lõi, mọi quyết định phân tích phải bảo toàn:** mỗi doanh nghiệp một bản độc lập,
on-premise qua Docker Compose, toàn bộ — kể cả LLM — chạy trong mạng nội bộ; dữ liệu không rời khỏi
tổ chức. (2026-09-09)

## Người dùng & thuật ngữ

| Nhóm user | Gọi là gì trong doc | Ghi chú | Ngày |
|---|---|---|---|
| Người dùng cuối, đại đa số | **Nhân viên** | Hỏi → nhận câu trả lời có trích dẫn → mở tài liệu gốc | 2026-09-09 |
| Người nạp/gỡ tài liệu, gán quyền | **Quản trị tri thức** | Xem cả báo cáo câu hỏi hệ thống không trả lời được (khoảng trống kho tri thức) | 2026-09-09 |
| Người cài đặt, cấu hình, sao lưu | **Quản trị hệ thống** | Docker Compose, không đụng nội dung tri thức | 2026-09-09 |

## Thị trường & ngôn ngữ

Doanh nghiệp Việt Nam có kho tài liệu nội bộ tiếng Việt. Giao diện, tài liệu và câu trả lời đều
tiếng Việt. Tài liệu nguồn tiếng Việt là chính, có lẫn thuật ngữ tiếng Anh trong tài liệu kỹ thuật.
(2026-09-09)

## Compliance

Không áp chuẩn compliance bên ngoài (không PDPA/GDPR-scope trong phạm vi đồ án). Ràng buộc bảo mật
là **nội bộ hoá tuyệt đối**: không gọi API LLM bên ngoài, không gửi dữ liệu ra khỏi mạng tổ chức.
(2026-09-09)

## Mô hình kinh doanh

Ngoài phạm vi đồ án. Bản triển khai là cài đặt độc lập cho từng doanh nghiệp (không SaaS,
không multi-tenant, không thanh toán). (2026-09-09)

## Ghi chú khác

| Câu hỏi | Trả lời | Ngày | Skill hỏi |
|---|---|---|---|
| Nguồn corpus thực nghiệm | Đa dạng loại tài liệu, ưu tiên **văn bản pháp quy công khai** trước (an toàn bản quyền, tái lập được) | 2026-09-09 | /brainstorming |
| Quy mô kho mục tiêu | ~1.000 tài liệu; ~30% nhiều bảng biểu; file tối đa 50MB | 2026-09-09 | /brainstorming |
| Điều kiện phần cứng bản giao | **Bắt buộc có GPU** ≥6GB VRAM; không hỗ trợ chế độ chỉ-CPU | 2026-09-09 | /brainstorming |
| Baseline so sánh ngoài | Không cần — chỉ ablation nội bộ | 2026-09-09 | /brainstorming |
| Chấm điểm bộ 50 câu agent | LLM-as-judge (Qwen2.5 cục bộ) + chấm tay đối chứng 20% mẫu | 2026-09-09 | /brainstorming |
