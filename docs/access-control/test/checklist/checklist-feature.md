#1. Đăng nhập và phiên làm việc

##1.1. Đăng nhập

[1] [Yes] CHK-access-control-001 → US-001/AC-001 · Verify đăng nhập đúng tên và mật khẩu thì vào được hệ thống
[1] [Yes] CHK-access-control-002 → US-001/AC-002 · Verify sai tên đăng nhập hiển thị đúng thông báo "Tên đăng nhập hoặc mật khẩu không đúng"
[1] [Yes] CHK-access-control-003 → US-001/AC-002 · Verify sai mật khẩu hiển thị thông báo giống hệt trường hợp sai tên đăng nhập
[1] [Yes] CHK-access-control-004 → US-001/AC-002 · Verify sau khi sai thông tin thì giá trị ô tên đăng nhập được giữ nguyên
[1] [Yes] CHK-access-control-005 → US-001/AC-002 · Verify sau khi sai thông tin thì ô mật khẩu được xoá trắng
[1] [Yes] CHK-access-control-006 → US-001/AC-003 · Verify tài khoản bị vô hiệu hoá nhận thông báo riêng nói rõ tài khoản đã ngừng hoạt động
[2] [Yes] CHK-access-control-007 → SCR-access-control-01 · Verify nút hiện mật khẩu chuyển ô mật khẩu sang dạng đọc được
[2] [Yes] CHK-access-control-008 → SCR-access-control-01 · Verify nút đăng nhập vô hiệu hoá khi một trong hai ô còn trống
[2] [Yes] CHK-access-control-009 → SCR-access-control-01 · Verify nút đăng nhập vào trạng thái chờ sau khi bấm, chặn bấm lần hai
[2] [Yes] CHK-access-control-010 → SCR-access-control-01 · Verify không gọi được máy chủ thì hiện thông báo lỗi kết nối kèm cách thử lại
[2] [Yes] CHK-access-control-011 → SCR-access-control-01 · Verify màn đăng nhập hiển thị dòng chỉ đường liên hệ quản trị khi quên mật khẩu

##1.2. Khoá tạm sau nhiều lần sai

[1] [No] CHK-access-control-012 → NFR-system-028 · Verify sau mười lần sai liên tiếp thì lần thử thứ mười một bị chặn
[1] [No] CHK-access-control-013 → NFR-system-028 · Verify bộ đếm lần sai tính theo nguồn gửi chứ không theo tên đăng nhập
[1] [No] CHK-access-control-014 → NFR-system-028 · Verify thông báo khi bị khoá không cho biết tên đăng nhập có tồn tại hay không
[2] [No] CHK-access-control-015 → NFR-system-028 · Verify sau mười lăm phút thì đăng nhập lại được

##1.3. Đổi mật khẩu lần đầu

[1] [Yes] CHK-access-control-016 → US-002/AC-005 · Verify đăng nhập lần đầu thì hệ thống buộc sang màn đổi mật khẩu
[1] [Yes] CHK-access-control-017 → US-002/AC-005 · Verify không có đường nào rời màn đổi mật khẩu lần đầu mà chưa đổi
[1] [Yes] CHK-access-control-018 → NFR-system-026 · Verify mật khẩu mới dưới mười hai ký tự bị từ chối
[1] [No] CHK-access-control-019 → NFR-system-026 · Verify mật khẩu nằm trong danh sách phổ biến bị từ chối
[2] [Yes] CHK-access-control-020 → NFR-system-026 · Verify mật khẩu mười hai ký tự không trộn loại ký tự vẫn được chấp nhận
[1] [Yes] CHK-access-control-021 → SCR-access-control-02 · Verify danh sách kiểm yêu cầu mật khẩu tự đánh dấu trong lúc gõ
[2] [Yes] CHK-access-control-022 → SCR-access-control-02 · Verify hai lần nhập mật khẩu mới không khớp thì báo lỗi tại ô nhập lại
[2] [Yes] CHK-access-control-023 → SCR-access-control-02 · Verify nhập sai mật khẩu hiện tại thì báo lỗi tại đúng ô đó
[1] [Yes] CHK-access-control-024 → US-002/AC-005 · Verify đổi mật khẩu thành công thì vào thẳng màn hỏi đáp mà không phải đăng nhập lại

