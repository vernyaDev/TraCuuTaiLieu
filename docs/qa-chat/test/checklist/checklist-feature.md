#1. Vào màn và gửi câu hỏi

##1.1. Trạng thái rỗng lần đầu

[2] [Yes] CHK-qa-chat-001 → SCR-qa-chat-01 · Verify màn Hỏi đáp lần đầu hiển thị đủ lời chào, ba câu hỏi mẫu và dòng giải thích hệ thống chỉ trả lời từ tài liệu nội bộ
[3] [Yes] CHK-qa-chat-002 → SCR-qa-chat-01 · Verify bấm một câu hỏi mẫu thì nội dung câu đó được đưa vào ô nhập
[2] [Yes] CHK-qa-chat-003 → US-026/AC-008 · Verify đóng trình duyệt rồi mở lại trong thời hạn phiên thì màn nói rõ hội thoại trước đã kết thúc

##1.2. Ô nhập và nút gửi

[1] [Yes] CHK-qa-chat-004 → SCR-qa-chat-01 · Verify nút gửi ở trạng thái vô hiệu hoá khi ô nhập rỗng
[2] [Yes] CHK-qa-chat-005 → SCR-qa-chat-01 · Verify trỏ vào nút gửi đang vô hiệu hoá thì hiện lý do
[2] [Yes] CHK-qa-chat-006 → SCR-qa-chat-01 · Verify nhấn Enter trong ô nhập thì câu hỏi được gửi
[2] [Yes] CHK-qa-chat-007 → SCR-qa-chat-01 · Verify nhấn Shift kèm Enter thì con trỏ xuống dòng và câu hỏi không được gửi
[3] [Yes] CHK-qa-chat-008 → SCR-qa-chat-01 · Verify ô nhập tự giãn theo nội dung tới trần rồi chuyển sang cuộn trong ô
[3] [Yes] CHK-qa-chat-009 → SCR-qa-chat-01 · Verify ô nhập vẫn nhìn thấy được khi cuộn vùng hội thoại lên đầu
[1] [Yes] CHK-qa-chat-010 → US-017/AC-008 · Verify gửi câu hỏi chỉ một từ không đủ nghĩa thì hệ thống đề nghị diễn đạt rõ hơn
[1] [No] CHK-qa-chat-011 → US-017/AC-008 · Verify câu hỏi không đủ nghĩa KHÔNG tạo bản ghi khoảng trống tri thức
[1] [Yes] CHK-qa-chat-012 → US-018/AC-006 · Verify đang có lượt chạy dở thì nút gửi vô hiệu hoá kèm giải thích ngắn

#2. Xử lý câu hỏi và hiện câu trả lời

##2.1. Chỉ báo tiến trình

[1] [Yes] CHK-qa-chat-013 → US-018/AC-002 · Verify trước khi có chữ đầu tiên, màn hiện bước đang chạy bằng lời thay vì vòng xoay không chú thích
[2] [Yes] CHK-qa-chat-014 → US-018/AC-002 · Verify chỉ báo tiến trình biến mất ngay khi ký tự đầu tiên của câu trả lời xuất hiện

##2.2. Câu trả lời hiện dần

[1] [Yes] CHK-qa-chat-015 → US-018/AC-001 · Verify câu trả lời hiện dần theo tiến độ sinh chứ không chờ trọn câu
[1] [Yes] CHK-qa-chat-016 → US-018/AC-004 · Verify trong lúc câu trả lời đang hiện, nút gửi chuyển thành nút dừng
[1] [Yes] CHK-qa-chat-017 → US-018/AC-004 · Verify bấm dừng thì việc sinh chữ dừng lại và phần đã hiện được giữ nguyên
[2] [Yes] CHK-qa-chat-018 → US-018/AC-004 · Verify lượt bị dừng mang dấu hiệu chưa hoàn tất
[2] [Yes] CHK-qa-chat-019 → US-018/AC-004 · Verify sau khi bấm dừng thì gửi được câu hỏi mới ngay
[2] [Yes] CHK-qa-chat-020 → US-017/AC-009 · Verify câu trả lời bằng tiếng Việt và giữ nguyên thuật ngữ tiếng Anh có trong tài liệu nguồn

##2.3. Hỏi một lượt

