#1. Nạp tài liệu

##1.1. Luồng nạp thành công

[1] [Yes] CHK-doc-ingestion-001 → US-009/AC-001 · Verify nạp một tệp hợp lệ thì tài liệu chuyển sang trạng thái đang xử lý ngay sau khi xác nhận
[1] [Yes] CHK-doc-ingestion-002 → US-009/AC-002 · Verify cả bốn định dạng docx, pdf, txt và Markdown đều nạp được
[1] [Yes] CHK-doc-ingestion-003 → US-009/AC-003 · Verify chọn nhiều tệp trong một đợt thì mọi tệp hợp lệ đều được đưa vào xử lý
[1] [No] CHK-doc-ingestion-004 → US-009/AC-004 · Verify tài liệu chỉ vào phạm vi truy hồi sau khi xử lý xong hoàn toàn
[1] [Yes] CHK-doc-ingestion-005 → US-009/AC-005 · Verify tệp vượt năm mươi megabyte bị từ chối trước khi tải lên
[2] [Yes] CHK-doc-ingestion-006 → US-009/AC-005 · Verify thông báo từ chối vì dung lượng nêu cả giới hạn lẫn kích thước thật của tệp
[1] [Yes] CHK-doc-ingestion-007 → US-009/AC-006 · Verify nạp lại đúng tệp đã có trong kho thì hệ thống hỏi bỏ qua hay coi là bản thay thế
[1] [Yes] CHK-doc-ingestion-008 → US-009/AC-006 · Verify hệ thống không tự tạo bản sao thứ hai khi phát hiện tệp trùng
[1] [No] CHK-doc-ingestion-009 → US-009/AC-007 · Verify đóng trình duyệt giữa lúc đang xử lý thì việc xử lý vẫn chạy tiếp
[2] [Yes] CHK-doc-ingestion-010 → US-009/AC-008 · Verify dòng đang xử lý hiển thị bước hiện tại kèm số đoạn đã xử lý trên tổng

##1.2. Cổng chặn nhãn mật

[1] [Yes] CHK-doc-ingestion-011 → CAP-access-control-05 · Verify nút xác nhận nạp vô hiệu hoá khi chưa chọn nhãn mật
[1] [Yes] CHK-doc-ingestion-012 → CAP-access-control-05 · Verify ô nhãn mật không có giá trị chọn sẵn
[2] [Yes] CHK-doc-ingestion-013 → SCR-doc-ingestion-02 · Verify trỏ vào nút xác nhận đang vô hiệu hoá thì hiện lý do còn thiếu gì
[2] [Yes] CHK-doc-ingestion-014 → SCR-doc-ingestion-02 · Verify nhãn nút xác nhận nêu số tệp sẽ nạp thật, đã trừ tệp lỗi

#2. Lỗi nạp

[1] [Yes] CHK-doc-ingestion-015 → US-010/AC-001 · Verify pdf không có lớp văn bản chuyển sang trạng thái lỗi kèm lý do có thể là bản scan
[1] [Yes] CHK-doc-ingestion-016 → US-010/AC-002 · Verify tệp đặt mật khẩu chuyển sang trạng thái lỗi kèm lý do tệp bị khoá
[1] [No] CHK-doc-ingestion-017 → US-010/AC-002 · Verify hệ thống không hỏi mật khẩu của tệp và không lưu mật khẩu nào
[1] [Yes] CHK-doc-ingestion-018 → US-010/AC-003 · Verify tệp hỏng cấu trúc chuyển sang trạng thái lỗi kèm gợi ý mở và lưu lại bằng phần mềm soạn thảo
[1] [Yes] CHK-doc-ingestion-019 → US-010/AC-004 · Verify tài liệu rỗng hoặc quá ngắn chuyển sang trạng thái lỗi kèm số ký tự bóc được
[1] [Yes] CHK-doc-ingestion-020 → US-010/AC-005 · Verify sau đợt nạp nhiều tệp, dải tổng kết nêu số thành công và số lỗi
[2] [Yes] CHK-doc-ingestion-021 → US-010/AC-005 · Verify danh sách tệp lỗi của một đợt nạp xem lại được sau khi đã rời màn
[2] [Yes] CHK-doc-ingestion-022 → US-010/AC-006 · Verify nạp lại tệp sau khi sửa thì tài liệu chuyển sang xử lý bình thường
[1] [No] CHK-doc-ingestion-023 → US-010/AC-007 · Verify tài liệu nạp lỗi giữa chừng không để lại đoạn nào trong phạm vi truy hồi
[2] [Yes] CHK-doc-ingestion-024 → US-010/AC-008 · Verify thông báo lỗi nạp viết bằng lời người vận hành hiểu được, không có vết gọi hàm