##1.4. Đăng xuất và phiên

[1] [Yes] CHK-access-control-025 → US-001/AC-004 · Verify chọn đăng xuất thì phiên kết thúc và quay về màn đăng nhập
[1] [Yes] CHK-access-control-026 → US-001/AC-004 · Verify sau khi đăng xuất thì bấm nút quay lại của trình duyệt không vào lại được màn hỏi đáp
[1] [Yes] CHK-access-control-027 → US-001/AC-005 · Verify mở thẳng đường dẫn được bảo vệ khi chưa đăng nhập thì bị đưa về màn đăng nhập
[1] [Yes] CHK-access-control-028 → US-001/AC-005 · Verify đăng nhập xong thì được đưa tới đúng màn đã định mở trước đó
[1] [Yes] CHK-access-control-029 → US-001/AC-006 · Verify phiên hết hạn thì màn đăng nhập hiện dải thông báo giải thích lý do
[2] [Yes] CHK-access-control-030 → NFR-system-027 · Verify phiên hết hạn sau tám giờ không thao tác

#2. Quản lý tài khoản

[1] [Yes] CHK-access-control-031 → US-002/AC-001 · Verify tạo tài khoản đủ thông tin thì tài khoản xuất hiện trong danh sách
[1] [Yes] CHK-access-control-032 → US-002/AC-002 · Verify tạo tài khoản với tên đăng nhập đã tồn tại thì bị từ chối kèm báo lỗi tại ô đó
[1] [Yes] CHK-access-control-033 → US-002/AC-003 · Verify thiếu trường bắt buộc thì bị chặn lưu và báo lỗi tại đúng ô thiếu
[2] [Yes] CHK-access-control-034 → US-002/AC-004 · Verify tạo được tài khoản không thuộc phòng ban nào
[1] [Yes] CHK-access-control-035 → US-002/AC-006 · Verify vô hiệu hoá tài khoản thì tài khoản đó không đăng nhập được nữa
[1] [No] CHK-access-control-036 → US-002/AC-006 · Verify vô hiệu hoá tài khoản KHÔNG xoá lịch sử câu hỏi của người đó
[2] [Yes] CHK-access-control-037 → US-002/AC-006 · Verify tài khoản đã vô hiệu hoá hiển thị ở dạng chỉ đọc kèm nút kích hoạt lại
[1] [No] CHK-access-control-038 → US-002/AC-007 · Verify đổi phòng ban của một người thì quyền đọc của họ đổi theo ngay ở câu hỏi kế tiếp
[1] [No] CHK-access-control-039 → US-002/AC-007 · Verify đổi phòng ban KHÔNG bắt người đó đăng nhập lại
[2] [Yes] CHK-access-control-040 → SCR-access-control-04 · Verify tên đăng nhập ở chế độ sửa hiển thị chỉ đọc kèm lý do khi trỏ vào
[2] [Yes] CHK-access-control-041 → SCR-access-control-04 · Verify khối Người này đọc được gì tách con số theo bốn nguồn quyền
[1] [Yes] CHK-access-control-042 → US-007/AC-006 · Verify hạ mức mật của một người thì hộp thoại nêu số tài liệu người đó sẽ mất quyền đọc trước khi lưu
[2] [Yes] CHK-access-control-043 → SCR-access-control-04 · Verify nút sinh mật khẩu ngẫu nhiên tạo chuỗi đạt chính sách mật khẩu

#3. Nhập hàng loạt tài khoản