[1] [Yes] CHK-qa-chat-021 → US-017/AC-001 · Verify hỏi câu có đáp án trong kho thì nhận câu trả lời nêu đúng số liệu quy định
[1] [Yes] CHK-qa-chat-022 → US-017/AC-002 · Verify hỏi bằng từ vựng khác với từ vựng trong văn bản thì hệ thống vẫn tìm đúng đoạn
[1] [Yes] CHK-qa-chat-023 → US-017/AC-004 · Verify câu trả lời ghép từ hai tài liệu thì cả hai nguồn đều được liệt kê
[1] [Yes] CHK-qa-chat-024 → US-024/AC-005 · Verify người đọc thấy được ý nào trong câu trả lời dựa trên nguồn nào
[1] [No] CHK-qa-chat-025 → US-017/AC-005 · Verify đáp án tốt nhất nằm ngoài phạm vi quyền thì không nội dung nào của tài liệu đó lọt vào câu trả lời

##2.4. Hỏi tiếp nối

[1] [Yes] CHK-qa-chat-026 → US-020/AC-001 · Verify câu hỏi tiếp nối rút gọn được truy hồi bằng câu đã bổ sung đủ chủ đề
[1] [Yes] CHK-qa-chat-027 → US-020/AC-002 · Verify khối thu gọn hiển thị câu đã được viết lại thành câu độc lập
[2] [Yes] CHK-qa-chat-028 → US-020/AC-003 · Verify đại từ trỏ về tài liệu nhắc ở lượt trước được hiểu đúng
[2] [Yes] CHK-qa-chat-029 → US-020/AC-004 · Verify câu hỏi tự nó đã đủ nghĩa thì được dùng nguyên vẹn, không bị nhét bối cảnh cũ
[2] [Yes] CHK-qa-chat-030 → US-020/AC-005 · Verify đổi chủ đề giữa phiên thì bối cảnh chủ đề cũ không bị kéo vào
[2] [Yes] CHK-qa-chat-031 → US-020/AC-006 · Verify sửa lại câu hỏi sau khi thấy bản viết lại sai thì hệ thống xử lý theo câu mới
[1] [Yes] CHK-qa-chat-032 → US-020/AC-007 · Verify lượt đầu tiên của phiên mà câu rút gọn thiếu chủ đề thì hệ thống hỏi lại làm rõ thay vì đoán
[2] [Yes] CHK-qa-chat-033 → SCR-qa-chat-01 · Verify việc viết lại câu hỏi thất bại thì hệ thống chạy trên câu gốc và nói rõ không chắc đã hiểu đúng ngữ cảnh

##2.5. Hội thoại dài

[2] [No] CHK-qa-chat-034 → US-021/AC-001 · Verify sau mười lăm lượt trong một phiên, câu rút gọn vẫn được hiểu đúng chủ đề xác lập ở lượt đầu
[2] [No] CHK-qa-chat-035 → US-021/AC-002 · Verify hội thoại vượt ngưỡng ngữ cảnh thì các lượt cũ được thay bằng tóm tắt giữ chủ đề, thực thể và ràng buộc
[3] [No] CHK-qa-chat-036 → US-021/AC-003 · Verify vượt ngưỡng lần nữa thì tóm tắt cũ được cập nhật chứ không sinh thêm đoạn tóm tắt mới
[1] [No] CHK-qa-chat-037 → US-021/AC-004 · Verify tóm tắt chỉ chứa nội dung đã thực sự xuất hiện trong hội thoại
[1] [No] CHK-qa-chat-038 → US-021/AC-005 · Verify câu trả lời cần dựa vào tài liệu đã bị đưa vào tóm tắt thì truy hồi lại tài liệu đó thay vì dùng nội dung nhớ từ tóm tắt
[2] [Yes] CHK-qa-chat-039 → US-021/AC-002 · Verify các lượt gần nhất vẫn giữ nguyên văn khi tóm tắt đã diễn ra
[2] [Yes] CHK-qa-chat-040 → SCR-qa-chat-01 · Verify việc tóm tắt thất bại thì hệ thống báo hội thoại quá dài và mời mở phiên mới

##2.6. Câu hỏi phức tạp nhiều bước

