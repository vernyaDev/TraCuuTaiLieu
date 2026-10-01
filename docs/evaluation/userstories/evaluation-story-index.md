---
type: userstory-index
feature: evaluation
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Bộ đánh giá định lượng — Story Index

## Stories

| ID | Tiêu đề | Persona | Năng lực | Ưu tiên | Trạng thái | Cập nhật |
|---|---|---|---|---|---|---|
| US-029 | Nạp và sửa bộ câu hỏi kèm đáp án chuẩn | Tác giả đồ án | `CAP-evaluation-03` | Must | draft | 2026-09-09 |
| US-030 | Chạy đo tầng truy hồi theo ma trận cấu hình | Tác giả đồ án | `CAP-evaluation-01` | Must | draft | 2026-09-09 |
| US-031 | Chạy đo tầng agent với ba cấu hình bóc tách | Tác giả đồ án | `CAP-evaluation-02` | Must | draft | 2026-09-09 |
| US-032 | Chấm bằng mô hình cục bộ và xuất mẫu đối chiếu tay | Tác giả đồ án | `CAP-evaluation-02` | Must | draft | 2026-09-09 |
| US-033 | Xuất bảng kết quả dùng thẳng cho báo cáo | Tác giả đồ án | `CAP-evaluation-04` | Must | draft | 2026-09-09 |

## Vì sao nhóm này là Must ngang tính năng

Chương thực nghiệm là phần quyết định điểm số của đồ án. Một hệ thống chạy được mà không có số liệu
chứng minh nó chạy tốt tới đâu là một hệ thống chưa xong.

Nhóm này cũng phải làm **sớm**, không để dồn: bộ đo tầng truy hồi là công cụ để chọn chiến lược
tách đoạn, nên nó phải có trước khi chốt cách tách đoạn, không phải sau.

## Ranh giới với phần còn lại của sản phẩm

Bộ đo là công cụ của tác giả, không phải tính năng người dùng cuối chạm vào. Nó chạy bằng lệnh,
không cần giao diện. Nhưng nó là **một phần của mã nguồn sản phẩm**, không phải kịch bản rời — để
mỗi lần đổi cấu hình là chạy lại được ngay, không phải dựng lại môi trường.

## Nguồn dữ liệu

Bộ ground truth 150 câu (100 câu tầng truy hồi, 50 câu tầng agent trong đó 10 câu cố tình không có
đáp án) là việc tài liệu, làm được song song với việc viết đề cương và không phụ thuộc vào việc có
code hay chưa.
