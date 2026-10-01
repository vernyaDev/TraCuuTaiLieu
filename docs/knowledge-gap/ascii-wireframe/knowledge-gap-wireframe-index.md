---
type: screen-index
feature: knowledge-gap
status: draft
updated: 2026-09-23
links:
  - docs/knowledge-gap/userstories/knowledge-gap-story-index.md
  - docs/_shared/design-system.md
---

# Báo cáo khoảng trống tri thức — Danh sách màn hình

> Hệ thống thị giác và luật phủ trạng thái ở `docs/_shared/design-system.md`.

## Screens

| Mã màn | Slug | Tên màn | Thuộc flow | Wireframe | Use case | Story | Trạng thái |
|---|---|---|---|---|---|---|---|
| `SCR-knowledge-gap-01` | `bao-cao-khoang-trong` | Báo cáo khoảng trống tri thức | `bao-cao-khoang-trong` | [bao-cao-khoang-trong.md](bao-cao-khoang-trong.md) | `UC-refuse-answer` | US-027, US-028 | draft |

> Cột `Slug` là định danh máy đọc, khớp `## Screen: {slug}` trong tệp wireframe và Mục 2 của
> `../srs/knowledge-gap-userflow.md`. Bảng mô tả từng phần tử sống trong tệp wireframe.

## Descriptions

### SCR-knowledge-gap-01 — Báo cáo khoảng trống tri thức

Màn trả lời một câu hỏi duy nhất: **kho tri thức đang thiếu gì, và thiếu cái nào trước.**

**Bố cục.** Thanh bên trái điều hướng khu quản trị. Vùng chính: tiêu đề màn, bộ lọc thời gian, ba
tab, rồi danh sách nhóm.

**Ba tab, không phải một danh sách trộn.** Tab *thiếu tài liệu* · tab *thiếu quyền* · tab *căn cứ
mỏng*. Ba loại này tách bạch vì việc cần làm khác hẳn nhau: nạp thêm tài liệu · gán quyền cho người
đã có sẵn tài liệu trong kho · củng cố tài liệu đang mỏng căn cứ. Trộn chung thì con số tổng không
nói lên điều gì hành động được.

Tab thứ ba chứa loại bản ghi mà hệ thống **đã trả lời được** nhưng chỉ dựa trên một đoạn liên quan
thấp (`AC-006` của US-027, nhánh 3a của `UC-refuse-answer`) — khoảng trống sắp thành vấn đề. Con số
trên thẻ ở hai tab đầu đếm số lần bị hỏi mà không trả lời được; ở tab thứ ba đếm số lần đã trả lời
được nhưng căn cứ mỏng. Gộp tab thứ ba vào tab *thiếu tài liệu* sẽ làm chính con số ấy sai nghĩa.
Chốt 2026-09-23.

Mỗi tab có thêm bộ lọc phụ theo trạng thái: *chờ xử lý* và *đã xử lý*.

**Danh sách nhóm.** Mỗi nhóm là một thẻ ngang, xếp theo số lần được hỏi giảm dần:

- Bên trái: một con số lớn — số lần chủ đề này bị hỏi mà không trả lời được.
- Giữa: **câu hỏi đại diện** của nhóm, viết như người dùng đã hỏi, cộng một dòng chữ phụ nêu khoảng
  thời gian từ lần hỏi đầu tới lần gần nhất.
- Bên phải: nút *đánh dấu đã xử lý*, và mũi tên mở rộng.

Mở rộng một nhóm thì thấy danh sách câu hỏi cụ thể thuộc nhóm đó, kèm thời điểm. Với nhóm thiếu
quyền, mỗi câu kèm thêm phòng ban của người hỏi — đủ để quản trị biết nên cấp quyền cho phòng nào,
mà không biến màn này thành công cụ theo dõi cá nhân.

**Ranh giới phải giữ, ghi ra vì rất dễ trượt.** Màn này trình bày theo **chủ đề và tần suất**, không
có bộ lọc theo người dùng và không có đường nào liệt kê mọi câu một người đã hỏi. Câu hỏi của nhân
viên tiết lộ họ đang quan tâm việc gì; một công cụ quản trị tri thức không được lặng lẽ biến thành
công cụ giám sát.

**Nhóm đã xử lý mà vẫn bị hỏi.** Khi một nhóm đã đánh dấu xử lý nhưng sau đó lại có người hỏi và
vẫn bị từ chối, nhóm quay lại tab chờ xử lý kèm một thẻ nhỏ *đã xử lý nhưng chưa hết*. Đây là tín
hiệu tài liệu vừa nạp không trả lời được câu hỏi thật — có giá trị hơn nhiều so với việc lặng lẽ
tạo một nhóm mới.

**Trạng thái phải phủ:**

| Trạng thái | Thể hiện |
|---|---|
| Rỗng lần đầu | Chưa có câu hỏi nào bị từ chối. Nói rõ đây là trạng thái tốt và giải thích màn này sẽ hiện gì khi có dữ liệu — màn rỗng không giải thích thì người dùng tưởng hỏng |
| Rỗng do bộ lọc | Nói rõ do khoảng thời gian đang chọn, kèm nút mở rộng khoảng |
| Đang tải | Khung xương giữ hình dạng danh sách thẻ |
| Lỗi tải | Thử lại tại chỗ |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn màn này thì nhận thông báo không có quyền truy cập, không hiện dữ liệu nào |
| Đang gửi | Khi đánh dấu đã xử lý |
| Chỉ đọc / Vô hiệu hoá | Không áp dụng — màn này chỉ có một thao tác ghi duy nhất |
