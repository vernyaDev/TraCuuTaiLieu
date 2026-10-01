---
type: test-checklist-index
feature: knowledge-gap
status: draft
updated: 2026-09-24
next_chk_id: 43
links:
  - docs/knowledge-gap/test/test-strategy.md
  - docs/knowledge-gap/userstories/knowledge-gap-story-index.md
  - docs/knowledge-gap/ascii-wireframe/knowledge-gap-wireframe-index.md
---

# Báo cáo khoảng trống tri thức — Danh mục kiểm thử

## Checklists

| Tệp | Phạm vi | Số mục | Cập nhật |
|---|---|---|---|
| [checklist-feature.md](checklist-feature.md) | Cả tính năng | 42 | 2026-09-24 |

Quy ước đọc một dòng: xem `../../../qa-chat/test/checklist/qa-chat-checklist-index.md`.

## Coverage — theo từng nghĩa vụ

| Story | Số tiêu chí | Nghĩa vụ → mục kiểm | Trạng thái |
|---|---|---|---|
| US-027 | 7 | AC-001→001 · AC-002→002 · AC-003→003 · AC-004→004/011/018 · AC-005→005 · AC-006→006 · AC-007→007 | covered |
| US-028 | 9 | AC-001→008 · AC-002→009 · AC-003→012/013 · AC-004→016 · AC-005→020/021/022/030 · AC-006→023 · AC-007→024/025/026 · AC-008→034/035 · AC-009→031/032/033 | covered |

**Tổng: 16 nghĩa vụ · 16 covered · 0 blocked.** Phạm vi đóng được.

## Phân bố mức và cách chạy

| | Số mục |
|---|---|
| Tổng | 42 |
| Mức 1 nghiêm trọng | 23 |
| Mức 2 cao | 17 |
| Mức 3 trung bình | 2 |
| Chạy máy được `[Yes]` | 29 |
| Người chạy tay `[No]` | 13 |

## Một nghĩa vụ được phủ bằng bốn mục, và đó là có chủ đích

`AC-005` của US-028 — *không dùng để theo dõi cá nhân* — được phủ bằng bốn mục: `020`, `021`, `022`
và `030`. Nhiều hơn hẳn các nghĩa vụ khác, vì đây là **nghĩa vụ mà bằng chứng nằm ở sự vắng mặt**.
Không có ô lọc theo người dùng, không có đường mạng trả về mọi câu của một người, không có định danh
cá nhân khi mở rộng nhóm, và liên kết khắc phục không mang tham số nào theo người hỏi. Mỗi mục kiểm
một chỗ vắng mặt khác nhau — kiểm một chỗ rồi kết luận là bỏ sót ba cửa còn lại.

Hai trong bốn mục ấy là `[No]`: việc **không tồn tại một đường mạng** không quan sát được qua giao
diện, phải rà mã nguồn hoặc rà danh sách đường mạng.

## Retired CHK-IDs

Chưa có.

## Điều danh mục này cố ý KHÔNG phủ

- **Ngưỡng gom nhóm câu hỏi.** `AC-001` của US-028 đòi gom câu cùng chủ đề, nhưng ngưỡng tương đồng
  chưa hiệu chỉnh — `CHK-knowledge-gap-008` kiểm *có gom hay không*, không kiểm *gom đúng mức nào*.
  Ngưỡng này đã được đưa vào `NFR-system-047` để hiệu chỉnh ở sprint 5.
- **Ngưỡng gộp phòng ban nhỏ.** Câu hỏi mở của sơ đồ luồng: phòng ban một hai người thì hiển thị
  phòng ban gần như chỉ đích danh người hỏi. Chưa chốt ngưỡng nên chưa có mục kiểm.
