---
type: srs-userflow
feature: access-control
updated: 2026-09-09
primary_device: desktop
stage: flow-approved
flow_approved_at: 2026-09-09
flow_hash: "81de4850"
links:
  - docs/access-control/ascii-wireframe/access-control-wireframe-index.md
  - docs/access-control/usecases/access-control-usecase-index.md
  - docs/_shared/nfr.md
---

# Tài khoản và phân quyền — User Flow

> Nguồn chia luồng duy nhất của tính năng này. Khung vẽ **1024 điểm ảnh** cho cả bốn luồng, theo
> `NFR-system-042` và mục 8 của hệ thống thị giác.

Khối gán quyền đọc tài liệu **cố ý không nằm trong bốn luồng này** — quyền gắn với tài liệu nên nó
sống ở màn Chi tiết tài liệu của `doc-ingestion`. Đây là quyết định có chủ đích, không phải bỏ sót.

## 1. User Flow (tổng)

### Flow: vao-he-thong

```mermaid
flowchart TD
    a1["[1] Đăng nhập<br/>khối hẹp căn giữa, dòng Quên mật khẩu liên hệ quản trị"]
    entry1["Mở thẳng đường dẫn được bảo vệ<br/>khi chưa từng đăng nhập"]
    batdau_phienhethan["Phiên hết hạn sau 8 giờ không thao tác<br/>dải thông báo trên đầu khối"]
    g1{"Thông tin đăng nhập"}
    e1["Tên đăng nhập hoặc mật khẩu không đúng<br/>giữ tên đăng nhập, xoá ô mật khẩu, KHÔNG lộ tên có tồn tại"]
    e2["Tài khoản đã ngừng hoạt động, đề nghị liên hệ quản trị<br/>ngoại lệ có chủ đích của luật trên"]
    e3["Khoá 15 phút sau 10 lần sai liên tiếp<br/>đếm theo nguồn gửi, không theo tên đăng nhập"]
    e4["Lỗi kết nối tới máy chủ — thử lại"]
    g2{"Lần đăng nhập đầu tiên?"}
    a2["[2] Đổi mật khẩu lần đầu<br/>không bỏ qua được, không có đường vòng"]
    chk["Danh sách kiểm yêu cầu mật khẩu<br/>tự đánh dấu trong lúc gõ"]
    g3{"Mật khẩu mới hợp lệ?"}
    e5["Mật khẩu xác nhận không khớp"]
    e6["Mật khẩu hiện tại sai"]
    e7["Chưa đạt chính sách<br/>tối thiểu 12 ký tự, chặn danh sách phổ biến"]
    out["Đăng xuất — phiên kết thúc<br/>nút quay lại của trình duyệt KHÔNG vào lại được"]
    ketthuc_hoidap["Kết thúc luồng — sang Hỏi đáp, thuộc qa-chat"]
    ketthuc_dich["Kết thúc luồng — sang đích người dùng đã định mở"]

    entry1 -->|"ghi nhớ đích"| a1
    batdau_phienhethan -->|"ghi nhớ trang đang mở"| a1
    a1 --> g1
    g1 -->|"sai tên hoặc sai mật khẩu"| e1
    e1 -.->|"nhập lại"| a1
    g1 -->|"tài khoản bị vô hiệu hoá"| e2
    g1 -->|"đã sai 10 lần liên tiếp"| e3
    e3 -.->|"hết 15 phút"| a1
    g1 -->|"không gọi được máy chủ"| e4
    e4 -.->|"thử lại"| a1
    g1 -->|"đúng"| g2
    g2 -->|"đúng, bắt buộc đổi"| a2
    a2 --> chk
    chk --> g3
    g3 -->|"xác nhận không khớp"| e5
    g3 -->|"mật khẩu hiện tại sai"| e6
    g3 -->|"chưa đạt chính sách"| e7
    e5 -.->|"sửa"| chk
    e6 -.->|"sửa"| chk
    e7 -.->|"sửa"| chk
    g3 -->|"hợp lệ"| ketthuc_hoidap
    g2 -->|"không phải lần đầu, không có đích ghi nhớ"| ketthuc_hoidap
    g2 -->|"có đích đã ghi nhớ"| ketthuc_dich
    ketthuc_hoidap -.->|"chọn đăng xuất"| out
    out --> a1
    e2 -.->|"quản trị kích hoạt lại tài khoản"| a1

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class a1,a2,chk,ketthuc_hoidap,ketthuc_dich happy
    class e1,e4,e5,e6,e7 error
    class entry1,batdau_phienhethan,e2,e3,out edge
```

