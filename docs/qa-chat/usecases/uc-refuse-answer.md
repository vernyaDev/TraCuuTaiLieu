# UC-refuse-answer — Từ chối trả lời khi không đủ căn cứ

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level) — kết cục hợp lệ của một lượt hỏi, không phải nhánh lỗi.

## Primary Actor

Nhân viên.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Nhân viên | Biết rõ hệ thống không trả lời được, và biết nên làm gì tiếp |
| Doanh nghiệp | Không có câu trả lời bịa nào được đem đi dùng vào việc thật |
| Quản trị tri thức | Mỗi lần từ chối là một tín hiệu về kho tri thức, phải được ghi lại |
| Tác giả đồ án | Tỉ lệ từ chối đúng và từ chối sai phải đo được tách bạch |

## Trigger

Một trong ba tình huống: không đoạn nào vượt ngưỡng liên quan; phạm vi quyền của nhân viên không
chứa tài liệu nào liên quan; hoặc mọi ý trong câu trả lời vừa sinh đều không gắn được về nguồn.

## Preconditions

Nhân viên đã gửi một câu hỏi hợp lệ và hệ thống đã xử lý tới bước phát hiện thiếu căn cứ.

## Minimal Guarantee

Nhân viên nhận thông báo từ chối nêu được lý do thuộc loại nào, và không nội dung nào ngoài quyền
bị tiết lộ — kể cả việc **có tồn tại** một tài liệu phù hợp mà họ không được xem.

## Success Guarantee

Nhân viên hiểu vì sao không có câu trả lời và biết việc cần làm tiếp. Câu hỏi được ghi vào dữ liệu
khoảng trống với đúng lý do, để quản trị tri thức xử lý.

## Main Success Scenario

1. Hệ thống xác định không đủ căn cứ để trả lời.
2. Hệ thống phân loại lý do: kho không có thông tin, phạm vi quyền không chứa tài liệu liên quan,
   hay câu hỏi nằm ngoài phạm vi kho tri thức nội bộ.
3. Hệ thống hiển thị thông báo từ chối tương ứng, viết bằng lời nghiệp vụ, kèm việc nhân viên có
   thể làm tiếp.
4. Hệ thống ghi câu hỏi, thời điểm, người hỏi và lý do từ chối vào dữ liệu khoảng trống tri thức.
5. Nhân viên đọc thông báo, và hỏi lại theo cách khác hoặc liên hệ quản trị tri thức.

## Extensions

**1a. Từ chối ở tầng một, trước khi gọi mô hình ngôn ngữ**
- 1a1. Không đoạn nào vượt ngưỡng liên quan. Hệ thống dừng ngay, không tốn thời gian sinh chữ cho
  một câu trả lời sẽ bị loại. Tiếp tục bước 2.

**1b. Từ chối ở tầng hai, sau khi đã sinh câu trả lời**
- 1b1. Mọi ý đều không gắn được về nguồn. Câu trả lời đã sinh bị loại bỏ hoàn toàn, không hiển thị
  phần nào cho nhân viên.
- 1b2. Lý do được ghi riêng, phân biệt với trường hợp không truy hồi được gì ngay từ đầu. Tiếp tục
  bước 2.

**2a. Lý do là thiếu quyền**
- 2a1. Thông báo nói nhân viên có thể chưa được cấp quyền trên tài liệu liên quan và đề nghị liên
  hệ quản trị tri thức.
- 2a2. Thông báo **không** tiết lộ có tồn tại tài liệu phù hợp — sự tồn tại của tài liệu cũng là
  thông tin cần giữ.
- 2a3. Bản ghi khoảng trống mang lý do thiếu quyền, để quản trị biết đây là việc cấu hình quyền
  chứ không phải thiếu tài liệu.

**2b. Lý do là câu hỏi ngoài phạm vi kho tri thức**
- 2b1. Hệ thống nói rõ đây không phải việc kho tri thức nội bộ trả lời được.
- 2b2. **Không** tạo bản ghi khoảng trống — đây không phải tín hiệu về kho tri thức.

**3a. Có căn cứ nhưng mỏng**
- 3a1. Hệ thống **không** từ chối. Nó trả lời dựa trên đoạn đã có và nêu rõ căn cứ còn mỏng — từ
  chối quá tay cũng là hỏng, và tỉ lệ từ chối sai được đo riêng vì lý do này.
- 3a2. Bản ghi khoảng trống vẫn được tạo với dấu hiệu căn cứ mỏng. Use case kết thúc theo hướng
  đã trả lời.

**4a. Câu hỏi là câu hỏi tiếp nối đã được viết lại**
- 4a1. Bản ghi lưu cả câu gốc lẫn câu đã viết lại.

**4b. Việc ghi nhận thất bại**
- 4b1. Không làm hỏng lượt hỏi đáp; nhân viên vẫn nhận thông báo từ chối bình thường.

**4c. Câu hỏi rỗng hoặc quá ngắn, đã bị chặn ngay ở khâu nhập**
- 4c1. Không tạo bản ghi khoảng trống.

## Ghi chú đo lường

Bộ 50 câu đánh giá tầng agent có 10 câu cố tình không có đáp án trong kho. Nhờ đó use case này đo
được bằng bốn con số tách bạch, thay vì một tỉ lệ đúng chung chung che mất hành vi từ chối:

| Con số | Đo trên | Ý nghĩa |
|---|---|---|
| Tỉ lệ từ chối đúng | 10 câu không có đáp án | Cơ chế chống bịa có hoạt động không |
| Tỉ lệ trả lời bịa | 10 câu không có đáp án | Phần lọt lưới |
| Tỉ lệ từ chối sai | 40 câu có đáp án | Cơ chế có quá tay không |
| Tỉ lệ trả lời đúng | 40 câu có đáp án | Chất lượng chung |

## Related Requirements

- Story: US-023, US-024, US-027, US-008, US-031
- Năng lực: `CAP-qa-chat-04`, `CAP-knowledge-gap-01`
