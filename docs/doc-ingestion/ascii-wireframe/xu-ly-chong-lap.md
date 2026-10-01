# Flow: Xử lý cảnh báo chồng lấn

> Màn hình thuộc flow này: `xu-ly-chong-lap`, vào từ thẻ *cảnh báo chồng lấn* ở Danh sách tài liệu
> hoặc từ khối cảnh báo ở Chi tiết tài liệu. Flow tổng xem `../srs/doc-ingestion-userflow.md` Mục 1.
>
> Khung vẽ máy bàn 1024 điểm ảnh. Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.
>
> Nguồn dữ liệu là bảng `document_overlap_pairs` trong `../../_shared/data-model.md` — chồng lấn là
> **quan hệ theo cặp**, không phải thuộc tính của một tài liệu. Bảng này thêm 2026-09-23; trước đó
> lược đồ chỉ có một số nguyên cụm chủ đề, không mang nổi mức độ giống nhau hay quyết định từng cặp.

---

## Screen: xu-ly-chong-lap — Xử lý cảnh báo chồng lấn

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ KHU QUẢN TRỊ │ Xử lý cảnh báo chồng lấn                            │
│              │            [1][ Chờ xử lý (3) ][ Đã xử lý (12) ]    │
│ > Tài liệu   ├─────────────────────────────────────────────────────┤
│ Người dùng   │ ┌─────────────────────────────────────────────────┐ │
│ Phòng ban    │ │ [2] Mức độ giống nhau: cao                      │ │
│ Khoảng trống │ │ ┌──────────────────────┬──────────────────────┐ │ │
│              │ │ │ [3] TÀI LIỆU MỚI     │ [4] ĐÃ CÓ TRONG KHO  │ │ │
│ ──────────── │ │ │ Quy chế lương 2025   │ Quy chế lương 2023   │ │ │
│ < Về Hỏi đáp │ │ │ Nhân sự · 01/01/2025 │ Nhân sự · 01/01/2023 │ │ │
│              │ │ │ "Mức lương tối thiểu │ "Mức lương tối thiểu │ │ │
│              │ │ │  vùng I là 4.960.000 │  vùng I là 4.680.000 │ │ │
│              │ │ │  đồng mỗi tháng..."  │  đồng mỗi tháng..."  │ │ │
│              │ │ └──────────────────────┴──────────────────────┘ │ │
│              │ │ [5][ Là bản thay thế ] [6][ Cùng tồn tại ]      │ │
│              │ │ [7][ Bỏ qua cảnh báo ]                          │ │
│              │ └─────────────────────────────────────────────────┘ │
│              │                                                     │
│              │ (còn 2 cặp, xếp theo mức giống nhau giảm dần)       │
└──────────────┴─────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Hai tab trạng thái | Tab | Click | • *Chờ xử lý* và *đã xử lý*. Tab đã xử lý cho xem lại các cặp đã quyết — hiện chưa có tiêu chí chấp nhận chống lưng, đã ghi nợ ở `CR-20260923-001`; giữ vì không có nó thì một quyết định sai không cách nào tìm lại.<br>• Số trong ngoặc là số cặp, không phải số tài liệu. |
| 2 | Mức độ giống nhau | Label | ReadOnly | • Cho quản trị biết nên xem cặp nào trước.<br>• Hiển thị dạng phần trăm hay dạng nhãn thấp/trung bình/cao **chưa chốt** — ngưỡng phát hiện chồng lấn cũng chưa hiệu chỉnh (câu hỏi mở OQ-3 của PRD), nên con số chưa có cơ sở để hiển thị. Xem OQ-1.<br>• Các cặp xếp theo mức này giảm dần. |
| 3 | Khối tài liệu mới | Label | ReadOnly | • Bốn thông tin: tên · phân loại · ngày hiệu lực · **một đoạn nội dung đại diện**.<br>• Đoạn đại diện là thứ làm màn này dùng được: **đủ để quyết mà không phải mở hai tài liệu ra đọc**. Thiếu nó thì quản trị phải rời màn, mở hai tệp, rồi quay lại — và sẽ không làm. |
| 4 | Khối tài liệu bị nghi | Label | ReadOnly | • Cùng bốn thông tin, đặt **cạnh nhau theo chiều ngang** để so sánh trực tiếp.<br>• Nhãn cột nói rõ bên nào là tài liệu mới, bên nào đã có trong kho — không có nhãn thì quản trị không biết đang thay thế cái nào bằng cái nào. |
| 5 | Là bản thay thế | Button | Click | • Xác nhận tài liệu mới thay thế tài liệu cũ.<br>• Hệ quả: chuyển sang luồng thay thế, **ghi vào chuỗi thay thế**; tài liệu cũ chuyển trạng thái *đã bị thay thế*; câu trả lời về sau ưu tiên bản mới và nêu rõ bản cũ đã bị thay bởi văn bản nào.<br>• States: mặc định · đang gửi (chặn bấm trùng). |
| 6 | Cùng tồn tại hợp lệ | Button | Click | • Xác nhận hai văn bản cùng còn hiệu lực và cùng đúng.<br>• Hệ quả: câu trả lời nào dùng cả hai sẽ **kèm cảnh báo chồng lấn** và nêu chỗ khác nhau, không tự chọn một bên rồi im lặng bỏ bên kia (`AC-003` của US-025).<br>• Quyết định này phải **đọc được ngay trong đường sinh câu trả lời**, không chỉ ở màn quản trị — đây là lý do nó nằm trong bảng cặp chứ không phải một ghi chú. |
| 7 | Bỏ qua cảnh báo | Button | Click | • Báo động giả. Hệ quả: nhãn biến mất và **không hiện lại cho đúng cặp này** (`AC-005` của US-014) — không phải tắt cảnh báo cho cả tài liệu, vì tài liệu ấy có thể còn chồng lấn với tài liệu khác. |