[1] [Yes] CHK-qa-chat-041 → US-022/AC-001 · Verify câu hỏi hai vế cần hai căn cứ khác nhau được đưa vào đường phân rã
[2] [Yes] CHK-qa-chat-042 → US-022/AC-002 · Verify câu hỏi một vế không bị phân rã thừa
[1] [Yes] CHK-qa-chat-043 → US-022/AC-003 · Verify câu hỏi hai vế thì cả hai vế đều được trả lời và mỗi vế có căn cứ riêng
[2] [No] CHK-qa-chat-044 → US-022/AC-004 · Verify vế phụ thuộc chạy sau và dùng kết quả vế trước làm đầu vào
[1] [Yes] CHK-qa-chat-045 → US-022/AC-005 · Verify một vế không có căn cứ thì câu trả lời nêu rõ vế nào thiếu mà không từ chối cả câu
[2] [Yes] CHK-qa-chat-046 → US-022/AC-006 · Verify khối thu gọn hiển thị danh sách truy vấn con đã chạy
[2] [Yes] CHK-qa-chat-047 → US-022/AC-007 · Verify vượt giới hạn số truy vấn con thì hệ thống trả lời phần đã có và đề nghị chia nhỏ câu hỏi
[2] [Yes] CHK-qa-chat-048 → SCR-qa-chat-01 · Verify việc phân rã thất bại thì hệ thống lùi về đường một lượt và nói rõ điều đó
[2] [Yes] CHK-qa-chat-049 → SCR-qa-chat-01 · Verify một truy vấn con quá thời gian chờ thì vế đó bị bỏ và được nêu rõ trong câu trả lời

#3. Nguồn và kiểm chứng

##3.1. Khối nguồn

[1] [Yes] CHK-qa-chat-050 → US-018/AC-003 · Verify khối nguồn chỉ hiện sau khi câu trả lời sinh xong
[1] [Yes] CHK-qa-chat-051 → US-017/AC-003 · Verify mỗi dòng nguồn nêu tên tài liệu kèm đường dẫn phân cấp tới đúng khoản
[2] [Yes] CHK-qa-chat-052 → US-017/AC-003 · Verify khối nguồn liệt kê đủ các nguồn thực sự được dùng, không cắt theo số cố định
[2] [Yes] CHK-qa-chat-053 → SCR-qa-chat-01 · Verify nguồn có trạng thái hiệu lực khác bình thường thì mang thẻ trạng thái riêng

##3.2. Mở tài liệu gốc

[1] [Yes] CHK-qa-chat-054 → US-019/AC-001 · Verify bấm một dòng nguồn thì ngăn kéo mở và tự cuộn tới đúng đoạn được trích
[1] [Yes] CHK-qa-chat-055 → US-019/AC-001 · Verify đoạn được trích được làm nổi bật rõ so với phần còn lại
[2] [Yes] CHK-qa-chat-056 → US-019/AC-002 · Verify cuộn lên xuống trong ngăn kéo thì đọc được phần trước và sau đoạn trích
[1] [Yes] CHK-qa-chat-057 → US-019/AC-007 · Verify trích dẫn trỏ tới mảnh của bảng dài thì bảng hiển thị kèm dòng tiêu đề
[2] [Yes] CHK-qa-chat-058 → US-019/AC-006 · Verify đóng ngăn kéo thì quay về đúng vị trí trong hội thoại với câu hỏi và câu trả lời còn nguyên
[2] [Yes] CHK-qa-chat-059 → US-026/AC-003 · Verify trích dẫn của lượt cũ trong cùng phiên vẫn mở được
[1] [No] CHK-qa-chat-060 → US-019/AC-005 · Verify bấm tải tệp gốc thì hệ thống kiểm lại quyền trước khi trả tệp

#4. Từ chối và căn cứ mỏng

##4.1. Từ chối tầng một

[1] [Yes] CHK-qa-chat-061 → US-023/AC-001 · Verify không đoạn nào vượt ngưỡng liên quan thì hệ thống trả lời không tìm thấy căn cứ trong kho
[1] [No] CHK-qa-chat-062 → US-023/AC-001 · Verify từ chối tầng một KHÔNG gọi mô hình sinh câu trả lời
[1] [Yes] CHK-qa-chat-063 → US-023/AC-002 · Verify câu hỏi là kiến thức phổ thông mà kho không có thì hệ thống vẫn từ chối thay vì trả lời bằng kiến thức của mô hình

