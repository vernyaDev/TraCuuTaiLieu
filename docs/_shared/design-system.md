---
type: design-system
status: draft
updated: 2026-09-09
links:
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Hệ thống thị giác dùng chung

> Nguồn duy nhất cho màu, chữ, khoảng cách, thành phần dùng lại và luật phủ trạng thái. Mọi tệp
> screen index **tham chiếu** file này, không định nghĩa lại. Sửa ở đây là sửa cho toàn sản phẩm.

## 0. "Đẹp" ở đây nghĩa là gì

Không phải nhiều hiệu ứng. Là bốn điều đo được:

1. **Nhất quán** — cùng một loại nút, cùng một loại bảng, cùng một kiểu thông báo lỗi ở mọi màn.
   Người dùng học một lần, dùng được cả sản phẩm.
2. **Phủ đủ trạng thái** — không màn nào có trạng thái chưa được thiết kế. Màn rỗng, màn đang tải,
   màn lỗi đều là màn phải vẽ, không phải chỗ để trống.
3. **Đọc được** — tiếng Việt có dấu, chữ nhỏ và dòng sát nhau là không đọc nổi. Mục 2 là ràng buộc
   cứng chứ không phải gợi ý thẩm mỹ.
4. **Yên tĩnh** — hệ thống này người ta dùng giữa lúc đang làm việc khác. Màu mạnh dành cho cảnh
   báo thật; giao diện thường ngày phải lùi lại phía sau nội dung.

## 1. Ràng buộc đến từ mô hình triển khai

Sản phẩm chạy trong mạng nội bộ, không gọi ra ngoài. Hệ quả trực tiếp lên phần nhìn:

- **Phông chữ phải đóng gói kèm.** Không nạp từ dịch vụ phông bên ngoài — nơi cài không có kết nối
  thì chữ rơi về phông mặc định và giao diện vỡ. Phông chọn phải có **bộ ký tự tiếng Việt đầy đủ**;
  nhiều phông trình bày đẹp nhưng thiếu ký tự như `ế ộ ỹ ữ ặ`, và lỗi này chỉ lộ ra khi gõ đúng từ
  có dấu đó.
- **Biểu tượng đóng gói kèm**, không nạp từ ngoài.
- **Không có công cụ đo hành vi người dùng bên thứ ba.** Mọi số liệu dùng được đều phải sinh trong
  hệ thống.

## 2. Chữ

| Vai trò | Cỡ | Dùng ở |
|---|---|---|
| Tiêu đề màn | lớn nhất, đậm vừa | Một dòng đầu mỗi màn |
| Tiêu đề khối | vừa, đậm | Đầu mỗi khối nội dung |
| Nội dung | cỡ nền | Câu trả lời, mô tả, ô nhập |
| Phụ trợ | nhỏ hơn nền một bậc | Nhãn trường, ghi chú, dấu thời gian |
| Trích dẫn nguồn | cỡ nền, phân biệt bằng nền và viền chứ không bằng cỡ chữ | Khối nguồn ở màn hỏi đáp |

**Ràng buộc cứng cho tiếng Việt:**

- Chiều cao dòng của nội dung **không dưới 1,6 lần cỡ chữ**. Dấu thanh và dấu mũ chồng lên nhau
  (`ế`, `ộ`, `ữ`) chiếm chiều cao mà tiếng Anh không cần; dòng sát nhau làm dấu của dòng dưới đụng
  chân chữ dòng trên.
- Cỡ chữ nội dung **không nhỏ hơn cỡ nền của trình duyệt**. Chữ nhỏ mất dấu trước khi mất chữ.
- **Không viết hoa toàn bộ** cho nhãn hay nút. Chữ hoa tiếng Việt mang dấu nhìn nặng và khó đọc.
- Không dùng phông chữ nét quá mảnh cho nội dung — nét mảnh làm dấu biến mất trên màn hình thường.
- Dòng văn bản dài **không quá khoảng 80 ký tự**; câu trả lời trải hết bề ngang màn hình lớn là
  không đọc được.

## 3. Màu — định nghĩa theo vai trò, không theo tên màu

