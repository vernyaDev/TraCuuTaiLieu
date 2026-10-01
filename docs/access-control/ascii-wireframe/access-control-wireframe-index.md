---
type: screen-index
feature: access-control
status: draft
updated: 2026-09-23
links:
  - docs/access-control/usecases/access-control-usecase-index.md
  - docs/access-control/userstories/access-control-story-index.md
  - docs/_shared/design-system.md
---

# Tài khoản và phân quyền — Danh sách màn hình

> Hệ thống thị giác và luật phủ trạng thái ở `docs/_shared/design-system.md`.

## Screens

| Mã màn | Slug | Tên màn | Thuộc flow | Wireframe | Use case | Story | Trạng thái |
|---|---|---|---|---|---|---|---|
| `SCR-access-control-01` | `dang-nhap` | Đăng nhập | `vao-he-thong` | [vao-he-thong.md](vao-he-thong.md) | — | US-001 | draft |
| `SCR-access-control-02` | `doi-mat-khau-lan-dau` | Đổi mật khẩu lần đầu | `vao-he-thong` | [vao-he-thong.md](vao-he-thong.md) | — | US-002 | draft |
| `SCR-access-control-03` | `danh-sach-nguoi-dung` | Danh sách người dùng | `quan-ly-nguoi-dung` | [quan-ly-nguoi-dung.md](quan-ly-nguoi-dung.md) | `UC-grant-document-access` | US-002, US-004, US-007 | draft |
| `SCR-access-control-04` | `chi-tiet-nguoi-dung` | Chi tiết người dùng | `quan-ly-nguoi-dung` | [quan-ly-nguoi-dung.md](quan-ly-nguoi-dung.md) | `UC-grant-document-access` | US-002, US-004, US-007 | draft |
| `SCR-access-control-05` | `nhap-hang-loat` | Nhập hàng loạt tài khoản | `nhap-hang-loat` | [nhap-hang-loat.md](nhap-hang-loat.md) | — | US-003 | draft |
| `SCR-access-control-06` | `quan-ly-phong-ban` | Quản lý phòng ban | `quan-ly-phong-ban` | [quan-ly-phong-ban.md](quan-ly-phong-ban.md) | `UC-grant-document-access` | US-004 | draft |

> Cột `Slug` là định danh máy đọc, khớp `## Screen: {slug}` trong tệp wireframe và Mục 2 của
> `../srs/access-control-userflow.md`. Bốn màn quản trị trước đây gộp chung một luồng *Quản trị
> người dùng*; userflow đã chia thành ba luồng riêng và bảng này cập nhật theo (2026-09-23).

Khối quyền truy cập của một tài liệu và ô tra cứu quyền nằm ở `SCR-doc-ingestion-04` — quyền gắn
với tài liệu nên nó sống ở màn tài liệu, không tách ra một màn riêng.

## Descriptions

### SCR-access-control-01 — Đăng nhập

Màn đầu tiên mọi người thấy, và là màn duy nhất người chưa đăng nhập vào được.

**Bố cục.** Khối hẹp căn giữa màn hình, bề ngang khoảng bằng một biểu mẫu đọc thoải mái — **không**
trải hết bề ngang màn hình lớn. Trên khối: tên sản phẩm và một dòng nói hệ thống này để làm gì.
Trong khối: ô tên đăng nhập, ô mật khẩu có nút hiện/ẩn, nút đăng nhập trải hết bề ngang khối.

**Lời lẽ khi sai.** Một thông báo duy nhất *"Tên đăng nhập hoặc mật khẩu không đúng"*, đặt trên
nút, giữ nguyên tên đăng nhập đã gõ và xoá ô mật khẩu. Thông báo **không** cho biết tên đăng nhập
có tồn tại hay không — nếu phân biệt được thì bất kỳ ai cũng dò ra được danh sách nhân sự của
doanh nghiệp.

**Tài khoản bị vô hiệu hoá** là thông báo khác: nói rõ tài khoản đã ngừng hoạt động và đề nghị liên
hệ quản trị. Đây là ngoại lệ có chủ đích của luật trên — người đã từng có tài khoản cần biết vì sao
mình không vào được, và thông tin này không giúp gì cho người dò danh sách.

**Trạng thái phải phủ:** mặc định · đang gửi (nút vào trạng thái chờ, chặn gửi trùng) · sai thông
tin · tài khoản vô hiệu hoá · phiên hết hạn (dải thông báo trên đầu khối, kèm ghi nhớ trang người
dùng định vào để đưa họ tới đó sau khi đăng nhập) · **bị khoá tạm 15 phút sau 10 lần sai liên tiếp**
· lỗi kết nối tới máy chủ.