[1] [Yes] CHK-access-control-044 → US-003/AC-001 · Verify nạp tệp hợp lệ thì mọi dòng trong tệp được tạo thành tài khoản
[1] [Yes] CHK-access-control-045 → US-003/AC-002 · Verify bảng xem trước hiện ra trước khi bất kỳ tài khoản nào được ghi
[1] [No] CHK-access-control-046 → US-003/AC-002 · Verify huỷ ở bước xem trước thì không tài khoản nào được tạo
[1] [Yes] CHK-access-control-047 → US-003/AC-003 · Verify tệp có cả dòng đúng lẫn dòng lỗi thì dòng đúng vẫn được tạo và dòng lỗi bị bỏ qua
[2] [Yes] CHK-access-control-048 → US-003/AC-003 · Verify mỗi dòng lỗi hiển thị số thứ tự dòng trong tệp gốc kèm lý do
[1] [Yes] CHK-access-control-049 → US-003/AC-004 · Verify phòng ban chưa tồn tại thì hệ thống hỏi tạo mới hay để trống thay vì tự tạo
[1] [Yes] CHK-access-control-050 → US-003/AC-005 · Verify hai dòng trùng tên đăng nhập trong cùng tệp thì cả hai dòng đều bị loại
[1] [Yes] CHK-access-control-051 → US-003/AC-003 · Verify dòng trùng với tài khoản đã có trong hệ thống bị loại kèm lý do phân biệt với trùng trong tệp
[1] [Yes] CHK-access-control-052 → US-003/AC-006 · Verify tệp sai định dạng hoặc thiếu cột thì không tài khoản nào được tạo
[1] [Yes] CHK-access-control-053 → US-003/AC-007 · Verify bước kết quả hiển thị cặp tên đăng nhập và mật khẩu ban đầu cho từng tài khoản vừa tạo
[1] [Yes] CHK-access-control-054 → US-003/AC-007 · Verify danh sách mật khẩu ban đầu tải về được đúng một lần
[2] [Yes] CHK-access-control-055 → US-003/AC-007 · Verify bước kết quả cảnh báo rõ đây là lần duy nhất xem được mật khẩu ban đầu
[2] [Yes] CHK-access-control-056 → SCR-access-control-05 · Verify liên kết tải tệp mẫu cho ra tệp có sẵn các cột đúng tên
[2] [Yes] CHK-access-control-057 → SCR-access-control-05 · Verify đóng hộp thoại ở bước ba thì danh sách người dùng làm mới và hiện các tài khoản vừa tạo

#4. Phòng ban

[1] [Yes] CHK-access-control-058 → US-004/AC-001 · Verify tạo phòng ban mới thì phòng ban xuất hiện trong danh sách
[1] [Yes] CHK-access-control-059 → US-004/AC-002 · Verify tạo phòng ban trùng tên thì bị từ chối kèm báo lỗi
[1] [No] CHK-access-control-060 → US-004/AC-003 · Verify người thuộc hai phòng ban nhận quyền hợp từ cả hai
[2] [Yes] CHK-access-control-061 → US-004/AC-004 · Verify chọn một phòng ban thì cột phải hiện đủ thành viên kèm trạng thái hoạt động từng người
[1] [Yes] CHK-access-control-062 → US-004/AC-005 · Verify bỏ chọn một phòng ban ở màn chi tiết người dùng thì người đó rời khỏi phòng ban ấy
[1] [No] CHK-access-control-063 → US-004/AC-005 · Verify người vừa rời phòng ban mất quyền đọc đến từ phòng ban đó
[1] [Yes] CHK-access-control-064 → US-004/AC-006 · Verify xoá phòng ban còn thành viên thì hộp thoại nêu đúng số thành viên và số tài liệu bị ảnh hưởng
[1] [Yes] CHK-access-control-065 → US-004/AC-006 · Verify nút xác nhận xoá phòng ban chỉ bật sau khi số liệu hậu quả đã hiện ra
[1] [No] CHK-access-control-066 → US-004/AC-006 · Verify sau khi xoá phòng ban thì thành viên cũ chỉ còn quyền gán trực tiếp
[1] [No] CHK-access-control-067 → US-004/AC-007 · Verify đổi tên phòng ban thì mọi quyền đã gán qua phòng ban đó giữ nguyên hiệu lực
[2] [Yes] CHK-access-control-068 → SCR-access-control-06 · Verify danh sách phòng ban hiển thị phẳng, không có cấp cha con
[2] [Yes] CHK-access-control-069 → SCR-access-control-06 · Verify phòng ban không có thành viên nào hiển thị dấu cảnh báo kèm giải thích

#5. Gán quyền đọc tài liệu

