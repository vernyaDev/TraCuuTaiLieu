# Flow: Quản lý người dùng

> Màn hình thuộc flow này: `danh-sach-nguoi-dung` → `chi-tiet-nguoi-dung`. Flow tổng xem
> `../srs/access-control-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.
>
> **Thao tác hàng loạt đã được gỡ khỏi bản vẽ** (chốt 2026-09-23): không tiêu chí chấp nhận nào
> chống lưng cho việc đổi phòng ban, đổi mức mật hay vô hiệu hoá nhiều tài khoản cùng lúc. Sửa từng
> người ở màn Chi tiết. Hạng mục này nằm trong danh sách hoãn của roadmap.

---

## Screen: danh-sach-nguoi-dung — Danh sách người dùng

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ KHU QUẢN TRỊ │ Danh sách người dùng                                │
│              │       [1][ + Thêm người dùng ] [2][ Nhập hàng loạt ]│
│ Tài liệu     ├─────────────────────────────────────────────────────┤
│ > Người dùng │ [3][_________________]  [4][v: Phòng ban ]          │
│ Phòng ban    │ [5][v: Mức mật ]        [6][v: Trạng thái ]         │
│ Khoảng trống │ [7] Đang lọc: (Nhân sự x) (Đang hoạt động x)  Xoá   │
│              ├─────────────────────────────────────────────────────┤
│ ──────────── │ [8] Họ tên     Tên ĐN    Phòng ban  Mật  TT    Lần  │
│ < Về Hỏi đáp │ ─────────────────────────────────────────────────── │
│              │ Nguyễn Văn A   an.nv     Nhân sự+2  Nội  Hoạt  22/09│
│              │ Trần Thị B     binh.tt   Kế toán    Mật  Hoạt  21/09│
│              │ Lê Văn C       cuong.lv  Kỹ thuật   Công Tắt   02/08│
│              │ Phạm Thị D     dung.pt   (chưa có)  Công Hoạt  --   │
│              │                                                     │
│              │ [9] 1-20 trên 47      < Trước   1  2  3   Sau >     │
└──────────────┴─────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Thêm người dùng | Button | Click | • Tạo tài khoản đơn lẻ.<br>• Điều hướng: mở ngăn kéo Chi tiết người dùng ở chế độ tạo mới — **cùng một ngăn kéo với chế độ sửa**, để hai luồng không trôi khác nhau về hình thức.<br>• States: mặc định · đang mở ngăn kéo. |
| 2 | Nhập hàng loạt | Button | Click | • Đường nhanh khi cần tạo nhiều tài khoản từ một tệp.<br>• Điều hướng: mở hộp thoại ba bước của luồng `nhap-hang-loat`. |
| 3 | Ô tìm | Textbox | Text | • Tìm theo họ tên hoặc tên đăng nhập.<br>• Không bắt buộc; gõ tới đâu lọc tới đó, không cần bấm nút.<br>• Bộ lọc đang bật hiện thành thẻ ở [7]. |
| 4 | Lọc theo phòng ban | Dropdown | Select | • Thu hẹp danh sách theo phòng ban.<br>• Một người có thể thuộc nhiều phòng ban, nên lọc một hay nhiều phòng ban cùng lúc là câu hỏi chưa chốt — xem OQ-1. |
| 5 | Lọc theo mức mật | Dropdown | Select | • Ba giá trị: công khai · nội bộ · mật.<br>• Mức mật của người dùng là **một trong hai điều kiện** quyết định họ đọc được tài liệu nào; điều kiện còn lại là danh sách cấp quyền trên từng tài liệu (`AC-003` của US-007). |
| 6 | Lọc theo trạng thái | Dropdown | Select | • Đang hoạt động · đã vô hiệu hoá. |
| 7 | Thẻ lọc đang bật | Button | Click | • Cho thấy **vì sao danh sách đang ngắn** — thiếu nó thì trạng thái rỗng do lọc bị hiểu nhầm là kho trống.<br>• Mỗi thẻ bấm gỡ được; kèm nút xoá hết lọc.<br>• States: ẩn khi không có bộ lọc nào bật. |
| 8 | Bảng người dùng | Table | Click | • Sáu cột: họ tên · tên đăng nhập · phòng ban · mức mật · trạng thái · lần đăng nhập gần nhất.<br>• Người thuộc nhiều phòng ban hiện nhiều thẻ nhỏ trong một ô, tràn thì thu lại thành *+2*.<br>• Cột mức mật là **thẻ có chữ, không chỉ có màu** — màu không được là kênh thông tin duy nhất; tương phản đạt `NFR-system-036`.<br>• Điều hướng: bấm một dòng mở ngăn kéo Chi tiết.<br>• Người chưa thuộc phòng ban nào hiện *(chưa có)* thay vì để trống — trống dễ bị đọc nhầm là lỗi hiển thị.<br>• Tiếp cận: bảng rộng cuộn ngang **trong khung của nó**, không làm cả trang cuộn ngang. |
| 9 | Phân trang | Button | Click | • Có tổng số và nhảy trang được, **không cuộn vô tận** — quản trị cần biết kho lớn cỡ nào.<br>• Đổi trang giữ nguyên mọi bộ lọc đang đặt. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Bảng đầy dữ liệu, thanh lọc trống |
| Rỗng lần đầu | Chỉ có tài khoản quản trị ban đầu. Gợi ý **nhập hàng loạt là đường nhanh hơn** thay vì tạo từng người |
| Rỗng do bộ lọc | Nói rõ do bộ lọc nào, kèm nút xoá lọc ngay tại chỗ — khác hẳn rỗng lần đầu |
| Đang tải | Khung xương giữ đúng hình dạng bảng; đích ≤2 giây theo `NFR-system-007` |
| Lỗi tải | Nói cái gì hỏng, cho thử lại tại chỗ, **không mất bộ lọc đang đặt** |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn khu quản trị: thông báo không đủ quyền, không hiện dữ liệu nào, kèm liên kết về màn Hỏi đáp. Ẩn mục trong thanh bên là chưa đủ — máy chủ phải chặn |
| Đang gửi | Không áp dụng ở cấp màn — màn này chỉ đọc, mọi thao tác ghi diễn ra trong ngăn kéo Chi tiết |
| Chỉ đọc · Vô hiệu hoá | Không áp dụng — đây là danh sách, không phải một bản ghi có thể khoá |
| Một phần thành công | Không áp dụng — thao tác hàng loạt đã gỡ khỏi bản vẽ |

---

## Screen: chi-tiet-nguoi-dung — Chi tiết người dùng

### Wireframe (ASCII)

```text
┌───────────────────┬────────────────────────────────────────────────┐
│ (danh sách mờ     │ [1] Sửa người dùng             [2]< Đóng >     │
│  phía sau)        ├────────────────────────────────────────────────┤
│                   │ [3] Họ tên                                     │
│                   │ [ Nguyễn Văn A                        ]        │
│                   │                                                │
│                   │ [4] Tên đăng nhập  (không sửa được)            │
│                   │ [ an.nv                               ]        │
│                   │                                                │
│                   │ [5] Phòng ban                                  │
│                   │ [ Nhân sự (x) ][ Kế toán (x) ][ + Thêm ]       │
│                   │                                                │
│                   │ [6] Mức mật        [7] Trạng thái              │
│                   │ [v: Nội bộ ]       [v: Đang hoạt động ]        │
│                   ├────────────────────────────────────────────────┤
│                   │ [8] Người này đọc được gì                      │
│                   │     312 tài liệu trong phạm vi quyền           │
│                   │     280 qua phòng ban  ·  32 gán trực tiếp     │
│                   │       4 bị mục từ chối chặn                    │
│                   │      18 bị nhãn mật chặn                       │
│                   ├────────────────────────────────────────────────┤
│                   │ [9][ Đặt lại mật khẩu ] [10][ Vô hiệu hoá ]    │
│                   │ [11][               Lưu                 ]      │
└───────────────────┴────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tiêu đề ngăn kéo | Label | ReadOnly | • Đổi theo chế độ: *Thêm người dùng* hoặc *Sửa người dùng* — người dùng phải biết mình đang tạo mới hay sửa một bản ghi có thật. |
| 2 | Đóng | Link | Click | • Quay về danh sách, không lưu.<br>• Hành vi khi đang có thay đổi chưa lưu **chưa chốt** — xem OQ-2.<br>• Tiếp cận: phím Esc đóng ngăn kéo. |
| 3 | Họ tên | Textbox | Text | • **Bắt buộc**. Tên hiển thị trong danh sách và trong khối quyền của từng tài liệu.<br>• Lỗi: để trống → thiếu trường bắt buộc (`AC-003` của US-002). |
| 4 | Tên đăng nhập | Textbox | Text / ReadOnly | • **Bắt buộc khi tạo**, **không sửa được sau khi tạo** — hiện ở dạng chỉ đọc kèm lý do khi trỏ vào. Tên đăng nhập là thứ nhật ký và lịch sử câu hỏi trỏ về; đổi nó là làm đứt vết.<br>• Lỗi: trùng tên đăng nhập đã có (`AC-002` của US-002).<br>• Câu chữ giải thích vì sao không sửa được chưa chốt — xem OQ-3. |
| 5 | Phòng ban | Dropdown | Select | • Chọn **nhiều** phòng ban; mỗi phòng ban là một thẻ có nút gỡ riêng.<br>• Quyền đến từ nhiều phòng ban được **hợp lại** (`AC-003` của US-004).<br>• Không bắt buộc — người chưa thuộc phòng ban nào vẫn hợp lệ, chỉ là chưa hưởng quyền nào qua phòng ban.<br>• **Cảnh báo trước khi lưu:** đổi phòng ban làm người này **mất quyền đọc bao nhiêu tài liệu** thì hiện con số đó **trước** khi lưu, trong hộp thoại xác nhận thao tác ảnh hưởng rộng. Cảnh báo này hiện chưa có tiêu chí chấp nhận chống lưng — đã ghi nợ ở `CR-20260923-001`. |
| 6 | Mức mật | Dropdown | Select | • **Bắt buộc**. Ba giá trị: công khai < nội bộ < mật.<br>• **Cảnh báo trước khi lưu** như [5]: hạ mức mật là thao tác ảnh hưởng rộng, phải nêu số tài liệu người này sẽ mất quyền đọc trước khi bật nút xác nhận.<br>• Mức mật **không** thay thế danh sách cấp quyền — hai điều kiện cùng phải thoả (`AC-003` của US-007). |
| 7 | Trạng thái hoạt động | Dropdown | Select | • Đang hoạt động · đã vô hiệu hoá.<br>• Tài khoản vô hiệu hoá **không đăng nhập được nữa nhưng lịch sử câu hỏi vẫn giữ** (`AC-006` của US-002) — đây không phải xoá. |
| 8 | Người này đọc được gì | Label | ReadOnly | • Trả lời câu hỏi mà quản trị hay hỏi nhất: *vì sao người này đọc được / không đọc được tài liệu kia*.<br>• Tách theo **bốn nguồn**: qua phòng ban · gán trực tiếp · bị mục từ chối chặn · bị nhãn mật chặn. Con số cuối thường là con số làm quản trị vỡ lẽ.<br>• **Chỉ hiện ở chế độ sửa** — người chưa tồn tại thì chưa có dữ liệu quyền để tính.<br>• Là biến thể xem-theo-người-dùng của ô tra cứu quyền ở màn Chi tiết tài liệu (`AC-005` của US-006). |
| 9 | Đặt lại mật khẩu | Button | Click | • Là kênh duy nhất xử lý quên mật khẩu, vì hệ thống không tự phục vụ.<br>• Kết quả hiển thị **chưa chốt** — nhập hàng loạt đã chốt mật khẩu ban đầu xem được đúng một lần (`AC-007` của US-003); thao tác đơn lẻ có cùng ràng buộc đó không thì chưa nguồn nào nói. Xem OQ-4. |
| 10 | Vô hiệu hoá tài khoản | Button | Click | • Ngăn người này đăng nhập, giữ nguyên dữ liệu.<br>• Sau khi vô hiệu hoá, thân ngăn kéo chuyển **chỉ đọc** kèm nút *Kích hoạt lại*.<br>• Số lần xác nhận chưa chốt — đây là thao tác **hoàn tác được**, khác xoá phòng ban. Xem OQ-5. |
| 11 | Lưu | Button | Click | • States: mặc định · vô hiệu hoá khi thiếu trường bắt buộc · đang gửi (chặn gửi trùng).<br>• Ba nhánh lỗi: trùng tên đăng nhập · thiếu trường bắt buộc · mật khẩu ban đầu không đạt chính sách khi tạo mới.<br>• Điều hướng: lưu xong đóng ngăn kéo, danh sách làm mới. |