### Flow: quan-ly-nguoi-dung

```mermaid
flowchart TD
    b3["[3] Danh sách người dùng<br/>lọc theo tên, phòng ban, mức mật, trạng thái"]
    s0["Rỗng lần đầu: chỉ có tài khoản quản trị ban đầu<br/>gợi ý nhập hàng loạt là đường nhanh hơn"]
    s1["Rỗng do bộ lọc"]
    s2["Đang tải"]
    s3["Lỗi tải danh sách — thử lại tại chỗ"]
    ketthuc_khongduquyen["Kết thúc luồng — không đủ quyền<br/>nhân viên thường mở thẳng đường dẫn khu quản trị, kèm liên kết về Hỏi đáp"]
    bulk["Thanh thao tác hàng loạt<br/>đổi phòng ban · đổi mức mật · vô hiệu hoá"]
    warn["Cảnh báo trước khi lưu: số tài liệu sẽ mất quyền đọc<br/>áp cho cả sửa từng người lẫn thao tác hàng loạt"]
    part["Một phần thành công sau thao tác hàng loạt"]
    b4["[4] Chi tiết người dùng — ngăn kéo<br/>dùng chung cho tạo mới và sửa"]
    f1["Trường: họ tên · tên đăng nhập · phòng ban chọn nhiều · mức mật · trạng thái"]
    f2["Tạo mới có thêm mật khẩu ban đầu kèm nút sinh ngẫu nhiên"]
    f3["Khối Người này đọc được gì<br/>tách theo nguồn: phòng ban · gán trực tiếp · bị từ chối · bị nhãn mật chặn"]
    g1{"Lưu được?"}
    e1["Tên đăng nhập trùng"]
    e2["Thiếu trường bắt buộc"]
    e3["Mật khẩu không đạt chính sách"]
    act["Thao tác: đặt lại mật khẩu · vô hiệu hoá tài khoản"]
    dis["Tài khoản đã vô hiệu hoá<br/>thân ở dạng chỉ đọc kèm nút kích hoạt lại"]
    ketthuc_hangloat["Kết thúc luồng — sang Nhập hàng loạt tài khoản"]

    b3 --> s0
    b3 --> s1
    b3 --> s2
    b3 --> s3
    b3 --> ketthuc_khongduquyen
    s1 -.->|"xoá lọc"| b3
    s0 -.->|"tạo tài khoản đầu tiên"| b3
    s2 --> b3
    s3 -.->|"thử lại"| b3
    b3 -->|"chọn nhiều dòng"| bulk
    bulk -->|"đổi mức mật hoặc phòng ban"| warn
    bulk --> part
    part -.->|"xem phần lỗi"| b3
    b3 -->|"bấm một dòng"| b4
    b3 -->|"nút thêm người dùng"| b4
    b3 -->|"nút nhập hàng loạt"| ketthuc_hangloat
    b4 --> f1
    b4 --> f2
    b4 --> f3
    f2 --> g1
    f3 -.->|"xem xong"| act
    f1 -->|"hạ mức mật hoặc đổi phòng ban"| warn
    warn -.->|"xác nhận"| g1
    f1 --> g1
    g1 -->|"trùng tên đăng nhập"| e1
    g1 -->|"thiếu trường"| e2
    g1 -->|"mật khẩu không đạt"| e3
    e1 -.->|"sửa"| f1
    e2 -.->|"sửa"| f1
    e3 -.->|"sửa"| f1
    g1 -->|"hợp lệ"| b3
    b4 --> act
    act -->|"vô hiệu hoá"| dis
    dis -.->|"kích hoạt lại"| b4
    b4 -.->|"đóng ngăn kéo"| b3

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class b3,b4,f1,f2,f3,act,ketthuc_hangloat happy
    class s3,ketthuc_khongduquyen,e1,e2,e3 error
    class s0,s1,s2,bulk,warn,part,dis edge
```

### Flow: nhap-hang-loat

