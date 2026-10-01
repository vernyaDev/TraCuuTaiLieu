# UC-follow-up-qa — Hỏi tiếp nối

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level).

## Primary Actor

Nhân viên.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Nhân viên | Hỏi tiếp bằng câu rút gọn tự nhiên mà vẫn được hiểu đúng |
| Doanh nghiệp | Câu hỏi rút gọn không dẫn tới truy hồi ngẫu nhiên rồi sinh ra câu trả lời nghe xuôi tai nhưng sai |

## Trigger

Nhân viên gửi một câu hỏi mới trong phiên đã có ít nhất một lượt trước.

## Preconditions

- Phiên hiện tại đã có ít nhất một cặp hỏi đáp.
- Các điều kiện của `UC-single-turn-qa` đều thoả.

## Minimal Guarantee

Hệ thống không truy hồi bằng một câu thiếu chủ đề. Nếu không bổ sung được ngữ cảnh thì nó hỏi lại
thay vì đoán.

## Success Guarantee

Câu hỏi rút gọn được viết lại thành câu độc lập đủ nghĩa, truy hồi chạy trên câu đã viết lại, và
nhân viên xem được câu đã viết lại để tự kiểm hệ thống có hiểu đúng ý mình không.

## Main Success Scenario

1. Nhân viên gửi câu hỏi rút gọn, ví dụ "còn nghỉ việc giữa năm thì sao?".
2. Hệ thống nhận ra câu hỏi này không tự đủ nghĩa và cần ngữ cảnh của các lượt trước.
3. Hệ thống lấy các lượt gần nhất trong phiên làm ngữ cảnh.
4. Hệ thống viết lại câu hỏi thành câu độc lập, ví dụ "Nghỉ việc giữa năm thì số ngày phép năm
   chưa dùng được tính thế nào?".
5. Hệ thống hiển thị câu đã viết lại ở dạng thu gọn để nhân viên mở ra xem khi cần.
6. Hệ thống thực hiện `UC-single-turn-qa` từ bước 3, dùng câu đã viết lại làm câu hỏi.
7. Nhân viên nhận câu trả lời kèm trích dẫn, và lượt này được thêm vào lịch sử phiên.

## Extensions

**2a. Câu hỏi tự nó đã đủ nghĩa, không liên quan lượt trước**
- 2a1. Hệ thống dùng nguyên câu hỏi, không nhét ngữ cảnh cũ vào làm lệch câu hỏi mới.
- 2a2. Tiếp tục bước 6.

**2b. Nhân viên đổi hẳn sang chủ đề khác**
- 2b1. Hệ thống nhận ra đây là chủ đề mới và không kéo ngữ cảnh cũ vào. Tiếp tục bước 6.

**2c. Đây là lượt đầu tiên của phiên, chưa có gì để bổ sung**
- 2c1. Hệ thống hỏi lại nhân viên làm rõ thay vì đoán. Use case kết thúc.

**3a. Hội thoại đã dài vượt cửa sổ ngữ cảnh**
- 3a1. Hệ thống thay các lượt cũ bằng một đoạn tóm tắt giữ chủ đề, thực thể và ràng buộc đã nêu,
  giữ nguyên văn các lượt gần nhất.
- 3a2. Phần hiển thị cho nhân viên **không** bị ảnh hưởng — họ vẫn cuộn lên xem đủ mọi lượt.
- 3a3. Tiếp tục bước 4.

**3b. Việc tóm tắt thất bại**
- 3b1. Hệ thống lùi về dùng các lượt gần nhất, báo hội thoại đã quá dài và mời mở phiên mới.

**4a. Việc viết lại thất bại**
- 4a1. Hệ thống lùi về dùng câu gốc và nói rõ nó không chắc đã hiểu đúng ngữ cảnh. Tiếp tục bước 6.

**5a. Nhân viên đọc câu đã viết lại và thấy sai ý mình**
- 5a1. Nhân viên gõ lại câu hỏi đầy đủ hơn. Quay lại bước 1.

**6a. Câu đã viết lại vẫn không tìm được căn cứ**
- 6a1. Hệ thống chuyển sang `UC-refuse-answer`, và ghi nhận **cả câu gốc lẫn câu đã viết lại** vào
  dữ liệu khoảng trống — nếu việc viết lại sai thì đó mới là nguyên nhân thật, không phải kho thiếu
  tài liệu.

## Related Requirements

- Story: US-020, US-021, US-026, US-017
- Năng lực: `CAP-qa-chat-02`
