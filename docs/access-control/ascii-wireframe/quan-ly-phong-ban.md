# Flow: Quản lý phòng ban

> Màn hình thuộc flow này: `quan-ly-phong-ban`, vào từ thanh bên khu quản trị. Flow tổng xem
> `../srs/access-control-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.

---

## Screen: quan-ly-phong-ban — Quản lý phòng ban

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ KHU QUẢN TRỊ │ Quản lý phòng ban        [1][ + Tạo phòng ban ]     │
│              ├──────────────────────┬──────────────────────────────┤
│ Tài liệu     │ [2] Phòng ban        │ [4] Nhân sự   [5]< Sửa tên > │
│ Người dùng   │ ──────────────────── │               [6]< Xoá >     │
│ > Phòng ban  │ > Nhân sự            │ ──────────────────────────── │
│ Khoảng trống │   12 người · 87 TL   │ [7] Thành viên (12)          │
│              │                      │   Nguyễn Văn A     Hoạt động │
│ ──────────── │   Kế toán            │   Trần Thị B       Hoạt động │
│ < Về Hỏi đáp │   8 người · 54 TL    │   Lê Văn C         Đã tắt    │
│              │                      │   (còn 9 người)              │
│              │   Kỹ thuật           │ ──────────────────────────── │
│              │   15 người · 120 TL  │ [8] Tài liệu được cấp quyền  │
│              │                      │     qua phòng ban này (87)   │
│              │   Vận hành           │   Nội quy lao động 2024      │
│              │   0 người · 3 TL     │   Quy chế lương 2025         │
│              │   [3](!) chưa có ai  │   (còn 85 tài liệu)          │
└──────────────┴──────────────────────┴──────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tạo phòng ban | Button | Click | • Mở hộp thoại nhỏ có một ô tên và nút Lưu/Huỷ.<br>• Lỗi: tên phòng ban trùng (`AC-002` của US-004).<br>• States: mặc định · đang gửi. |
| 2 | Danh sách phòng ban | Table | Click | • **Danh sách phẳng, không có cây phòng ban.** Đây là quyết định có chủ đích, lý do ghi ở US-004: kế thừa quyền theo cây là nguồn lỗi phân quyền phổ biến nhất — người quản trị tưởng gán ở nhánh cha là xong, rồi không ai biết nhánh con thừa hưởng những gì.<br>• Mỗi dòng kèm **số thành viên** và **số tài liệu đang được cấp quyền qua phòng ban đó** — hai con số này là thứ cho biết xoá nó thì ảnh hưởng tới đâu.<br>• Điều hướng: chọn một dòng thì cột phải hiện chi tiết.<br>• Cuộn dọc riêng nếu danh sách dài, không làm cuộn cả trang. |
| 3 | Dấu phòng ban rỗng | Label | ReadOnly | • Phòng ban chưa có thành viên nào **không phải lỗi**, nhưng phải nói rõ — vì gán quyền cho nó lúc này không có tác dụng gì, và quản trị dễ tưởng đã gán xong.<br>• Dùng màu cảnh báo kèm chữ, không chỉ màu. |
| 4 | Tên phòng ban | Label | ReadOnly | • Tên của phòng ban đang chọn, hiện ở đầu cột phải. |
| 5 | Sửa tên | Link | Click | • Mở hộp thoại đổi tên.<br>• **Mọi quyền đã gán giữ nguyên hiệu lực khi đổi tên** (`AC-007` của US-004) — chỉ tên hiển thị đổi, không phải tạo phòng ban mới.<br>• Lỗi: trùng tên phòng ban khác. |
| 6 | Xoá | Link | Click | • Thao tác **không hoàn tác được**, dùng hộp thoại xác nhận riêng chứ không phải hộp thoại xác nhận thường.<br>• Hộp thoại nêu **đúng số thành viên và đúng số tài liệu bị ảnh hưởng**, và nói rõ điều gì xảy ra sau đó: quyền gán qua phòng ban này biến mất, thành viên chỉ còn quyền gán trực tiếp (`AC-006` của US-004).<br>• Nút xác nhận chỉ bật **sau khi** số liệu đã hiện ra — không hiện đồng thời, để người dùng buộc phải đọc con số trước khi bấm.<br>• States: mặc định · hộp thoại đang mở · đang gửi. |
| 7 | Danh sách thành viên | Table | ReadOnly | • Hiện **mọi người dùng thuộc phòng ban** kèm trạng thái hoạt động của từng người (`AC-004` của US-004) — người đã vô hiệu hoá vẫn nằm trong danh sách, vì họ vẫn thuộc phòng ban.<br>• **Chỉ đọc, không có nút gỡ thành viên tại đây.** Gỡ một người khỏi phòng ban làm ở màn Chi tiết người dùng, bằng cách bỏ chọn phòng ban trong ô chọn nhiều. Ghi ra để không ai tưởng đây là tính năng còn thiếu — một thao tác một chỗ, tránh hai đường sửa cùng một thứ.<br>• Danh sách dài thì cuộn trong khung của nó. |
| 8 | Danh sách tài liệu được cấp quyền | Table | ReadOnly | • Cho quản trị thấy **phòng ban này mở cửa vào những tài liệu nào** — đây là thứ trả lời câu hỏi "xoá phòng ban này thì ai mất gì".<br>• Chỉ đọc; gán hoặc gỡ quyền của một tài liệu làm ở khối Quyền truy cập của màn Chi tiết tài liệu, vì quyền gắn với tài liệu. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Cột trái có danh sách, cột phải hiện phòng ban đang chọn |
| Rỗng lần đầu | Chưa có phòng ban nào. Cột trái hiện lời giải thích và nút tạo phòng ban làm hành động chính; cột phải để trống kèm một dòng hướng dẫn |
| Đang tải | Khung xương giữ hình hai cột; đích ≤2 giây theo `NFR-system-007` |
| Lỗi tải | Nói cái gì hỏng, cho thử lại tại chỗ |
| Đang gửi | Khi tạo, đổi tên hoặc xoá — chặn gửi trùng |
| Tên phòng ban trùng | Báo ngay trong hộp thoại tạo hoặc đổi tên, không đóng hộp thoại |
| Phòng ban không có thành viên | Không phải lỗi — dấu cảnh báo ở [3] và một dòng giải thích ở cột phải |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn: thông báo không đủ quyền, không hiện dữ liệu nào, kèm liên kết về màn Hỏi đáp |
| Rỗng do bộ lọc | Không áp dụng — màn này không có bộ lọc |
| Chỉ đọc · Vô hiệu hoá | Không áp dụng — phòng ban không có khái niệm khoá hay chỉ xem |

---

## Open Questions

Không có. Mọi hành vi của màn này đều truy được về tiêu chí chấp nhận của US-004.
