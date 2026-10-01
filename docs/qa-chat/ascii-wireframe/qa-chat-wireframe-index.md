---
type: screen-index
feature: qa-chat
status: draft
updated: 2026-09-23
links:
  - docs/qa-chat/srs/qa-chat-userflow.md
  - docs/qa-chat/usecases/qa-chat-usecase-index.md
  - docs/qa-chat/userstories/qa-chat-story-index.md
  - docs/_shared/design-system.md
---

# Hỏi đáp có trích dẫn — Danh sách màn hình

> Hệ thống thị giác, luật phủ trạng thái và luật lời lẽ ở `docs/_shared/design-system.md`. File này
> chỉ mô tả từng màn; không định nghĩa lại màu, chữ hay thành phần dùng chung.

## Screens

| Mã màn | Slug | Tên màn | Thuộc flow | Wireframe | Use case | Story | Trạng thái |
|---|---|---|---|---|---|---|---|
| `SCR-qa-chat-01` | `hoi-dap` | Hỏi đáp | `hoi-dap` | [hoi-dap.md](hoi-dap.md#screen-hoi-dap--hỏi-đáp) | `UC-single-turn-qa`, `UC-follow-up-qa`, `UC-multi-step-qa`, `UC-refuse-answer` | US-017, US-018, US-020, US-021, US-022, US-023, US-024, US-025, US-026 | draft |
| `SCR-qa-chat-02` | `xem-tai-lieu-goc` | Xem tài liệu gốc | `hoi-dap` | [hoi-dap.md](hoi-dap.md#screen-xem-tai-lieu-goc--xem-tài-liệu-gốc) | `UC-single-turn-qa` | US-019, US-008 | draft |

> Cột `Slug` là định danh máy đọc, khớp `## Screen: {slug}` trong tệp wireframe và Mục 2 của
> `../srs/qa-chat-userflow.md`. Bảng mô tả từng phần tử sống trong tệp wireframe, không lặp ở đây.

## Descriptions

### SCR-qa-chat-01 — Hỏi đáp

Màn duy nhất nhân viên dùng hằng ngày. Mọi thứ khác của sản phẩm tồn tại để màn này trả lời đúng.

**Bố cục.** Ba vùng dọc. Trên cùng là thanh mảnh: tên sản phẩm bên trái, tên người dùng và nút đăng
xuất bên phải, nút bắt đầu phiên mới ở giữa lệch phải. Giữa là vùng hội thoại cuộn được, chiếm gần
hết chiều cao, nội dung căn giữa trong bề rộng đọc được — không trải hết màn hình lớn. Dưới cùng là
ô nhập câu hỏi neo cố định, luôn nhìn thấy dù cuộn lên bao xa.

**Vùng hội thoại.** Các lượt xếp theo thời gian, cũ trên mới dưới, tự cuộn xuống lượt mới nhất. Câu
hỏi của người dùng canh phải trong một khối nền nhạt, hẹp hơn. Câu trả lời của hệ thống canh trái,
trải hết bề rộng đọc được, không có khung bao — nội dung là thứ chính, không cần viền để được chú ý.

**Một lượt trả lời gồm bốn phần, theo thứ tự xuất hiện:**

1. *Chỉ báo tiến trình* — hiện ngay khi gửi, nói bằng lời hệ thống đang ở bước nào: đang hiểu câu
   hỏi, đang tìm tài liệu liên quan, đang soạn câu trả lời. Biến mất khi chữ đầu tiên xuất hiện.
   Đây không phải vòng xoay trang trí; ba bước này là ba module agent và người dùng nên thấy chúng.
2. *Câu trả lời* — hiện dần từng chữ. Trong lúc đang hiện, nút gửi chuyển sang nút dừng.
3. *Khối cảnh báo* — chỉ hiện khi có. Tách hẳn khỏi câu trả lời bằng nền và biểu tượng, dùng màu
   cảnh báo chứ không phải màu lỗi. Ba loại: nguồn đã hết hiệu lực, nguồn đã bị thay thế bởi văn
   bản nào, và có hai văn bản chồng lấn cùng còn hiệu lực.
4. *Khối nguồn* — hiện sau khi câu trả lời xong. Mỗi nguồn một dòng: tên tài liệu, đường dẫn phân
   cấp tới đoạn (ví dụ *Chương VII › Điều 113 › Khoản 2*), và một thẻ nhỏ ghi trạng thái hiệu lực
   nếu khác bình thường. Bấm vào dòng nào mở `SCR-qa-chat-02` tại đúng đoạn đó.

**Hai khối mở rộng được, thu gọn mặc định.** Đặt ngay dưới câu trả lời, dạng dòng chữ nhỏ có mũi
tên, không chiếm chỗ khi không cần:

- *Hệ thống đã hiểu câu hỏi của bạn là* — chỉ hiện với câu hỏi tiếp nối. Mở ra thấy câu đã được
  viết lại thành câu độc lập. Đây là chỗ người dùng phát hiện hệ thống hiểu lệch trước khi tin vào
  câu trả lời.
- *Các bước đã thực hiện* — chỉ hiện với câu hỏi phức tạp. Mở ra thấy danh sách truy vấn con theo
  thứ tự đã chạy, mỗi truy vấn kèm số nguồn tìm được.

**Ô nhập.** Ô nhiều dòng tự giãn theo nội dung tới một trần rồi cuộn trong ô. Nhãn thật ở trên,
không chỉ chữ mờ gợi ý. Enter gửi, Shift kèm Enter xuống dòng — quy ước này ghi ngay dưới ô bằng
chữ phụ. Nút gửi vô hiệu hoá khi ô rỗng và khi đang có lượt chạy dở, kèm lý do khi trỏ vào.

**Trạng thái phải phủ** (theo mục 6 của hệ thống thị giác):

| Trạng thái | Thể hiện |
|---|---|
| Rỗng lần đầu | Lời chào ngắn, ba câu hỏi mẫu bấm được lấy từ chính kho của tổ chức, một dòng giải thích hệ thống chỉ trả lời từ tài liệu nội bộ |
| Đang tải | Chỉ báo tiến trình theo bước, không phải vòng xoay |
| Đang gửi | Nút gửi thành nút dừng; ô nhập vẫn gõ được nhưng chưa gửi được |
| Lỗi | Ba loại tách bạch: mất kết nối giữa chừng (giữ phần đã nhận), hệ thống đang bận, phiên hết hạn (đưa về đăng nhập) |
| Không đủ quyền | Không xuất hiện ở màn này dưới dạng thông báo quyền — nó hiện ra dưới dạng lời từ chối, xem dưới |
| Từ chối | Không phải lỗi, không tô màu lỗi. Khối riêng, giọng bình thường, nêu lý do thuộc loại nào và việc nên làm tiếp |
| Một phần thành công | Câu hỏi nhiều vế mà chỉ một số vế có căn cứ: các vế trả lời được hiện bình thường, vế thiếu căn cứ ghi rõ trong cùng câu trả lời |
| Lượt bị dừng | Phần đã hiện giữ nguyên, gắn dấu hiệu chưa hoàn tất |
| Chỉ đọc / Vô hiệu hoá | Không áp dụng — màn này không có chế độ chỉ đọc |

**Điểm dễ làm sai, ghi ra để không phải phát hiện lúc đã code:** lời từ chối phải phân biệt được ba
lý do (kho không có thông tin · có thể chưa được cấp quyền · câu hỏi ngoài phạm vi kho tri thức)
mà **không** để lộ rằng có tồn tại tài liệu phù hợp người dùng không được xem. Đây là ràng buộc
bảo mật, không phải chuyện câu chữ.

### SCR-qa-chat-02 — Xem tài liệu gốc

Mở ra từ một dòng trong khối nguồn. Mục đích duy nhất: cho người dùng tự kiểm trong vài giây rồi
quay lại hội thoại.

**Bố cục.** Ngăn kéo trượt từ phải, chiếm khoảng hai phần ba bề ngang, hội thoại vẫn thấy mờ phía
sau — người dùng không mất ngữ cảnh. Trên ngăn kéo: tên tài liệu, đường dẫn phân cấp tới đoạn đang
xem, thẻ nhãn mật, thẻ trạng thái hiệu lực, nút tải tệp gốc, nút đóng.

**Thân.** Nội dung tài liệu ở dạng đọc được, cuộn tự do lên xuống để đọc ngữ cảnh trước và sau.
Đoạn được trích **làm nổi bật bằng nền nhạt và một vạch dọc bên trái**, và màn hình tự cuộn tới nó
khi mở. Bảng biểu hiển thị đúng dạng bảng kèm dòng tiêu đề — kể cả khi đoạn được trích chỉ là một
mảnh của bảng dài, phần hiển thị vẫn phải có tiêu đề cột, nếu không người dùng nhìn thấy một dãy số
không đầu không cuối.

**Trạng thái phải phủ:**

| Trạng thái | Thể hiện |
|---|---|
| Đang tải | Khung xương giữ hình dạng trang tài liệu |
| Lỗi tải | Nói rõ không đọc được tệp và cho thử lại |
| Không đủ quyền | Quyền bị thu hồi sau khi trích dẫn đã hiện: ngăn kéo mở ra nhưng chỉ có thông báo không còn quyền truy cập, không hiện nội dung nào |
| Tài liệu đã gỡ | Thông báo tài liệu không còn dùng làm căn cứ, phân biệt với đã xoá |
| Tài liệu đã xoá vĩnh viễn | Thông báo tài liệu không còn trong hệ thống |
| Rỗng | Không áp dụng — luôn mở từ một trích dẫn cụ thể |

**Ràng buộc thi hành:** quyền được kiểm **tại thời điểm mở**, không dựa vào việc trích dẫn đã hiện
ra trước đó. Nút tải tệp gốc kiểm quyền lần nữa trước khi trả tệp.