```mermaid
flowchart TD
    c0["[3] Danh sách người dùng — nút nhập hàng loạt"]
    c1["[5] Bước 1 — chọn tệp<br/>vùng kéo thả + liên kết tải tệp mẫu có sẵn cột đúng tên"]
    g1{"Tệp đọc được?"}
    e1["Sai định dạng hoặc thiếu cột<br/>KHÔNG tạo tài khoản nào"]
    c2["[5] Bước 2 — xem trước<br/>dòng hợp lệ và dòng lỗi trộn theo đúng thứ tự trong tệp"]
    r1["Mỗi dòng lỗi ghi số thứ tự dòng trong tệp và lý do<br/>nút tải về danh sách dòng lỗi để sửa ngoài"]
    r2["Thiếu tên đăng nhập"]
    r3["Trùng tên đăng nhập với tài khoản đã có<br/>loại dòng đó, ghi lý do"]
    r4["Trùng tên đăng nhập ngay trong tệp<br/>loại CẢ HAI dòng, để người dùng tự quyết"]
    dept["Phòng ban chưa tồn tại — hỏi rõ<br/>tạo mới phòng ban, hay để trống cho các dòng đó"]
    sum["Dải tổng kết: N dòng hợp lệ · M dòng lỗi"]
    c3["[5] Bước 3 — kết quả<br/>danh sách tên đăng nhập kèm mật khẩu ban đầu"]
    once["Tải về được ĐÚNG MỘT LẦN<br/>cảnh báo rõ đây là lần duy nhất xem được"]
    note["Nhắc mọi tài khoản đều phải đổi mật khẩu ở lần đăng nhập đầu"]
    back["Đóng hộp thoại — về danh sách đã làm mới, thấy N tài khoản vừa tạo"]

    c0 --> c1
    c1 --> g1
    g1 -->|"không đọc được"| e1
    e1 -.->|"chọn tệp khác"| c1
    g1 -->|"đọc được"| c2
    c2 --> r1
    r1 --> r2
    r1 --> r3
    r1 --> r4
    c2 --> dept
    c2 --> sum
    r2 -.->|"sửa ngoài rồi nạp lại"| c1
    r3 -.->|"sửa ngoài rồi nạp lại"| c1
    r4 -.->|"sửa ngoài rồi nạp lại"| c1
    dept -->|"đã chọn cách xử lý"| sum
    sum -->|"xác nhận tạo — bước duy nhất ghi dữ liệu"| c3
    c3 --> once
    c3 --> note
    note --> back
    once --> back
    back --> c0

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class c0,c1,c2,c3,sum,once,back happy
    class e1,r2,r3,r4 error
    class r1,dept,note edge
```

### Flow: quan-ly-phong-ban

```mermaid
flowchart TD
    d0["Thanh bên trái khu quản trị"]
    d6["[6] Quản lý phòng ban — hai cột<br/>danh sách PHẲNG, không có cây phòng ban"]
    s0["Rỗng lần đầu: chưa có phòng ban nào"]
    s1["Đang tải"]
    s2["Lỗi tải — thử lại tại chỗ"]
    ketthuc_khongquyen_pb["Kết thúc luồng — không đủ quyền, kèm liên kết về Hỏi đáp"]
    left["Trái: mỗi dòng kèm số thành viên<br/>và số tài liệu đang gán quyền qua phòng ban đó"]
    right["Phải: tên · danh sách thành viên · danh sách tài liệu được cấp quyền"]
    g1{"Thao tác"}
    add["Tạo phòng ban"]
    edit["Sửa tên"]
    e1["Tên phòng ban trùng"]
    empty["Phòng ban không có thành viên nào<br/>không phải lỗi, nhưng nói rõ gán quyền lúc này không có tác dụng"]
    del["Xoá — hộp thoại nêu đúng số thành viên và số tài liệu bị ảnh hưởng<br/>và nói rõ quyền gán qua phòng ban này sẽ biến mất"]
    sent["Đang gửi"]

    d0 --> d6
    d6 --> s0
    d6 --> s1
    d6 --> s2
    d6 --> ketthuc_khongquyen_pb
    s1 --> d6
    empty -.->|"gán thành viên vào phòng ban"| right
    s2 -.->|"thử lại"| d6
    s0 -.->|"tạo phòng ban đầu tiên"| add
    d6 --> left
    left -->|"chọn một phòng ban"| right
    right --> empty
    d6 --> g1
    g1 -->|"tạo mới"| add
    g1 -->|"sửa"| edit
    g1 -->|"xoá"| del
    add --> sent
    edit --> sent
    del -->|"xác nhận"| sent
    sent -->|"tên trùng"| e1
    e1 -.->|"đổi tên"| add
    sent -->|"xong"| d6

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class d0,d6,left,right,add,edit,sent happy
    class s2,ketthuc_khongquyen_pb,e1 error
    class s0,s1,empty,del edge
```

