# Flow: Vào hệ thống

> Màn hình thuộc flow này: `dang-nhap` → `doi-mat-khau-lan-dau`. Flow tổng xem
> `../srs/access-control-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.
>
> Hai màn này dùng **khối hẹp căn giữa** (~380-460 điểm ảnh), không trải hết bề ngang máy bàn.

---

## Screen: dang-nhap — Đăng nhập

### Wireframe (ASCII)

```text
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│          ┌──────────────────────────────────────────────┐          │
│          │ [1] (!) Phiên làm việc đã hết hạn.           │          │
│          │     Đăng nhập lại để tiếp tục.               │          │
│          └──────────────────────────────────────────────┘          │
│                                                                    │
│                    [2] Tra cứu tri thức nội bộ                     │
│              [3] Hỏi bằng tiếng Việt, nhận câu trả lời             │
│                  kèm trích dẫn từ tài liệu của bạn                 │
│                                                                    │
│          ┌──────────────────────────────────────────────┐          │
│          │ [4] Tên đăng nhập                            │          │
│          │ [__________________________________________] │          │
│          │                                              │          │
│          │ [5] Mật khẩu                                 │          │
│          │ [______________________________] [6](eye)    │          │
│          │                                              │          │
│          │ [7] (!) Tên đăng nhập hoặc mật khẩu không    │          │
│          │         đúng                                 │          │
│          │                                              │          │
│          │ [8][           Đăng nhập                   ] │          │
│          │                                              │          │
│          │ [9] Quên mật khẩu? Liên hệ quản trị tri thức │          │
│          └──────────────────────────────────────────────┘          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Dải thông báo phiên hết hạn | Label | ReadOnly | • Giải thích vì sao người dùng bị đưa về đây giữa chừng (`AC-006` của US-001).<br>• **Chỉ hiện khi tới từ phiên hết hạn**, không hiện khi vào trực tiếp.<br>• Hệ thống ghi nhớ trang đang mở; đăng nhập xong đưa thẳng về đúng trang đó.<br>• Phiên hết hạn sau **8 giờ không thao tác** (`NFR-system-027`). |
| 2 | Tên sản phẩm | Label | ReadOnly | • Xác nhận người dùng đang ở đúng hệ thống. |
| 3 | Dòng mô tả | Label | ReadOnly | • Một câu nói hệ thống này để làm gì — người mới được cấp tài khoản thường không biết. |
| 4 | Tên đăng nhập | Textbox | Text | • Xác thực danh tính, để hệ thống lọc quyền theo đúng người hỏi ở bước truy hồi.<br>• **Bắt buộc**; cùng với ô mật khẩu quyết định nút đăng nhập có bật không.<br>• **Không** áp chính sách độ dài hay ký tự ở đây — `NFR-system-026` chỉ áp lúc đặt hoặc đổi mật khẩu; ô này chỉ so khớp.<br>• States: mặc định · lỗi · đang gửi (khoá nhập).<br>• Khi sai thông tin: **giữ nguyên giá trị đã gõ** (`AC-002` của US-001). |
| 5 | Mật khẩu | Textbox | Text | • **Bắt buộc**.<br>• Khi sai thông tin: **xoá trắng ô này**, chỉ giữ tên đăng nhập.<br>• States: mặc định · lỗi · đang gửi.<br>• Enter trong ô này gửi biểu mẫu. |
| 6 | Hiện/ẩn mật khẩu | Button | Click | • Cho người dùng tự kiểm chuỗi vừa gõ, giảm số lần sai do gõ nhầm.<br>• States: đang ẩn · đang hiện.<br>• Tiếp cận: có nhãn cho trình đọc màn hình, không chỉ biểu tượng. |
| 7 | Thông báo lỗi | Label | ReadOnly | • Đặt **trên nút**, không phải dưới, để người dùng thấy trước khi bấm lại.<br>• Ba dạng loại trừ nhau, mỗi lúc chỉ một dạng hiện:<br>&nbsp;&nbsp;— *Sai thông tin*: **một thông báo duy nhất** "Tên đăng nhập hoặc mật khẩu không đúng" (`AC-002` của US-001). Tuyệt đối không phân biệt sai tên với sai mật khẩu; phân biệt được thì bất kỳ ai cũng dò ra danh sách nhân sự của doanh nghiệp.<br>&nbsp;&nbsp;— *Tài khoản bị vô hiệu hoá*: thông báo riêng, nói rõ tài khoản đã ngừng hoạt động và đề nghị liên hệ quản trị (`AC-003` của US-001). Đây là **ngoại lệ có chủ đích** của luật trên — người đã từng có tài khoản cần biết vì sao mình không vào được, và thông tin ấy không giúp gì cho người dò danh sách.<br>&nbsp;&nbsp;— *Bị khoá tạm*: sau 10 lần sai liên tiếp, khoá 15 phút (`NFR-system-028`).<br>• **Bảo mật — điểm dễ trượt nhất:** bộ đếm 10 lần đếm theo **nguồn gửi**, không theo tên đăng nhập (chốt 2026-09-09). Đếm theo tên đăng nhập thì tên có thật sẽ bị khoá còn tên bịa thì không bao giờ khoá, và chính khác biệt hành vi đó là kênh dò tài khoản dù chữ nghĩa hai bên giống hệt. Kéo theo: thông báo khoá phải trung lập, **không được ngụ ý "tài khoản của bạn bị khoá"**.<br>• Câu chữ cho hai thông báo *khoá tạm* và *vô hiệu hoá* chưa chốt — xem OQ-1. |
| 8 | Đăng nhập | Button | Click | • Trải hết bề ngang khối, là hành động chính duy nhất của màn.<br>• States: mặc định · **đang gửi** (vào trạng thái chờ, chặn gửi trùng) · vô hiệu hoá khi một trong hai ô còn trống.<br>• Điều hướng: lần đăng nhập đầu tiên thì sang màn đổi mật khẩu; ngược lại vào thẳng màn Hỏi đáp, hoặc về đúng trang đã ghi nhớ.<br>• Lỗi kết nối tới máy chủ là dạng lỗi thứ tư, khác ba dạng ở [7] — cho thử lại. |
| 9 | Chỉ đường quên mật khẩu | Label | ReadOnly | • Hệ thống **không tự phục vụ đặt lại mật khẩu**: chạy hoàn toàn nội bộ, chưa có kênh gửi thư (câu hỏi mở của US-002). Quản trị đặt lại giúp ở màn Chi tiết người dùng.<br>• Đây là **văn bản tĩnh**, không phải liên kết mở biểu mẫu — thiếu nó thì người quên mật khẩu rơi vào ngõ cụt mà không biết hỏi ai.<br>• Câu chữ chính xác chưa chốt — xem OQ-1. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Khối hẹp căn giữa, hai ô trống, nút vô hiệu hoá |
| Đang gửi | Nút vào trạng thái chờ, hai ô khoá nhập, chặn gửi trùng |
| Sai thông tin | Một thông báo duy nhất; giữ tên đăng nhập, xoá mật khẩu |
| Tài khoản vô hiệu hoá | Thông báo riêng, đề nghị liên hệ quản trị |
| Bị khoá tạm | Thông báo trung lập, không ngụ ý tài khoản có tồn tại |
| Phiên hết hạn | Dải thông báo [1] trên đầu khối, ghi nhớ trang định vào |
| Lỗi kết nối máy chủ | Nói rõ không gọi được máy chủ, cho thử lại |
| Đang tải · Rỗng · Chỉ đọc · Không đủ quyền | Không áp dụng — đây là biểu mẫu tĩnh trước khi có phiên, không có dữ liệu để tải và chưa có quyền nào để kiểm |