##4.2. Từ chối tầng hai

[1] [Yes] CHK-qa-chat-064 → US-024/AC-002 · Verify không ý nào gắn được về nguồn thì lượt chuyển thành từ chối chứ không trả về câu trả lời cụt
[1] [Yes] CHK-qa-chat-065 → US-024/AC-001 · Verify ý không có trong đoạn nào được truy hồi thì bị loại và câu trả lời còn lại vẫn mạch lạc
[1] [No] CHK-qa-chat-066 → US-024/AC-003 · Verify con số hoặc mốc thời hạn không xuất hiện nguyên văn trong đoạn nguồn thì bị loại
[1] [No] CHK-qa-chat-067 → US-024/AC-004 · Verify hệ thống không suy chế độ của nhóm không được nguồn đề cập từ quy định của nhóm khác
[2] [Yes] CHK-qa-chat-068 → US-024/AC-006 · Verify câu nối không mang thông tin nghiệp vụ không bị loại vì thiếu nguồn

##4.3. Lời lẽ khi từ chối

[1] [Yes] CHK-qa-chat-069 → US-023/AC-003 · Verify khối từ chối nêu cùng một bộ khả năng ở mọi lượt từ chối vì thiếu căn cứ
[1] [No] CHK-qa-chat-070 → US-023/AC-003 · Verify khối từ chối KHÔNG cho người hỏi biết khả năng nào là lý do thật
[1] [No] CHK-qa-chat-071 → CAP-access-control-04 · Verify lượt bị chặn vì thiếu quyền hiển thị y hệt lượt bị chặn vì kho không có thông tin
[1] [Yes] CHK-qa-chat-072 → US-023/AC-003b, US-017/AC-007 · Verify câu hỏi ngoài phạm vi kho tri thức nhận khối thông báo riêng được nói thẳng
[1] [No] CHK-qa-chat-073 → US-023/AC-003b · Verify câu hỏi ngoài phạm vi kho KHÔNG tạo bản ghi khoảng trống tri thức
[2] [Yes] CHK-qa-chat-074 → US-023/AC-004 · Verify thông báo từ chối vì kho không có thông tin kèm gợi ý việc nên làm tiếp
[2] [Yes] CHK-qa-chat-075 → SCR-qa-chat-01 · Verify khối từ chối không dùng màu lỗi
[1] [No] CHK-qa-chat-076 → US-023/AC-005 · Verify mỗi lượt từ chối vì thiếu căn cứ sinh một bản ghi khoảng trống kèm đúng lý do

##4.4. Căn cứ mỏng

[1] [Yes] CHK-qa-chat-077 → US-023/AC-008 · Verify căn cứ mỏng thì hệ thống vẫn trả lời và nêu rõ căn cứ còn mỏng thay vì từ chối
[2] [No] CHK-qa-chat-078 → CAP-knowledge-gap-01 · Verify lượt trả lời với căn cứ mỏng vẫn sinh bản ghi khoảng trống mang dấu hiệu căn cứ mỏng

#5. Cảnh báo nguồn

[1] [Yes] CHK-qa-chat-079 → US-025/AC-001 · Verify căn cứ nằm trong tài liệu đã qua ngày hết hiệu lực thì câu trả lời kèm cảnh báo nêu rõ ngày hết hiệu lực
[1] [Yes] CHK-qa-chat-080 → US-025/AC-002 · Verify căn cứ nằm trong tài liệu đã bị thay thế thì hệ thống ưu tiên trả lời theo bản mới
[2] [Yes] CHK-qa-chat-081 → US-025/AC-002 · Verify khi vẫn phải nhắc bản cũ thì cảnh báo nêu rõ nó bị thay bởi văn bản nào
[1] [Yes] CHK-qa-chat-082 → US-025/AC-003 · Verify hai văn bản đã xác nhận cùng tồn tại hợp lệ thì câu trả lời nêu cả hai và chỉ ra chỗ khác nhau
[2] [Yes] CHK-qa-chat-083 → US-025/AC-004 · Verify khối cảnh báo tách bạch khỏi nội dung câu trả lời và đặt sau câu trả lời
[2] [Yes] CHK-qa-chat-084 → US-025/AC-005 · Verify mọi nguồn còn hiệu lực và không chồng lấn thì không có cảnh báo nào hiện ra
[2] [Yes] CHK-qa-chat-085 → US-025/AC-006 · Verify nhiều nguồn mà chỉ một nguồn có vấn đề thì cảnh báo chỉ đúng nguồn đó
[3] [Yes] CHK-qa-chat-086 → SCR-qa-chat-01 · Verify khối cảnh báo dùng màu cảnh báo kèm nhãn chữ chứ không chỉ truyền nghĩa bằng màu

