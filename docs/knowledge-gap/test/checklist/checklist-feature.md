#1. Ghi nhận khoảng trống

[1] [No] CHK-knowledge-gap-001 → US-027/AC-001 · Verify lượt từ chối vì kho không có thông tin sinh một bản ghi khoảng trống mang đúng lý do đó
[1] [No] CHK-knowledge-gap-002 → US-027/AC-002 · Verify lượt từ chối vì thiếu quyền sinh bản ghi mang lý do thiếu quyền, phân biệt với lý do kho không có
[1] [No] CHK-knowledge-gap-003 → US-027/AC-003 · Verify lượt bị loại hết ý ở tầng hai sinh bản ghi mang lý do riêng, không lẫn với lý do không truy hồi được gì
[1] [No] CHK-knowledge-gap-004 → US-027/AC-004 · Verify bản ghi của một lượt tiếp nối lưu cả câu gốc lẫn câu đã viết lại
[1] [No] CHK-knowledge-gap-005 → US-027/AC-005 · Verify câu hỏi rỗng hoặc quá ngắn KHÔNG sinh bản ghi khoảng trống
[1] [No] CHK-knowledge-gap-006 → US-027/AC-006 · Verify lượt trả lời được nhưng căn cứ mỏng vẫn sinh bản ghi mang dấu hiệu căn cứ mỏng
[1] [No] CHK-knowledge-gap-007 → US-027/AC-007 · Verify bản ghi khoảng trống không lưu nội dung của tài liệu nằm ngoài quyền người hỏi

#2. Gom nhóm và sắp xếp

[1] [No] CHK-knowledge-gap-008 → US-028/AC-001 · Verify các câu hỏi cùng chủ đề được gom vào một nhóm thay vì liệt kê rời
[1] [Yes] CHK-knowledge-gap-009 → US-028/AC-002 · Verify danh sách nhóm sắp theo số lần được hỏi giảm dần
[2] [Yes] CHK-knowledge-gap-010 → SCR-knowledge-gap-01 · Verify mỗi thẻ nhóm hiển thị con số lần hỏi, câu hỏi đại diện và khoảng thời gian từ lần đầu tới lần gần nhất
[2] [Yes] CHK-knowledge-gap-011 → US-027/AC-004 · Verify câu hỏi đại diện của nhóm dùng câu đã viết lại chứ không dùng câu gốc rút gọn

#3. Ba tab phân loại

[1] [Yes] CHK-knowledge-gap-012 → US-028/AC-003 · Verify màn có đủ ba tab thiếu tài liệu, thiếu quyền và căn cứ mỏng
[1] [Yes] CHK-knowledge-gap-013 → US-028/AC-003 · Verify nhóm thuộc tab căn cứ mỏng không xuất hiện trong tab thiếu tài liệu
[2] [Yes] CHK-knowledge-gap-014 → SCR-knowledge-gap-01 · Verify đổi tab thì bộ lọc thời gian và bộ lọc trạng thái đang chọn được giữ nguyên
[2] [Yes] CHK-knowledge-gap-015 → SCR-knowledge-gap-01 · Verify nhãn dưới con số lớn nói rõ đang đếm gì, khác nhau giữa tab căn cứ mỏng và hai tab kia

#4. Xem chi tiết một nhóm

[1] [Yes] CHK-knowledge-gap-016 → US-028/AC-004 · Verify mở rộng một nhóm thì thấy danh sách câu hỏi cụ thể kèm thời điểm
[2] [Yes] CHK-knowledge-gap-017 → SCR-knowledge-gap-01 · Verify nội dung mở rộng chèn ngay dưới đúng thẻ nhóm chứ không mở ngăn kéo hay hộp thoại
[2] [Yes] CHK-knowledge-gap-018 → US-027/AC-004 · Verify câu gốc hiển thị thu gọn bên dưới câu đã viết lại để đối chiếu
[2] [Yes] CHK-knowledge-gap-019 → SCR-knowledge-gap-01 · Verify nhóm ở tab thiếu quyền hiển thị phòng ban của người hỏi

#5. Ranh giới chống giám sát

