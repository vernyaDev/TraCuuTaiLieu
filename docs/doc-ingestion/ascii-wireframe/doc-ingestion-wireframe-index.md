---
type: screen-index
feature: doc-ingestion
status: draft
updated: 2026-09-23
links:
  - docs/doc-ingestion/usecases/doc-ingestion-usecase-index.md
  - docs/doc-ingestion/userstories/doc-ingestion-story-index.md
  - docs/_shared/design-system.md
---

# Nạp và quản lý vòng đời tài liệu — Danh sách màn hình

> Hệ thống thị giác và luật phủ trạng thái ở `docs/_shared/design-system.md`.

## Screens

| Mã màn | Slug | Tên màn | Thuộc flow | Wireframe | Use case | Story | Trạng thái |
|---|---|---|---|---|---|---|---|
| `SCR-doc-ingestion-01` | `danh-sach-tai-lieu` | Danh sách tài liệu | `quan-ly-kho-tai-lieu` | [quan-ly-kho-tai-lieu.md](quan-ly-kho-tai-lieu.md) | `UC-ingest-document` | US-016, US-009, US-010, US-015, US-005 | draft |
| `SCR-doc-ingestion-02` | `tai-tai-lieu-len` | Tải tài liệu lên | `nap-tai-lieu` | [nap-tai-lieu.md](nap-tai-lieu.md) | `UC-ingest-document` | US-009, US-010, US-007 | draft |
| `SCR-doc-ingestion-03` | `xac-nhan-metadata` | Xác nhận metadata vòng đời | `nap-tai-lieu` | [nap-tai-lieu.md](nap-tai-lieu.md) | `UC-ingest-document` | US-013 | draft |
| `SCR-doc-ingestion-04` | `chi-tiet-tai-lieu` | Chi tiết tài liệu | `quan-ly-kho-tai-lieu` | [quan-ly-kho-tai-lieu.md](quan-ly-kho-tai-lieu.md) | `UC-ingest-document`, `UC-grant-document-access` | US-015, US-005, US-007, US-013 + quyền từ chối (`docs/access-control/userstories/us-006.md`) | draft |
| `SCR-doc-ingestion-05` | `xu-ly-chong-lap` | Xử lý cảnh báo chồng lấn | `xu-ly-chong-lap` | [xu-ly-chong-lap.md](xu-ly-chong-lap.md) | `UC-ingest-document` | US-014 | draft |
| `SCR-doc-ingestion-06` | `xem-truoc-doan` | Xem trước các đoạn đã tách | `quan-ly-kho-tai-lieu` | [quan-ly-kho-tai-lieu.md](quan-ly-kho-tai-lieu.md) | `UC-ingest-document` | US-011, US-012 | draft |

> Cột `Slug` là định danh máy đọc, khớp `## Screen: {slug}` trong tệp wireframe và Mục 2 của
> `../srs/doc-ingestion-userflow.md`. Sáu màn trước đây gộp chung một luồng *Quản trị tri thức*;
> userflow đã chia ba luồng và bảng này cập nhật theo (2026-09-23). `SCR-doc-ingestion-01` và
> `SCR-doc-ingestion-04` **dùng chung cho luồng nạp** — màn danh sách là điểm khởi phát, màn chi
> tiết là nơi cổng chặn thứ ba (gán quyền) diễn ra.

## Descriptions

### SCR-doc-ingestion-01 — Danh sách tài liệu

Màn chính của quản trị tri thức, và là màn phải dùng được ở quy mô 1.000 tài liệu chứ không chỉ ở
quy mô demo.

**Bố cục.** Thanh bên trái điều hướng giữa các khu quản trị. Vùng chính: tiêu đề màn kèm nút tải
tài liệu lên ở góc phải, dải việc-đang-chờ ngay dưới tiêu đề, thanh lọc, rồi bảng.

**Dải việc đang chờ.** **Bốn** thẻ nằm ngang, mỗi thẻ một con số lớn và một nhãn: *chờ xác nhận
metadata*, *cảnh báo chồng lấn chưa xử lý*, *nạp lỗi*, và *sắp hết hiệu lực trong 30 ngày* (thẻ thứ
tư thêm 2026-09-23 để phục vụ `AC-005` của US-016 — tiêu chí đã duyệt nhưng trước đó không màn nào
có chỗ hiển thị). Thẻ sắp hết hiệu lực khi lọc thì sắp theo ngày gần nhất trước, kèm số ngày còn
lại. Bấm vào thẻ là lọc bảng theo đúng nhóm đó.
Đây là thứ trả lời câu hỏi "hôm nay tôi phải làm gì" mà không bắt người dùng tự dò trong 1.000 dòng.
Thẻ nào bằng không thì hiện nhạt, không biến mất — biến mất thì người dùng không biết nó tồn tại.