| Vai trò | Dùng cho | Ràng buộc |
|---|---|---|
| Nền | Nền trang và nền khối | Hai bậc nền phân biệt được khối với trang |
| Chữ chính | Nội dung | Tương phản với nền tối thiểu 4,5:1 |
| Chữ phụ | Nhãn, ghi chú | Tối thiểu 4,5:1 — không dùng xám nhạt cho chữ nhỏ |
| Nhấn | Nút chính, liên kết, phần tử đang chọn | Một màu nhấn duy nhất toàn sản phẩm |
| Thành công | Nạp xong, lưu xong | Ít dùng; thành công thường chỉ cần biến mất trạng thái chờ |
| Cảnh báo | Văn bản hết hiệu lực, nghi ngờ chồng lấn, căn cứ mỏng | Phải phân biệt được với lỗi |
| Lỗi | Thao tác thất bại, dữ liệu không hợp lệ | Chỉ dùng cho việc thật sự hỏng |
| Nhãn mật | Ba mức công khai / nội bộ / mật | Xem ràng buộc dưới |

**Ba ràng buộc không được vi phạm:**

1. **Màu không bao giờ là kênh thông tin duy nhất.** Nhãn mật, trạng thái tài liệu, mức cảnh báo
   đều phải có chữ hoặc biểu tượng đi kèm — người mù màu chiếm khoảng một phần hai mươi nam giới,
   và ở đây đọc nhầm nhãn mật là sự cố bảo mật chứ không phải bất tiện.
2. **Cảnh báo khác lỗi.** "Văn bản này đã hết hiệu lực" không phải lỗi của người dùng; tô nó màu
   lỗi thì người dùng tưởng mình làm sai. Đây là chỗ hay bị gộp nhất.
3. **Nhãn mật không tô nền toàn dòng.** Một danh sách 1.000 tài liệu mà mỗi dòng một mảng màu là
   không đọc được; nhãn mật đi ở một cột riêng dạng thẻ nhỏ.

## 4. Khoảng cách và bố cục

- Một thang khoảng cách duy nhất, bội của một đơn vị nền. Không có khoảng cách "gõ tay cho vừa mắt".
- **Bề rộng nội dung có trần.** Trên màn hình lớn, nội dung dừng ở bề rộng đọc được và căn giữa;
  chỉ bảng dữ liệu mới được trải rộng.
- Khối nội dung tách nhau bằng khoảng trắng và một bậc nền, **không bằng đường kẻ chồng chất**.
- Ba vùng cố định ở mọi màn quản trị: thanh điều hướng, tiêu đề màn kèm thao tác chính, vùng nội
  dung. Người dùng luôn biết mình đang ở đâu.

## 5. Thành phần dùng lại

Danh mục thành phần; đặc tả chi tiết từng trường và từng nút dùng chung sống ở
`docs/_shared/spec-components.md` khi bước đặc tả màn hình sinh ra chúng.

| Nhóm | Thành phần |
|---|---|
| Nhập liệu | Ô nhập một dòng · ô nhập nhiều dòng · ô chọn một · ô chọn nhiều có thẻ · ô tìm kiếm · ô chọn ngày · ô tải tệp kéo thả |
| Hiển thị | Bảng có phân trang và sắp xếp · thẻ · thẻ nhãn nhỏ · huy hiệu trạng thái · thanh tiến độ · khối trích dẫn · khối cảnh báo |
| Điều hướng | Thanh bên · đường dẫn phân cấp · tab · phân trang |
| Phản hồi | Thông báo nổi · hộp thoại xác nhận · hộp thoại xác nhận thao tác không hoàn tác được · khung xương lúc tải |
| Bố cục | Ngăn kéo bên phải cho chi tiết · hộp thoại giữa màn · vùng trống có hướng dẫn |

**Hộp thoại xác nhận thao tác không hoàn tác được** là thành phần riêng, không dùng chung với hộp
thoại xác nhận thường: nó buộc người dùng đọc hậu quả cụ thể (số thành viên, số tài liệu bị ảnh
hưởng) trước khi nút xác nhận bật lên. Dùng ở xoá vĩnh viễn tài liệu, xoá phòng ban còn ràng buộc,
nâng nhãn mật gây mất quyền hàng loạt, và phục hồi đè dữ liệu.

## 6. Luật phủ trạng thái — bắt buộc, không phải khuyến nghị