**Ba nút, không nút nào là mặc định.** Chúng không được làm nổi bật khác nhau và không có nút nào
được chọn sẵn — hệ thống **không chọn giúp** (`AC-003` của US-014). Ba đường dẫn tới ba hành vi khác
hẳn nhau ở màn hỏi đáp, nên gợi ý một đường là đẩy quản trị theo hướng mình đoán.

**Một tài liệu nghi chồng lấn với nhiều tài liệu** thì hiện **nhiều thẻ**, mỗi cặp một thẻ, xếp theo
mức giống nhau giảm dần, **xử lý được từng cặp riêng** (`AC-006` của US-014). Quyết định ở cặp này
không ảnh hưởng cặp kia.

**Cảnh báo chồng lấn không chặn luồng nạp** (`AC-007` của US-014). Tài liệu vẫn vào kho và vẫn trả
lời được câu hỏi trong lúc chờ xử lý — chặn lại thì một cảnh báo chưa ai xem sẽ giữ tài liệu ngoài
kho vô thời hạn.

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Danh sách thẻ cặp đang chờ xử lý, xếp theo mức giống nhau giảm dần |
| Rỗng | Không có cảnh báo nào. **Nói rõ đây là trạng thái tốt, không phải lỗi** — màn rỗng không giải thích thì quản trị tưởng chức năng hỏng |
| Đang tải | Khung xương giữ hình dạng thẻ |
| Đang gửi | Khi bấm một trong ba nút, chặn bấm trùng |
| Đã xử lý | Cặp chuyển sang tab đã xử lý, xem lại được |
| Lỗi tải | Nói cái gì hỏng, cho thử lại tại chỗ |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn: thông báo, không hiện dữ liệu, kèm liên kết về màn Hỏi đáp |
| Chỉ đọc · Vô hiệu hoá | Không áp dụng — mọi cặp đang chờ đều chờ quyết định, ba nút luôn bật |
| Một phần thành công | Không áp dụng — mỗi cặp xử lý độc lập, không có khái niệm đợt |

---

## Việc phải đồng bộ ngược sang wireframe index

> Chủ ghi của `doc-ingestion-wireframe-index.md` là `/wireframe-ascii`. Áp qua L2 diff.

1. **Thêm cột `Slug`** vào bảng Screens và cập nhật cột *Thuộc luồng* theo ba flow đã duyệt.
2. **Thẻ thứ tư *sắp hết hiệu lực*** ở dải việc đang chờ, và giá trị lọc tương ứng ở ô trạng thái
   hiệu lực.
3. **Màn Xác nhận metadata có ba chế độ**, không phải hai — thêm chế độ *chỉnh sửa* tách khỏi *chỉ
   đọc*.
4. **Dấu suy giảm chất lượng** trên bảng danh sách tài liệu.
5. **Nút Đổi nhãn mật** trong cụm nút của màn Chi tiết tài liệu.
6. **Gỡ mô tả thanh thao tác hàng loạt** khỏi màn Danh sách tài liệu.

## Open Questions

- [ ] OQ-1: Mức độ giống nhau hiển thị dạng phần trăm cụ thể hay nhãn thấp/trung bình/cao? Ngưỡng
      phát hiện chưa hiệu chỉnh nên con số chưa có cơ sở.
