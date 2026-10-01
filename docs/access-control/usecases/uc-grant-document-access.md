# UC-grant-document-access — Gán quyền đọc tài liệu

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level).

## Primary Actor

Quản trị tri thức.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Quản trị tri thức | Cấu hình quyền nhanh cho kho 1.000 tài liệu, và xử lý được ngoại lệ mà không phá cách gán theo phòng ban |
| Nhân viên | Đọc được đúng những gì công việc của mình cần |
| Doanh nghiệp | Tài liệu nhạy cảm không lọt vào câu trả lời của người không có quyền, kể cả gián tiếp |

## Trigger

Quản trị tri thức mở khối quyền truy cập của một tài liệu, hoặc chọn nhiều tài liệu để gán hàng loạt.

## Preconditions

- Quản trị tri thức đã đăng nhập với quyền quản trị tri thức.
- Tài liệu đã ở trạng thái sẵn sàng.
- Phòng ban và người dùng cần gán đã tồn tại.

## Minimal Guarantee

Cấu hình quyền không bao giờ rơi vào trạng thái mở rộng hơn ý định của người gán. Tài liệu chưa
gán quyền cho ai thì không ai đọc được.

## Success Guarantee

Đúng những người được dự định đọc được tài liệu; thay đổi có hiệu lực ngay ở câu hỏi kế tiếp mà
không cần ai đăng nhập lại; và quản trị tra được vì sao một người cụ thể đọc được hay không.

## Main Success Scenario

1. Quản trị tri thức mở khối quyền truy cập của tài liệu.
2. Hệ thống hiển thị ai đang đọc được, phân biệt rõ quyền gán trực tiếp với quyền đến từ phòng ban,
   và các mục từ chối đang áp.
3. Quản trị tri thức chọn chủ thể được cấp quyền — một phòng ban hoặc một người dùng.
4. Hệ thống ghi nhận quyền cho phép.
5. Quản trị tri thức kiểm lại bằng công cụ tra cứu: chọn một người dùng cụ thể và xem hệ thống trả
   lời người đó đọc được hay không, kèm lý do.
6. Ở câu hỏi kế tiếp của nhân viên thuộc phạm vi vừa cấp, tài liệu nằm trong tập được phép và có
   thể xuất hiện trong trích dẫn.

## Extensions

**2a. Tài liệu chưa gán quyền cho ai**
- 2a1. Hệ thống hiển thị rõ trạng thái này, và tài liệu không xuất hiện trong kết quả của bất kỳ ai.

**3a. Cần cấp quyền cho một người ngoài phòng ban được cấp**
- 3a1. Quản trị gán quyền trực tiếp cho người đó trên đúng tài liệu ấy.
- 3a2. Người đó đọc được **chỉ** tài liệu ấy, không kéo theo tài liệu nào khác của phòng ban.

**3b. Cần chặn một người trong phòng ban đã được cấp quyền**
- 3b1. Quản trị đặt mục **từ chối** cho người đó trên tài liệu.
- 3b2. Từ chối thắng mọi cho phép, không xét thứ tự gán, không xét chủ thể gán là người hay phòng ban.
- 3b3. Các thành viên khác của phòng ban vẫn đọc được bình thường.

**3c. Gán hàng loạt nhiều tài liệu**
- 3c1. Quản trị chọn nhiều tài liệu và cấp quyền cho một chủ thể trong một thao tác.
- 3c2. Hệ thống áp cho toàn bộ và báo số tài liệu đã áp dụng.

**3d. Chủ thể được chọn đã bị xoá**
- 3d1. Hệ thống từ chối và báo chủ thể không còn tồn tại.

**4a. Nhãn mật của tài liệu cao hơn mức mật của người được cấp**
- 4a1. Quyền vẫn ghi nhận được, nhưng hệ thống cảnh báo người đó vẫn không đọc được vì nhãn mật —
  hai điều kiện cùng phải thoả, mức mật cao không tự sinh ra quyền đọc và quyền đọc không vượt
  được nhãn mật.

**5a. Người dùng không đọc được mà quản trị không hiểu vì sao**
- 5a1. Công cụ tra cứu nêu rõ nguyên nhân: chưa được cấp qua đường nào, bị mục từ chối nào chặn,
  hay bị nhãn mật chặn.

**6a. Quyền bị thu hồi sau đó**
- 6a1. Ở câu hỏi kế tiếp, tài liệu biến khỏi tập được phép của người đó, kể cả khi họ vừa thấy nó
  một phút trước.
- 6a2. Trích dẫn cũ trong hội thoại đang mở không mở được nữa — quyền kiểm tại thời điểm mở.

**6b. Phòng ban được cấp quyền bị xoá**
- 6b1. Hệ thống cảnh báo số thành viên và số tài liệu bị ảnh hưởng, buộc xác nhận lần hai.
- 6b2. Sau khi xoá, quyền gán qua phòng ban đó biến mất; thành viên chỉ còn quyền gán trực tiếp.

**6c. Người dùng đổi phòng ban**
- 6c1. Quyền đọc thay đổi theo ngay ở câu hỏi kế tiếp, không cần họ đăng nhập lại.

**6d. Nâng nhãn mật của tài liệu đã cấp quyền rộng**
- 6d1. Hệ thống cảnh báo số người sẽ mất quyền đọc trước khi xác nhận.

## Ghi chú thi hành

Luật đọc được phát biểu một lần duy nhất tại US-008 và thi hành tại bước truy hồi:

> Đọc được khi và chỉ khi `(cấp qua phòng ban HOẶC cấp trực tiếp)` **và** `nhãn mật ≤ mức mật`
> **và** `không có mục từ chối nào áp lên người đó hoặc phòng ban của người đó`.

Lọc quyền là **pre-filter** — tập được phép xác định trước khi xếp hạng, không phải lọc bỏ sau khi
đã lấy kết quả.

## Related Requirements

- Story: US-005, US-006, US-007, US-008, US-004, US-002
- Năng lực: `CAP-access-control-03`, `CAP-access-control-04`, `CAP-access-control-05`
