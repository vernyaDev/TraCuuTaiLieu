---
type: decision
decision_id: D-2026-09-09-methodology-shape
feature: _project
status: active
decided_at: 2026-09-09
decided_by: "@longdt"
source: "Phiên brainstorming 2026-09-09"
updated: 2026-09-09
links:
  - .project-config.yml
  - docs/_shared/project-profile.md
---

# Decision: Hình thái đặc tả `prd-stories`, PRD hợp nhất ở cấp dự án

> Decided: 2026-09-09 | By: @longdt | Source: Phiên brainstorming 2026-09-09

## Context

Engagement là đồ án tốt nghiệp một người làm, 14h/tuần, mốc bảo vệ 10/01/2027. Quy tắc
`methodology-shape.md` bắt chốt hình thái trước khi viết dòng spec đầu tiên. Bảng đếm 8 câu
cho kết quả hoà 4–4 giữa `srs-only` và `stories-only`.

## Decision

Hình thái đặc tả là **`prd-stories`**: một PRD cấp dự án làm nguồn năng lực, backlog user story
kèm acceptance criteria làm xương sống, cộng một tầng đặc tả mỏng cho use case và yêu cầu phi
chức năng. **Không tách PRD riêng cho từng feature** — năng lực `CAP-{feature}-NN` khai thẳng
trong Feature Map của `docs/_product/prd.md`.

## Rationale

Bảng bias:

| Câu hỏi | Trả lời | Nghiêng về |
|---|---|---|
| Ràng buộc compliance? | Không | stories-only |
| Bàn giao cho vendor/đội ngoài? | Không | stories-only |
| Cỡ đội | 1 người | stories-only |
| QA tách riêng? | Không, tự test | stories-only |
| Phạm vi biến động? | Cố định bởi đề cương + GVHD | srs-only |
| Người đọc cần gì? | Hội đồng cần đặc tả có cấu trúc, truy vết được | srs-only |
| Văn bản ràng buộc? | Đề cương duyệt 20/09/2026 | srs-only |
| Vòng release | Một lần duy nhất (bảo vệ) | srs-only |

Hoà 4–4; tie-breaker của rule (compliance > vendor > độ biến động > cỡ đội) đẩy về `srs-only`.
Nhưng bản yêu cầu của Product Owner đã liệt kê thẳng bộ deliverable: user story + AC, use case,
danh sách màn hình, NFR có con số, ưu tiên MoSCoW. Theo mục "Hard override" của
`methodology-shape.md`, deliverable đã được chỉ định thì bảng đếm không có quyền phủ quyết.

Quyết định phụ **gộp PRD**: tách 6 PRD feature cho một đội một người là chi phí tài liệu thuần
tuý, đúng thứ `doc-leanness.md` cấm. Mã `CAP-{feature}-NN` vẫn giữ nguyên ngữ pháp canon nên
truy vết `CAP → US → AC` không mất mắt xích nào.

## Alternatives Considered

| Option | Pros | Cons | Why rejected |
|---|---|---|---|
| `srs-only` | Bảng bias nghiêng về; hội đồng quen đọc SRS | Không có backlog để lập sprint; PO đã yêu cầu story + MoSCoW | Trái deliverable đã chỉ định |
| `stories-only` | Rẻ nhất về tài liệu | Không có tầng năng lực để lập MoSCoW cấp tính năng; NFR không có chỗ ở | Mất tầng cần cho chương "Phân tích thiết kế" của báo cáo |
| `prd-stories` + 6 PRD feature | Đúng canon nhất | ~6 tài liệu nữa, ~10h công cho một người đã thiếu giờ | Chi phí tài liệu không đổi lấy giá trị nào |

## Consequences

### Positive
- Backlog story dùng thẳng để xếp sprint 2 tuần từ 05/10 đến 20/12/2026.
- Một nguồn năng lực duy nhất, không phải đồng bộ 6 PRD khi phạm vi bị cắt.

### Negative / Trade-offs
- `docs/_product/prd.md` phình hơn PRD project-level thông thường vì ôm cả Feature Map lẫn CAP.
- Không có tài liệu SRS đầy đủ 9 section; chương "Phân tích thiết kế" của báo cáo phải tổng hợp
  từ PRD + use case + NFR thay vì trích một file.

### Risks
- Nếu GVHD yêu cầu nộp SRS đúng khuôn ở buổi duyệt đề cương 20/09/2026 thì phải đổi shape —
  đó là Change Request, không phải sửa `.project-config.yml` im lặng.

## Affected Artifacts

* `.project-config.yml` (`methodology: prd-stories`)
* `docs/_product/prd.md`
* Toàn bộ `docs/{feature}/userstories/**` sẽ sinh sau

## Review / Supersede

* Status: `active`
* Replacement: —
