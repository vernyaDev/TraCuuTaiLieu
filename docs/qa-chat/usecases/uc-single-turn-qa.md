# UC-single-turn-qa — Hỏi đáp một lượt

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level).

## Primary Actor

Nhân viên.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Nhân viên | Nhận câu trả lời đúng, đủ nhanh, và kiểm chứng được nguồn |
| Quản trị tri thức | Câu trả lời chỉ dựa trên tài liệu đã được duyệt đưa vào kho |
| Doanh nghiệp | Không nội dung ngoài quyền lọt tới người hỏi; không câu trả lời bịa được dùng vào việc thật |

## Trigger

Nhân viên gửi một câu hỏi ở màn hình hỏi đáp.

## Preconditions

- Nhân viên đã đăng nhập và phiên còn hiệu lực.
- Kho có ít nhất một tài liệu ở trạng thái sẵn sàng.
- Dịch vụ mô hình ngôn ngữ đang hoạt động.

## Minimal Guarantee

Nhân viên nhận một phản hồi rõ ràng — câu trả lời, câu hỏi lại làm rõ, hoặc lời từ chối có lý do.
Không nội dung nào ngoài phạm vi quyền của họ bị tiết lộ, kể cả gián tiếp.

## Success Guarantee

Nhân viên nhận câu trả lời cho câu hỏi của mình, mọi ý trong câu trả lời đều gắn được về một đoạn
tài liệu cụ thể mà họ có quyền đọc, và trích dẫn kèm theo mở được tới đúng đoạn ấy.

## Main Success Scenario

1. Nhân viên gõ câu hỏi tiếng Việt và gửi.
2. Hệ thống chuẩn hoá câu hỏi và nhận diện đây là câu hỏi đơn giản, một vế.
3. Hệ thống xác định tập tài liệu nhân viên được phép đọc.
4. Hệ thống tìm trong tập ấy bằng cả ba đường — theo từ khoá trên văn bản đã tách từ tiếng Việt,
   theo nghĩa, và hợp nhất hai kết quả — rồi lấy ra các đoạn liên quan nhất.
5. Hệ thống xác nhận có ít nhất một đoạn vượt ngưỡng liên quan.
6. Hệ thống tra trạng thái hiệu lực và tình trạng chồng lấn của các tài liệu chứa những đoạn ấy.
7. Hệ thống soạn câu trả lời từ các đoạn, và hiển thị dần từng chữ cho nhân viên ngay khi bắt đầu
   có nội dung.
8. Hệ thống kiểm mọi ý trong câu trả lời đều gắn được về một đoạn cụ thể; ý nào không gắn được thì loại.
9. Hệ thống hiển thị khối trích dẫn, mỗi trích dẫn kèm tên tài liệu và đường dẫn phân cấp tới đoạn.
10. Nhân viên đọc câu trả lời, bấm vào một trích dẫn để mở tài liệu gốc tại đúng đoạn, và tự xác nhận.

## Extensions

**2a. Câu hỏi rỗng hoặc quá ngắn không đủ nghĩa**
- 2a1. Hệ thống không xử lý, đề nghị nhân viên diễn đạt rõ hơn. Use case kết thúc.

**2b. Câu hỏi có nhiều vế cần nhiều căn cứ khác nhau**
- 2b1. Hệ thống chuyển sang `UC-multi-step-qa`.

**2c. Câu hỏi rút gọn, dựa vào lượt trước để hiểu**
- 2c1. Hệ thống chuyển sang `UC-follow-up-qa`.

**2d. Câu hỏi có thể hiểu theo hai cách dẫn tới hai đáp án khác nhau**
- 2d1. Hệ thống hỏi lại một câu làm rõ thay vì tự chọn một cách hiểu.
- 2d2. Nhân viên trả lời; quay lại bước 2.

**2e. Câu hỏi nằm ngoài phạm vi kho tri thức nội bộ**
- 2e1. Hệ thống nói rõ đây không phải việc kho tri thức trả lời được, và **không** trả lời bằng
  kiến thức chung của mô hình ngôn ngữ. Use case kết thúc.

**3a. Nhân viên chưa được cấp quyền trên tài liệu nào**
- 3a1. Hệ thống từ chối với lý do phân biệt được là vấn đề quyền, gợi ý liên hệ quản trị tri thức.
- 3a2. Hệ thống ghi nhận câu hỏi vào dữ liệu khoảng trống với lý do thiếu quyền. Use case kết thúc.

**5a. Không đoạn nào vượt ngưỡng liên quan**
- 5a1. Hệ thống chuyển sang `UC-refuse-answer` — dừng trước khi gọi mô hình ngôn ngữ.

**6a. Tài liệu chứa đoạn được dùng đã hết hiệu lực hoặc đã bị thay thế**
- 6a1. Hệ thống ưu tiên bản còn hiệu lực nếu có, và đính kèm cảnh báo vào câu trả lời ở bước 9.

**6b. Hai tài liệu chồng lấn cùng còn hiệu lực cùng trả lời được**
- 6b1. Hệ thống nêu cả hai và chỉ ra chỗ khác nhau, không tự chọn một bên.

**7a. Mô hình ngôn ngữ không phản hồi hoặc quá thời gian chờ**
- 7a1. Hệ thống dừng lượt, báo hệ thống đang bận và mời thử lại. Use case kết thúc.

**7b. Nhân viên bấm dừng giữa chừng**
- 7b1. Việc sinh dừng, phần đã hiện được giữ và đánh dấu chưa hoàn tất. Use case kết thúc.

**7c. Mất kết nối giữa chừng**
- 7c1. Hệ thống báo mất kết nối và giữ phần đã nhận. Use case kết thúc.

**8a. Mọi ý đều bị loại vì không gắn được về nguồn**
- 8a1. Hệ thống chuyển lượt này thành từ chối theo `UC-refuse-answer`.

**8b. Một phần ý bị loại**
- 8b1. Hệ thống trả về các ý còn lại, giữ câu trả lời mạch lạc. Tiếp tục bước 9.

**10a. Quyền của nhân viên trên tài liệu bị thu hồi sau khi câu trả lời đã hiện**
- 10a1. Hệ thống từ chối mở trích dẫn và báo không còn quyền truy cập.

**10b. Tài liệu đã bị gỡ hoặc xoá**
- 10b1. Hệ thống báo tài liệu không còn khả dụng, nêu rõ là đã gỡ hay đã xoá.

## Related Requirements

- Story: US-017, US-018, US-019, US-008, US-023, US-024, US-025
- Năng lực: `CAP-qa-chat-01`, `CAP-qa-chat-06`, `CAP-access-control-04`