#3. Tách đoạn

[1] [No] CHK-doc-ingestion-025 → US-011/AC-001 · Verify tài liệu có cấu trúc rõ được tách theo đúng ranh giới chương, điều và khoản
[1] [Yes] CHK-doc-ingestion-026 → US-011/AC-002 · Verify mỗi đoạn mang đường dẫn phân cấp tới vị trí của nó trong tài liệu
[2] [No] CHK-doc-ingestion-027 → US-011/AC-003 · Verify đơn vị nội dung dài hơn kích thước đoạn được chia kèm dấu hiệu phần thứ mấy
[1] [Yes] CHK-doc-ingestion-028 → US-011/AC-004 · Verify tài liệu không có cấu trúc rõ lùi về cách tách dự phòng kèm cảnh báo
[2] [No] CHK-doc-ingestion-029 → US-011/AC-005 · Verify cùng một nội dung ở bốn định dạng cho ra cấu trúc đoạn tương đương
[1] [No] CHK-doc-ingestion-030 → US-011/AC-006 · Verify đổi chiến lược tách trong cấu hình rồi khởi động lại thì tài liệu mới được tách theo chiến lược mới
[1] [No] CHK-doc-ingestion-031 → US-011/AC-007 · Verify tổng nội dung của mọi đoạn phủ hết nội dung tài liệu gốc, không mất đoạn nào
[2] [Yes] CHK-doc-ingestion-032 → SCR-doc-ingestion-06 · Verify màn xem trước đoạn hiển thị chiến lược tách đang dùng và tổng số đoạn
[2] [Yes] CHK-doc-ingestion-033 → SCR-doc-ingestion-06 · Verify chọn một đoạn ở cột phải thì cột trái làm nổi bật đúng phần tương ứng

#4. Bảng biểu

[1] [No] CHK-doc-ingestion-034 → US-012/AC-001 · Verify bảng nhỏ nằm trọn trong một đoạn, không bị chia
[1] [No] CHK-doc-ingestion-035 → US-012/AC-002 · Verify bảng lớn được chia tại ranh giới giữa hai hàng chứ không cắt ngang một hàng
[1] [Yes] CHK-doc-ingestion-036 → US-012/AC-002 · Verify mỗi mảnh của bảng lớn mang lại dòng tiêu đề
[2] [No] CHK-doc-ingestion-037 → US-012/AC-003 · Verify đoạn chứa bảng giữ được câu dẫn trước bảng làm ngữ cảnh
[1] [No] CHK-doc-ingestion-038 → US-012/AC-004 · Verify quan hệ giữa hàng và cột được giữ nguyên sau khi tách
[1] [Yes] CHK-doc-ingestion-039 → US-012/AC-005 · Verify hỏi một con số nằm trong ô của bảng thì hệ thống trả lời đúng con số đó
[2] [No] CHK-doc-ingestion-040 → US-012/AC-006 · Verify bảng trải qua nhiều trang trong pdf được ghép lại thay vì cắt rời theo trang
[2] [No] CHK-doc-ingestion-041 → US-012/AC-007 · Verify ô gộp không làm lệch quan hệ hàng và cột của các ô còn lại
[2] [No] CHK-doc-ingestion-042 → US-012/AC-008 · Verify bộ câu hỏi kiểm thử có ít nhất một câu mà đáp án nằm trong ô của bảng
[2] [Yes] CHK-doc-ingestion-043 → SCR-doc-ingestion-06 · Verify mảnh của bảng dài hiển thị dấu hiệu mảnh thứ mấy trên tổng
[2] [Yes] CHK-doc-ingestion-044 → SCR-doc-ingestion-06 · Verify bảng không bóc được cấu trúc hiện dải cảnh báo chứ không phải trạng thái lỗi