---

## Screen: doi-mat-khau-lan-dau — Đổi mật khẩu lần đầu

### Wireframe (ASCII)

```text
┌────────────────────────────────────────────────────────────────────┐
│                                                                    │
│                    [1] Tra cứu tri thức nội bộ                     │
│                                                                    │
│          ┌──────────────────────────────────────────────┐          │
│          │ [2] Đây là lần đăng nhập đầu tiên của bạn.   │          │
│          │     Hãy đặt mật khẩu riêng trước khi dùng.   │          │
│          │                                              │          │
│          │ [3] Mật khẩu hiện tại                        │          │
│          │ [__________________________________________] │          │
│          │                                              │          │
│          │ [4] Mật khẩu mới                             │          │
│          │ [__________________________________________] │          │
│          │   [x] Tối thiểu 12 ký tự                [5]  │          │
│          │   [ ] Không nằm trong danh sách phổ biến     │          │
│          │                                              │          │
│          │ [6] Nhập lại mật khẩu mới                    │          │
│          │ [__________________________________________] │          │
│          │ [7] (!) Hai lần nhập chưa khớp nhau          │          │
│          │                                              │          │
│          │ [8][        Đặt mật khẩu và tiếp tục       ] │          │
│          └──────────────────────────────────────────────┘          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tên sản phẩm | Label | ReadOnly | • Dùng đúng khối hẹp căn giữa như màn đăng nhập, để người dùng thấy đây là phần tiếp của cùng một luồng chứ không phải một màn lạ. |
| 2 | Dòng giải thích | Label | ReadOnly | • Nói rõ vì sao bị bắt đổi — người dùng vừa nhận mật khẩu từ quản trị, không tự hiểu được.<br>• **Màn này không bỏ qua được, không có đường vòng sang màn khác** và không có nút Huỷ dẫn ra ngoài. |
| 3 | Mật khẩu hiện tại | Textbox | Text | • Xác nhận chính chủ đang đổi, không phải người ngồi vào máy bỏ ngỏ.<br>• **Bắt buộc**.<br>• Lỗi: sai mật khẩu hiện tại — báo tại ô này, khác với lỗi chính sách ở [4]. |
| 4 | Mật khẩu mới | Textbox | Text | • **Bắt buộc**. Chính sách: **tối thiểu 12 ký tự**, chặn danh sách mật khẩu phổ biến, **không** bắt buộc trộn loại ký tự (`NFR-system-026`).<br>• Quyết định không bắt trộn ký tự là có chủ đích: quy tắc trộn đẩy người dùng tới những mật khẩu ngắn khó nhớ dễ đoán.<br>• States: mặc định · đang gõ (danh sách kiểm [5] cập nhật) · lỗi chính sách. |
| 5 | Danh sách kiểm yêu cầu | Checkbox | ReadOnly | • Người dùng thấy **mình còn thiếu điều kiện nào ngay trong lúc gõ** — không phải một dòng chữ nhỏ, và tuyệt đối không phải một thông báo lỗi hiện ra sau khi bấm gửi.<br>• Đây là ô đánh dấu **chỉ để hiển thị**, không bấm được.<br>• Điều kiện *không nằm trong danh sách phổ biến* có kiểm được ngay lúc gõ không, hay phải đợi rời ô — xem OQ-2. Câu trả lời đổi cách vẽ trạng thái đang gõ. |
| 6 | Nhập lại mật khẩu mới | Textbox | Text | • **Bắt buộc**, phải khớp [4].<br>• Lỗi: hai lần nhập không khớp — báo ngay tại đây, không đợi bấm gửi. |
| 7 | Thông báo lỗi | Label | ReadOnly | • Ba dạng loại trừ nhau: xác nhận không khớp · mật khẩu hiện tại sai · mật khẩu mới chưa đạt chính sách.<br>• Tiếp cận: lỗi gắn với đúng ô gây lỗi, không gom hết lên đầu biểu mẫu. |
| 8 | Đặt mật khẩu và tiếp tục | Button | Click | • Trải hết bề ngang khối.<br>• States: mặc định · vô hiệu hoá khi chưa đủ điều kiện · đang gửi.<br>• Điều hướng: thành công thì **vào thẳng màn Hỏi đáp, không bắt đăng nhập lại** (`AC-007` của US-002). |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Ba ô trống, danh sách kiểm chưa mục nào đánh dấu |
| Đang gõ | Danh sách kiểm tự cập nhật theo từng ký tự |
| Xác nhận không khớp | Báo tại ô nhập lại |
| Mật khẩu hiện tại sai | Báo tại ô mật khẩu hiện tại |
| Chưa đạt chính sách | Danh sách kiểm chỉ rõ điều kiện còn thiếu |
| Đang gửi | Nút vào trạng thái chờ, chặn gửi trùng |
| Thành công | Chuyển thẳng vào màn Hỏi đáp |
| Đang tải · Rỗng · Chỉ đọc · Không đủ quyền | Không áp dụng — biểu mẫu bắt buộc, không có dữ liệu tải trước và người dùng đã qua xác thực |

---

## Open Questions

- [ ] OQ-1: Câu chữ chính xác cho ba chỗ chưa chốt: thông báo khi bị khoá 15 phút (phải trung lập,
      không ngụ ý tài khoản có tồn tại) · thông báo tài khoản bị vô hiệu hoá · dòng chỉ đường quên
      mật khẩu.
- [ ] OQ-2: Điều kiện *không nằm trong danh sách mật khẩu phổ biến* kiểm được ngay trong lúc gõ, hay
      chỉ xác nhận được sau khi rời ô? Ảnh hưởng trực tiếp cách vẽ trạng thái đang gõ.
