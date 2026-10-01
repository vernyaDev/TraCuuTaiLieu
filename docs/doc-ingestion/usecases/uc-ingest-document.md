# UC-ingest-document — Nạp tài liệu vào kho

## Scope

Hệ thống tra cứu tri thức nội bộ.

## Level

Mục tiêu người dùng (sea-level).

## Primary Actor

Quản trị tri thức.

## Stakeholders and Interests

| Bên liên quan | Quan tâm điều gì |
|---|---|
| Quản trị tri thức | Đưa tài liệu vào kho nhanh, biết rõ tài liệu nào hỏng và hỏng vì gì |
| Nhân viên | Tài liệu vào kho rồi thì hỏi được, và trích dẫn chỉ đúng điều khoản |
| Doanh nghiệp | Không tài liệu nào vào kho mà chưa ai đặt nhãn mật và xác nhận hiệu lực |

## Trigger

Quản trị tri thức chọn tệp và bắt đầu nạp ở màn hình quản trị tài liệu.

## Preconditions

- Quản trị tri thức đã đăng nhập với quyền quản trị tri thức.
- Hệ thống đang chạy đủ bốn dịch vụ.

## Minimal Guarantee

Kho không chứa mảnh vụn của tài liệu hỏng. Tài liệu chưa xác nhận metadata thì chưa vào phạm vi
truy hồi. Mọi tệp nạp hỏng đều có lý do đọc được.

## Success Guarantee

Tài liệu ở trạng thái sẵn sàng, đã tách đoạn theo cấu trúc với bảng biểu còn nguyên, có nhãn mật
và metadata vòng đời đã được người xác nhận, và nhân viên có quyền hỏi được nội dung của nó ngay.

## Main Success Scenario

1. Quản trị tri thức chọn một hoặc nhiều tệp docx, pdf, txt hoặc Markdown.
2. Quản trị tri thức chọn nhãn mật cho tài liệu.
3. Hệ thống nhận tệp và chuyển tài liệu sang trạng thái đang xử lý.
4. Hệ thống bóc văn bản từ tệp.
5. Hệ thống tách đoạn theo cấu trúc tài liệu, giữ đường dẫn phân cấp cho từng đoạn, và giữ nguyên
   bảng biểu — bảng dài thì chia theo hàng và lặp lại dòng tiêu đề ở mỗi mảnh.
6. Hệ thống đọc tài liệu và điền sẵn metadata vòng đời: ngày hiệu lực, ngày hết hiệu lực, văn bản
   bị thay thế, phân loại — kèm trích đoạn nguyên văn làm căn cứ cho từng giá trị.
7. Hệ thống hiển thị biểu mẫu xác nhận metadata.
8. Quản trị tri thức xem, sửa nếu cần, và xác nhận.
9. Hệ thống so tài liệu mới với các tài liệu còn hiệu lực cùng phân loại để phát hiện chồng lấn
   chủ đề.
10. Hệ thống sinh biểu diễn ngữ nghĩa cho từng đoạn và lập chỉ mục tìm kiếm.
11. Hệ thống chuyển tài liệu sang trạng thái sẵn sàng.
12. Quản trị tri thức gán quyền đọc cho tài liệu theo `UC-grant-document-access`.

## Extensions

**1a. Tệp vượt 50MB**
- 1a1. Hệ thống từ chối ngay trước khi tải lên, nêu giới hạn và kích thước tệp thật. Use case kết thúc.

**1b. Định dạng không hỗ trợ**
- 1b1. Hệ thống từ chối và nêu bốn định dạng được hỗ trợ. Use case kết thúc.

**1c. Tệp trùng với tài liệu đã có trong kho**
- 1c1. Hệ thống cảnh báo trùng và hỏi chọn: bỏ qua, hay coi đây là phiên bản thay thế.
- 1c2. Nếu chọn thay thế, chuyển sang luồng thay thế của US-015.