#6. Phiên và lịch sử

[1] [Yes] CHK-qa-chat-087 → US-026/AC-001 · Verify cuộn lên trong phiên thì thấy đủ mọi cặp câu hỏi và câu trả lời theo thứ tự thời gian
[2] [Yes] CHK-qa-chat-088 → US-026/AC-002 · Verify lượt cũ vẫn hiển thị nguyên văn kể cả khi chúng đã bị nén lại làm bối cảnh bên trong
[2] [Yes] CHK-qa-chat-089 → US-026/AC-004 · Verify lượt bị từ chối trước đó vẫn hiển thị trong lịch sử kèm lý do
[2] [Yes] CHK-qa-chat-090 → US-026/AC-005 · Verify lượt bị dừng giữa chừng hiển thị trong lịch sử kèm dấu hiệu chưa hoàn tất
[1] [Yes] CHK-qa-chat-091 → US-026/AC-006, US-021/AC-006 · Verify bắt đầu phiên mới thì vùng hội thoại về trạng thái rỗng lần đầu
[1] [No] CHK-qa-chat-092 → US-026/AC-006, US-021/AC-007 · Verify bối cảnh phiên cũ không ảnh hưởng kết quả truy hồi của phiên mới
[2] [Yes] CHK-qa-chat-093 → US-026/AC-007 · Verify sao chép câu trả lời thì phần sao chép kèm theo thông tin nguồn

#7. Bảo mật cơ bản

[1] [Yes] CHK-qa-chat-094 → CAP-access-control-04 · Verify mở thẳng đường dẫn màn Hỏi đáp khi chưa đăng nhập thì bị đưa về màn Đăng nhập
[1] [Yes] CHK-qa-chat-095 → NFR-system-027 · Verify phiên hết hạn giữa lúc đang dùng thì hệ thống đưa về màn Đăng nhập
[1] [Yes] CHK-qa-chat-096 → NFR-system-027 · Verify đăng nhập lại sau khi phiên hết hạn thì quay về đúng trang đang mở lúc hết hạn
[1] [No] CHK-qa-chat-097 → US-019/AC-003 · Verify quyền bị thu hồi sau khi trích dẫn đã hiện thì mở trích dẫn nhận thông báo không còn quyền và không hiện nội dung nào
[1] [Yes] CHK-qa-chat-098 → US-019/AC-004 · Verify tài liệu đã gỡ thì mở trích dẫn báo tài liệu không còn dùng làm căn cứ
[1] [Yes] CHK-qa-chat-099 → US-019/AC-004 · Verify tài liệu đã xoá vĩnh viễn thì mở trích dẫn báo tài liệu không còn trong hệ thống
[2] [Yes] CHK-qa-chat-100 → SCR-qa-chat-01 · Verify nút Khu quản trị không hiển thị với tài khoản nhân viên thường
[1] [No] CHK-qa-chat-101 → SCR-qa-chat-01 · Verify nhân viên thường gõ thẳng đường dẫn khu quản trị thì máy chủ chặn dù nút đã bị ẩn

#8. Trạng thái tải và lỗi

