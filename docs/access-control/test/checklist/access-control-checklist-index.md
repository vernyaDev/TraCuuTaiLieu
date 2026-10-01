---
type: test-checklist-index
feature: access-control
status: draft
updated: 2026-09-24
next_chk_id: 119
links:
  - docs/access-control/test/test-strategy.md
  - docs/access-control/userstories/access-control-story-index.md
  - docs/access-control/ascii-wireframe/access-control-wireframe-index.md
---

# Tài khoản và phân quyền — Danh mục kiểm thử

## Checklists

| Tệp | Phạm vi | Số mục | Cập nhật |
|---|---|---|---|
| [checklist-feature.md](checklist-feature.md) | Cả tính năng | 118 | 2026-09-24 |

Cách đọc một dòng, và lý do cột nghĩa vụ trỏ về `US-NNN/AC-NNN` thay vì mã đặc tả: xem phần tương
ứng ở `../../../qa-chat/test/checklist/qa-chat-checklist-index.md` — quy ước dùng chung, không lặp.

## Coverage — theo từng nghĩa vụ

Mỗi ô ghi `tiêu chí→mục kiểm`. Số là ba chữ số cuối của mã `CHK-access-control-NNN`.

| Story | Số tiêu chí | Nghĩa vụ → mục kiểm | Trạng thái |
|---|---|---|---|
| US-001 | 6 | AC-001→001 · AC-002→002/003/004/005 · AC-003→006 · AC-004→025/026 · AC-005→027/028 · AC-006→029 | covered |
| US-002 | 7 | AC-001→031 · AC-002→032 · AC-003→033 · AC-004→034 · AC-005→016/017/024 · AC-006→035/036/037 · AC-007→038/039 | covered |
| US-003 | 7 | AC-001→044 · AC-002→045/046 · AC-003→047/048/051 · AC-004→049 · AC-005→050 · AC-006→052 · AC-007→053/054/055 | covered |
| US-004 | 7 | AC-001→058 · AC-002→059 · AC-003→060 · AC-004→061 · AC-005→062/063 · AC-006→064/065/066 · AC-007→067 | covered |
| US-005 | 7 | AC-001→070 · AC-002→071 · AC-003→072 · AC-004→073 · AC-005→074/075 · AC-006→076 · AC-007→077 | covered |
| US-006 | 6 | AC-001→078 · AC-002→079 · AC-003→080 · AC-004→081 · AC-005→082 · AC-006→083 | covered |
| US-007 | 7 | AC-001→085/086 · AC-002→087 · AC-003→088 · AC-004→089 · AC-005→090 · AC-006→042 · AC-007→091 | covered |
| US-008 | 8 | AC-001→092 · AC-002→093 · AC-003→094 · AC-004→095 · AC-005→096 · AC-006→097 · AC-007→098 · AC-008→099 | covered |

**Tổng: 55 nghĩa vụ · 55 covered · 0 blocked.** Phạm vi đóng được.

## Phân bố mức và cách chạy

| | Số mục |
|---|---|
| Tổng | 118 |
| Mức 1 nghiêm trọng | 79 |
| Mức 2 cao | 34 |
| Mức 3 trung bình | 5 |
| Chạy máy được `[Yes]` | 76 |
| Người chạy tay `[No]` | 42 |

**Hai phần ba số mục ở mức nghiêm trọng, và hơn một phần ba phải chạy tay** — cao hơn hẳn ba tính
năng kia. Đây là hệ quả tự nhiên của bản chất tính năng: phần lớn hành vi của nó **không hiện ra
trên giao diện**. Quyền có áp đúng không, lọc có diễn ra trước bước xếp hạng không, mục từ chối có
thắng quyền cho phép không — muốn biết thì phải hỏi bằng hai tài khoản khác quyền rồi so kết quả,
hoặc soi thẳng dữ liệu. Không màn hình nào nói cho biết.

## Retired CHK-IDs

Chưa có.

## Điều danh mục này cố ý KHÔNG phủ

- **Kiểm thử thâm nhập.** Mục bảo mật ở đây là kiểm nghiệm thu: có chặn đúng vai không, có lộ sự
  tồn tại tài khoản không. Không dò lỗ hổng, không thử tiêm mã.
- **Đo hiệu năng lọc quyền có ngưỡng.** `CHK-access-control-097` kiểm độ phủ không tụt khi phạm vi
  hẹp, nhưng con số ngưỡng chờ phép đo ở sprint 3.