**Thanh lọc.** Ô tìm theo tên tài liệu, ô chọn phân loại, ô chọn trạng thái xử lý, ô chọn trạng
thái hiệu lực. Bộ lọc đang bật hiện thành các thẻ nhỏ bấm gỡ được, kèm nút xoá hết lọc.

**Bảng.** Các cột: tên tài liệu · phân loại · nhãn mật · trạng thái · hiệu lực · số người đọc được
· ngày nạp. Sắp mặc định theo ngày nạp mới nhất trước. Phân trang có tổng số và nhảy trang được,
không cuộn vô tận. Bấm một dòng mở `SCR-doc-ingestion-04`.

**Chọn nhiều dòng thì hiện đúng MỘT thao tác hàng loạt: gán quyền.** `AC-005` của US-005 đòi tường
minh — chọn 20 tài liệu vừa nạp, cấp quyền cho một phòng ban trong một thao tác, và hệ thống báo số
tài liệu đã áp dụng.

*Gỡ hàng loạt* và *đổi phân loại hàng loạt* **không có tiêu chí nào chống lưng** — đã gỡ khỏi bản vẽ
ngày 2026-09-23 và ghi vào danh sách đã-gỡ-khỏi-phạm-vi của roadmap. Chúng là thao tác dễ sai diện
rộng mà không yêu cầu nào đòi.

> **Đính chính 2026-09-24.** Ngày 23/09 gỡ nhầm cả *gán quyền hàng loạt* vì một đợt rà soát báo là
> không có tiêu chí chống lưng. Rà lại danh sách tiêu chí của US-005 thì `AC-005` có thật và nói
> thẳng về thao tác này. Khôi phục.

Cột *hiệu lực* phải phân biệt được **bốn** trạng thái bằng chữ chứ không chỉ bằng màu: còn hiệu lực ·
**sắp hết hiệu lực** · đã hết hiệu lực · chưa xác định. Cột *nhãn mật* là thẻ nhỏ ở một cột riêng,
không tô nền cả dòng.

**Dấu suy giảm chất lượng** trên dòng tài liệu (thêm 2026-09-23): tài liệu nạp thành công nhưng bảng
không bóc được cấu trúc, hoặc tài liệu không có cấu trúc rõ nên phải tách dự phòng. Đây là nhóm dễ
bỏ sót nhất — tài liệu vẫn *sẵn sàng*, không lỗi, không nằm trong thẻ việc nào; trước đó chỉ thấy
được khi mở đúng tài liệu ấy ra xem trước đoạn, tức phải mở mù từng tài liệu trong kho 1.000 tài
liệu. Bấm vào dấu mở thẳng màn xem trước đoạn.

**Trạng thái phải phủ:**

| Trạng thái | Thể hiện |
|---|---|
| Rỗng lần đầu | Giải thích kho đang trống và nút tải tài liệu lên làm hành động chính giữa vùng trống |
| Rỗng do bộ lọc | Nói rõ do bộ lọc, kèm nút xoá lọc ngay tại chỗ |
| Đang tải | Khung xương giữ đúng hình dạng bảng |
| Lỗi tải | Thử lại tại chỗ, không mất bộ lọc đang đặt |
| Đang xử lý | Dòng tài liệu đang nạp hiện thanh tiến độ nhỏ ngay trong ô trạng thái, tự cập nhật |
| Một phần thành công | Sau đợt nạp nhiều tệp: dải tổng kết tạm thời nêu số thành công và số lỗi, bấm được vào phần lỗi |

### SCR-doc-ingestion-02 — Tải tài liệu lên

Hộp thoại giữa màn, không phải trang riêng — người dùng không rời khỏi ngữ cảnh danh sách.

**Thân.** Vùng kéo thả chiếm phần trên, kèm nút chọn tệp cho người không kéo thả. Dưới vùng kéo thả
là danh sách tệp đã chọn, mỗi tệp một dòng có tên, dung lượng và nút gỡ. Tệp vượt 50MB hoặc sai
định dạng bị đánh dấu đỏ ngay tại dòng kèm lý do, **trước khi** tải lên, và không tính vào số tệp
sẽ nạp.

**Trường bắt buộc chung cho cả đợt:** nhãn mật và phân loại. Nhãn mật **không có giá trị mặc định**
— nút xác nhận vô hiệu hoá tới khi người dùng chọn, và khi trỏ vào nút vô hiệu hoá thì hiện lý do.
Đây là một trong ba cổng chặn của luồng nạp, và nó phải chặn ở giao diện chứ không chỉ ở máy chủ.

