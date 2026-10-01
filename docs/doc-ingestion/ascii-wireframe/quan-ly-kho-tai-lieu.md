# Flow: Trông coi kho tài liệu

> Màn hình thuộc flow này: `danh-sach-tai-lieu` → `chi-tiet-tai-lieu` → `xem-truoc-doan`. Flow tổng
> xem `../srs/doc-ingestion-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.
>
> **Thao tác hàng loạt: đúng một cái, là gán quyền** (`AC-005` của US-005 — chọn nhiều tài liệu, cấp
> quyền cho một phòng ban trong một thao tác, hệ thống báo số tài liệu đã áp dụng). *Gỡ hàng loạt*
> và *đổi phân loại hàng loạt* không có tiêu chí nào chống lưng nên đã gỡ khỏi bản vẽ.
>
> **Đính chính 2026-09-24:** ngày 23/09 gỡ nhầm cả gán quyền hàng loạt theo một báo cáo rà soát sai;
> `AC-005` của US-005 có thật nên đã khôi phục.

---

## Screen: danh-sach-tai-lieu — Danh sách tài liệu

Màn chính của quản trị tri thức, và là màn phải dùng được ở quy mô **1.000 tài liệu** chứ không chỉ
ở quy mô demo.

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ KHU QUẢN TRỊ │ Danh sách tài liệu     [1][ + Tải tài liệu lên ]    │
│              ├─────────────────────────────────────────────────────┤
│ > Tài liệu   │ [2] ┌────────┐┌────────┐┌────────┐┌───────────────┐ │
│ Người dùng   │     │   7    ││   3    ││   2    ││      11       │ │
│ Phòng ban    │     │chờ xác ││ chồng  ││  nạp   ││ sắp hết hiệu  │ │
│ Khoảng trống │     │ nhận   ││  lấn   ││  lỗi   ││ lực (30 ngày) │ │
│              │     └────────┘└────────┘└────────┘└───────────────┘ │
│ ──────────── │ [3][________________]  [4][v: Phân loại ]           │
│ < Về Hỏi đáp │ [5][v: Trạng thái  ]   [6][v: Hiệu lực  ]           │
│              ├─────────────────────────────────────────────────────┤
│              │ Đã chọn 3 trên 847  [12][ Gán quyền cho... ]        │
│              │ ─────────────────────────────────────────────────── │
│              │ [11] Tên           PL   Mật  TT      HL   Đọc   Nạp │
│              │ ─────────────────────────────────────────────────── │
│              │ [x] Nội quy LĐ 24  Nội  Nội  Sẵn     Còn  312 01/09 │
│              │ [x] Quy chế lương  Nhân Mật  Sẵn [8] Còn   54 12/09 │
│              │ [ ] Hướng dẫn ATLĐ HD   Công Đang    --    -- 23/09 │
│              │        [9] bóc văn bản ███░░░░  84/214 đoạn         │
│              │ [ ] Bảng phụ cấp   Nhân Nội  Lỗi     --    -- 23/09 │
│              │ [x] Quy định 2020  Nội  Nội  Thay    Hết  312 01/03 │
│              │                                                     │
│              │ [10] 1-20 trên 847   < Trước  1 2 .. 43  Sau >      │
└──────────────┴─────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tải tài liệu lên | Button | Click | • Hành động chính của màn.<br>• Điều hướng: mở hộp thoại của luồng `nap-tai-lieu`. |
| 2 | Bốn thẻ việc đang chờ | Button | Click | • Trả lời câu hỏi **"hôm nay tôi phải làm gì"** mà không bắt người dùng tự dò trong 1.000 dòng. Đây là phần tử có giá trị cao nhất của màn.<br>• Bốn nhóm: *chờ xác nhận metadata* · *cảnh báo chồng lấn chưa xử lý* · *nạp lỗi* · ***sắp hết hiệu lực trong 30 ngày*** (`AC-005` của US-016 — thẻ này thêm 2026-09-23, trước đó tiêu chí có mà không màn nào phục vụ).<br>• Thẻ *sắp hết hiệu lực* khi lọc thì sắp theo ngày gần nhất trước, kèm số ngày còn lại.<br>• **Thẻ bằng không hiện nhạt, không biến mất** — biến mất thì người dùng không biết nó tồn tại và không bao giờ nghĩ tới nhóm việc đó.<br>• Điều hướng: bấm một thẻ thì lọc bảng theo đúng nhóm ấy, ở lại màn này (`AC-004` của US-016).<br>• Tiếp cận: luôn có số và nhãn chữ, không truyền nghĩa chỉ bằng màu. |
| 3 | Ô tìm theo tên | Textbox | Text | • Tìm theo **tên tài liệu**, không phải tìm nội dung như màn Hỏi đáp — chữ gợi ý trong ô phải nói rõ điều đó, nếu không quản trị gõ câu hỏi vào đây.<br>• Gõ tới đâu lọc tới đó. |
| 4 | Lọc theo phân loại | Dropdown | Select | • Thu hẹp theo danh mục phân loại (chưa chốt danh mục — xem OQ-1). |
| 5 | Lọc theo trạng thái xử lý | Dropdown | Select | • Sáu giá trị (`AC-002` của US-016): đang xử lý · nạp lỗi · chờ xác nhận metadata · sẵn sàng · đã bị thay thế · đã gỡ. |
| 6 | Lọc theo trạng thái hiệu lực | Dropdown | Select | • **Bốn giá trị**: còn hiệu lực · **sắp hết hiệu lực** · đã hết hiệu lực · chưa xác định (`AC-003` của US-016 cộng giá trị thêm 2026-09-23).<br>• *Chưa xác định* **không được gộp vào** *còn hiệu lực* — một bên nghĩa là chắc chắn còn giá trị, một bên nghĩa là chưa ai kiểm. |
| 7 | Bảng tài liệu | Table | Click | • Bảy cột: tên · phân loại · nhãn mật · trạng thái · hiệu lực · **số người đọc được** · ngày nạp.<br>• Sắp mặc định theo ngày nạp mới nhất trước.<br>• Cột hiệu lực phân biệt bằng **chữ, không chỉ màu**.<br>• Cột nhãn mật là thẻ nhỏ ở cột riêng, **không tô nền cả dòng** — tô cả dòng làm mất khả năng đọc các cột khác.<br>• Cột *số người đọc được* hiện chưa có tiêu chí chấp nhận chống lưng, đã ghi nợ ở `CR-20260923-001`; giữ vì nó là thứ cho biết ngay tác động khi định gỡ hay xoá.<br>• Điều hướng: bấm một dòng mở ngăn kéo Chi tiết.<br>• Quy mô đo ở `NFR-system-007` (≤2 giây ở kho đầy) và `NFR-system-012`.<br>• Bảng rộng cuộn ngang trong khung của nó, không làm cả trang cuộn ngang. |
| 8 | Dấu suy giảm chất lượng | Label | ReadOnly | • Tài liệu nạp **thành công nhưng chất lượng kém** — bảng không bóc được cấu trúc, hoặc tài liệu không có cấu trúc rõ nên phải tách dự phòng.<br>• Đây là nhóm dễ bỏ sót nhất: tài liệu vẫn *sẵn sàng*, không lỗi, không nằm trong thẻ việc nào. Trước 2026-09-23 chỉ thấy được khi mở đúng tài liệu ấy ra xem trước đoạn — tức phải mở mù từng tài liệu trong kho 1.000 tài liệu.<br>• Điều hướng: bấm vào dấu này mở thẳng màn Xem trước đoạn. |
| 9 | Tiến độ đang xử lý | Label | ReadOnly | • Thanh tiến độ nhỏ **ngay trong ô trạng thái**, tự cập nhật, nêu bước hiện tại và số đoạn đã xử lý trên tổng (`AC-008` của US-009).<br>• Ba bước: bóc văn bản → tách đoạn → lập chỉ mục. |
| 10 | Phân trang | Button | Click | • Có tổng số và nhảy trang được, **không cuộn vô tận** — quản trị cần biết kho lớn cỡ nào và quay lại đúng trang cũ.<br>• Đổi trang giữ nguyên bộ lọc. |
| 11 | Ô đánh dấu chọn dòng | Checkbox | Check | • Chọn nhiều tài liệu để gán quyền một lượt.<br>• States: ẩn cho tới khi người dùng bắt đầu chọn · đã chọn một phần · đã chọn cả trang.<br>• Chọn cả trang **không** có nghĩa là chọn cả kho — phải nói rõ đang chọn bao nhiêu trên tổng bao nhiêu, nếu không quản trị tưởng đã gán cho toàn bộ. |
| 12 | Gán quyền hàng loạt | Button | Click | • Hiện khi có ít nhất một dòng được chọn.<br>• Cấp quyền đọc cho một người dùng hoặc một phòng ban **trên mọi tài liệu đang chọn trong một thao tác** (`AC-005` của US-005) — đây là đường đi thật của tình huống vừa nạp một đợt 20 tài liệu rồi mở quyền cho một phòng.<br>• Kết quả **nêu số tài liệu đã áp dụng**, không chỉ báo thành công chung chung.<br>• States: ẩn · hiện · đang gửi · một phần thành công (một số tài liệu áp được, một số không).<br>• **Chỉ có đúng thao tác hàng loạt này.** Gỡ hàng loạt và đổi phân loại hàng loạt không có tiêu chí nào chống lưng nên không vẽ. |

**Dải tổng kết đợt nạp.** Sau một đợt nạp nhiều tệp, một dải tạm thời hiện ngay dưới tiêu đề: số
thành công, số lỗi, kèm tên từng tệp lỗi và lý do; bấm vào phần lỗi thì lọc bảng theo nhóm đó
(`AC-005` của US-010). Dải này **xem lại được sau khi rời màn**, không phải thông báo thoáng qua.

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Bảng đầy dữ liệu, bốn thẻ việc hiện đủ |
| Rỗng lần đầu | Giải thích kho đang trống, nút tải tài liệu lên làm hành động chính giữa vùng trống |
| Rỗng do bộ lọc | Nói rõ do bộ lọc, kèm nút xoá lọc ngay tại chỗ — khác hẳn rỗng lần đầu |
| Đang tải | Khung xương giữ đúng hình dạng bảng |
| Lỗi tải | Nói cái gì hỏng, thử lại tại chỗ, **không mất bộ lọc đang đặt** |
| Đang xử lý | Dòng tài liệu đang nạp hiện thanh tiến độ trong ô trạng thái, tự cập nhật |
| Một phần thành công | Dải tổng kết đợt nạp, bấm được vào phần lỗi |
| Không đủ quyền | Nhân viên thường mở thẳng đường dẫn: thông báo, không hiện dữ liệu, kèm liên kết về màn Hỏi đáp |
| Đang gửi | Không áp dụng ở cấp màn — mọi thao tác ghi diễn ra trong hộp thoại tải lên hoặc ngăn kéo Chi tiết |
| Chỉ đọc · Vô hiệu hoá | Không áp dụng — đây là danh sách, không phải một bản ghi có thể khoá |

---

## Screen: chi-tiet-tai-lieu — Chi tiết tài liệu

### Wireframe (ASCII)

```text
┌──────────────┬─────────────────────────────────────────────────────┐
│ (danh sách   │ [1] Nội quy lao động 2024           [2]< Đóng >     │
│  mờ phía sau)│     (Nội bộ) (Sẵn sàng) (Còn hiệu lực)              │
│              │ [3][Xem trước đoạn] [4][Sửa metadata]               │
│              │ [5][Đổi nhãn mật]   [6][Gỡ]  [7][Xoá vĩnh viễn]     │
│              ├─────────────────────────────────────────────────────┤
│              │ [8] Thông tin chung                                 │
│              │     Phân loại: Nội quy      Số đoạn: 214            │
│              │     Ngày nạp: 01/09/2026    Kích thước: 1,2 MB      │
│              │     Người nạp: an.nv                                │
│              ├─────────────────────────────────────────────────────┤
│              │ [9] Vòng đời                                        │
│              │     Hiệu lực từ 01/01/2024, không có thời hạn       │
│              │ [10] Chuỗi thay thế                                 │
│              │       Nội quy lao động 2020  (đã bị thay thế)       │
│              │             ^                                       │
│              │       Nội quy lao động 2024  << đang hiệu lực >>    │
│              ├─────────────────────────────────────────────────────┤
│              │ [11] Quyền truy cập                                 │
│              │      Được phép (3)     [12][ + Thêm quyền ]         │
│              │        Nhân sự     (phòng ban)              (x)     │
│              │        Kế toán     (phòng ban)              (x)     │
│              │        an.nv       (gán trực tiếp)          (x)     │
│              │      Bị từ chối (1) [13][ + Thêm mục từ chối ]      │
│              │        cuong.lv    (gán trực tiếp)          (x)     │
│              │ [14] Tra cứu một người:                             │
│              │      [v: chọn người dùng    ]  -> kết quả và lý do  │
│              ├─────────────────────────────────────────────────────┤
│              │ [15](!) Nghi chồng lấn với 1 tài liệu   < Xử lý >   │
└──────────────┴─────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tên và ba thẻ trạng thái | Label | ReadOnly | • Nhãn mật · trạng thái xử lý · trạng thái hiệu lực, đều là thẻ **có chữ**.<br>• Ngăn kéo trượt từ phải, mở rộng thành trang riêng được khi cần chỗ. |
| 2 | Đóng | Link | Click | • Quay về danh sách. Tiếp cận: phím Esc. |
| 3 | Xem trước các đoạn | Button | Click | • Điều hướng: mở màn Xem trước đoạn. |
| 4 | Sửa metadata | Button | Click | • Mở màn Xác nhận metadata ở **chế độ chỉnh sửa** khi người xem có quyền sửa, **chế độ chỉ đọc** khi không.<br>• Trước 2026-09-23 màn ấy chỉ khai chế độ chỉ đọc khi mở lại — nghĩa là nút này hứa một hành vi mà màn không cho làm, trong khi `AC-009` của US-013 bắt buộc sửa được ngày hiệu lực sai. Đã sửa. |
| 5 | Đổi nhãn mật | Button | Click | • **Thêm 2026-09-23** để phục vụ `AC-005` của US-007 — tiêu chí đã duyệt nhưng trước đó không màn nào có chỗ làm.<br>• Nâng nhãn mật là thao tác **ảnh hưởng rộng**: hộp thoại xác nhận nêu **số người sẽ mất quyền đọc** trước khi bật nút xác nhận.<br>• Hạ nhãn mật cũng cần xác nhận, vì nó mở tài liệu ra cho nhiều người hơn. |
| 6 | Gỡ | Button | Click | • Loại tài liệu khỏi phạm vi trả lời nhưng **vẫn giữ trong kho để tra cứu** (`AC-006` của US-015).<br>• Sau khi gỡ, thân ngăn kéo chuyển **chỉ đọc** kèm dải thông báo, thời điểm gỡ, và nút Khôi phục. |
| 7 | Xoá vĩnh viễn | Button | Click | • Dành cho tài liệu nạp nhầm, không được phép lưu (`AC-007` của US-015).<br>• **Thao tác không hoàn tác được**, dùng hộp thoại xác nhận riêng chứ không phải hộp thoại thường.<br>• Hộp thoại nêu **hai con số cụ thể trước khi bật nút**: số người sẽ mất quyền đọc, và số trích dẫn đang trỏ tới tài liệu. Hai con số đứng riêng, chữ lớn, không lẫn vào đoạn văn giải thích.<br>• Sau khi xoá, hội thoại cũ có trích dẫn tới tài liệu này sẽ báo *tài liệu không còn trong hệ thống*, không phải trang trắng (`AC-008` của US-015). |
| 8 | Thông tin chung | Label | ReadOnly | • Phân loại · ngày nạp · kích thước · số đoạn · người nạp. |
| 9 | Vòng đời | Label | ReadOnly | • Bốn trường metadata ở dạng đọc. Phân biệt rõ *không có thời hạn* với *chưa xác định*. |
| 10 | Chuỗi thay thế | Label | Click | • Vẽ theo chiều dọc: bản trước ← bản này ← bản sau, **bản đang hiệu lực đánh dấu rõ**.<br>• **Chuỗi nhiều đời hiển thị đủ** (`AC-003` của US-015) — xem một bản bất kỳ thấy được cả chuỗi. Rút gọn chuỗi là làm mất đúng thứ người dùng cần.<br>• Mỗi bản trong chuỗi bấm được để mở tài liệu đó.<br>• States: ẩn khi tài liệu không nằm trong chuỗi nào. |
| 11 | Quyền truy cập | Table | ReadOnly | • **CỔNG CHẶN THỨ BA.** Tài liệu chưa gán quyền cho ai thì ở trạng thái sẵn sàng nhưng **không xuất hiện trong kết quả của bất kỳ ai** — mặc định là đóng, không phải mở.<br>• **Hai danh sách tách bạch**: được phép và bị từ chối.<br>• Mỗi mục ghi rõ chủ thể là người dùng hay phòng ban; quyền đến từ phòng ban hiển thị khác quyền gán trực tiếp bằng thẻ nhỏ — để quản trị biết **gỡ ở đâu**.<br>• Luật kết hợp: **từ chối cá nhân thắng cho phép theo phòng ban, không ngoại lệ** (`AC-001`, `AC-002` của story *Đặt quyền từ chối*, [[docs/access-control/userstories/us-006.md|access-control]]). Nhãn mật và danh sách cấp quyền là hai điều kiện **cùng phải thoả** (`AC-003` của US-007). |
| 12 | Thêm quyền | Button | Click | • Gán quyền đọc cho một người dùng hoặc một phòng ban. |
| 13 | Thêm mục từ chối | Button | Click | • **Tách riêng khỏi nút thêm quyền**, không gộp thành một nút rồi chọn kiểu trong hộp thoại — từ chối là thao tác nặng hơn hẳn và phải khó bấm nhầm hơn.<br>• Lỗi: đặt từ chối cho chủ thể không tồn tại. |
| 14 | Tra cứu một người | Dropdown | Select | • Chọn một người, hệ thống trả lời **đọc được hay không và vì sao**: được phép qua đường nào · bị mục từ chối nào chặn · hay bị nhãn mật chặn (`AC-005` của story *Đặt quyền từ chối*, [[docs/access-control/userstories/us-006.md|access-control]]).<br>• Đây là công cụ gỡ rối phân quyền — thiếu nó thì quản trị phải tự suy ra từ hai danh sách cộng với nhãn mật cộng với mức mật của người đó. |
| 15 | Cảnh báo chồng lấn | Label | Click | • Chỉ hiện khi tài liệu đang mang nhãn nghi chồng lấn.<br>• Nêu số tài liệu bị nghi, dẫn sang màn Xử lý cảnh báo chồng lấn.<br>• Dùng màu cảnh báo, không phải màu lỗi — chồng lấn không chặn tài liệu khỏi kho. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Bốn khối hiện đủ, cụm nút hoạt động bình thường |
| Đang tải | Ngăn kéo mở, khung xương chờ dữ liệu |
| Lỗi tải | Nói cái gì hỏng, cho thử lại, không đóng ngăn kéo |
| Chỉ đọc | Tài liệu đã gỡ — thân chuyển chỉ đọc kèm dải thông báo và nút Khôi phục |
| Vô hiệu hoá | Tài liệu đang xử lý — khối Quyền truy cập vô hiệu hoá **kèm lý do**, vì gán quyền cho tài liệu chưa lập chỉ mục xong là gán vào khoảng không |
| Đang gửi | Ở từng thao tác (thêm quyền, đổi nhãn mật, xoá), không phải cả màn |
| Rỗng | Không áp dụng — ngăn kéo luôn ứng với một tài liệu có thật |
| Một phần thành công | Không áp dụng — thao tác trên đúng một tài liệu |