[1] [No] CHK-access-control-070 → US-005/AC-001 · Verify gán quyền cho một phòng ban thì mọi thành viên phòng ban đó đọc được tài liệu
[1] [No] CHK-access-control-071 → US-005/AC-002 · Verify gán quyền cho một người cụ thể thì chỉ người đó đọc được thêm tài liệu
[1] [No] CHK-access-control-072 → US-005/AC-003 · Verify người vừa có quyền qua phòng ban vừa có quyền gán trực tiếp vẫn đọc được khi gỡ một trong hai đường
[1] [No] CHK-access-control-073 → US-005/AC-004 · Verify thu hồi quyền thì người đó không còn đọc được tài liệu ở câu hỏi kế tiếp
[1] [Yes] CHK-access-control-074 → US-005/AC-005 · Verify chọn nhiều tài liệu rồi cấp quyền cho một phòng ban trong một thao tác thì mọi tài liệu đã chọn đều được cấp
[2] [Yes] CHK-access-control-075 → US-005/AC-005 · Verify gán quyền hàng loạt báo số tài liệu đã áp dụng
[1] [Yes] CHK-access-control-076 → US-005/AC-006 · Verify khối quyền của một tài liệu liệt kê đủ chủ thể đang đọc được, tách bạch quyền trực tiếp với quyền qua phòng ban
[1] [No] CHK-access-control-077 → US-005/AC-007 · Verify tài liệu chưa gán quyền cho ai thì không xuất hiện trong kết quả của bất kỳ người dùng nào

#6. Từ chối quyền

[1] [No] CHK-access-control-078 → US-006/AC-001 · Verify mục từ chối cá nhân thắng quyền cho phép đến từ phòng ban
[1] [No] CHK-access-control-079 → US-006/AC-002 · Verify mục từ chối cá nhân thắng cả quyền cho phép gán trực tiếp
[1] [No] CHK-access-control-080 → US-006/AC-003 · Verify đặt từ chối cho một phòng ban thì mọi thành viên phòng ban đó không đọc được tài liệu
[1] [No] CHK-access-control-081 → US-006/AC-004 · Verify người bị từ chối nhận kết quả giống hệt người mà tài liệu không tồn tại
[1] [Yes] CHK-access-control-082 → US-006/AC-005 · Verify ô tra cứu trả lời một người có đọc được tài liệu không kèm lý do cụ thể
[1] [Yes] CHK-access-control-083 → US-006/AC-006 · Verify gỡ mục từ chối thì người đó đọc lại được tài liệu theo quyền sẵn có
[2] [Yes] CHK-access-control-084 → SCR-doc-ingestion-04 · Verify nút thêm quyền và nút thêm mục từ chối là hai nút tách biệt

#7. Nhãn mật và mức mật

[1] [Yes] CHK-access-control-085 → US-007/AC-001 · Verify không chọn nhãn mật thì không hoàn tất được việc nạp tài liệu
[1] [Yes] CHK-access-control-086 → US-007/AC-001 · Verify ô nhãn mật không có giá trị chọn sẵn
[1] [No] CHK-access-control-087 → US-007/AC-002 · Verify người có mức mật thấp hơn nhãn mật của tài liệu thì không đọc được tài liệu đó
[1] [No] CHK-access-control-088 → US-007/AC-003 · Verify người đủ mức mật nhưng không có trong danh sách cấp quyền thì vẫn không đọc được
[1] [No] CHK-access-control-089 → US-007/AC-004 · Verify tài liệu nhãn công khai vẫn cần được gán quyền mới đọc được
[1] [Yes] CHK-access-control-090 → US-007/AC-005 · Verify nâng nhãn mật của tài liệu đã nạp thì hộp thoại nêu số người sẽ mất quyền đọc trước khi xác nhận
[1] [No] CHK-access-control-091 → US-007/AC-007 · Verify trích dẫn trong hội thoại đang mở bị chặn ngay khi nhãn mật được nâng

#8. Lọc quyền tại bước truy hồi