**Sau khi xác nhận.** Hộp thoại đóng, danh sách phía sau hiện các dòng mới ở trạng thái đang xử lý
với tiến độ theo bước: bóc văn bản, tách đoạn, lập chỉ mục, kèm số đoạn đã xử lý trên tổng. Đóng
trình duyệt không dừng việc xử lý.

**Trạng thái phải phủ:** mặc định · đang gửi (nút thành trạng thái chờ, chặn gửi trùng) · lỗi từng
tệp · trùng với tài liệu đã có (hỏi bỏ qua hay coi là bản thay thế) · một phần thành công.

### SCR-doc-ingestion-03 — Xác nhận metadata vòng đời

Cổng chặn thứ hai của luồng nạp. Tài liệu không qua màn này thì không vào phạm vi truy hồi.

**Bố cục hai cột.** Cột trái là nội dung tài liệu vừa bóc được, cuộn được. Cột phải là biểu mẫu
metadata. Hai cột cạnh nhau có chủ đích: người xác nhận phải đối chiếu được với văn bản thật chứ
không xác nhận mù.

**Biểu mẫu.** Bốn trường: ngày hiệu lực · ngày hết hiệu lực · thay thế tài liệu nào · phân loại.
Mỗi trường hệ thống điền sẵn thì kèm ngay dưới một dòng chữ phụ *trích từ tài liệu:* và đoạn nguyên
văn làm căn cứ; bấm vào đoạn đó thì cột trái cuộn tới và làm nổi bật chỗ ấy. Trường nào không tìm
thấy căn cứ thì để trống kèm ghi chú *không tìm thấy trong tài liệu* — hệ thống **không** đoán.

Trường ngày hết hiệu lực có thêm lựa chọn *không có thời hạn*, phân biệt rõ với *chưa xác định*.
Hai thứ này khác nhau và gộp lại là mất thông tin.

Trường thay thế tài liệu là ô tìm kiếm trong kho; nếu hệ thống đã đề xuất được tài liệu cũ thì nó
điền sẵn kèm căn cứ như các trường khác.

**Trạng thái phải phủ:** mặc định · đã sửa (đánh dấu trường nào người sửa, khác trường máy điền) ·
lỗi hợp lệ (ngày hết hiệu lực sớm hơn ngày hiệu lực; quan hệ thay thế tạo vòng lặp) · đang gửi ·
**chỉnh sửa** (mở lại từ nút *Sửa metadata* ở `SCR-doc-ingestion-04` khi người xem **có** quyền sửa —
sửa xong có hiệu lực ngay ở câu hỏi kế tiếp theo `AC-009` của US-013) · chỉ đọc (khi người xem
**không** có quyền sửa).

> **Ba chế độ, không phải hai** (sửa 2026-09-23). Bản trước khai mở lại từ `SCR-doc-ingestion-04` là
> chỉ đọc — nghĩa là nút *Sửa metadata* hứa một hành vi mà màn không cho làm, đúng lúc `AC-009` của
> US-013 bắt buộc quản trị sửa được ngày hiệu lực sai mà không phải nạp lại tài liệu.

### SCR-doc-ingestion-04 — Chi tiết tài liệu

Ngăn kéo trượt từ phải khi bấm một dòng ở danh sách, mở rộng thành trang riêng được khi cần chỗ.

**Đầu ngăn kéo.** Tên tài liệu, thẻ nhãn mật, thẻ trạng thái, thẻ hiệu lực, và cụm **năm** nút: xem
trước các đoạn, sửa metadata, **đổi nhãn mật**, gỡ, xoá vĩnh viễn.

Nút *đổi nhãn mật* thêm 2026-09-23 để phục vụ `AC-005` của US-007 — tiêu chí đã duyệt nhưng trước đó
không màn nào có chỗ thực hiện. Nâng nhãn mật là thao tác ảnh hưởng rộng: hộp thoại xác nhận nêu số
người sẽ mất quyền đọc trước khi bật nút.

**Bốn khối nội dung, xếp dọc:**

1. *Thông tin chung* — phân loại, ngày nạp, kích thước, số đoạn, người nạp.
2. *Vòng đời* — bốn trường metadata ở dạng đọc, cộng **chuỗi thay thế** vẽ theo chiều dọc: bản
   trước ← bản này ← bản sau, bản đang hiệu lực đánh dấu rõ. Chuỗi nhiều đời hiển thị đủ.
