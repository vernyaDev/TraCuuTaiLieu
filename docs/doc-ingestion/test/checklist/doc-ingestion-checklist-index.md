---
type: test-checklist-index
feature: doc-ingestion
status: draft
updated: 2026-09-24
next_chk_id: 110
links:
  - docs/doc-ingestion/test/test-strategy.md
  - docs/doc-ingestion/userstories/doc-ingestion-story-index.md
  - docs/doc-ingestion/ascii-wireframe/doc-ingestion-wireframe-index.md
---

# Nạp và quản lý vòng đời tài liệu — Danh mục kiểm thử

## Checklists

| Tệp | Phạm vi | Số mục | Cập nhật |
|---|---|---|---|
| [checklist-feature.md](checklist-feature.md) | Cả tính năng | 109 | 2026-09-24 |

Quy ước đọc một dòng và lý do cột nghĩa vụ trỏ về `US-NNN/AC-NNN`: xem
`../../../qa-chat/test/checklist/qa-chat-checklist-index.md`.

## Coverage — theo từng nghĩa vụ

| Story | Số tiêu chí | Nghĩa vụ → mục kiểm | Trạng thái |
|---|---|---|---|
| US-009 | 8 | AC-001→001 · AC-002→002 · AC-003→003 · AC-004→004 · AC-005→005/006 · AC-006→007/008 · AC-007→009 · AC-008→010 | covered |
| US-010 | 8 | AC-001→015 · AC-002→016/017 · AC-003→018 · AC-004→019 · AC-005→020/021 · AC-006→022 · AC-007→023 · AC-008→024 | covered |
| US-011 | 7 | AC-001→025 · AC-002→026 · AC-003→027 · AC-004→028 · AC-005→029 · AC-006→030 · AC-007→031 | covered |
| US-012 | 8 | AC-001→034 · AC-002→035/036 · AC-003→037 · AC-004→038 · AC-005→039 · AC-006→040 · AC-007→041 · AC-008→042 | covered |
| US-013 | 9 | AC-001→045/046 · AC-002→047/048 · AC-003→049/050 · AC-004→051/052 · AC-005→053 · AC-006→054 · AC-007→055 · AC-008→056 · AC-009→057/058 | covered |
| US-014 | 8 | AC-001→061 · AC-002→062 · AC-003→063 · AC-004→064 · AC-005→065 · AC-006→066 · AC-007→067 · AC-008→068 | covered |
| US-015 | 8 | AC-001→071 · AC-002→072 · AC-003→073/074 · AC-004→075 · AC-005→076 · AC-006→077/078 · AC-007→079/080 · AC-008→081 | covered |
| US-016 | 7 | AC-001→083 · AC-002→084 · AC-003→085 · AC-004→086/087 · AC-005→088/089 · AC-006→090 · AC-007→091 | covered |

**Tổng: 63 nghĩa vụ · 63 covered · 0 blocked.** Phạm vi đóng được.

## Phân bố mức và cách chạy

| | Số mục |
|---|---|
| Tổng | 109 |
| Mức 1 nghiêm trọng | 62 |
| Mức 2 cao | 43 |
| Mức 3 trung bình | 4 |
| Chạy máy được `[Yes]` | 77 |
| Người chạy tay `[No]` | 32 |

Ba mươi hai mục chạy tay tập trung ở **chất lượng tách đoạn và bảng biểu** — thứ không kiểm được
bằng cách bấm nút. Bảng lớn có bị cắt ngang một hàng không, ô gộp có làm lệch quan hệ hàng cột
không, bốn định dạng có cho ra cấu trúc tương đương không: những câu ấy phải mở tài liệu gốc ra đối
chiếu bằng mắt. Đó cũng chính là lý do màn xem trước đoạn tồn tại.

## Ba cổng chặn — nơi tập trung mục mức nghiêm trọng

| Cổng | Mục kiểm |
|---|---|
| Nhãn mật, ở màn tải lên | `011`, `012`, `013` |
| Xác nhận metadata, trước khi vào phạm vi truy hồi | `047`, `048` |
| Gán quyền, mặc định đóng | `095`, `096` |

Bỏ sót một cổng nào cũng dẫn tới hậu quả khác nhau, nên mỗi cổng có mục kiểm riêng thay vì gộp
chung vào luồng nạp.

## Retired CHK-IDs

Chưa có.

## Điều danh mục này cố ý KHÔNG phủ

- **Ngưỡng thời gian nạp.** `NFR-system-016` đến `018` có ngưỡng nhưng đo được trên kho thật, không
  đo trong đợt nghiệm thu giao diện này.
- **Chất lượng tách đoạn tính bằng số.** Việc so ba chiến lược tách bằng độ chính xác và độ phủ
  thuộc bộ đo của `evaluation`, không thuộc danh mục này.