#5. Metadata vòng đời

[1] [Yes] CHK-doc-ingestion-045 → US-013/AC-001 · Verify hệ thống điền sẵn metadata vòng đời từ nội dung tài liệu
[1] [Yes] CHK-doc-ingestion-046 → US-013/AC-001 · Verify mỗi trường máy điền kèm đoạn nguyên văn làm căn cứ
[1] [No] CHK-doc-ingestion-047 → US-013/AC-002 · Verify tài liệu chưa xác nhận metadata thì không vào phạm vi truy hồi
[1] [Yes] CHK-doc-ingestion-048 → US-013/AC-002 · Verify không có đường nào đưa tài liệu vào kho mà bỏ qua bước xác nhận metadata
[1] [Yes] CHK-doc-ingestion-049 → US-013/AC-003 · Verify sửa được mọi giá trị hệ thống đề xuất
[2] [Yes] CHK-doc-ingestion-050 → US-013/AC-003 · Verify trường người sửa được đánh dấu phân biệt với trường máy điền
[1] [Yes] CHK-doc-ingestion-051 → US-013/AC-004 · Verify trường không tìm thấy căn cứ để trống kèm ghi chú không tìm thấy trong tài liệu
[1] [No] CHK-doc-ingestion-052 → US-013/AC-004 · Verify hệ thống không tự đoán một giá trị cho trường không có căn cứ
[1] [Yes] CHK-doc-ingestion-053 → US-013/AC-005 · Verify lựa chọn không có thời hạn phân biệt được với trạng thái chưa xác định
[1] [Yes] CHK-doc-ingestion-054 → US-013/AC-006 · Verify khai báo văn bản bị thay thế thì tài liệu cũ chuyển sang trạng thái đã bị thay thế
[2] [Yes] CHK-doc-ingestion-055 → US-013/AC-007 · Verify phân loại tài liệu là trường bắt buộc khi xác nhận metadata
[1] [No] CHK-doc-ingestion-056 → US-013/AC-008 · Verify tài liệu hết hiệu lực vẫn giữ trong kho chứ không bị xoá
[1] [No] CHK-doc-ingestion-057 → US-013/AC-009 · Verify sửa metadata sau khi đã xác nhận thì thay đổi có hiệu lực ngay ở câu hỏi kế tiếp
[1] [Yes] CHK-doc-ingestion-058 → US-013/AC-009 · Verify nút sửa metadata mở màn xác nhận ở chế độ chỉnh sửa được với người có quyền sửa
[2] [Yes] CHK-doc-ingestion-059 → SCR-doc-ingestion-03 · Verify ngày hết hiệu lực sớm hơn ngày hiệu lực thì bị từ chối lưu kèm nêu rõ mâu thuẫn
[2] [Yes] CHK-doc-ingestion-060 → SCR-doc-ingestion-03 · Verify bấm vào đoạn trích căn cứ thì cột trái cuộn tới và làm nổi bật đúng chỗ

#6. Chồng lấn chủ đề

[1] [No] CHK-doc-ingestion-061 → US-014/AC-001 · Verify tài liệu chồng lấn chủ đề với tài liệu đã có được gắn nhãn nghi ngờ lúc nạp
[2] [No] CHK-doc-ingestion-062 → US-014/AC-002 · Verify việc so chồng lấn chỉ diễn ra trong phạm vi tài liệu cùng phân loại và còn hiệu lực
[1] [Yes] CHK-doc-ingestion-063 → US-014/AC-003 · Verify màn xử lý chồng lấn không có nút nào được chọn sẵn
[1] [Yes] CHK-doc-ingestion-064 → US-014/AC-004 · Verify chọn cùng tồn tại hợp lệ thì cặp tài liệu chuyển sang trạng thái đã xử lý
[1] [Yes] CHK-doc-ingestion-065 → US-014/AC-005 · Verify chọn bỏ qua cảnh báo thì nhãn biến mất và không hiện lại cho đúng cặp đó
[2] [Yes] CHK-doc-ingestion-066 → US-014/AC-006 · Verify một tài liệu nghi chồng lấn với nhiều tài liệu thì hiện nhiều thẻ xếp theo mức giống nhau giảm dần
[1] [No] CHK-doc-ingestion-067 → US-014/AC-007 · Verify cảnh báo chồng lấn không chặn tài liệu vào kho và không chặn việc trả lời câu hỏi
[2] [No] CHK-doc-ingestion-068 → US-014/AC-008 · Verify bước tra chồng lấn lúc trả lời không gọi mô hình ngôn ngữ
[2] [Yes] CHK-doc-ingestion-069 → SCR-doc-ingestion-05 · Verify mỗi thẻ cảnh báo hiển thị hai tài liệu cạnh nhau kèm đoạn nội dung đại diện
[2] [Yes] CHK-doc-ingestion-070 → SCR-doc-ingestion-05 · Verify không còn cảnh báo nào thì màn nói rõ đây là trạng thái tốt chứ không phải lỗi

