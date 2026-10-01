# UC-multi-step-qa — Câu hỏi phức tạp đa bước

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level).

## Primary Actor

Nhân viên.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Nhân viên | Hỏi một câu nhiều vế và được trả lời đủ mọi vế, không phải tự tách câu hỏi |
| Tác giả đồ án | Đây là use case chứng minh kiến trúc agent hơn hỏi đáp một lượt; nó phải bật tắt được để chạy thí nghiệm bóc tách |
| Doanh nghiệp | Vế nào không có căn cứ thì phải nói rõ, không được lấp bằng suy diễn |

## Trigger

Nhân viên gửi câu hỏi chứa nhiều vế cần nhiều căn cứ khác nhau, ví dụ "phép năm chưa dùng có
chuyển sang năm sau không, nghỉ việc giữa năm thì tính sao?".

## Preconditions

- Các điều kiện của `UC-single-turn-qa` đều thoả.
- Bước phân rã đang được bật trong cấu hình.

## Minimal Guarantee

Nhân viên biết được vế nào đã được trả lời và vế nào không. Hệ thống không im lặng bỏ qua một vế.

## Success Guarantee

Mọi vế của câu hỏi đều được trả lời, mỗi vế có căn cứ riêng trích dẫn được, và các vế phụ thuộc
nhau được xử lý đúng thứ tự.

## Main Success Scenario

1. Nhân viên gửi câu hỏi nhiều vế.
2. Hệ thống nhận diện đây là câu hỏi phức tạp.
3. Hệ thống phân rã câu hỏi thành các truy vấn con, xác định vế nào phụ thuộc kết quả vế nào, và
   sắp thứ tự chạy theo đúng quan hệ phụ thuộc.
4. Hệ thống xác định tập tài liệu nhân viên được phép đọc.
5. Với truy vấn con thứ nhất, hệ thống truy hồi trong tập ấy và lấy ra các đoạn liên quan.
6. Hệ thống chạy tiếp các truy vấn con còn lại theo thứ tự, dùng kết quả vế trước làm đầu vào cho
   vế sau khi có quan hệ phụ thuộc.
7. Hệ thống tra trạng thái hiệu lực và chồng lấn của các tài liệu liên quan.
8. Hệ thống tổng hợp một câu trả lời duy nhất trả lời đủ mọi vế, hiển thị dần từng chữ.
9. Hệ thống kiểm mọi ý đều gắn được về một đoạn cụ thể; ý nào không gắn được thì loại.
10. Hệ thống hiển thị trích dẫn, và danh sách các truy vấn con đã chạy ở dạng thu gọn.
11. Nhân viên đọc câu trả lời và mở được từng trích dẫn để kiểm từng vế.

## Extensions

**2a. Hệ thống nhận nhầm câu đơn giản thành phức tạp**
- 2a1. Việc phân rã cho ra một truy vấn con duy nhất; hệ thống chạy như `UC-single-turn-qa`, chỉ
  tốn thêm một chút thời gian, không sai kết quả.

**2b. Bước phân rã bị tắt trong cấu hình**
- 2b1. Hệ thống xử lý như câu hỏi một lượt. Đây là nhánh dùng cho thí nghiệm bóc tách, không phải
  lỗi. Chuyển sang `UC-single-turn-qa`.

**3a. Số truy vấn con vượt giới hạn cấu hình**
- 3a1. Hệ thống dừng ở giới hạn, trả lời phần đã có căn cứ, và nói rõ câu hỏi quá rộng kèm đề nghị
  nhân viên chia nhỏ.

**3b. Việc phân rã thất bại**
- 3b1. Hệ thống lùi về đường một lượt và nói rõ nó xử lý câu hỏi như một khối.

**5a. Truy vấn con thứ nhất không tìm được căn cứ, và các vế sau phụ thuộc nó**
- 5a1. Hệ thống dừng chuỗi phụ thuộc đó, nêu rõ không trả lời được vế nào vì thiếu căn cứ ở vế nào.
- 5a2. Các vế độc lập còn lại vẫn được chạy. Tiếp tục bước 6.

**6a. Một vế độc lập không tìm được căn cứ**
- 6a1. Vế đó được đánh dấu không đủ căn cứ; các vế khác vẫn được trả lời. Tiếp tục bước 7.

**6b. Một truy vấn con quá thời gian chờ**
- 6b1. Hệ thống bỏ vế đó, nêu rõ trong câu trả lời, và tiếp tục các vế còn lại.

**8a. Không vế nào có căn cứ**
- 8a1. Hệ thống chuyển sang `UC-refuse-answer`.

**9a. Ý của một vế bị loại vì không gắn được về nguồn**
- 9a1. Vế đó chuyển thành không đủ căn cứ trong câu trả lời cuối; các vế khác giữ nguyên.

**10a. Nhân viên mở danh sách truy vấn con và thấy hệ thống hiểu sai câu hỏi**
- 10a1. Nhân viên diễn đạt lại câu hỏi. Quay lại bước 1.

## Related Requirements

- Story: US-022, US-017, US-023, US-024, US-031
- Năng lực: `CAP-qa-chat-03`