**2a. Chưa chọn nhãn mật**
- 2a1. Hệ thống không cho hoàn tất việc nạp. Không có nhãn mặc định ngầm.

**4a. Pdf không có lớp văn bản (bản scan)**
- 4a1. Trạng thái lỗi, lý do nêu rõ tệp có thể là bản scan và hệ thống không nhận dạng ký tự từ
  ảnh, kèm gợi ý dùng bản văn bản gốc. Use case kết thúc với tài liệu ở trạng thái lỗi.

**4b. Tệp đặt mật khẩu**
- 4b1. Trạng thái lỗi, lý do tệp bị khoá. Hệ thống không hỏi mật khẩu và không lưu mật khẩu.

**4c. Tệp hỏng cấu trúc**
- 4c1. Trạng thái lỗi kèm gợi ý mở và lưu lại bằng phần mềm soạn thảo rồi nạp lại.

**5a. Tài liệu rỗng hoặc quá ngắn**
- 5a1. Trạng thái lỗi kèm số ký tự bóc được.

**5b. Tài liệu không có cấu trúc rõ**
- 5b1. Hệ thống lùi về tách theo đoạn văn có chồng lấn; trích dẫn sẽ chỉ tới vị trí trong tài liệu
  thay vì tới điều khoản. Tiếp tục bước 6.

**5c. Bảng không bóc được cấu trúc**
- 5c1. Bảng rơi về văn bản phẳng, và hệ thống cảnh báo cho quản trị biết tài liệu này có nguy cơ
  trả lời sai — không âm thầm nạp.

**6a. Không tìm thấy ngày hiệu lực trong tài liệu**
- 6a1. Trường để trống kèm ghi chú không tìm thấy. Hệ thống **không** đoán một ngày nào đó.

**6b. Tài liệu khai thay thế một văn bản trong kho**
- 6b1. Hệ thống đề xuất đúng tài liệu cũ; sau khi xác nhận thì tài liệu cũ chuyển sang trạng thái
  đã bị thay thế.

**8a. Quản trị chưa xác nhận metadata**
- 8a1. Tài liệu dừng ở trạng thái chờ xác nhận và **không** vào phạm vi truy hồi. Không có đường
  nào đưa tài liệu vào kho mà bỏ qua bước này.

**8b. Ngày hết hiệu lực sớm hơn ngày hiệu lực**
- 8b1. Hệ thống từ chối lưu và nêu mâu thuẫn.

**8c. Quan hệ thay thế tạo thành vòng lặp**
- 8c1. Hệ thống từ chối lưu và nêu rõ vòng lặp.

**9a. Phát hiện chồng lấn chủ đề**
- 9a1. Tài liệu được gắn nhãn nghi ngờ chồng lấn kèm tên tài liệu bị nghi và mức độ giống nhau.
- 9a2. Cảnh báo **không** chặn luồng nạp; tài liệu vẫn vào kho và vẫn trả lời được câu hỏi.
- 9a3. Quản trị xử lý sau: xác nhận là văn bản thay thế, xác nhận cùng tồn tại hợp lệ, hoặc bỏ qua.

**10a. Lỗi trong lúc lập chỉ mục**
- 10a1. Hệ thống dừng và **không** để lại đoạn nào của tài liệu trong phạm vi truy hồi — kho không
  chứa mảnh vụn của tài liệu hỏng.

**12a. Chưa gán quyền cho ai**
- 12a1. Tài liệu ở trạng thái sẵn sàng nhưng **không** xuất hiện trong kết quả của bất kỳ ai. Mặc
  định là đóng, không phải mở.

## Related Requirements

- Story: US-009, US-010, US-011, US-012, US-013, US-014, US-015, US-007
- Năng lực: `CAP-doc-ingestion-01`, `CAP-doc-ingestion-02`, `CAP-doc-ingestion-03`,
  `CAP-doc-ingestion-04`, `CAP-doc-ingestion-05`