**Trường chỉ có ở chế độ tạo mới:** ô *mật khẩu ban đầu* kèm nút sinh ngẫu nhiên. Chuỗi sinh ra phải
đạt `NFR-system-026`. Mật khẩu ban đầu là dữ liệu nhạy — hiện rõ lúc tạo nhưng không đọc lại được ở
đâu khác về sau, vì hệ thống lưu dạng băm có muối (`NFR-system-025`).

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Chế độ sửa, các trường mang giá trị hiện tại |
| Tạo mới | Thêm ô mật khẩu ban đầu; khối *Người này đọc được gì* và cụm thao tác ẩn đi |
| Đang gửi | Nút Lưu vào trạng thái chờ, chặn gửi trùng |
| Tên đăng nhập trùng | Báo tại ô tên đăng nhập |
| Thiếu trường bắt buộc | Báo tại từng ô thiếu, không gom lên đầu |
| Mật khẩu không đạt chính sách | Chỉ ở chế độ tạo mới, báo tại ô mật khẩu ban đầu |
| Tài khoản đã vô hiệu hoá | Thân ở dạng chỉ đọc kèm nút Kích hoạt lại |
| Đang tải | Ngăn kéo mở, chờ dữ liệu người dùng — khung xương |
| Rỗng | Không áp dụng — ngăn kéo luôn ứng với một người cụ thể hoặc một bản ghi đang tạo |
| Không đủ quyền | Không áp dụng — ngăn kéo chỉ mở được từ màn đã qua cổng kiểm quyền |

---

## Việc phải đồng bộ ngược sang wireframe index

1. **Thêm cột `Slug`** vào bảng Screens.
2. **Cột *Thuộc luồng*** hiện gộp bốn màn quản trị vào một; userflow đã chia ba luồng riêng.
3. **Gỡ mô tả thanh thao tác hàng loạt** khỏi màn Danh sách người dùng.
4. **Thêm dòng chỉ đường quên mật khẩu** vào mô tả màn Đăng nhập.

## Open Questions

- [ ] OQ-1: Ô lọc phòng ban chọn một hay nhiều phòng ban cùng lúc?
- [ ] OQ-2: Đóng ngăn kéo lúc đang có thay đổi chưa lưu thì cảnh báo hay bỏ thẳng?
- [ ] OQ-3: Câu chữ giải thích vì sao tên đăng nhập không sửa được.
- [ ] OQ-4: Đặt lại mật khẩu đơn lẻ hiển thị kết quả thế nào — có cùng ràng buộc xem-một-lần như
      nhập hàng loạt không?
- [ ] OQ-5: Vô hiệu hoá tài khoản cần xác nhận một lần hay hai lần? Đây là thao tác hoàn tác được,
      khác xoá phòng ban.
