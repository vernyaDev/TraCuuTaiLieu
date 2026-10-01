---
type: test-checklist-index
feature: qa-chat
status: draft
updated: 2026-09-24
next_chk_id: 125
links:
  - docs/qa-chat/test/test-strategy.md
  - docs/qa-chat/userstories/qa-chat-story-index.md
  - docs/qa-chat/ascii-wireframe/qa-chat-wireframe-index.md
---

# Hỏi đáp có trích dẫn — Danh mục kiểm thử

## Checklists

| Tệp | Phạm vi | Số mục | Cập nhật |
|---|---|---|---|
| [checklist-feature.md](checklist-feature.md) | Cả tính năng | 124 | 2026-09-24 |

## Cách đọc một dòng

```
[Mức] [Tự động] CHK-qa-chat-NNN → {nghĩa vụ nguồn} · {nội dung kiểm}
```

Mức: `[1]` nghiêm trọng · `[2]` cao · `[3]` trung bình · `[4]` thấp. Tự động: `[Yes]` chạy máy được ·
`[No]` người phải chạy tay — soi cơ sở dữ liệu, đua tranh đồng thời, phán đoán trải nghiệm, hoặc
việc cần đổi cấu hình rồi khởi động lại.

**Thích ứng của dự án này.** Khuôn gốc trỏ nghĩa vụ về mã `FR`/`BR`/`E` của đặc tả. Dự án chạy hình
thái PRD cộng story nên **không có mã nào trong số đó** — và chính **tiêu chí chấp nhận là nghĩa vụ
kiểm**: mỗi tiêu chí đã mang sẵn đúng một mệnh đề pass/fail, vừa khít cái mà cổng phủ cần. Nên cột
nghĩa vụ ở đây trỏ `US-NNN/AC-NNN`, cộng `SCR-*`, `CAP-*`, `NFR-system-*` cho những mục mức nền
không sinh ra từ một tiêu chí cụ thể.

## Coverage — theo từng nghĩa vụ

Mỗi ô ghi `tiêu chí→mục kiểm`. Số là ba chữ số cuối của mã `CHK-qa-chat-NNN`.

| Story | Số tiêu chí | Nghĩa vụ → mục kiểm | Trạng thái |
|---|---|---|---|
| US-017 | 9 | AC-001→021 · AC-002→022 · AC-003→051/052 · AC-004→023 · AC-005→025 · AC-006→121 · AC-007→072 · AC-008→010/011 · AC-009→020 | covered |
| US-018 | 7 | AC-001→015 · AC-002→013/014 · AC-003→050 · AC-004→016/017/018/019 · AC-005→102/103 · AC-006→012/112 · AC-007→104 | covered |
| US-019 | 7 | AC-001→054/055 · AC-002→056 · AC-003→097 · AC-004→098/099 · AC-005→060 · AC-006→058 · AC-007→057 | covered |
| US-020 | 7 | AC-001→026 · AC-002→027 · AC-003→028 · AC-004→029 · AC-005→030 · AC-006→031 · AC-007→032 | covered |
| US-021 | 7 | AC-001→034 · AC-002→035/039 · AC-003→036 · AC-004→037 · AC-005→038 · AC-006→091 · AC-007→092 | covered |
| US-022 | 8 | AC-001→041 · AC-002→042 · AC-003→043 · AC-004→044 · AC-005→045 · AC-006→046 · AC-007→047 · AC-008→118 | covered |
| US-023 | 9 | AC-001→061/062 · AC-002→063 · AC-003→069/070 · AC-003b→072/073 · AC-004→074 · AC-005→076 · AC-006→120 · **AC-007→loại trừ** · AC-008→077 | excluded-approved |
| US-024 | 8 | AC-001→065 · AC-002→064 · AC-003→066 · AC-004→067 · AC-005→024 · AC-006→068 · AC-007→119 · **AC-008→loại trừ** | excluded-approved |
| US-025 | 8 | AC-001→079 · AC-002→080/081 · AC-003→082 · AC-004→083 · AC-005→084 · AC-006→085 · AC-007→124 · AC-008→122/123 | covered |
| US-026 | 8 | AC-001→087 · AC-002→088 · AC-003→059 · AC-004→089 · AC-005→090 · AC-006→091/092 · AC-007→093 · AC-008→003/114 | covered |

**Tổng: 78 nghĩa vụ · 76 covered · 2 excluded-approved · 0 blocked.** Phạm vi đóng được.

### Hai nghĩa vụ loại trừ, kèm lý do

| Nghĩa vụ | Nội dung | Lý do loại trừ khỏi danh mục này | Duyệt |
|---|---|---|---|
| US-023/AC-007 | Báo cáo nêu tách bạch tỉ lệ từ chối đúng trên 10 câu không có đáp án và tỉ lệ từ chối sai trên 40 câu còn lại | Đây là nghĩa vụ của **báo cáo bộ đo**, không phải hành vi giao diện. Nó được kiểm trong danh mục của `evaluation`, nơi có bộ 50 câu và lệnh chạy | @longdt, 2026-09-24 |
| US-024/AC-008 | Báo cáo nêu tỉ lệ câu trả lời chứa ý không có trong nguồn, đo được cả khi bật và khi tắt tầng kiểm | Như trên. Mục `CHK-qa-chat-119` đã kiểm phần **tắt được tầng kiểm**; phần *đo ra con số* thuộc bộ đo | @longdt, 2026-09-24 |

> Hai nghĩa vụ này **không biến mất** — chúng chuyển sang danh mục của `evaluation`. Loại trừ ở đây
> nghĩa là "không kiểm bằng giao diện", không phải "không kiểm".

## Phân bố mức và cách chạy

| | Số mục |
|---|---|
| Tổng | 124 |
| Mức 1 nghiêm trọng | 61 |
| Mức 2 cao | 52 |
| Mức 3 trung bình | 11 |
| Chạy máy được `[Yes]` | 95 |
| Người chạy tay `[No]` | 29 |

Gần một nửa số mục ở mức nghiêm trọng — hợp lý với tính năng này, vì phần lớn hành vi của nó **là**
luận điểm được chấm: trích dẫn đúng chỗ, không bịa, không lộ tài liệu ngoài quyền.

Hai mươi chín mục phải chạy tay chia làm bốn nhóm: **soi cơ sở dữ liệu** khi kết quả không hiện lên
giao diện (bản ghi khoảng trống, việc không gọi mô hình) · **đổi cấu hình rồi khởi động lại** (ba
mục thí nghiệm bóc tách) · **phán đoán chất lượng** (tóm tắt có bịa không, có suy vượt nguồn không)
· **tiếp cận** (bàn phím, thứ tự tiêu điểm, trình đọc màn hình).

## Retired CHK-IDs

Chưa có. Mã đã cấp không tái dùng, không lấp khoảng trống.

## Điều danh mục này cố ý KHÔNG phủ

- **Ngưỡng hiệu năng định lượng.** Phần lớn phụ thuộc phép đo mô hình chưa chạy — xem Mục 7 của
  `../../_shared/architecture.md`. Bật bây giờ chỉ sinh ra mục chờ.
- **Kiểm thử thâm nhập.** Mục bảo mật ở đây là kiểm nghiệm thu, không phải dò lỗ hổng.
- **Tiếp cận sâu.** Đo tương phản và trình đọc màn hình đầy đủ nằm ngoài mức nền của đợt này.
