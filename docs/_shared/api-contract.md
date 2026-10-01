---
type: api-contract
status: draft
updated: 2026-09-24
links:
  - docs/_shared/architecture.md
  - docs/_shared/data-model.md
  - docs/_shared/nfr.md
---

# Hợp đồng giao diện lập trình

> Mô tả **ranh giới hành vi** giữa giao diện và máy chủ: gọi cái gì thì hệ thống làm gì và trả về
> ý nghĩa nghiệp vụ nào. Không mô tả khung công tác, không mô tả cấu trúc gói tin chi tiết — những
> thứ đó thuộc mã nguồn.

## 1. Bốn nguyên tắc

**Một — mã trạng thái không được tiết lộ sự tồn tại.** Đây là nguyên tắc quan trọng nhất và cũng là
chỗ dễ vi phạm nhất mà không ai nhận ra.

| Tình huống | Trả về | Vì sao |
|---|---|---|
| Tài liệu không tồn tại | *không tìm thấy* | — |
| Tài liệu tồn tại nhưng người gọi không có quyền đọc | **cũng là *không tìm thấy*** | Trả *không đủ quyền* là xác nhận tài liệu đó có thật. `AC-004` của US-006 và `AC-002` của US-008 đều đòi giữ kín cả sự tồn tại |
| Người gọi không có quyền vào khu quản trị | *không đủ quyền* | Ở đây quyền là quyền trên **chức năng**, không phải trên một tài liệu cụ thể, nên không lộ gì |

Ranh giới: giấu sự tồn tại của **dữ liệu**, không giấu sự tồn tại của **chức năng**.

**Hai — mọi lỗi trả về cùng một hình dạng**: một mã lỗi máy đọc được, một câu nghiệp vụ cho người
đọc, và khi áp dụng thì kèm tên trường bị lỗi. Câu cho người đọc phải trả lời đủ ba vế theo mục 7
của hệ thống thị giác: chuyện gì xảy ra, vì sao, giờ làm gì.

**Ba — không dấu vết kỹ thuật lọt ra ngoài** (`NFR-system-031`). Vết gọi hàm, câu truy vấn, đường
dẫn tệp máy chủ đều dừng lại ở nhật ký.

**Bốn — mọi đường dẫn tới nội dung tài liệu đều đi qua cùng một bộ giải quyết quyền** (luật R1 của
kiến trúc). Không có ngoại lệ nào cho "đường tải tệp" hay "đường xem trước".

## 2. Xác thực

| Việc | Trả về |
|---|---|
| Đăng nhập | Phiên làm việc; nếu tài khoản phải đổi mật khẩu thì trả cờ báo giao diện chuyển sang màn đổi mật khẩu, **chưa** cho vào màn hỏi đáp |
| Đăng xuất | Kết thúc phiên; phiên cũ không dùng lại được |
| Đổi mật khẩu | Thành công thì vào thẳng màn hỏi đáp, không bắt đăng nhập lại (`AC-007` của US-002) |
| Lấy thông tin người đang đăng nhập | Họ tên, vai trò, mức mật, phòng ban — giao diện cần để quyết hiện khu quản trị hay không |

Sai thông tin đăng nhập trả về **một** thông báo duy nhất, không phân biệt sai tên hay sai mật khẩu.
Tài khoản bị vô hiệu hoá là ngoại lệ có chủ đích và trả về thông báo riêng — lý do ghi ở
`SCR-access-control-01`.

## 3. Hỏi đáp — nơi luồng sự kiện một chiều được dùng

Gửi câu hỏi mở một luồng sự kiện. Bảy loại sự kiện, theo đúng thứ tự có thể xuất hiện:

| Sự kiện | Mang gì | Ứng với |
|---|---|---|
| `queued` | Vị trí trong hàng đợi | `NFR-system-008` — quá 5 câu đang xử lý |
| `status` | Bước đang chạy, bằng lời nghiệp vụ | Chỉ báo tiến trình của `SCR-qa-chat-01` |
| `rewritten` | Câu hỏi đã viết lại thành câu độc lập | `AC-002` của US-020 |
| `plan` | Danh sách truy vấn con theo thứ tự | `AC-006` của US-022 |
| `token` | Một mẩu chữ của câu trả lời | US-018 |
| `warning` | Loại cảnh báo và tài liệu liên quan | US-025 |
| `citations` | Danh sách nguồn, mỗi nguồn kèm đường dẫn phân cấp | US-017 |
| `refusal` | Lý do từ chối, thuộc một trong ba loại | US-023 |
| `done` | Kết thúc lượt, kèm thời gian tới chữ đầu và tổng thời gian | Nguồn cho `NFR-system-001` và `002` |
| `error` | Mất kết nối, quá thời gian chờ, hệ thống bận | US-018 |

Ba điều hợp đồng này ràng buộc:

- **`refusal` và `token` loại trừ nhau.** Một lượt hoặc trả lời hoặc từ chối, không có lượt trả lời
  nửa vời rồi từ chối. Khi tầng chặn thứ hai loại hết ý, các mẩu chữ đã gửi phải bị thu hồi bằng
  một sự kiện `refusal` và giao diện xoá phần đã hiện — `AC-002` của US-024.
- **`citations` luôn đến sau mẩu chữ cuối**, vì danh sách nguồn chỉ chốt được khi câu trả lời xong.
- **Người gọi ngắt kết nối thì máy chủ dừng sinh chữ**, không chạy tiếp cho hết rồi vứt kết quả.

Ngoài luồng sự kiện còn: lấy lịch sử lượt trong phiên; bắt đầu phiên mới; dừng lượt đang chạy.

## 4. Đọc tài liệu