**Quên mật khẩu.** Dưới nút đăng nhập có một dòng chữ tĩnh chỉ đường liên hệ quản trị tri thức. Hệ
thống chạy hoàn toàn nội bộ, chưa có kênh gửi thư nên **không tự phục vụ đặt lại mật khẩu** — quản
trị đặt lại giúp ở màn chi tiết người dùng. Thiếu dòng này thì người quên mật khẩu rơi vào ngõ cụt
mà không biết hỏi ai.

**Bộ đếm khoá tài khoản đếm theo nguồn gửi, không theo tên đăng nhập** (chốt 2026-09-09). Đếm theo
tên đăng nhập thì tên có thật sẽ bị khoá còn tên bịa thì không bao giờ khoá — chính khác biệt hành
vi đó là kênh dò danh sách nhân sự, dù chữ nghĩa hai bên giống hệt nhau. Kéo theo: thông báo khi bị
khoá phải trung lập, không được ngụ ý *tài khoản của bạn bị khoá*.

### SCR-access-control-02 — Đổi mật khẩu lần đầu

Hiện ngay sau lần đăng nhập đầu tiên, không bỏ qua được, không có đường vòng sang màn khác.

**Bố cục.** Cùng khối hẹp căn giữa như màn đăng nhập, để người dùng thấy đây là phần tiếp của cùng
một luồng. Một dòng giải thích vì sao phải đổi. Ba ô: mật khẩu hiện tại, mật khẩu mới, xác nhận
mật khẩu mới.

**Yêu cầu mật khẩu hiện ngay dưới ô mật khẩu mới** dưới dạng danh sách kiểm tự đánh dấu khi gõ —
không phải một dòng chữ nhỏ, và tuyệt đối không phải một thông báo lỗi hiện ra sau khi bấm gửi.
Người dùng phải thấy mình còn thiếu điều kiện nào trong lúc đang gõ.

**Trạng thái phải phủ:** mặc định · đang gõ (danh sách kiểm cập nhật) · mật khẩu xác nhận không
khớp · mật khẩu hiện tại sai · đang gửi · thành công (chuyển thẳng vào màn hỏi đáp, không bắt đăng
nhập lại).

### SCR-access-control-03 — Danh sách người dùng

**Bố cục.** Thanh bên trái điều hướng khu quản trị. Vùng chính: tiêu đề màn, nút *thêm người dùng*
và nút *nhập hàng loạt* ở góc phải, thanh lọc, bảng.

**Thanh lọc.** Ô tìm theo tên hoặc tên đăng nhập, ô chọn phòng ban, ô chọn mức mật, ô chọn trạng
thái hoạt động.

**Bảng.** Các cột: họ tên · tên đăng nhập · phòng ban · mức mật · trạng thái · lần đăng nhập gần
nhất. Người thuộc nhiều phòng ban hiện nhiều thẻ nhỏ trong một ô, tràn thì thu lại thành *+2*. Cột
mức mật là thẻ có chữ, không chỉ có màu. Bấm một dòng mở `SCR-access-control-04`.

**Không có thao tác hàng loạt** trên màn này (gỡ 2026-09-23). Đổi phòng ban, đổi mức mật hay vô hiệu
hoá nhiều tài khoản cùng lúc không có tiêu chí chấp nhận nào chống lưng — US-002 `AC-007` chỉ nói
sửa một tài khoản, US-004 chỉ nói về phòng ban. Sửa từng người ở màn Chi tiết; hạng mục hàng loạt
nằm trong danh sách hoãn của roadmap.

**Trạng thái phải phủ:** rỗng lần đầu (chỉ có tài khoản quản trị ban đầu — gợi ý nhập hàng loạt là
đường nhanh hơn) · rỗng do bộ lọc · đang tải · lỗi · không đủ quyền. *Một phần thành công* không còn
áp dụng sau khi gỡ thao tác hàng loạt.

### SCR-access-control-04 — Chi tiết người dùng

Ngăn kéo trượt từ phải. Cùng ngăn kéo này dùng cho cả tạo mới và sửa, để hai luồng không trôi khác
nhau về mặt hình thức.