[1] [Yes] CHK-knowledge-gap-020 → US-028/AC-005 · Verify màn không có ô tìm kiếm hay bộ lọc theo người dùng ở bất kỳ vị trí nào
[1] [No] CHK-knowledge-gap-021 → US-028/AC-005 · Verify không có đường mạng nào trả về danh sách mọi câu hỏi của một người cụ thể
[1] [Yes] CHK-knowledge-gap-022 → US-028/AC-005 · Verify phần mở rộng nhóm không hiển thị tên, thư điện tử hay định danh cá nhân nào của người hỏi

#6. Lọc và đánh dấu đã xử lý

[1] [Yes] CHK-knowledge-gap-023 → US-028/AC-006 · Verify đổi khoảng thời gian thì danh sách nhóm lọc lại ngay tại chỗ
[1] [Yes] CHK-knowledge-gap-024 → US-028/AC-007 · Verify đánh dấu một nhóm đã xử lý thì nhóm chuyển sang bộ lọc đã xử lý
[1] [Yes] CHK-knowledge-gap-025 → US-028/AC-007 · Verify nhóm đã đánh dấu xử lý mà sau đó lại bị hỏi và vẫn từ chối thì quay về trạng thái chờ xử lý
[1] [Yes] CHK-knowledge-gap-026 → US-028/AC-007 · Verify nhóm quay về chờ xử lý mang thẻ đã xử lý nhưng chưa hết thay vì tạo nhóm mới
[2] [Yes] CHK-knowledge-gap-027 → SCR-knowledge-gap-01 · Verify nút đánh dấu đã xử lý vào trạng thái chờ sau khi bấm, chặn bấm lần hai

#7. Liên kết sang màn khắc phục

[2] [Yes] CHK-knowledge-gap-028 → SCR-knowledge-gap-01 · Verify nhóm thiếu tài liệu có liên kết dẫn sang màn danh sách tài liệu
[2] [Yes] CHK-knowledge-gap-029 → SCR-knowledge-gap-01 · Verify nhóm thiếu quyền có liên kết dẫn sang khối quyền của màn chi tiết tài liệu
[1] [No] CHK-knowledge-gap-030 → US-028/AC-005 · Verify liên kết khắc phục không mang theo tham số lọc nào theo người hỏi

#8. Bảo mật và trạng thái

[1] [Yes] CHK-knowledge-gap-031 → US-028/AC-009 · Verify nhân viên thường mở thẳng đường dẫn báo cáo thì nhận thông báo không đủ quyền
[1] [No] CHK-knowledge-gap-032 → US-028/AC-009 · Verify màn không đủ quyền không hiển thị bất kỳ dữ liệu khoảng trống nào
[2] [Yes] CHK-knowledge-gap-033 → US-028/AC-009 · Verify màn không đủ quyền kèm liên kết quay về màn hỏi đáp
[1] [Yes] CHK-knowledge-gap-034 → US-028/AC-008 · Verify chưa có bản ghi nào thì màn nói rõ đây là trạng thái tốt chứ không phải lỗi
[2] [Yes] CHK-knowledge-gap-035 → US-028/AC-008 · Verify trạng thái rỗng lần đầu giải thích màn này sẽ hiện gì khi có dữ liệu
[2] [Yes] CHK-knowledge-gap-036 → SCR-knowledge-gap-01 · Verify rỗng do bộ lọc hiển thị khác rỗng lần đầu và kèm nút mở rộng khoảng thời gian
[2] [Yes] CHK-knowledge-gap-037 → SCR-knowledge-gap-01 · Verify màn khi đang tải hiện khung xương giữ hình dạng danh sách thẻ
[2] [Yes] CHK-knowledge-gap-038 → SCR-knowledge-gap-01 · Verify lỗi tải cho thử lại tại chỗ

#9. Tiếp cận và ca biên

[3] [No] CHK-knowledge-gap-039 → NFR-system-037 · Verify mở rộng và thu gọn một nhóm làm được bằng bàn phím
[3] [No] CHK-knowledge-gap-040 → NFR-system-036 · Verify thẻ đã xử lý nhưng chưa hết đọc được bằng chữ chứ không chỉ phân biệt bằng màu
[2] [Yes] CHK-knowledge-gap-041 → SCR-knowledge-gap-01 · Verify nhóm có rất nhiều câu hỏi cụ thể thì phần mở rộng cuộn trong khung của nó
[2] [Yes] CHK-knowledge-gap-042 → SCR-knowledge-gap-01 · Verify tải lại trang thì tab và bộ lọc quay về giá trị mặc định thay vì trạng thái không xác định