#7. Thay thế, gỡ và xoá

[1] [Yes] CHK-doc-ingestion-071 → US-015/AC-001 · Verify nạp bản thay thế thì tài liệu cũ chuyển sang trạng thái đã bị thay thế
[1] [No] CHK-doc-ingestion-072 → US-015/AC-002 · Verify câu trả lời ưu tiên dùng bản mới khi cả hai bản cùng liên quan
[1] [Yes] CHK-doc-ingestion-073 → US-015/AC-003 · Verify xem một bản bất kỳ trong chuỗi thay thế thì thấy được cả chuỗi nhiều đời
[2] [Yes] CHK-doc-ingestion-074 → US-015/AC-003 · Verify chuỗi thay thế đánh dấu rõ bản nào đang hiệu lực
[1] [Yes] CHK-doc-ingestion-075 → US-015/AC-004 · Verify khai báo quan hệ thay thế tạo thành vòng lặp thì bị từ chối lưu kèm nêu rõ vòng lặp
[1] [No] CHK-doc-ingestion-076 → US-015/AC-005 · Verify bản thay thế kế thừa quyền truy cập của bản bị thay thế
[1] [No] CHK-doc-ingestion-077 → US-015/AC-006 · Verify tài liệu đã gỡ biến khỏi mọi kết quả truy hồi
[2] [Yes] CHK-doc-ingestion-078 → US-015/AC-006 · Verify tài liệu đã gỡ hiển thị ở dạng chỉ đọc kèm nút khôi phục
[1] [Yes] CHK-doc-ingestion-079 → US-015/AC-007 · Verify hộp thoại xoá vĩnh viễn nêu số người sẽ mất quyền đọc và số trích dẫn đang trỏ tới
[1] [Yes] CHK-doc-ingestion-080 → US-015/AC-007 · Verify nút xác nhận xoá vĩnh viễn chỉ bật sau khi số liệu hậu quả đã hiện ra
[1] [Yes] CHK-doc-ingestion-081 → US-015/AC-008 · Verify trích dẫn cũ trỏ tới tài liệu đã xoá thì báo tài liệu không còn trong hệ thống
[1] [Yes] CHK-doc-ingestion-082 → SCR-doc-ingestion-04 · Verify nút đổi nhãn mật có mặt trong cụm nút của màn chi tiết tài liệu

#8. Danh sách và tìm kiếm

[1] [Yes] CHK-doc-ingestion-083 → US-016/AC-001 · Verify danh sách tài liệu phân trang có tổng số và nhảy trang được
[1] [Yes] CHK-doc-ingestion-084 → US-016/AC-002 · Verify lọc theo trạng thái xử lý cho đúng nhóm tài liệu tương ứng
[1] [Yes] CHK-doc-ingestion-085 → US-016/AC-003 · Verify lọc theo trạng thái hiệu lực phân biệt được còn hiệu lực với chưa xác định
[1] [Yes] CHK-doc-ingestion-086 → US-016/AC-004 · Verify dải việc đang chờ hiển thị đủ bốn thẻ kèm số đếm
[2] [Yes] CHK-doc-ingestion-087 → US-016/AC-004 · Verify bấm một thẻ việc đang chờ thì bảng lọc theo đúng nhóm của thẻ đó
[1] [Yes] CHK-doc-ingestion-088 → US-016/AC-005 · Verify thẻ sắp hết hiệu lực gom đúng tài liệu hết hiệu lực trong ba mươi ngày tới
[2] [Yes] CHK-doc-ingestion-089 → US-016/AC-005 · Verify nhóm sắp hết hiệu lực sắp theo ngày gần nhất trước và hiện số ngày còn lại
[2] [Yes] CHK-doc-ingestion-090 → US-016/AC-006 · Verify tìm theo tên tài liệu lọc đúng kết quả ngay khi gõ
[2] [Yes] CHK-doc-ingestion-091 → US-016/AC-007 · Verify danh sách rỗng lần đầu hiển thị khác danh sách rỗng do bộ lọc
[2] [Yes] CHK-doc-ingestion-092 → SCR-doc-ingestion-01 · Verify thẻ việc đang chờ có số đếm bằng không thì hiện nhạt chứ không biến mất
[2] [Yes] CHK-doc-ingestion-093 → SCR-doc-ingestion-01 · Verify dấu suy giảm chất lượng hiện trên dòng tài liệu tách bằng cách dự phòng