**Trường.** Họ tên · tên đăng nhập (không sửa được sau khi tạo, hiện ở dạng chỉ đọc kèm lý do khi
trỏ vào) · phòng ban (chọn nhiều, hiện dạng thẻ có nút gỡ từng cái) · mức mật · trạng thái hoạt
động. Khi tạo mới có thêm ô mật khẩu ban đầu, kèm nút sinh ngẫu nhiên.

**Khối thông tin bổ sung, chỉ có ở chế độ sửa:**

- *Người này đọc được gì* — số tài liệu trong phạm vi quyền, tách theo nguồn: bao nhiêu đến từ
  phòng ban, bao nhiêu gán trực tiếp, bao nhiêu đang bị chặn bởi mục từ chối, bao nhiêu bị chặn bởi
  nhãn mật. Con số cuối cùng thường là con số làm người quản trị vỡ lẽ vì sao ai đó không đọc được.
- *Thao tác* — đặt lại mật khẩu, vô hiệu hoá tài khoản.

**Cảnh báo tại chỗ.** Khi hạ mức mật của người dùng, hệ thống hiện ngay số tài liệu người đó sẽ mất
quyền đọc, **trước** khi lưu. Khi đổi phòng ban cũng vậy. Đây là hộp thoại xác nhận thao tác ảnh
hưởng rộng theo mục 5 của hệ thống thị giác.

**Trạng thái phải phủ:** mặc định · tạo mới · đang gửi · tên đăng nhập trùng · thiếu trường bắt
buộc · mật khẩu không đạt chính sách · tài khoản đã vô hiệu hoá (thân ở dạng chỉ đọc kèm nút kích
hoạt lại).

### SCR-access-control-05 — Nhập hàng loạt tài khoản

Ba bước trong cùng một hộp thoại, có chỉ báo bước ở trên đầu. Không bước nào ghi dữ liệu trừ bước ba.

**Bước 1 — chọn tệp.** Vùng kéo thả, kèm liên kết tải tệp mẫu có sẵn các cột đúng tên. Tệp mẫu là
thứ tiết kiệm nhiều thời gian hỗ trợ nhất ở màn này.

**Bước 2 — xem trước.** Bảng toàn bộ dòng trong tệp, dòng hợp lệ và dòng lỗi trộn lẫn theo đúng thứ
tự trong tệp gốc, mỗi dòng lỗi ghi **số thứ tự dòng trong tệp** và lý do ngay tại dòng. Trên bảng
là dải tổng kết: *45 dòng hợp lệ · 5 dòng lỗi*. Nút tải về danh sách dòng lỗi để sửa ngoài rồi nạp
lại. Phòng ban chưa tồn tại xử lý riêng: hiện một khối hỏi rõ *tạo mới phòng ban này* hay *để trống
phòng ban cho các dòng đó* — hệ thống **không** tự tạo phòng ban.

**Bước 3 — kết quả.** Số tài khoản đã tạo, và **danh sách cặp tên đăng nhập với mật khẩu ban đầu,
tải về được đúng một lần**. Màn này cảnh báo rõ đây là lần duy nhất xem được, và nhắc mọi tài khoản
đều phải đổi mật khẩu ở lần đăng nhập đầu.

**Trạng thái phải phủ:** mặc định · đang đọc tệp · sai định dạng hoặc thiếu cột (không tạo tài
khoản nào) · trùng tên đăng nhập ngay trong tệp (cả hai dòng đều bị loại, để người dùng tự quyết) ·
một phần thành công · đang gửi.

### SCR-access-control-06 — Quản lý phòng ban

**Bố cục hai cột.** Trái: danh sách phòng ban, mỗi dòng kèm số thành viên và số tài liệu đang được
gán quyền qua phòng ban đó. Phải: chi tiết phòng ban đang chọn — tên, danh sách thành viên, danh
sách tài liệu được cấp quyền.

Danh sách phẳng, **không có cây phòng ban**. Quyết định này ghi ở US-004 kèm lý do: kế thừa quyền
theo cây là nguồn lỗi phân quyền phổ biến nhất.

**Xoá phòng ban** dùng hộp thoại xác nhận thao tác không hoàn tác được: nêu đúng số thành viên và
số tài liệu bị ảnh hưởng, và nói rõ điều gì xảy ra sau đó — quyền gán qua phòng ban này biến mất,
thành viên chỉ còn quyền gán trực tiếp.

**Trạng thái phải phủ:** rỗng lần đầu · đang tải · lỗi · đang gửi · tên phòng ban trùng · phòng ban
không có thành viên nào (không phải lỗi, nhưng nói rõ để người quản trị biết gán quyền cho nó lúc
này không có tác dụng gì).