[1] [No] CHK-access-control-092 → US-008/AC-001 · Verify hai người khác phạm vi quyền hỏi cùng một câu thì nhận hai câu trả lời khác nhau
[1] [No] CHK-access-control-093 → US-008/AC-002 · Verify không đoạn nào của tài liệu ngoài quyền lọt vào nội dung câu trả lời
[1] [No] CHK-access-control-094 → US-008/AC-003 · Verify việc lọc quyền diễn ra trước bước xếp hạng chứ không phải sau
[1] [No] CHK-access-control-095 → US-008/AC-004 · Verify người chưa được cấp quyền trên tài liệu nào nhận lời từ chối thay vì danh sách rỗng khó hiểu
[1] [No] CHK-access-control-096 → US-008/AC-005 · Verify quyền bị đổi giữa phiên thì câu hỏi kế tiếp áp quyền mới
[1] [No] CHK-access-control-097 → US-008/AC-006 · Verify độ phủ truy hồi không tụt khi phạm vi quyền của người hỏi hẹp
[1] [No] CHK-access-control-098 → US-008/AC-007 · Verify mở trích dẫn thì quyền được kiểm lại tại thời điểm mở
[1] [No] CHK-access-control-099 → US-008/AC-008 · Verify không có đường mạng nào trả về nội dung đoạn mà bỏ qua lọc quyền

#9. Bảo mật cơ bản

[1] [Yes] CHK-access-control-100 → CAP-access-control-04 · Verify nhân viên thường mở thẳng đường dẫn danh sách người dùng thì nhận thông báo không đủ quyền
[1] [Yes] CHK-access-control-101 → CAP-access-control-04 · Verify nhân viên thường mở thẳng đường dẫn quản lý phòng ban thì nhận thông báo không đủ quyền
[1] [No] CHK-access-control-102 → CAP-access-control-04 · Verify màn không đủ quyền không hiển thị bất kỳ dữ liệu nào của khu quản trị
[1] [No] CHK-access-control-103 → NFR-system-025 · Verify mật khẩu không xuất hiện ở dạng đọc lại được trong nhật ký hệ thống
[2] [No] CHK-access-control-104 → NFR-system-029 · Verify mọi thao tác đổi quyền đều để lại bản ghi kiểm toán có người thực hiện và thời điểm

#10. Trạng thái tải và lỗi

[2] [Yes] CHK-access-control-105 → SCR-access-control-03 · Verify danh sách người dùng khi đang tải hiện khung xương giữ hình dạng bảng
[2] [Yes] CHK-access-control-106 → SCR-access-control-03 · Verify lỗi tải danh sách thì cho thử lại tại chỗ mà không mất bộ lọc đang đặt
[2] [Yes] CHK-access-control-107 → SCR-access-control-03 · Verify danh sách rỗng lần đầu gợi ý nhập hàng loạt là đường nhanh hơn
[2] [Yes] CHK-access-control-108 → SCR-access-control-03 · Verify rỗng do bộ lọc hiển thị khác rỗng lần đầu và kèm nút xoá lọc
[2] [Yes] CHK-access-control-109 → SCR-access-control-06 · Verify danh sách phòng ban rỗng lần đầu hiện nút tạo phòng ban làm hành động chính

#11. Tiếp cận cơ bản

[3] [No] CHK-access-control-110 → NFR-system-037 · Verify đăng nhập làm được hoàn toàn bằng bàn phím
[3] [No] CHK-access-control-111 → NFR-system-037 · Verify tiêu điểm bàn phím luôn nhìn thấy được trên mọi ô của biểu mẫu đăng nhập
[3] [No] CHK-access-control-112 → NFR-system-037 · Verify phím Esc đóng ngăn kéo chi tiết người dùng
[3] [No] CHK-access-control-113 → NFR-system-036 · Verify thẻ mức mật đọc được bằng chữ chứ không chỉ phân biệt bằng màu

#12. Ca biên

[2] [Yes] CHK-access-control-114 → SCR-access-control-01 · Verify bấm nút đăng nhập hai lần thật nhanh thì chỉ một phiên được tạo
[2] [Yes] CHK-access-control-115 → SCR-access-control-04 · Verify tải lại trang giữa lúc đang sửa người dùng thì thay đổi chưa lưu không bị ghi
[2] [No] CHK-access-control-116 → SCR-access-control-04 · Verify hai quản trị cùng sửa một tài khoản thì lần lưu sau không âm thầm ghi đè lần lưu trước
[2] [Yes] CHK-access-control-117 → SCR-access-control-05 · Verify nạp lại đúng tệp vừa nhập hàng loạt thì mọi dòng bị báo trùng với tài khoản đã có
[3] [Yes] CHK-access-control-118 → SCR-access-control-03 · Verify bảng người dùng cuộn ngang trong khung của nó mà không làm cả trang cuộn ngang