| Việc | Ghi chú |
|---|---|
| Lấy nội dung tài liệu quanh một đoạn | Kiểm quyền **tại thời điểm gọi**, không dựa vào việc trích dẫn đã hiện trước đó (`AC-003` của US-019) |
| Tải tệp gốc | Kiểm quyền lần nữa; không có đường tải nào bỏ qua bước này |

Cả hai trả về *không tìm thấy* khi người gọi không có quyền, theo nguyên tắc một.

## 5. Quản trị tài liệu

| Nhóm | Việc |
|---|---|
| Nạp | Tải tệp lên · theo dõi tiến độ từng tệp · xem lý do lỗi · huỷ đợt nạp |
| Metadata | Lấy metadata hệ thống đã đề xuất **kèm trích đoạn làm căn cứ từng giá trị** · xác nhận hoặc sửa |
| Vòng đời | Khai bản thay thế · gỡ · xoá vĩnh viễn · xem chuỗi thay thế |
| Chồng lấn | Danh sách cảnh báo chờ xử lý · chọn một trong ba hướng xử lý |
| Xem xét | Danh sách tài liệu có lọc và phân trang · xem trước các đoạn đã tách |

Hai ràng buộc hành vi:

- **Nạp tài liệu là thao tác không đồng bộ.** Gọi xong trả về ngay mã theo dõi; tiến độ đọc qua
  đường riêng. `AC-007` của US-009 đòi việc xử lý không phụ thuộc việc người dùng còn mở màn hình.
- **Xoá vĩnh viễn và nâng nhãn mật trả về trước con số hậu quả** (số người mất quyền đọc, số trích
  dẫn đang trỏ tới) để giao diện hiện hộp thoại xác nhận có nội dung thật, chứ không phải một câu
  hỏi chung chung.

## 6. Quản trị người dùng và quyền

| Nhóm | Việc |
|---|---|
| Người dùng | Danh sách có lọc · tạo · sửa · vô hiệu hoá · đặt lại mật khẩu · nhập hàng loạt hai bước (xem trước rồi mới ghi) |
| Phòng ban | Danh sách · tạo · sửa · xoá kèm con số hậu quả · quản lý thành viên |
| Quyền | Xem quyền của một tài liệu, **tách bạch quyền trực tiếp với quyền đến từ phòng ban** · thêm cho phép · thêm từ chối · gỡ · **gán quyền cho nhiều tài liệu trong một thao tác** (`AC-005` của US-005), trả về số tài liệu đã áp dụng |
| Tra cứu | Hỏi "người này đọc được tài liệu này không" và nhận **lý do**: được phép qua đường nào, bị mục từ chối nào chặn, hay bị nhãn mật chặn |

Đường tra cứu ở dòng cuối là hiện thực của `AC-005` trong US-006. Nó không thêm năng lực nào cho hệ
thống nhưng là thứ duy nhất giúp người quản trị gỡ rối cấu hình quyền mà không phải đoán.

**Mọi đường ghi trong nhóm này đều tăng số hiệu phiên bản quyền** (mục 8 của mô hình dữ liệu). Quên
một đường là tạo ra lỗ rò rỉ chỉ xuất hiện sau khi cấu hình thay đổi — loại lỗi khó tái hiện nhất.

## 7. Báo cáo khoảng trống

Danh sách nhóm khoảng trống, lọc theo khoảng thời gian và theo lý do, xem câu hỏi cụ thể trong một
nhóm, đánh dấu đã xử lý.

**Không có đường nào lọc theo người dùng.** Ràng buộc này thuộc về hợp đồng, không chỉ thuộc về
giao diện: thiếu đường đó ở tầng máy chủ thì không ai dựng được công cụ giám sát trên nền hệ thống
này, kể cả người sau này sửa giao diện.

## 8. Bộ đánh giá — cố ý không có đường mạng

Bộ đo chạy bằng **lệnh**, không qua giao diện lập trình.

Lý do không phải là tiết kiệm công. Bộ đo là công cụ của tác giả, không phải tính năng của người
dùng; mở nó ra thành đường mạng nghĩa là phải phân quyền cho nó, phải giới hạn tần suất, phải xử lý
lượt chạy kéo dài hàng giờ qua kết nối HTTP. Chạy bằng lệnh không có vấn đề nào trong số đó.

Bộ đo gọi thẳng bộ điều phối agent theo luật R6 của kiến trúc — cùng đường mã người dùng đi.

## 9. Điều hợp đồng này KHÔNG có

- **Không có đường MẠNG nào cho phép truy hồi mà bỏ qua lọc quyền**, kể cả cho mục đích gỡ lỗi. Đọc
  không lọc quyền chỉ tồn tại ở đúng một chỗ — khâu phân loại lý do khoảng trống — và nó đi qua giá
  trị `unfiltered` của phạm vi quyền theo luật R1 của kiến trúc, có ghi nhật ký kiểm toán mọi lần.
  Sửa 2026-09-24: bản trước viết *"không có đường nào"* rồi câu ngay sau lại cho phép chạy bằng
  lệnh — cấm và cho trong hai câu liền nhau, nên người hiện thực không biết luật thật là gì.
- **Không có đường xoá hàng loạt tài liệu.** Xoá vĩnh viễn là thao tác từng tài liệu một, có xác
  nhận riêng. Xoá hàng loạt là thứ chỉ cần sai một lần là mất cả kho.
- **Không có phiên bản hoá giao diện lập trình.** Giao diện và máy chủ dựng cùng một ảnh và luôn
  cùng phiên bản; thêm tầng phiên bản là giải một bài toán không tồn tại ở đây.
- **Không có đường nào trả về danh sách toàn bộ người dùng cho nhân viên thường.** Danh bạ nhân sự
  không phải thứ hệ thống này được phép làm lộ.