---

## Screen: xem-truoc-doan — Xem trước các đoạn đã tách

Màn kiểm chất lượng tách đoạn. Không dùng hằng ngày, nhưng là màn quyết định **niềm tin của quản
trị vào kho** — và là công cụ để tác giả đối chiếu khi so sánh các chiến lược tách.

### Wireframe (ASCII)

```text
┌────────────────────────────────────────────────────────────────────┐
│ Xem trước các đoạn — Nội quy lao động 2024        [1]< Quay lại >  │
│ [2] Chiến lược: bám cấu trúc · 214 đoạn · 18 đoạn chứa bảng        │
│ [3](!) Một bảng không bóc được cấu trúc, đã về văn bản phẳng       │
├──────────────────────────────────┬─────────────────────────────────┤
│ [4] Tài liệu gốc                 │ [5] Đoạn đã tách                │
│ ──────────────────────────────── │ ─────────────────────────────── │
│ Chương VII. Thời giờ nghỉ ngơi   │ ┌─────────────────────────────┐ │
│                                  │ │ [6] Đoạn 112                │ │
│ Điều 113. Nghỉ hằng năm          │ │ Chương VII > Điều 113 >     │ │
│                                  │ │ Khoản 2                     │ │
│ 1. Người lao động làm việc đủ    │ │ "Cứ đủ 05 năm làm việc cho  │ │
│ 12 tháng cho một người sử dụng   │ │ một người sử dụng lao động  │ │
│ lao động thì được nghỉ hằng năm  │ │ thì số ngày nghỉ hằng năm.."│ │
│ 12 ngày làm việc.                │ └─────────────────────────────┘ │
│                                  │ ┌─────────────────────────────┐ │
│ [7] 2. Cứ đủ 05 năm làm việc     │ │ [8] Đoạn 113  (bảng)        │ │
│ cho một người sử dụng lao động   │ │ mảnh 2/4, đã lặp dòng tiêu  │ │
│ thì số ngày nghỉ hằng năm...     │ │ đề                          │ │
│                                  │ │  Thâm niên | Số ngày        │ │
│ Bảng 1. Số ngày nghỉ theo thâm   │ │  Đủ 5 năm  | 13             │ │
│ niên                             │ │  Đủ 10 năm | 14             │ │
│ ...                              │ └─────────────────────────────┘ │
└──────────────────────────────────┴─────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Quay lại | Link | Click | • Về ngăn kéo Chi tiết tài liệu, hoặc về danh sách nếu vào từ dấu suy giảm chất lượng. |
| 2 | Dải thông tin đầu | Label | ReadOnly | • **Chiến lược tách đang dùng** (`AC-006` của US-011 — đổi được bằng cấu hình, là biến thí nghiệm của bộ đánh giá) · tổng số đoạn · số đoạn chứa bảng.<br>• Người đọc cần biết mình đang xem kết quả của chiến lược nào, nếu không thì so sánh giữa các lần chạy vô nghĩa. |
| 3 | Cảnh báo suy giảm chất lượng | Label | ReadOnly | • Hai trường hợp: **bảng không bóc được cấu trúc** (về văn bản phẳng — `AC-004` của US-011) và **tài liệu không có cấu trúc rõ** (lùi về tách theo đoạn văn có chồng lấn, trích dẫn sẽ chỉ tới vị trí thay vì tới điều khoản).<br>• Là **cảnh báo, không phải lỗi** — tài liệu vẫn vào kho. Dùng màu cảnh báo, đặt thành dải trên đầu.<br>• Hệ thống **không âm thầm nạp**: đây là chỗ quản trị biết tài liệu này có nguy cơ trả lời sai. |
| 4 | Cột trái — tài liệu gốc | Label | ReadOnly | • Cuộn độc lập.<br>• Chọn một đoạn bên phải thì cột này **làm nổi bật đúng phần tương ứng**, và ngược lại. |
| 5 | Cột phải — danh sách đoạn | Table | Click | • Mỗi đoạn một thẻ có viền phân tách rõ, cuộn độc lập. |
| 6 | Thẻ đoạn | Label | Click | • Ba thông tin: số thứ tự · **đường dẫn phân cấp** · nội dung.<br>• Đường dẫn phân cấp là thứ biến trích dẫn từ "tên tệp" thành "đúng điều khoản" (`AC-002` của US-011). Điều bị chia thì ghi rõ phần thứ mấy (`AC-003`). |
| 7 | Vùng làm nổi bật | Label | ReadOnly | • Phần văn bản gốc tương ứng với đoạn đang chọn. |
| 8 | Đoạn dạng bảng | Table | ReadOnly | • Hiển thị **đúng dạng bảng kèm dòng tiêu đề**, không phải văn bản thuần.<br>• Mảnh của bảng dài ghi rõ **mảnh thứ mấy trên tổng và dòng tiêu đề đã được lặp** (`AC-002` của US-012 — chia tại ranh giới giữa hai hàng, không cắt ngang một hàng).<br>• **Đây là chỗ quản trị phát hiện bảng bị cắt hỏng trước khi người dùng nhận câu trả lời sai** — thiếu dấu hiệu này thì một mảnh bảng chỉ là dãy số không đầu không cuối.<br>• Bảng rộng cuộn ngang trong thẻ của nó. |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Hai cột đầy dữ liệu, chọn đoạn thì hai bên đồng bộ |
| Cảnh báo suy giảm chất lượng | Dải cảnh báo trên đầu, không phải lỗi |
| Đang tải | Khung xương giữ hình hai cột |
| Lỗi tải | Nói cái gì hỏng, cho thử lại |
| Chỉ đọc | Cả màn vốn chỉ đọc theo bản chất — không có thao tác ghi nào |
| Rỗng | Không áp dụng — màn chỉ mở với tài liệu đã tách đoạn xong; tách không được thì đã dừng ở trạng thái lỗi từ trước |
| Vô hiệu hoá · Đang gửi · Một phần thành công | Không áp dụng — không có control nào khoá có điều kiện và không có thao tác gửi dữ liệu |

---

## Open Questions

- [ ] OQ-1: Danh mục phân loại tài liệu gồm những mục cụ thể nào?
- [ ] OQ-2: Đóng ngăn kéo Chi tiết tài liệu lúc đang có thay đổi chưa lưu thì cảnh báo hay bỏ thẳng?
- [ ] OQ-3: Hộp thoại xoá vĩnh viễn có ngưỡng cảnh báo mạnh hơn khi số người mất quyền lớn không,
      hay chỉ hiện số thô?
- [ ] OQ-4: Lỗi lập chỉ mục xảy ra **sau** khi đã xác nhận metadata thì tài liệu vào thẻ *nạp lỗi*
      và thử lại được mà không phải xác nhận metadata lại — đúng không? Chưa tiêu chí nào chốt.