## 2. Danh sách màn hình

| [#] | Slug | Mã màn | Màn hình | Mục đích | Luồng nhà | Dùng chung ở |
|-----|------|--------|----------|----------|-----------|--------------|
| 1 | `dang-nhap` | `SCR-access-control-01` | Đăng nhập | Màn duy nhất người chưa đăng nhập vào được | `vao-he-thong` | — |
| 2 | `doi-mat-khau-lan-dau` | `SCR-access-control-02` | Đổi mật khẩu lần đầu | Buộc đổi mật khẩu ban đầu trước khi dùng hệ thống | `vao-he-thong` | — |
| 3 | `danh-sach-nguoi-dung` | `SCR-access-control-03` | Danh sách người dùng | Tìm và thao tác trên tài khoản, đơn lẻ hoặc hàng loạt | `quan-ly-nguoi-dung` | `nhap-hang-loat` |
| 4 | `chi-tiet-nguoi-dung` | `SCR-access-control-04` | Chi tiết người dùng | Tạo mới hoặc sửa một tài khoản, và thấy người đó đọc được gì | `quan-ly-nguoi-dung` | — |
| 5 | `nhap-hang-loat` | `SCR-access-control-05` | Nhập hàng loạt tài khoản | Tạo nhiều tài khoản từ một tệp, có bước xem trước | `nhap-hang-loat` | — |
| 6 | `quan-ly-phong-ban` | `SCR-access-control-06` | Quản lý phòng ban | Trông coi danh sách phòng ban phẳng và quyền gán qua chúng | `quan-ly-phong-ban` | — |

## 3. Danh sách flow

| Flow-slug | Tên flow | Màn hình gồm | Cases phủ |
|-----------|----------|--------------|-----------|
| `vao-he-thong` | Vào hệ thống | `dang-nhap` → `doi-mat-khau-lan-dau` | happy · đăng xuất kèm chặn nút quay lại · sai thông tin · tài khoản vô hiệu hoá · khoá 15 phút · lỗi kết nối · phiên hết hạn · mở thẳng đường dẫn khi chưa đăng nhập · mật khẩu không khớp, sai, hoặc chưa đạt chính sách |
| `quan-ly-nguoi-dung` | Quản lý người dùng | `danh-sach-nguoi-dung` → `chi-tiet-nguoi-dung` | happy tạo và sửa · cảnh báo mất quyền đọc trước khi lưu, cả đơn lẻ lẫn hàng loạt · trùng tên đăng nhập · thiếu trường · mật khẩu không đạt · tài khoản đã vô hiệu hoá · rỗng lần đầu · rỗng do lọc · đang tải · lỗi · không đủ quyền · một phần thành công |
| `nhap-hang-loat` | Nhập hàng loạt tài khoản | `danh-sach-nguoi-dung` → `nhap-hang-loat` | happy ba bước · sai định dạng hoặc thiếu cột · thiếu tên đăng nhập · trùng với tài khoản đã có · trùng ngay trong tệp · phòng ban chưa tồn tại · mật khẩu ban đầu xem một lần |
| `quan-ly-phong-ban` | Quản lý phòng ban | `quan-ly-phong-ban` | happy tạo, sửa, xoá · xoá có nêu số liệu ảnh hưởng · tên trùng · phòng ban không có thành viên · rỗng lần đầu · đang tải · lỗi · đang gửi · không đủ quyền |

## 3.5. Chuyển màn

| Từ màn [#] | Đến màn [#] | Trigger | Điều kiện |
|-----------|------------|---------|-----------|
| Đăng nhập [1] | Đổi mật khẩu [2] | Đăng nhập thành công | Là lần đăng nhập đầu tiên |
| Danh sách người dùng [3] | Chi tiết [4] | Bấm một dòng, hoặc nút *thêm người dùng* | Cùng ngăn kéo cho cả tạo mới và sửa |
| Danh sách người dùng [3] | Nhập hàng loạt [5] | Nút *nhập hàng loạt* | — |
| Nhập hàng loạt [5] | Danh sách người dùng [3] | Đóng hộp thoại ở bước 3 | Danh sách làm mới, thấy các tài khoản vừa tạo |
| Chi tiết [4] | Danh sách người dùng [3] | Lưu thành công, hoặc đóng ngăn kéo | — |

**Lối vào và lối ra khỏi phạm vi tính năng:**

- Thanh bên khu quản trị → **Quản lý phòng ban [6]**, bấm mục *Phòng ban*. Ba màn quản trị còn lại
  cũng vào từ thanh bên này.
- Đăng nhập [1] → màn **Hỏi đáp** của `qa-chat`, khi đăng nhập thành công mà không phải lần đầu và
  không có đích đã ghi nhớ.
- Đăng nhập [1] → **đích người dùng đã định mở**, khi trước đó họ mở thẳng đường dẫn được bảo vệ
  hoặc phiên vừa hết hạn.
- Đổi mật khẩu [2] → màn **Hỏi đáp** của `qa-chat` ngay sau khi đổi thành công, không bắt đăng nhập
  lại.
- Bất kỳ màn nào → **Đăng nhập [1]**, khi chọn đăng xuất hoặc phiên hết hạn. Phiên kết thúc và nút
  quay lại của trình duyệt không đưa vào lại được.

## 4. Điều phải giữ khi vẽ wireframe và viết spec

- **Sai thông tin đăng nhập chỉ có một thông báo duy nhất.** Phân biệt được sai tên với sai mật
  khẩu là đủ để dò ra danh sách nhân sự của doanh nghiệp.
- **Bộ đếm 10 lần sai đếm theo nguồn gửi, không theo tên đăng nhập** (chốt 2026-09-09). Nếu đếm
  theo tên đăng nhập thì tên có thật sẽ bị khoá còn tên bịa thì không — chính khác biệt hành vi đó
  là kênh dò tài khoản, dù chữ nghĩa hai bên giống hệt nhau.
- **Đăng xuất phải làm phiên cũ không dùng lại được**, kể cả qua nút quay lại của trình duyệt.
- **Quên mật khẩu không tự phục vụ** — hệ thống chạy nội bộ, chưa có kênh gửi thư. Màn đăng nhập
  cần một dòng chữ tĩnh chỉ đường liên hệ quản trị, để người dùng không rơi vào ngõ cụt.
- **Danh sách phòng ban phẳng, không có cây.** Kế thừa quyền theo cây là nguồn lỗi phân quyền phổ
  biến nhất — lý do ghi ở US-004.

## 5. Việc phải đồng bộ ngược sang wireframe index

> Chủ ghi của `ascii-wireframe/access-control-wireframe-index.md` là `/wireframe-ascii`. Áp khi
> chạy skill đó, qua L2 diff.

1. **Cột *Thuộc luồng* hiện gộp bốn màn quản trị vào một luồng duy nhất.** Userflow này chia thành
   ba luồng (`quan-ly-nguoi-dung`, `nhap-hang-loat`, `quan-ly-phong-ban`) — cột đó phải cập nhật
   theo, nếu không hai nguồn lệch nhau ngay từ đầu chuỗi tài liệu.
2. **Thêm dòng chữ chỉ đường quên mật khẩu** vào mô tả màn Đăng nhập.
3. **Thêm cột `Slug` vào bảng Screens của wireframe index**, lấy đúng slug ở Mục 2 trên. Hiện index
   chỉ có mã màn và tên tiếng Việt, nên đồ thị tri thức tự suy slug từ tên và sinh ra node trùng
   (`nhap-hang-loat-tai-khoan` bên cạnh `nhap-hang-loat`). Slug ở userflow là bản chuẩn.

## 6. Open Questions

- [ ] OQ-1: Thao tác *đặt lại mật khẩu* cho một người ở màn Chi tiết người dùng hiển thị kết quả
      thế nào? Nhập hàng loạt đã chốt là xem được đúng một lần; thao tác đơn lẻ có cùng ràng buộc
      đó không thì chưa nguồn nào nói.
- [ ] OQ-2: Đóng ngăn kéo Chi tiết người dùng lúc đang có thay đổi chưa lưu thì cảnh báo hay bỏ?
