# Flow: Nhập hàng loạt tài khoản

> Màn hình thuộc flow này: `nhap-hang-loat`, vào từ nút *Nhập hàng loạt* ở màn Danh sách người dùng.
> Flow tổng xem `../srs/access-control-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.

---

## Screen: nhap-hang-loat — Nhập hàng loạt tài khoản

Hộp thoại giữa màn, ba bước, có chỉ báo bước trên đầu. **Chỉ bước ba ghi dữ liệu** — bước một và
bước hai thuần đọc và xem trước, huỷ giữa chừng không để lại tài khoản nào.

### Wireframe (ASCII)

**Bước 1 — chọn tệp**

```text
┌────────────────────────────────────────────────────────────────────┐
│ Nhập hàng loạt tài khoản             [1] (1)──( )──( )             │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│    ┌────────────────────────────────────────────────────────┐      │
│    │ [2]                                                    │      │
│    │           Kéo thả tệp danh sách vào đây                │      │
│    │           hoặc [ Chọn tệp ]                            │      │
│    │                                                        │      │
│    └────────────────────────────────────────────────────────┘      │
│                                                                    │
│ [3] < Tải tệp mẫu có sẵn các cột đúng tên >                        │
│                                                                    │
│                              [ Huỷ ]      [4][ Tiếp tục ]          │
└────────────────────────────────────────────────────────────────────┘
```

**Bước 2 — xem trước**

```text
┌────────────────────────────────────────────────────────────────────┐
│ Nhập hàng loạt tài khoản             [1] ( )──(2)──( )             │
├────────────────────────────────────────────────────────────────────┤
│ [5] 45 dòng hợp lệ · 5 dòng lỗi   [6]< Tải danh sách dòng lỗi >    │
│ ────────────────────────────────────────────────────────────────── │
│ [7] Dòng  Họ tên        Tên ĐN     Phòng ban   Ghi chú             │
│      2    Nguyễn Văn A  an.nv      Nhân sự     hợp lệ              │
│      3    Trần Thị B    (trống)    Kế toán     thiếu tên đăng nhập │
│      4    Lê Văn C      cuong.lv   Kỹ thuật    trùng tài khoản đã  │
│                                                có trong hệ thống   │
│      7    Phạm Thị D    dung.pt    Vận hành    trùng với dòng 12   │
│     12    Phạm Thị E    dung.pt    Vận hành    trùng với dòng 7    │
│ ────────────────────────────────────────────────────────────────── │
│ [8] Phòng ban "Vận hành" chưa tồn tại:                             │
│     ( ) Tạo mới phòng ban này                                      │
│     ( ) Để trống phòng ban cho các dòng đó                         │
│                        [ Quay lại ]    [9][ Xác nhận tạo ]         │
└────────────────────────────────────────────────────────────────────┘
```

**Bước 3 — kết quả**

```text
┌────────────────────────────────────────────────────────────────────┐
│ Nhập hàng loạt tài khoản             [1] ( )──( )──(3)             │
├────────────────────────────────────────────────────────────────────┤
│ [10] Đã tạo 45 tài khoản.                                          │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ (!) [11] Đây là lần duy nhất xem được mật khẩu ban đầu.        │ │
│ │          Tải về trước khi đóng hộp thoại.                      │ │
│ └────────────────────────────────────────────────────────────────┘ │
│ [12] Tên đăng nhập        Mật khẩu ban đầu                         │
│      an.nv                7fK2-mQx9-Lp4v                           │
│      binh.tt              3nRt-wY8c-Jh6d                           │
│      cuong.lv             8pWs-dE5a-Vn2k                           │
│      (còn 42 dòng)                                                 │
│                                                                    │
│ [13] Mọi tài khoản phải đổi mật khẩu ở lần đăng nhập đầu.          │
│                        [14][ Tải về ]      [15][ Đóng ]            │
└────────────────────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Chỉ báo bước | Label | ReadOnly | • Cho biết đang ở đâu trong ba bước và còn mấy bước nữa.<br>• Tiếp cận: bước hiện tại phân biệt bằng chữ số và hình dạng, không chỉ bằng màu. |
| 2 | Vùng kéo thả tệp | File-upload | Upload | • Nhận tệp danh sách tài khoản.<br>• **Hai cách nhập**: kéo thả, hoặc nút chọn tệp cho người không quen kéo thả.<br>• States: mặc định · đang rê tệp lên vùng · đang đọc tệp.<br>• Lỗi: sai định dạng hoặc thiếu cột → **không tạo tài khoản nào** (`AC-006` của US-003), báo rõ thiếu cột nào. |
| 3 | Tải tệp mẫu | Link | Click | • Tệp mẫu có sẵn các cột đúng tên — đây là thứ tiết kiệm nhiều thời gian hỗ trợ nhất ở màn này, vì đa số lỗi nhập hàng loạt là lỗi sai tên cột. |
| 4 | Tiếp tục | Button | Click | • States: vô hiệu hoá khi chưa chọn tệp · đang đọc tệp.<br>• Điều hướng: sang bước hai. **Không ghi dữ liệu.** |
| 5 | Dải tổng kết | Label | ReadOnly | • Cho biết quy mô kết quả trước khi đọc chi tiết: bao nhiêu dòng hợp lệ, bao nhiêu dòng lỗi.<br>• Dòng bị loại vì trùng tính vào cột nào **chưa chốt** — xem OQ-1. |
| 6 | Tải danh sách dòng lỗi | Link | Click | • Để quản trị sửa ngoài rồi nạp lại, thay vì phải tự dò trong tệp gốc.<br>• States: ẩn khi không có dòng lỗi nào. |
| 7 | Bảng xem trước | Table | ReadOnly | • Dòng hợp lệ và dòng lỗi **trộn lẫn theo đúng thứ tự trong tệp gốc**, không gom lỗi xuống cuối — để quản trị đối chiếu được với tệp đang mở.<br>• Mỗi dòng lỗi ghi **số thứ tự dòng trong tệp** và lý do ngay tại dòng.<br>• Bốn lý do lỗi phân biệt được: thiếu tên đăng nhập · **trùng với tài khoản đã có trong hệ thống** (`AC-003` của US-003, loại dòng đó và ghi lý do) · **trùng ngay trong chính tệp** (`AC-005` của US-003, loại **cả hai** dòng và nói rõ trùng với dòng nào, để quản trị tự quyết giữ dòng nào) · sai định dạng dữ liệu.<br>• Hai loại trùng là hai lý do khác nhau, cột ghi chú phải phân biệt được — gộp thì quản trị không biết phải sửa gì. |
| 8 | Xử lý phòng ban chưa tồn tại | Radio | Select | • Hệ thống **không tự tạo phòng ban** (`AC-004` của US-003) — tự tạo thì một lỗi chính tả trong tệp sinh ra một phòng ban ma mà không ai biết.<br>• Hai lựa chọn, **không có mặc định**: tạo mới phòng ban, hay để trống phòng ban cho các dòng đó.<br>• States: ẩn khi mọi phòng ban trong tệp đều đã tồn tại. |
| 9 | Xác nhận tạo | Button | Click | • **Đây là bước ghi dữ liệu duy nhất của cả hộp thoại.**<br>• States: vô hiệu hoá khi chưa chọn cách xử lý phòng ban chưa tồn tại · đang gửi (chặn gửi trùng).<br>• Điều hướng: sang bước ba. |
| 10 | Kết quả | Label | ReadOnly | • Số tài khoản đã tạo thật, có thể ít hơn số dòng hợp lệ nếu có lỗi lúc ghi. |
| 11 | Cảnh báo xem một lần | Label | ReadOnly | • **Nói rõ đây là lần duy nhất xem được mật khẩu ban đầu** (`AC-007` của US-003). Hệ thống lưu mật khẩu dạng băm có muối (`NFR-system-025`), nên sau khi đóng hộp thoại thì không ai đọc lại được, kể cả quản trị.<br>• Dùng màu cảnh báo kèm chữ và biểu tượng, không truyền nghĩa chỉ bằng màu. |
| 12 | Bảng mật khẩu ban đầu | Table | ReadOnly | • Cặp tên đăng nhập và mật khẩu ban đầu cho từng tài khoản vừa tạo.<br>• Đây là dữ liệu nhạy: không ghi vào nhật ký, không hiện lại ở bất kỳ màn nào khác. |
| 13 | Nhắc đổi mật khẩu | Label | ReadOnly | • Nhắc mọi tài khoản đều bị bắt đổi mật khẩu ở lần đăng nhập đầu — để quản trị biết mà nói lại với người nhận. |
| 14 | Tải về | Button | Click | • Tải danh sách cặp tài khoản và mật khẩu, **được đúng một lần**.<br>• States: mặc định · đã tải (đổi nhãn, không cho tải lại). |
| 15 | Đóng | Button | Click | • Điều hướng: đóng hộp thoại, quay về Danh sách người dùng **đã làm mới**, thấy ngay các tài khoản vừa tạo.<br>• Cảnh báo nếu chưa tải về danh sách mật khẩu — đóng rồi thì không lấy lại được. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Bước một, vùng kéo thả trống |
| Đang đọc tệp | Chỉ báo trên vùng kéo thả; chưa sang bước hai |
| Sai định dạng hoặc thiếu cột | Báo ngay ở bước một, nêu thiếu cột nào, **không tạo tài khoản nào** |
| Một phần thành công | 45 hợp lệ trên 50 dòng — bảng xem trước hiện đủ cả hai loại, không giấu dòng lỗi |
| Đang gửi | Chỉ ở nút Xác nhận tạo, chặn gửi trùng |
| Lỗi khi ghi | Một số dòng hợp lệ vẫn tạo thất bại — bước ba nêu rõ số tạo được và số thất bại |
| Đang tải · Rỗng | Không áp dụng — hộp thoại không tải dữ liệu từ máy chủ lúc mở |
| Chỉ đọc · Không đủ quyền | Không áp dụng — chỉ mở được từ màn đã qua cổng kiểm quyền |

---

## Open Questions

- [ ] OQ-1: Dòng bị loại vì trùng tính vào cột *hợp lệ* hay cột *lỗi* của dải tổng kết, hay cần cột
      thứ ba?
- [ ] OQ-2: Đóng hộp thoại ở bước ba mà chưa tải danh sách mật khẩu thì chặn lại hay chỉ cảnh báo?