Mỗi màn và mỗi khối dữ liệu phải khai đủ các trạng thái dưới. Trạng thái nào không áp dụng thì ghi
rõ **"không áp dụng vì …"** trong screen index — bỏ trắng là thiếu, không phải là không cần.

| Trạng thái | Yêu cầu |
|---|---|
| Mặc định | Trạng thái thường ngày |
| Đang tải | Khung xương giữ đúng hình dạng nội dung sắp tới, không phải một vòng xoay giữa màn trống |
| Rỗng lần đầu | Giải thích đây là gì và chỉ đường tới thao tác đầu tiên |
| Rỗng do bộ lọc | Nói rõ là do bộ lọc và cho xoá lọc ngay tại chỗ — phân biệt với rỗng lần đầu |
| Lỗi tải | Nói cái gì hỏng và cho thử lại tại chỗ |
| Không đủ quyền | Nói bằng lời nghiệp vụ, không tiết lộ nội dung hay sự tồn tại của thứ bị chặn |
| Đang gửi | Nút vào trạng thái chờ, chặn gửi trùng |
| Chỉ đọc | Phân biệt được với vô hiệu hoá |
| Vô hiệu hoá | Luôn kèm lý do khi trỏ vào — nút xám không giải thích là lỗi thiết kế |
| Một phần thành công | Nạp 20 tệp, 3 tệp hỏng: phải có cách hiển thị riêng, không phải chỉ thành công hoặc chỉ thất bại |

Trạng thái cuối là trạng thái hay bị quên nhất và xuất hiện ở đúng những chỗ quan trọng: nạp nhiều
tệp, nhập hàng loạt tài khoản, gán quyền hàng loạt, chạy bộ đo nhiều tổ hợp.

## 7. Lời lẽ trong giao diện

- Viết bằng ngôn ngữ nghiệp vụ, xưng hô trung tính. Không hiện dấu vết kỹ thuật của chương trình
  cho người dùng cuối.
- Mỗi thông báo lỗi trả lời được ba câu: **chuyện gì xảy ra · vì sao · giờ làm gì**. Thiếu vế thứ
  ba là thông báo chưa xong.
- Không đổ lỗi cho người dùng. "Tệp không đọc được, có thể là bản scan" chứ không phải "Bạn đã tải
  lên tệp sai".
- Thuật ngữ giữ nguyên tiếng Anh khi đó là cách gọi chuẩn trong tài liệu nguồn; còn lại tiếng Việt.
- Nhóm người dùng gọi đúng tên đã chốt: **nhân viên · quản trị tri thức · quản trị hệ thống**.

## 8. Màn hình và thiết bị

- Đích chính là **màn hình laptop và màn hình lớn**. Sản phẩm dùng tại bàn làm việc.
- Phải dùng được trên bề ngang hẹp mà không vỡ bố cục; **không** cam kết trải nghiệm tối ưu trên
  điện thoại ở phiên bản này.
- Bảng dữ liệu rộng cuộn ngang trong khung của nó, **không** làm cả trang cuộn ngang.

## 9. Tiếp cận được

- Mọi thao tác làm được bằng bàn phím; thứ tự chuyển tiêu điểm theo thứ tự đọc.
- Tiêu điểm luôn nhìn thấy được — không xoá viền tiêu điểm để "cho sạch".
- Ô nhập luôn có nhãn thật, không chỉ có chữ mờ gợi ý. Chữ mờ biến mất khi gõ, nhãn thì không.
- Hộp thoại giữ tiêu điểm bên trong và trả tiêu điểm về đúng chỗ cũ khi đóng.
- Tương phản chữ tối thiểu 4,5:1 với nền, kể cả chữ phụ và chữ trong huy hiệu trạng thái.

## 10. Điều KHÔNG làm

- Không hiệu ứng chuyển động kéo dài. Chuyển động chỉ để giải thích cái gì vừa đổi chỗ.
- Không hộp thoại chồng hộp thoại.
- Không thanh cuộn vô tận cho dữ liệu quản trị — quản trị cần biết tổng số và nhảy tới trang.
- Không chế độ tối ở phiên bản này. Một chế độ làm cho tốt hơn hai chế độ làm nửa vời.
- Không tự động làm mới nội dung dưới tay người đang thao tác.