3. *Quyền truy cập* — hai danh sách tách bạch: *được phép* và *bị từ chối*. Mỗi mục ghi rõ chủ thể
   là người dùng hay phòng ban. Quyền đến từ phòng ban hiển thị khác quyền gán trực tiếp bằng một
   thẻ nhỏ, để người quản trị biết gỡ ở đâu. Nút thêm quyền và nút thêm mục từ chối tách riêng —
   không gộp thành một nút rồi chọn kiểu trong hộp thoại, vì từ chối là thao tác nặng hơn hẳn.
   Dưới cùng khối này là **ô tra cứu**: chọn một người dùng, hệ thống trả lời họ đọc được hay không
   và vì sao (được phép qua đường nào, bị mục từ chối nào chặn, hay bị nhãn mật chặn).
4. *Cảnh báo* — chỉ hiện khi tài liệu đang mang nhãn nghi ngờ chồng lấn, dẫn sang `SCR-doc-ingestion-05`.

**Thao tác không hoàn tác được.** Xoá vĩnh viễn và nâng nhãn mật dùng hộp thoại xác nhận riêng theo
mục 5 của hệ thống thị giác: nêu con số cụ thể (số người sẽ mất quyền đọc; số trích dẫn đang trỏ
tới tài liệu) rồi mới bật nút xác nhận.

**Trạng thái phải phủ:** mặc định · đang tải · lỗi · tài liệu đã gỡ (thân hiện ở dạng chỉ đọc kèm
dải thông báo và nút khôi phục) · tài liệu đang xử lý (khối quyền vô hiệu hoá kèm lý do).

### SCR-doc-ingestion-05 — Xử lý cảnh báo chồng lấn

Danh sách cảnh báo chờ xử lý, vào từ thẻ ở dải việc-đang-chờ hoặc từ chi tiết tài liệu.

**Bố cục.** Mỗi cảnh báo là một thẻ ngang chứa **hai tài liệu đặt cạnh nhau**: bên trái tài liệu
mới, bên phải tài liệu bị nghi chồng lấn, giữa là mức độ giống nhau. Mỗi bên hiện tên, phân loại,
ngày hiệu lực và một đoạn nội dung đại diện — đủ để quyết mà không phải mở hai tài liệu ra đọc.

**Ba nút, không có nút mặc định.** *Xác nhận đây là bản thay thế* (chuyển sang luồng thay thế, ghi
chuỗi) · *Hai văn bản cùng tồn tại hợp lệ* (từ đó câu trả lời nào dùng cả hai sẽ kèm cảnh báo) ·
*Bỏ qua cảnh báo* (báo động giả, không hiện lại cho đúng cặp này). Hệ thống không chọn giúp; ba
đường dẫn tới ba hành vi khác nhau ở màn hỏi đáp.

Một tài liệu bị nghi chồng lấn với nhiều tài liệu thì hiện nhiều thẻ, xếp theo mức độ giống nhau
giảm dần, xử lý được từng cặp riêng.

**Trạng thái phải phủ:** rỗng (không có cảnh báo nào — nói rõ đây là trạng thái tốt, không phải
lỗi) · đang tải · đang gửi · đã xử lý (chuyển sang danh sách đã xử lý, xem lại được).

### SCR-doc-ingestion-06 — Xem trước các đoạn đã tách

Màn kiểm chất lượng tách đoạn. Không phải màn dùng hằng ngày, nhưng là màn quyết định niềm tin của
người quản trị vào kho — và là công cụ để tác giả đối chiếu khi so sánh các chiến lược tách.

**Bố cục hai cột.** Trái: tài liệu gốc. Phải: danh sách đoạn đã tách, mỗi đoạn một thẻ có số thứ
tự, đường dẫn phân cấp, và nội dung. Chọn một đoạn bên phải thì bên trái làm nổi bật đúng phần
tương ứng, và ngược lại.

**Đoạn là bảng** hiển thị đúng dạng bảng, kèm dấu hiệu nếu đây là một mảnh của bảng dài (*mảnh 2/4,
đã lặp dòng tiêu đề*). Đây là chỗ người quản trị phát hiện bảng bị cắt hỏng trước khi người dùng
nhận câu trả lời sai.

**Dải thông tin trên đầu:** chiến lược tách đang dùng, tổng số đoạn, số đoạn chứa bảng, và cảnh báo
nếu tài liệu rơi về cách tách dự phòng vì không có cấu trúc rõ.

**Trạng thái phải phủ:** mặc định · đang tải · lỗi · cảnh báo suy giảm chất lượng (bảng không bóc
được cấu trúc; tài liệu không có cấu trúc rõ) — hiện thành dải cảnh báo trên đầu, không phải lỗi.
