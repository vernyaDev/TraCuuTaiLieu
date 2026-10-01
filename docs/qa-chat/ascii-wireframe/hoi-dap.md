# Flow: Hỏi đáp có trích dẫn

> Màn hình thuộc flow này: `hoi-dap` → `xem-tai-lieu-goc`. Flow tổng xem
> `../srs/qa-chat-userflow.md` Mục 1. Khung vẽ máy bàn 1024 điểm ảnh.
>
> Sản phẩm đơn ngữ (tiếng Việt) — localization không áp dụng.

---

## Screen: hoi-dap — Hỏi đáp

### Wireframe (ASCII)

```text
┌────────────────────────────────────────────────────────────────────┐
│ Tra cứu tri thức nội bộ     [1][ + Phiên mới ] [2][ Khu quản trị ] │
│                                   Nguyễn Văn A   [3]< Đăng xuất >  │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│                     ┌──────────────────────────────────────┐ [4]   │
│                     │ Một năm tôi được nghỉ phép bao nhiêu │       │
│                     │ ngày?                                │       │
│                     └──────────────────────────────────────┘       │
│                                                                    │
│ [5] (info) Đang tìm tài liệu liên quan...                          │
│                                                                    │
│ [6] Theo quy định hiện hành, người lao động làm việc đủ 12 tháng   │
│     được nghỉ hằng năm 12 ngày làm việc, hưởng nguyên lương.       │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ (!) Cảnh báo                                              [7]  │ │
│ │ Văn bản căn cứ đã hết hiệu lực từ 01/01/2026.                  │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ [8] Nguồn                                                          │
│   < Nội quy lao động 2024 > Chương VII > Điều 113 > Khoản 2   [9]  │
│   < Quy chế nhân sự 2025 > Mục 4 > Điểm b        (hết hiệu lực)    │
│                                                                    │
│ [10] v Hệ thống đã hiểu câu hỏi của bạn là                         │
│ [11] v Các bước đã thực hiện (3 truy vấn con)                      │
│ [12] [ Sao chép kèm nguồn ]                                        │
│                                                                    │
│                                   (cuộn lên xem các lượt trước)    │
├────────────────────────────────────────────────────────────────────┤
│ [13] Câu hỏi của bạn                                               │
│ [__________________________________________________________]  [14] │
│ Enter để gửi, Shift+Enter xuống dòng              [15][ Gửi ]      │
└────────────────────────────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Phiên mới | Button | Click | • Kết thúc hội thoại hiện tại và bắt đầu phiên sạch.<br>• Bối cảnh cũ **không** theo sang và không ảnh hưởng kết quả truy hồi của phiên mới (`AC-006` của US-021 và US-026).<br>• States: mặc định · vô hiệu hoá khi vùng hội thoại đang rỗng.<br>• Điều hướng: ở lại màn này, vùng hội thoại về trạng thái rỗng lần đầu.<br>• Hội thoại cũ **không mở lại được** — hội thoại chỉ sống trong phiên (ghi chú phạm vi của story index, ADR-0002). |
| 2 | Khu quản trị | Button | Click | • Lối vào duy nhất từ màn nhân viên sang khu quản trị.<br>• **Chỉ hiện với người có quyền quản trị**; nhân viên thường không thấy nút này.<br>• Điều hướng: màn Danh sách tài liệu của `doc-ingestion` — ra ngoài phạm vi flow.<br>• Bảo mật: ẩn nút không thay cho kiểm quyền ở máy chủ; người gõ thẳng đường dẫn vẫn phải nhận *không đủ quyền*. |
| 3 | Đăng xuất | Link | Click | • Kết thúc phiên làm việc, quay về màn Đăng nhập.<br>• Nút quay lại của trình duyệt **không** đưa trở vào màn này (`AC-004` của US-001).<br>• States: mặc định · đang gửi. |
| 4 | Lượt hỏi của người dùng | Label | ReadOnly | • Hiện nguyên văn câu hỏi đã gửi, canh phải, khối nền nhạt hẹp hơn vùng trả lời.<br>• Xếp theo thời gian, cũ trên mới dưới; tự cuộn xuống lượt mới nhất.<br>• Lượt cũ giữ nguyên văn kể cả khi hệ thống đã nén chúng lại làm bối cảnh bên trong (`AC-002` của US-026). |
| 5 | Chỉ báo tiến trình | Label | ReadOnly | • Nói hệ thống đang ở bước nào bằng lời: *đang hiểu câu hỏi* → *đang tìm tài liệu liên quan* → *đang soạn câu trả lời*. Ba bước này là ba module agent, không phải vòng xoay trang trí (`AC-002` của US-018).<br>• Biến mất khi chữ đầu tiên của câu trả lời xuất hiện.<br>• States: chỉ tồn tại ở trạng thái đang tải.<br>• Lỗi: quá ngưỡng chờ mà chưa sinh được chữ nào → dừng lượt, báo hệ thống đang bận, mời thử lại (`AC-007` của US-018). Ngưỡng cụ thể là câu hỏi mở của US-018, chưa chốt. |
| 6 | Câu trả lời | Label | ReadOnly | • Hiện **dần từng chữ** theo tiến độ sinh, không chờ trọn câu (`AC-001` của US-018). Canh trái, trải hết bề rộng đọc được, không khung bao.<br>• Tiếng Việt; giữ nguyên thuật ngữ tiếng Anh khi đó là cách gọi chuẩn trong tài liệu nguồn (`AC-009` của US-017).<br>• States: đang sinh · hoàn tất · **chưa hoàn tất** khi người dùng bấm dừng, phần đã hiện giữ nguyên kèm dấu hiệu (`AC-004` của US-018, `AC-005` của US-026) · mất kết nối giữa chừng thì giữ phần đã nhận, không xoá trắng (`AC-005` của US-018).<br>• Trả lời một phần: câu nhiều vế mà một số vế thiếu căn cứ thì nêu rõ vế nào thiếu ngay trong câu trả lời, không im lặng bỏ vế.<br>• Từ chối vì không đủ căn cứ: thay câu trả lời bằng khối từ chối — không tô màu lỗi, giọng bình thường. Khối này nêu **cùng một bộ khả năng, không đổi theo dữ liệu thật**: kho có thể chưa có thông tin, hoặc người hỏi có thể chưa được cấp quyền trên tài liệu liên quan; kèm gợi ý bước tiếp (`AC-003`, `AC-004` của US-023).<br>• Từ chối vì câu hỏi **ngoài phạm vi kho tri thức** là khối riêng, được nói thẳng (`AC-003b` của US-023) — nó không tiết lộ gì về nội dung kho hay về quyền.<br>• Bảo mật — điểm dễ trượt nhất của cả màn: **giao diện không được cho người hỏi biết khả năng nào mới là lý do thật.** Nếu câu *"có thể chưa được cấp quyền"* chỉ hiện khi thật sự có tài liệu bị chặn thì nó thôi là một khả năng và thành một câu khẳng định — lúc đó người hỏi biết có tồn tại tài liệu phù hợp ngoài quyền mình, đúng thứ `AC-001` của US-008 và mục 1 của `api-contract.md` cấm. Lý do thật vẫn được ghi chính xác vào dữ liệu khoảng trống tri thức, nơi chỉ quản trị đọc. |
| 7 | Khối cảnh báo | Label | ReadOnly | • Chỉ hiện khi có; không sinh cảnh báo lấy lệ làm loãng tín hiệu (`AC-005` của US-025).<br>• Ba loại: nguồn đã hết hiệu lực từ ngày nào · nguồn đã bị thay thế bởi văn bản nào · hai văn bản chồng lấn cùng còn hiệu lực, nêu cả hai và chỉ chỗ khác nhau (`AC-001` đến `AC-003` của US-025).<br>• Tách bạch khỏi câu trả lời bằng nền và biểu tượng, dùng **màu cảnh báo chứ không phải màu lỗi**; đặt sau câu trả lời để không lấn át nội dung (`AC-004` của US-025).<br>• Tiếp cận: nghĩa cảnh báo không truyền chỉ bằng màu — luôn có nhãn chữ và biểu tượng. |
| 8 | Khối nguồn | Label | ReadOnly | • Hiện **sau khi câu trả lời sinh xong**, vì danh sách nguồn chỉ chốt được lúc đó (`AC-003` của US-018).<br>• Liệt kê đủ các nguồn thực sự được dùng, không cắt theo số cố định — câu hỏi mở của US-017 đề xuất như vậy.<br>• Nhiều nguồn cho một câu trả lời thì thấy được ý nào dựa trên nguồn nào (`AC-004` của US-017). |
| 9 | Một dòng nguồn | Link | Click | • Mỗi nguồn một dòng: tên tài liệu + **đường dẫn phân cấp tới đoạn** (vd *Chương VII › Điều 113 › Khoản 2*), không phải chỉ tên tài liệu hay số trang (`AC-003` của US-017).<br>• Thẻ trạng thái hiệu lực chỉ hiện khi khác bình thường.<br>• Điều hướng: mở `xem-tai-lieu-goc` tại đúng đoạn đó.<br>• Trích dẫn của lượt cũ vẫn mở được, sau khi hệ thống **kiểm lại quyền hiện tại** (`AC-003` của US-026). |
| 10 | Hệ thống đã hiểu câu hỏi của bạn là | Link | Click | • Khối mở rộng được, **thu gọn mặc định**, chỉ hiện với câu hỏi tiếp nối.<br>• Mở ra thấy câu đã được viết lại thành câu độc lập — đây là chỗ người dùng phát hiện hệ thống hiểu lệch trước khi tin vào câu trả lời.<br>• States: ẩn (lượt không phải tiếp nối) · thu gọn · mở · **cảnh báo** khi việc viết lại thất bại, lúc đó hệ thống chạy trên câu gốc và phải nói rõ là không chắc đã hiểu đúng ngữ cảnh. |
| 11 | Các bước đã thực hiện | Link | Click | • Khối mở rộng được, thu gọn mặc định, chỉ hiện với câu hỏi phức tạp nhiều bước.<br>• Mở ra thấy danh sách truy vấn con theo thứ tự đã chạy, mỗi truy vấn kèm số nguồn tìm được.<br>• Người dùng thấy hệ thống hiểu sai thì diễn đạt lại câu hỏi — đây là đường phản hồi duy nhất của bước phân rã. |
| 12 | Sao chép kèm nguồn | Button | Click | • Sao chép nội dung câu trả lời **kèm thông tin nguồn**, để câu trả lời không bị tách khỏi căn cứ khi ra khỏi hệ thống (`AC-007` của US-026).<br>• States: mặc định · đã sao chép (phản hồi ngắn tại chỗ) · vô hiệu hoá khi lượt đang sinh dở hoặc là lượt bị từ chối. |
| 13 | Nhãn ô nhập | Label | ReadOnly | • Nhãn thật đặt trên ô, không chỉ dùng chữ mờ gợi ý bên trong ô.<br>• Tiếp cận: nhãn gắn với ô nhập để trình đọc màn hình đọc đúng. |
| 14 | Ô nhập câu hỏi | Textarea | Text | • **Bắt buộc** có nội dung mới gửi được. Nhiều dòng, tự giãn theo nội dung tới một trần rồi cuộn trong ô. Neo cố định dưới màn, luôn nhìn thấy dù cuộn lên bao xa.<br>• Enter gửi, Shift kèm Enter xuống dòng — quy ước ghi ngay dưới ô bằng chữ phụ.<br>• Validation: ô rỗng thì nút gửi vô hiệu hoá. Câu **gửi được nhưng không đủ nghĩa** thì hệ thống không gọi xử lý mà đề nghị diễn đạt rõ hơn (`AC-008` của US-017); trường hợp này **không** tạo bản ghi khoảng trống tri thức.<br>• States: mặc định · đang gõ · đang có lượt chạy dở (vẫn gõ được nhưng chưa gửi được) · phiên hết hạn.<br>• Tiếp cận: đưa được tiêu điểm bằng bàn phím, thứ tự tab theo thứ tự đọc. |
| 15 | Gửi | Button | Click | • Gửi câu hỏi, khởi động một lượt mới.<br>• **Vô hiệu hoá** khi ô rỗng và khi đang có lượt chạy dở, kèm giải thích ngắn khi trỏ vào — để hai lượt không chồng lên nhau (`AC-006` của US-018).<br>• Trong lúc câu trả lời đang hiện dần, nút **chuyển thành nút dừng**; bấm dừng thì việc sinh dừng lại và hỏi câu mới được ngay (`AC-004` của US-018).<br>• Lỗi: hệ thống đang bận thì lượt vào hàng đợi kèm thông báo vị trí (`NFR-system-008` đến `NFR-system-011`); phiên hết hạn sau 8 giờ không thao tác thì đưa về Đăng nhập, có ghi nhớ trang đang mở. |

**Trạng thái của cả màn** (mục 6 của hệ thống thị giác):

| Trạng thái | Thể hiện |
|---|---|
| Rỗng lần đầu | Lời chào ngắn, **ba câu hỏi mẫu bấm được** lấy từ chính kho của tổ chức, một dòng giải thích hệ thống chỉ trả lời từ tài liệu nội bộ |
| Quay lại sau khi đóng trình duyệt | Nói rõ hội thoại trước đã kết thúc và đang bắt đầu phiên mới, thay vì màn trống không giải thích (`AC-008` của US-026) |
| Đang tải | Chỉ báo tiến trình theo bước [5], không phải vòng xoay |
| Đang gửi | Nút gửi thành nút dừng; ô nhập vẫn gõ được nhưng chưa gửi được |
| Lỗi | Ba loại tách bạch: mất kết nối giữa chừng (giữ phần đã nhận) · hệ thống đang bận · phiên hết hạn (đưa về đăng nhập) |
| Không đủ quyền | Không xuất hiện ở màn này dưới dạng thông báo quyền — nó hiện ra dưới dạng lời từ chối ở [6] |
| Chỉ đọc / Vô hiệu hoá | Không áp dụng — màn này không có chế độ chỉ đọc |

---

## Screen: xem-tai-lieu-goc — Xem tài liệu gốc

### Wireframe (ASCII)

```text
┌──────────────────────┬─────────────────────────────────────────────┐
│ (hội thoại mờ phía   │ [1] Nội quy lao động 2024                   │
│  sau, không thao tác │ [2] Chương VII > Điều 113 > Khoản 2         │
│  được)               │ [3](Mật: Nội bộ)  [4](Còn hiệu lực)         │
│                      │ [5][ Tải tệp gốc ]          [6]< Đóng >     │
│                      ├─────────────────────────────────────────────┤
│                      │ Điều 112. Nghỉ lễ, tết                      │
│                      │ ... nội dung trước đoạn được trích ...      │
│                      │                                             │
│                      │ │ Điều 113. Nghỉ hằng năm             [7]   │
│                      │ │ 1. Người lao động làm việc đủ 12 tháng    │
│                      │ │    cho một người sử dụng lao động thì     │
│                      │ │    được nghỉ hằng năm 12 ngày làm việc.   │
│                      │                                             │
│                      │ [8] Bảng 1 - Số ngày nghỉ theo thâm niên    │
│                      │  Thâm niên | Số ngày                        │
│                      │  Đủ 5 năm  | 13                             │
│                      │  (mảnh 2/4, đã lặp dòng tiêu đề)            │
│                      │                                             │
│                      │ ... nội dung sau đoạn được trích ...        │
│                      │ (cuộn để đọc ngữ cảnh trước và sau)         │
└──────────────────────┴─────────────────────────────────────────────┘
```

### Screen description

| # | Items | Control type | Data type | Description |
|---|-------|--------------|-----------|--------------|
| 1 | Tên tài liệu | Label | ReadOnly | • Cho biết đang đọc văn bản nào — người dùng mở ngăn kéo từ một dòng nguồn nên cần xác nhận đúng tài liệu.<br>• Ngăn kéo trượt từ phải, chiếm khoảng hai phần ba bề ngang; hội thoại vẫn thấy mờ phía sau để không mất ngữ cảnh. |
| 2 | Đường dẫn phân cấp | Label | ReadOnly | • Vị trí của đoạn đang xem trong cấu trúc văn bản (*Chương › Điều › Khoản*).<br>• Đây là thứ phân biệt trích dẫn dùng được với trích dẫn chỉ ghi tên tệp (`AC-003` của US-017). |
| 3 | Thẻ nhãn mật | Label | ReadOnly | • Cho người đọc biết mức mật của tài liệu trước khi họ sao chép hay chia sẻ nội dung.<br>• Thẻ có chữ, không truyền nghĩa chỉ bằng màu. |
| 4 | Thẻ trạng thái hiệu lực | Label | ReadOnly | • Ba giá trị phân biệt bằng chữ: còn hiệu lực · đã hết hiệu lực · chưa xác định.<br>• Tài liệu đã bị thay thế thì nêu rõ bị thay bởi văn bản nào (`AC-002` của US-025). |
| 5 | Tải tệp gốc | Button | Click | • Dành cho lúc người dùng cần bản gốc để gửi cho người khác hoặc in (`AC-005` của US-019).<br>• **Kiểm quyền lần nữa trước khi trả tệp** — không dựa vào việc ngăn kéo đã mở được.<br>• States: mặc định · đang chuẩn bị tệp · lỗi không đọc được tệp gốc. |
| 6 | Đóng | Link | Click | • Điều hướng: quay về đúng vị trí trong hội thoại, câu hỏi và câu trả lời còn nguyên (`AC-006` của US-019).<br>• Có ở mọi trạng thái, kể cả khi ngăn kéo chỉ hiện thông báo không còn quyền — để màn không bao giờ cụt.<br>• Tiếp cận: phím Esc đóng ngăn kéo. |
| 7 | Đoạn được trích | Label | ReadOnly | • Làm nổi bật bằng nền nhạt và một vạch dọc bên trái; màn hình **tự cuộn tới nó khi mở** (`AC-001` của US-019).<br>• Cuộn tự do lên xuống đọc được phần trước và sau trong cùng tài liệu, không bị giới hạn ở mỗi đoạn trích (`AC-002` của US-019). |
| 8 | Đoạn là bảng | Label | ReadOnly | • Hiển thị **đúng dạng bảng kèm dòng tiêu đề**, kể cả khi đoạn được trích chỉ là một mảnh của bảng dài (`AC-007` của US-019).<br>• Ghi rõ đây là mảnh thứ mấy trên tổng và dòng tiêu đề đã được lặp lại — thiếu dấu hiệu này người đọc nhìn thấy một dãy số không đầu không cuối.<br>• Bảng rộng cuộn ngang trong khung của nó, không làm cả trang cuộn ngang (mục 8 của hệ thống thị giác). |

**Trạng thái của cả màn:**

| Trạng thái | Thể hiện |
|---|---|
| Đang tải | Khung xương giữ hình dạng trang tài liệu |
| Lỗi tải | Nói rõ không đọc được tệp và cho thử lại ngay tại chỗ, không đóng ngăn kéo |
| Không đủ quyền | Quyền bị thu hồi **sau khi** trích dẫn đã hiện: ngăn kéo mở ra nhưng chỉ có thông báo không còn quyền truy cập, không hiện nội dung nào (`AC-003` của US-019) |
| Tài liệu đã gỡ | Báo tài liệu không còn dùng làm căn cứ, **phân biệt rõ với đã xoá** (`AC-004` của US-019) |
| Tài liệu đã xoá vĩnh viễn | Báo tài liệu không còn trong hệ thống |
| Rỗng | Không áp dụng — ngăn kéo luôn mở từ một trích dẫn cụ thể |
| Chỉ đọc | Cả màn là chỉ đọc theo thiết kế; không có thao tác ghi nào |

---

## Ghi chú cho bước sau

**Hai phần tử dưới đây có trong story nhưng chưa có trong mô tả màn của wireframe index** — đã vẽ
vào đây, cần bổ sung ngược khi cập nhật index:

- Nút **Sao chép kèm nguồn** [12] — `AC-007` của US-026.
- Thông báo **quay lại sau khi đóng trình duyệt** — `AC-008` của US-026.

Không phần tử nào trên hai màn này nằm ngoài phạm vi story đã duyệt, nên không có scope creep cần
mở CR.