#9. Bảo mật cơ bản

[1] [Yes] CHK-doc-ingestion-094 → CAP-access-control-04 · Verify nhân viên thường mở thẳng đường dẫn danh sách tài liệu thì nhận thông báo không đủ quyền
[1] [No] CHK-doc-ingestion-095 → CAP-access-control-03 · Verify tài liệu vừa nạp chưa gán quyền thì không ai đọc được
[1] [Yes] CHK-doc-ingestion-096 → CAP-access-control-03 · Verify chọn nhiều tài liệu rồi gán quyền cho một phòng ban trong một thao tác thì mọi tài liệu đã chọn đều được cấp
[2] [Yes] CHK-doc-ingestion-097 → SCR-doc-ingestion-01 · Verify chọn cả trang hiển thị rõ đang chọn bao nhiêu trên tổng bao nhiêu

#10. Trạng thái tải và lỗi

[2] [Yes] CHK-doc-ingestion-098 → SCR-doc-ingestion-01 · Verify danh sách tài liệu khi đang tải hiện khung xương giữ hình dạng bảng
[2] [Yes] CHK-doc-ingestion-099 → SCR-doc-ingestion-01 · Verify lỗi tải danh sách cho thử lại tại chỗ mà không mất bộ lọc đang đặt
[2] [Yes] CHK-doc-ingestion-100 → SCR-doc-ingestion-04 · Verify tài liệu đang xử lý thì khối quyền truy cập vô hiệu hoá kèm lý do
[2] [Yes] CHK-doc-ingestion-101 → SCR-doc-ingestion-02 · Verify nút xác nhận nạp vào trạng thái chờ sau khi bấm, chặn bấm lần hai

#11. Tiếp cận cơ bản

[3] [No] CHK-doc-ingestion-102 → NFR-system-037 · Verify luồng nạp tài liệu làm được hoàn toàn bằng bàn phím
[3] [No] CHK-doc-ingestion-103 → NFR-system-037 · Verify phím Esc đóng hộp thoại tải tài liệu lên
[3] [No] CHK-doc-ingestion-104 → NFR-system-036 · Verify cột hiệu lực phân biệt bốn trạng thái bằng chữ chứ không chỉ bằng màu

#12. Ca biên

[2] [Yes] CHK-doc-ingestion-105 → SCR-doc-ingestion-02 · Verify kéo thả một thư mục thay vì tệp thì hệ thống báo lỗi thay vì im lặng bỏ qua
[2] [Yes] CHK-doc-ingestion-106 → SCR-doc-ingestion-02 · Verify gỡ hết tệp khỏi danh sách đã chọn thì nút xác nhận trở lại trạng thái vô hiệu hoá
[2] [No] CHK-doc-ingestion-107 → SCR-doc-ingestion-01 · Verify tiến trình dừng giữa lúc xử lý thì tài liệu không kẹt vĩnh viễn ở trạng thái đang xử lý
[2] [Yes] CHK-doc-ingestion-108 → SCR-doc-ingestion-04 · Verify tải lại trang giữa lúc đang sửa metadata thì thay đổi chưa lưu không bị ghi
[3] [Yes] CHK-doc-ingestion-109 → SCR-doc-ingestion-01 · Verify bảng tài liệu cuộn ngang trong khung của nó mà không làm cả trang cuộn ngang
