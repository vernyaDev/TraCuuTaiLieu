# Flow: Báo cáo khoảng trống tri thức

> Màn hình thuộc flow này: `bao-cao-khoang-trong`. Flow tổng xem
> `../srs/knowledge-gap-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.

---

## Screen: bao-cao-khoang-trong — Báo cáo khoảng trống tri thức

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ KHU QUẢN TRỊ │ Báo cáo khoảng trống tri thức    [1][v: 30 ngày ]   │
│              ├─────────────────────────────────────────────────────┤
│ Tài liệu     │ [2] [ Thiếu tài liệu ] [ Thiếu quyền ] [ Căn cứ     │
│ Người dùng   │                                         mỏng ]      │
│ Phòng ban    │                          [3][ Chờ xử lý ][ Đã xử lý]│
│ > Khoảng     │ ┌─────────────────────────────────────────────────┐ │
│   trống      │ │ [4]  [5] Nghỉ thai sản được hưởng bao nhiêu     │ │
│              │ │  17      phần trăm lương?                       │ │
│ ──────────── │ │ lần     Hỏi từ 02/09 đến 21/09          [6] v   │ │
│ < Về Hỏi đáp │ │         [7][ Đánh dấu đã xử lý ]                │ │
│              │ └─────────────────────────────────────────────────┘ │
│              │ ┌─────────────────────────────────────────────────┐ │
│              │ │ [8]  Quy trình xin nghỉ không lương             │ │
│              │ │   9  (!) đã xử lý nhưng chưa hết        [6] ^   │ │
│              │ │ lần  Hỏi từ 12/08 đến 22/09                     │ │
│              │ │ ─────────────────────────────────────────────── │ │
│              │ │ [9] 22/09 09:14  Xin nghỉ không lương tối đa    │ │
│              │ │                  bao nhiêu ngày?                │ │
│              │ │      (câu gốc: "còn bao nhiêu ngày nữa")        │ │
│              │ │      18/09 15:40  Nghỉ không lương có bị trừ    │ │
│              │ │                  phép năm không?                │ │
│              │ │ [10] < Nạp tài liệu cho chủ đề này >            │ │
│              │ └─────────────────────────────────────────────────┘ │
│              │                    (cuộn xem các nhóm còn lại)      │
└──────────────┴─────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Bộ lọc thời gian | Dropdown | Select | • Giới hạn dữ liệu theo khoảng ngày (`AC-006` của US-028).<br>• Ràng buộc: ngày bắt đầu ≤ ngày kết thúc.<br>• States: mặc định · đang mở · đã chọn.<br>• Điều hướng: đổi giá trị thì tải lại danh sách **tại chỗ**, không chuyển màn.<br>• Giá trị mặc định lúc vào màn **chưa chốt** — xem câu hỏi mở OQ-2. |
| 2 | Ba tab phân loại | Tab | Click | • Tách ba loại khoảng trống vì **việc cần làm khác hẳn nhau**: nạp tài liệu mới · gán quyền cho người đã có sẵn tài liệu · củng cố tài liệu đang mỏng căn cứ (`AC-003` của US-028, sửa 2026-09-23).<br>• Ràng buộc: đúng một tab được chọn tại một thời điểm; đổi tab **giữ nguyên** bộ lọc thời gian và bộ lọc trạng thái.<br>• States: đang chọn · không chọn; mỗi tab tự rỗng độc lập.<br>• Ranh giới quan trọng nhất của tab thứ ba: nó chỉ chứa nhóm **đã trả lời được** (`AC-006` của US-027, nhánh 3a của `UC-refuse-answer`). Con số trên thẻ ở hai tab đầu đếm *số lần bị hỏi mà không trả lời được*; ở tab thứ ba đếm *số lần đã trả lời được nhưng chỉ dựa trên một đoạn liên quan thấp*. Trộn lẫn làm chính con số ấy mất nghĩa — đây là lý do kỹ thuật của việc tách tab. |
| 3 | Bộ lọc trạng thái | Button | Click | • Tách nhóm chờ xử lý khỏi nhóm đã xử lý (`AC-007` của US-028).<br>• States: chờ xử lý · đã xử lý.<br>• Điều hướng: lọc lại tại chỗ.<br>• Mặc định khi vào màn và việc có cần lựa chọn *Tất cả* hay không **chưa chốt** — xem OQ-3. |
| 4 | Con số lần bị hỏi | Label | ReadOnly | • Mức độ nghiêm trọng của nhóm, và là căn cứ xếp thứ tự giảm dần (`AC-001`, `AC-002` của US-028).<br>• Ràng buộc: số nguyên ≥ 1.<br>• Ý nghĩa **đổi theo tab** — xem [2]. Nhãn dưới số phải nói rõ đang đếm gì, không để con số trần. |
| 5 | Câu hỏi đại diện | Label | ReadOnly | • Cho quản trị hiểu nhóm nói về việc gì mà không cần mở rộng.<br>• Hiển thị **câu đã viết lại** làm câu chính (`AC-004` của US-027) — câu gốc của một lượt tiếp nối thường cụt lủn kiểu *"còn bao nhiêu ngày nữa"*, dùng làm đại diện thì vô nghĩa.<br>• Dòng phụ ghi khoảng thời gian từ lần hỏi đầu tới lần gần nhất.<br>• Ranh giới: dòng phụ **chỉ** ghi thời gian, không có gì nhận dạng người hỏi (`AC-005` của US-028). |
| 6 | Mũi tên mở rộng | Button | Click | • Mở phần chi tiết ngay dưới đúng thẻ đó — kiểu xếp chồng dọc, **không** dùng ngăn kéo bên phải hay hộp thoại, để không mất ngữ cảnh thứ hạng theo tần suất.<br>• States: thu gọn · đã mở (đổi hướng mũi tên).<br>• Bấm cả vùng câu hỏi đại diện có mở rộng như bấm mũi tên không — xem OQ-4. |
| 7 | Đánh dấu đã xử lý | Button | Click | • Chuyển nhóm sang trạng thái đã xử lý (`AC-007` của US-028).<br>• Ràng buộc: chỉ bấm được khi nhóm đang ở trạng thái chờ xử lý.<br>• States: mặc định · **đang gửi** (vô hiệu hoá, chặn bấm trùng) · xong thì nhóm rời khỏi danh sách đang xem.<br>• Đây là **thao tác ghi duy nhất của cả màn**.<br>• Nhánh thất bại của thao tác này chưa có tiêu chí nào mô tả — xem OQ-5. |
| 8 | Thẻ *đã xử lý nhưng chưa hết* | Label | ReadOnly | • Báo nhóm đã đánh dấu xử lý nhưng sau đó vẫn có người hỏi và vẫn bị từ chối — tín hiệu tài liệu vừa nạp **không trả lời được câu hỏi thật** (`AC-007` của US-028).<br>• Nhóm quay lại tab chờ xử lý kèm thẻ này, **không** lặng lẽ tạo nhóm mới — nhóm mới sẽ giấu mất chính thông tin đáng giá ấy.<br>• Dùng màu cảnh báo chứ không phải màu lỗi: đây không phải lỗi của ai.<br>• Tiếp cận: luôn kèm chữ và biểu tượng, không truyền nghĩa chỉ bằng màu. |
| 9 | Danh sách câu hỏi cụ thể | Label | ReadOnly | • Cho quản trị thấy người dùng thật sự đã hỏi gì, kèm thời điểm (`AC-004` của US-028).<br>• Câu đã viết lại là câu chính; câu gốc thu gọn bên dưới để đối chiếu khi nghi việc viết lại làm sai ý (`AC-004` của US-027).<br>• Ở tab *thiếu quyền*, mỗi câu kèm **phòng ban** của người hỏi — đủ để biết cấp quyền cho phòng nào.<br>• **Ranh giới cứng:** không hiện tên, thư điện tử hay bất kỳ định danh cá nhân nào, ở bất kỳ tab nào, kể cả khi mở rộng. Không có ô tìm kiếm hay bộ lọc theo người dùng ở bất kỳ đâu trên màn. Câu hỏi của nhân viên tiết lộ họ đang quan tâm việc gì; công cụ quản trị tri thức không được lặng lẽ thành công cụ giám sát.<br>• Nhóm có hàng trăm câu thì cuộn hay phân trang — xem OQ-6. |
| 10 | Liên kết khắc phục | Link | Click | • Dẫn sang màn làm việc thật, đóng vòng *phát hiện → xử lý → đánh dấu*.<br>• Là **liên kết điều hướng, không phải nút hành động** — nó không tự nạp tài liệu hay tự gán quyền.<br>• Nhóm *thiếu tài liệu* dẫn sang Danh sách tài liệu; nhóm *thiếu quyền* dẫn sang Chi tiết tài liệu khối quyền, khi xác định được tài liệu liên quan.<br>• Liên kết **không** mang theo bộ lọc nào theo người hỏi.<br>• Khi không xác định được tài liệu cụ thể thì dẫn đi đâu — xem OQ-7. |

**Trạng thái của cả màn** (mục 6 của hệ thống thị giác):

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Danh sách nhóm theo tab và bộ lọc đang chọn, xếp theo số lần hỏi giảm dần |
| Đang tải | Khung xương giữ hình dạng danh sách thẻ |
| Rỗng lần đầu | Chưa có bản ghi nào. Nói rõ **đây là trạng thái tốt** và giải thích màn này sẽ hiện gì khi có dữ liệu — màn rỗng không giải thích thì người dùng tưởng hỏng |
| Rỗng do bộ lọc | Nói rõ do bộ lọc nào đang chặn, kèm nút mở rộng khoảng thời gian. Phủ cả trường hợp rỗng do tab hoặc do bộ lọc trạng thái, không chỉ do thời gian (`AC-008` của US-028) |
| Lỗi tải | Nói cái gì hỏng và cho thử lại ngay tại chỗ |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn: thông báo không có quyền truy cập, **không hiện dữ liệu nào**, kèm liên kết quay về màn hỏi đáp (`AC-009` của US-028). Ẩn mục trong thanh bên là chưa đủ — máy chủ phải chặn |
| Đang gửi | Phạm vi hẹp: chỉ ở nút đánh dấu đã xử lý của từng thẻ, không phải cả màn |
| Chỉ đọc | Không áp dụng — màn chỉ có đúng một thao tác ghi, không có khái niệm chế độ chỉ xem |
| Vô hiệu hoá | Không áp dụng ở cấp màn — phần tử duy nhất vô hiệu hoá tạm thời là nút đánh dấu lúc đang gửi, đã phủ ở trạng thái trên |
| Một phần thành công | Không áp dụng — thao tác ghi tác động đúng một nhóm mỗi lần, không có xử lý hàng loạt |

---

## Việc phải đồng bộ ngược sang wireframe index

> Chủ ghi của `knowledge-gap-wireframe-index.md` là `/wireframe-ascii`. Áp qua L2 diff.

1. **Ba tab thay vì hai** — mô tả màn hiện còn ghi *"Hai tab, không phải một danh sách trộn"*.
2. **Thêm cột `Slug`** vào bảng Screens, lấy từ Mục 2 của userflow.
3. **Thêm liên kết khắc phục** và quy ước câu-đã-viết-lại-là-câu-chính vào mô tả.

## Open Questions

- [ ] OQ-1: Phòng ban chỉ có một hai người thì hiển thị phòng ban gần như chỉ đích danh người hỏi. Có
      gộp phòng ban dưới N người vào cấp khối cha khi hiển thị không, N bằng bao nhiêu?
- [ ] OQ-2: Bộ lọc thời gian mặc định lúc vào màn là bao lâu — 7, 30 hay 90 ngày?
- [ ] OQ-3: Bộ lọc trạng thái mặc định chọn *chờ xử lý*? Có cần thêm lựa chọn *tất cả* không?
- [ ] OQ-4: Bấm cả vùng câu hỏi đại diện có mở rộng nhóm như bấm mũi tên không?
- [ ] OQ-5: Đánh dấu đã xử lý mà thất bại thì báo gì? Chưa tiêu chí nào của US-027/US-028 mô tả nhánh
      này.
- [ ] OQ-6: Nhóm có hàng trăm câu hỏi cụ thể thì cuộn tự do hay phân trang?
- [ ] OQ-7: Nhóm *thiếu tài liệu* mà không xác định được tài liệu liên quan thì liên kết khắc phục
      dẫn tới đâu — danh sách tài liệu tổng, hay có kèm từ khoá gợi ý?
