# Flow: Nạp tài liệu vào kho

> Màn hình thuộc flow này: `tai-tai-lieu-len` → `xac-nhan-metadata`. Luồng đi qua
> `danh-sach-tai-lieu` (điểm khởi phát) và kết ở khối quyền của `chi-tiet-tai-lieu` — hai màn ấy vẽ
> ở luồng `quan-ly-kho-tai-lieu`. Flow tổng xem `../srs/doc-ingestion-userflow.md` Mục 1.
>
> Khung vẽ máy bàn 1024 điểm ảnh. Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.
>
> **Ba cổng chặn nằm trên luồng này**, không có đường nào lách: nhãn mật (màn tải lên) → xác nhận
> metadata (màn này) → gán quyền (màn chi tiết tài liệu).

---

## Screen: tai-tai-lieu-len — Tải tài liệu lên

### Wireframe (ASCII)

```text
┌────────────────────────────────────────────────────────────────────┐
│ Tải tài liệu lên                                    [1]< Đóng >    │
├────────────────────────────────────────────────────────────────────┤
│    ┌───────────────────────────────────────────────────────┐       │
│    │ [2]        Kéo thả tệp vào đây                        │       │
│    │            hoặc [ Chọn tệp ]                          │       │
│    │  docx · pdf · txt · Markdown, tối đa 50MB mỗi tệp     │       │
│    └───────────────────────────────────────────────────────┘       │
│                                                                    │
│ [3] Tệp đã chọn (4)                                                │
│   Noi-quy-lao-dong-2024.docx       1,2 MB            [4](x)        │
│   Quy-che-luong-2025.pdf           8,4 MB               (x)        │
│   Bang-luong-Q3.xlsx               0,3 MB               (x)        │
│   [5](!) sai định dạng - chỉ nhận docx, pdf, txt, Markdown         │
│   Tai-lieu-scan.pdf               62,0 MB               (x)        │
│   [6](!) vượt giới hạn 50MB - tệp này 62,0 MB                      │
│ ────────────────────────────────────────────────────────────────── │
│ [7] Nhãn mật *              [8] Phân loại *                        │
│ [v: -- chọn --        ]     [v: -- chọn --        ]                │
│                                                                    │
│ [9](i) Chọn nhãn mật rồi mới nạp được                              │
│                        [ Huỷ ]    [10][ Xác nhận nạp 2 tệp ]       │
└────────────────────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Đóng | Link | Click | • Hộp thoại giữa màn, **không phải trang riêng** — người dùng không rời khỏi ngữ cảnh danh sách.<br>• Đóng trước khi xác nhận thì không tệp nào được nạp.<br>• Tiếp cận: phím Esc đóng hộp thoại. |
| 2 | Vùng kéo thả | File-upload | Upload | • Nhận một hoặc nhiều tệp cùng lúc.<br>• **Hai cách nhập**: kéo thả, hoặc nút chọn tệp cho người không quen kéo thả.<br>• Bốn định dạng nhận được: docx · pdf · txt · Markdown (`AC-002` của US-009). Giới hạn 50MB mỗi tệp (`AC-005` của US-009, `NFR-system-013`).<br>• Ghi sẵn cả hai ràng buộc ngay trong vùng kéo thả, để người dùng biết trước khi chọn nhầm.<br>• States: mặc định · đang rê tệp lên vùng · đang đọc tệp. |
| 3 | Danh sách tệp đã chọn | Table | ReadOnly | • Xem trước từng tệp **trước khi tải lên thật**, gỡ tệp không muốn nạp.<br>• Mỗi dòng: tên tệp · dung lượng · nút gỡ.<br>• Số trong tiêu đề là **tổng số tệp đã chọn**, khác với số tệp sẽ nạp thật ở nút [10] — tệp lỗi không tính vào. |
| 4 | Gỡ tệp | Button | Click | • Bỏ một tệp khỏi đợt mà không phải chọn lại từ đầu. |
| 5 | Lỗi sai định dạng | Label | ReadOnly | • Đánh dấu **ngay tại dòng, trước khi tải lên**, kèm lý do và danh sách bốn định dạng nhận được.<br>• Tệp này **không tính vào số tệp sẽ nạp** — không phải lỗi giữa chừng mà là loại ngay từ đầu.<br>• Câu chữ chính xác chưa chốt — xem OQ-2. |
| 6 | Lỗi vượt dung lượng | Label | ReadOnly | • Từ chối **trước khi tải lên**, nêu cả giới hạn lẫn **kích thước thật của tệp** (`NFR-system-013`) — chỉ nói "quá lớn" thì người dùng không biết phải cắt bao nhiêu.<br>• Cũng không tính vào số tệp sẽ nạp. |
| 7 | Nhãn mật | Dropdown | Select | • **CỔNG CHẶN THỨ NHẤT.** Đặt tầng chặn theo mức mật ngay từ lúc nạp, độc lập với danh sách cấp quyền trên từng tài liệu.<br>• **Bắt buộc, và không có giá trị mặc định** (`AC-001` của US-007). Ba giá trị: công khai < nội bộ < mật.<br>• Không có nhãn mặc định ngầm là có chủ đích: mặc định *công khai* thì một lần quên là lộ tài liệu mật cho toàn công ty; mặc định *mật* thì mọi tài liệu vào kho đều không ai đọc được và người ta sẽ học cách bấm qua.<br>• Áp chung cho **cả đợt** — mỗi đợt nạp một nhãn mật.<br>• States: chưa chọn (nút xác nhận vô hiệu hoá) · đã chọn.<br>• **Cổng này phải chặn ở giao diện, không chỉ ở máy chủ.** |
| 8 | Phân loại | Dropdown | Select | • **Bắt buộc**. Phân loại quyết định phạm vi so sánh chồng lấn về sau — chỉ tài liệu cùng phân loại mới đem so với nhau.<br>• Danh mục phân loại cụ thể **chưa chốt** — xem OQ-3. |
| 9 | Lý do nút bị khoá | Label | ReadOnly | • Nút xám không giải thích là lỗi thiết kế. Khi nút [10] còn vô hiệu hoá, dòng này nói rõ còn thiếu gì.<br>• Cũng hiện khi trỏ vào nút bị khoá.<br>• States: ẩn khi đã đủ điều kiện. |
| 10 | Xác nhận nạp | Button | Click | • Nhãn nút nêu **số tệp sẽ nạp thật**, đã trừ tệp lỗi — để người dùng thấy ngay là hai tệp kia bị loại.<br>• States: vô hiệu hoá khi chưa chọn nhãn mật · đang gửi (chặn gửi trùng).<br>• Điều hướng: hộp thoại đóng, danh sách phía sau hiện các dòng mới ở trạng thái **đang xử lý với tiến độ theo bước** — bóc văn bản, tách đoạn, lập chỉ mục, kèm số đoạn đã xử lý trên tổng (`AC-008` của US-009).<br>• **Đóng trình duyệt không dừng việc xử lý** (`AC-007` của US-009) — người quản trị nạp xong đi làm việc khác được. |

**Nhánh trùng tài liệu.** Tệp trùng nội dung với tài liệu đã có trong kho thì hệ thống hỏi rõ tại
dòng đó: *bỏ qua* hay *coi là bản thay thế* (`AC-006` của US-009). Hai lựa chọn, **không có mặc
định** — hệ thống không tạo bản sao thứ hai một cách im lặng. Chọn *bản thay thế* thì chuyển sang
luồng thay thế của US-015 và ghi vào chuỗi thay thế.

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Vùng kéo thả trống, danh sách tệp trống, hai ô bắt buộc chưa chọn |
| Lỗi từng tệp | Đánh dấu tại dòng, **không chặn các tệp hợp lệ khác trong cùng đợt** |
| Trùng tài liệu đã có | Hỏi bỏ qua hay coi là bản thay thế, không có mặc định |
| Vô hiệu hoá | Nút xác nhận khoá khi chưa chọn nhãn mật, kèm lý do ở [9] |
| Đang gửi | Nút vào trạng thái chờ, chặn gửi trùng |
| Một phần thành công | Đợt nhiều tệp có cả thành công lẫn lỗi — dải tổng kết hiện ở màn danh sách sau khi hộp thoại đóng |
| Đang tải | Không áp dụng — hộp thoại không tải dữ liệu từ máy chủ lúc mở |
| Rỗng | Không áp dụng — đây là hộp thoại nhập liệu, không có danh sách nào để rỗng |
| Chỉ đọc · Không đủ quyền | Không áp dụng — chỉ mở được từ nút trên màn đã qua cổng kiểm quyền |

---

## Screen: xac-nhan-metadata — Xác nhận metadata vòng đời

**Ba chế độ**, khác nhau ở quyền ghi chứ không khác bố cục: *xác nhận lần đầu* (vào từ thẻ chờ xác
nhận, có nút Xác nhận) · *chỉnh sửa* (vào từ nút **Sửa metadata** ở màn Chi tiết tài liệu, có nút
Lưu, sửa xong có hiệu lực ngay ở câu hỏi kế tiếp theo `AC-009` của US-013) · *chỉ đọc* (người xem
không có quyền sửa, không nút ghi nào). Khung dưới vẽ chế độ xác nhận lần đầu.

### Wireframe (ASCII)

```text
┌─────────────────────────────────┬──────────────────────────────────┐
│ [1] Nội quy lao động 2024       │ [2] Xác nhận metadata vòng đời   │
│ ─────────────────────────────── │ ──────────────────────────────── │
│ Điều 1. Phạm vi áp dụng         │ [3] Ngày hiệu lực *              │
│                                 │ [ 01/01/2024              ]      │
│ Nội quy này có hiệu lực kể từ   │ [4] trích từ tài liệu:           │
│ ngày 01 tháng 01 năm 2024 và    │ < "có hiệu lực kể từ ngày 01     │
│ thay thế Nội quy ban hành năm   │   tháng 01 năm 2024" >           │
│ 2020.                           │                                  │
│ [5] (đoạn trên đang được làm    │ [6] Ngày hết hiệu lực            │
│      nổi bật ở cột này)         │ [ (trống)                 ]      │
│                                 │ [ ] Không có thời hạn            │
│ Điều 2. Đối tượng áp dụng       │ [7](i) không tìm thấy trong tài  │
│                                 │     liệu - hệ thống không đoán   │
│ Nội quy áp dụng cho toàn thể    │                                  │
│ người lao động...               │ [8] Thay thế tài liệu nào        │
│                                 │ [ Nội quy lao động 2020    v]    │
│                                 │ [4] trích: "thay thế Nội quy     │
│                                 │     ban hành năm 2020"           │
│                                 │                                  │
│                                 │ [9] Phân loại *                  │
│                                 │ [v: Nội quy               ]      │
│                                 │ ──────────────────────────────── │
│ (cuộn đọc toàn văn)             │ [10][        Xác nhận        ]   │
└─────────────────────────────────┴──────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Cột trái — nội dung tài liệu | Label | ReadOnly | • Cho người xác nhận **đối chiếu trực tiếp với văn bản thật, không xác nhận mù**. Đây là lý do có chủ đích của bố cục hai cột — metadata vòng đời sai nguy hiểm hơn không trả lời, vì câu trả lời sai vẫn *có vẻ đúng và có nguồn*.<br>• Cuộn độc lập với cột phải.<br>• Bấm vào một đoạn trích ở cột phải thì cột này **cuộn tới và làm nổi bật** đúng chỗ ấy. |
| 2 | Tiêu đề | Label | ReadOnly | • Đổi theo chế độ: *Xác nhận metadata vòng đời* · *Sửa metadata* · *Xem metadata*. |
| 3 | Ngày hiệu lực | Textbox | Text | • Mốc tài liệu bắt đầu có giá trị.<br>• **Bắt buộc**. Hệ thống điền sẵn khi tìm được căn cứ (`AC-001` của US-013).<br>• Người xác nhận **sửa được mọi giá trị hệ thống đề xuất** (`AC-003` của US-013).<br>• Lỗi: ngày hết hiệu lực sớm hơn ngày này → từ chối lưu, nêu rõ mâu thuẫn.<br>• States: máy điền · **đã sửa** (đánh dấu riêng, phân biệt với trường máy điền) · trống chưa tìm thấy · lỗi hợp lệ. |
| 4 | Trích đoạn làm căn cứ | Link | Click | • **Phần tử quan trọng nhất của màn, không phải chú thích phụ.** Mỗi trường máy điền đều kèm đoạn nguyên văn làm căn cứ — đây là thứ biến việc xác nhận từ "tin máy" thành "kiểm được".<br>• Bấm vào thì cột trái cuộn tới và làm nổi bật đúng chỗ.<br>• States: ẩn khi trường không có căn cứ. |
| 5 | Vùng làm nổi bật | Label | ReadOnly | • Đoạn đang được trích dẫn, làm nổi bật bằng nền nhạt và vạch dọc bên trái. |
| 6 | Ngày hết hiệu lực | Textbox | Text | • Không bắt buộc.<br>• **Ba trạng thái hiển thị phải phân biệt được**: có ngày cụ thể · *không có thời hạn* (lựa chọn tường minh, `AC-005` của US-013) · *chưa xác định* (không tìm thấy căn cứ, `AC-004`). Hai giá trị sau là **hai thứ khác nhau** — gộp lại là mất thông tin: một bên nghĩa là văn bản vô thời hạn, một bên nghĩa là chưa ai biết.<br>• Lỗi: sớm hơn ngày hiệu lực → từ chối lưu. |
| 7 | Ghi chú không tìm thấy | Label | ReadOnly | • Trường không có căn cứ thì **để trống kèm ghi chú, hệ thống không đoán** (`AC-004` của US-013, nhánh 6a của use case).<br>• Đây là chỗ dễ bị "cải tiến" hỏng nhất: điền đại một ngày hợp lý trông thân thiện hơn, nhưng nó biến một ô trống thành một dữ kiện sai không ai kiểm. |
| 8 | Thay thế tài liệu nào | Dropdown | Select | • Ô tìm kiếm trong kho. Hệ thống tự đề xuất được thì điền sẵn kèm căn cứ như các trường khác (`AC-006` của US-013).<br>• Lỗi: quan hệ thay thế tạo thành **vòng lặp** → từ chối lưu và nêu rõ vòng lặp (`AC-004` của US-015). Vòng lặp nhiều đời không phát hiện được bằng ràng buộc cơ sở dữ liệu, nên phải kiểm lúc ghi bằng cách đi ngược cả chuỗi.<br>• Xác nhận xong thì tài liệu cũ chuyển sang trạng thái *đã bị thay thế*. |
| 9 | Phân loại | Dropdown | Select | • **Bắt buộc**. Quyết định phạm vi so sánh chồng lấn ở bước sau. |
| 10 | Xác nhận | Button | Click | • **CỔNG CHẶN THỨ HAI.** Chưa xác nhận thì tài liệu dừng ở trạng thái *chờ xác nhận metadata* và **không vào phạm vi truy hồi** (`AC-002` của US-013, nhánh 8a của use case). Không có đường nào đưa tài liệu vào kho mà bỏ qua bước này.<br>• Nhãn nút đổi theo chế độ: *Xác nhận* · *Lưu* · ẩn hẳn ở chế độ chỉ đọc.<br>• States: mặc định · vô hiệu hoá khi dữ liệu chưa hợp lệ · đang gửi.<br>• Điều hướng: xác nhận xong, hệ thống so chồng lấn với tài liệu còn hiệu lực cùng phân loại, sinh biểu diễn ngữ nghĩa, lập chỉ mục, rồi chuyển tài liệu sang trạng thái *sẵn sàng*; màn đóng và quay về danh sách.<br>• Ở chế độ chỉnh sửa, thay đổi **có hiệu lực ngay ở câu hỏi kế tiếp**, không phải nạp lại tài liệu (`AC-009` của US-013). |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Mặc định | Biểu mẫu điền sẵn, trường nào có căn cứ thì kèm trích đoạn |
| Đã sửa | Đánh dấu riêng trường người sửa khác trường máy điền — để người duyệt sau biết chỗ nào là phán đoán của người |
| Chỉnh sửa | Vào từ nút Sửa metadata, nút ghi đổi nhãn thành *Lưu*, có hiệu lực ngay ở câu hỏi kế tiếp |
| Chỉ đọc | Người xem không có quyền sửa — mọi ô ở dạng đọc, không nút ghi nào |
| Lỗi hợp lệ | Ngày hết hiệu lực sớm hơn ngày hiệu lực; quan hệ thay thế tạo vòng lặp — từ chối lưu, nêu rõ mâu thuẫn |
| Đang gửi | Nút vào trạng thái chờ |
| Đang tải | Không áp dụng — màn chỉ mở sau khi bóc văn bản xong, dữ liệu đã sẵn |
| Rỗng | Không áp dụng — luôn ứng với đúng một tài liệu cụ thể |
| Một phần thành công | Không áp dụng — xử lý một tài liệu mỗi lần |

---

## Open Questions

- [ ] OQ-1: Tệp bị **bỏ qua vì trùng** tính vào cột nào của dải tổng kết đợt nạp? `AC-005` của
      US-010 chỉ có hai cột thành công và lỗi.
- [ ] OQ-2: Câu chữ chính xác cho năm thông báo lỗi nạp: vượt 50MB · sai định dạng · tệp đặt mật
      khẩu · tệp hỏng cấu trúc · tài liệu rỗng hoặc quá ngắn kèm số ký tự bóc được.
- [ ] OQ-3: Danh mục phân loại tài liệu gồm những mục cụ thể nào? Câu hỏi mở của US-013 đề xuất để
      quản trị tự định nghĩa thay vì cài cứng.