[1] [Yes] CHK-qa-chat-102 → US-018/AC-005 · Verify mất kết nối giữa lúc câu trả lời đang hiện thì phần đã nhận được giữ lại
[1] [Yes] CHK-qa-chat-103 → US-018/AC-005 · Verify mất kết nối giữa chừng thì hệ thống báo mất kết nối thay vì treo vô hạn
[1] [Yes] CHK-qa-chat-104 → US-018/AC-007 · Verify vượt ngưỡng chờ mà chưa sinh được chữ nào thì hệ thống dừng lượt và báo đang bận
[2] [Yes] CHK-qa-chat-105 → NFR-system-008 · Verify câu hỏi thứ sáu khi đã có năm câu đang xử lý thì vào hàng đợi kèm thông báo vị trí
[2] [Yes] CHK-qa-chat-106 → SCR-qa-chat-02 · Verify ngăn kéo tài liệu gốc khi đang tải hiện khung xương giữ hình dạng trang tài liệu
[2] [Yes] CHK-qa-chat-107 → SCR-qa-chat-02 · Verify tệp tài liệu không đọc được thì ngăn kéo báo lỗi và cho thử lại tại chỗ

#9. Tiếp cận cơ bản

[3] [No] CHK-qa-chat-108 → NFR-system-037 · Verify thao tác gửi câu hỏi làm được hoàn toàn bằng bàn phím
[3] [No] CHK-qa-chat-109 → NFR-system-037 · Verify thứ tự chuyển tiêu điểm trên màn Hỏi đáp đi theo thứ tự đọc
[3] [No] CHK-qa-chat-110 → NFR-system-037 · Verify phím Esc đóng ngăn kéo Xem tài liệu gốc
[3] [No] CHK-qa-chat-111 → NFR-system-037 · Verify nhãn của ô nhập câu hỏi đọc được bằng trình đọc màn hình

#10. Ca biên

[1] [Yes] CHK-qa-chat-112 → US-018/AC-006 · Verify bấm gửi hai lần liên tiếp thật nhanh thì chỉ một lượt được tạo
[2] [Yes] CHK-qa-chat-113 → SCR-qa-chat-01 · Verify bấm nút quay lại của trình duyệt giữa lúc đang có lượt chạy thì hệ thống không sinh lượt trùng
[2] [Yes] CHK-qa-chat-114 → US-026/AC-008 · Verify tải lại trang giữa phiên thì hệ thống nói rõ hội thoại trước đã kết thúc
[2] [Yes] CHK-qa-chat-115 → SCR-qa-chat-01 · Verify dán một đoạn văn dài vào ô nhập thì ô chuyển sang cuộn thay vì giãn vô hạn
[3] [No] CHK-qa-chat-116 → SCR-qa-chat-01 · Verify mở cùng một phiên trên hai tab thì lượt hỏi ở tab này không làm hỏng lịch sử ở tab kia
[2] [Yes] CHK-qa-chat-117 → SCR-qa-chat-01 · Verify bảng biểu trong ngăn kéo tài liệu gốc cuộn ngang trong khung của nó mà không làm cả trang cuộn ngang

#11. Cấu hình phục vụ thí nghiệm bóc tách

[1] [No] CHK-qa-chat-118 → US-022/AC-008 · Verify tắt bước phân rã trong cấu hình rồi khởi động lại thì câu hỏi nhiều vế chạy theo đường một lượt mà không phải sửa mã nguồn
[1] [No] CHK-qa-chat-119 → US-024/AC-007 · Verify tắt tầng kiểm gắn nguồn trong cấu hình rồi khởi động lại thì hệ thống vẫn chạy được trọn một lượt hỏi đáp
[2] [No] CHK-qa-chat-120 → US-023/AC-006 · Verify đổi ngưỡng từ chối trong cấu hình rồi khởi động lại thì hệ thống áp ngưỡng mới mà không phải sửa mã nguồn

#12. Bổ sung theo tiêu chí còn trống

[1] [Yes] CHK-qa-chat-121 → US-017/AC-006 · Verify câu hỏi hiểu được theo hai cách dẫn tới hai đáp án khác nhau thì hệ thống hỏi lại làm rõ thay vì tự chọn một cách hiểu
[2] [Yes] CHK-qa-chat-122 → US-025/AC-008 · Verify nguồn có trạng thái hiệu lực chưa xác định thì câu trả lời nêu rõ chưa xác định được hiệu lực
[2] [Yes] CHK-qa-chat-123 → US-025/AC-008 · Verify cảnh báo chưa xác định hiệu lực phân biệt được với trường hợp đã xác định là còn hiệu lực
[3] [No] CHK-qa-chat-124 → US-025/AC-007 · Verify bước kiểm hiệu lực và chồng lấn không gọi mô hình ngôn ngữ
