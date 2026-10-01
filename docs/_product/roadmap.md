---
type: roadmap
status: draft
updated: 2026-09-23
format: sprint
links:
  - docs/_product/prd.md
  - docs/_shared/nfr.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Lộ trình thực hiện

## 1. Ưu tiên MoSCoW — toàn bộ 36 story

**Ưu tiên ở đây là thứ tự làm.** Quyết định *không cắt phạm vi* ngày 2026-09-09 **đã được thay** bởi
`ADR-0004` ngày 2026-09-24: **có cắt, và cắt ngay** thay vì đợi cơ chế cảnh báo kích hoạt vào tháng
11. Danh sách cắt cụ thể chưa chốt — xem Open Questions của bản ghi quyết định ấy. Ý nghĩa ba mức:

- **Must** — thiếu nó thì sản phẩm không dùng được, hoặc chương thực nghiệm không có số liệu.
- **Should** — sản phẩm chạy được khi thiếu, nhưng thiếu thì có chỗ khó chịu thấy rõ.
- **Could** — hạng mục vận hành, không ảnh hưởng luận điểm nào của đồ án.

| Mã | Story | Nhóm | Mức | Vì sao ở mức này |
|---|---|---|---|---|
| US-001 | Đăng nhập và đăng xuất | access-control | Must | Không có danh tính thì không lọc được quyền, mà lọc quyền quyết định kết quả truy hồi |
| US-002 | Tạo tài khoản bằng tay | access-control | Must | Đường duy nhất đưa người dùng vào hệ thống |
| US-003 | Nhập hàng loạt tài khoản | access-control | Should | Tạo tay vẫn dùng được; đây là rào cản triển khai chứ không phải rào cản kỹ thuật |
| US-004 | Phòng ban và thành viên | access-control | Must | Gán quyền từng người trên 1.000 tài liệu là không khả thi |
| US-005 | Gán quyền đọc tài liệu | access-control | Must | Cơ chế cấp quyền chính |
| US-006 | Quyền từ chối | access-control | Must | Không có nó thì mọi ngoại lệ đều phải phá cách gán theo phòng ban |
| US-007 | Nhãn mật và mức mật | access-control | Must | Tầng chặn không phụ thuộc trí nhớ người gán |
| US-008 | Lọc quyền tại bước truy hồi | access-control | Must | Nơi luật đọc được thi hành; thiếu nó thì ba story trên chỉ là cấu hình trên giấy |
| US-009 | Nạp bốn định dạng | doc-ingestion | Must | Không có tài liệu thì không có gì để hỏi |
| US-010 | Báo lỗi nạp có lý do | doc-ingestion | Must | Trên kho thật một tỉ lệ tệp sẽ hỏng; không nói lý do thì người quản trị bế tắc |
| US-011 | Tách đoạn theo cấu trúc | doc-ingestion | Must | Quyết định gần như toàn bộ chất lượng truy hồi, và là biến thí nghiệm |
| US-012 | Giữ nguyên bảng biểu | doc-ingestion | Must | 30% kho là tài liệu nhiều bảng — hỏng đúng nhóm người dùng cần nhất |
| US-013 | Xác nhận metadata vòng đời | doc-ingestion | Must | Không có nó thì hệ thống tự tin trích dẫn quy định đã bãi bỏ |
| US-014 | Cảnh báo chồng lấn chủ đề | doc-ingestion | Should | Chuỗi thay thế bắt được phần lớn; đây là phần còn lại |
| US-015 | Gỡ và thay thế tài liệu | doc-ingestion | Must | Văn bản không biến mất, chúng bị thay thế |
| US-016 | Danh sách và bộ lọc tài liệu | doc-ingestion | Should | Không thêm năng lực mới, nhưng làm những năng lực đã có dùng được ở quy mô 1.000 |
| US-017 | Hỏi một lượt có trích dẫn | qa-chat | Must | Story trung tâm của sản phẩm |
| US-018 | Câu trả lời hiện dần | qa-chat | Must | Trên GPU 6GB, chờ trắng màn hình hàng chục giây là hỏng trải nghiệm |
| US-019 | Mở tài liệu gốc từ trích dẫn | qa-chat | Should | Trích dẫn không mở được là lời hứa suông, nhưng câu trả lời vẫn dùng được |
| US-020 | Hỏi tiếp nối | qa-chat | Must | Không viết lại câu rút gọn thì truy hồi ngẫu nhiên rồi sinh ra câu trả lời có trích dẫn mà sai |
| US-021 | Tóm tắt cuộn hội thoại dài | qa-chat | Should | Hội thoại ngắn vẫn chạy tốt khi thiếu |
| US-022 | Phân rã câu hỏi phức tạp | qa-chat | Must | Đây là thứ chứng minh kiến trúc agent hơn hỏi đáp một lượt |
| US-023 | Từ chối tầng một | qa-chat | Must | Luận điểm chống bịa đáp án |
| US-024 | Từ chối tầng hai | qa-chat | Must | Chặn kiểu bịa nguy hiểm nhất: ý sai nằm giữa các ý đúng, có trích dẫn đàng hoàng |
| US-025 | Cảnh báo hết hiệu lực và chồng lấn | qa-chat | Should | Đúng trích dẫn nhưng sai áp dụng; cần US-013 và US-014 làm nền |
| US-026 | Xem lại lịch sử trong phiên | qa-chat | Must | Điều kiện để người dùng hỏi tiếp có ý thức |
| US-027 | Ghi lại câu hỏi bị từ chối | knowledge-gap | Should | Không có nó thì mỗi lần từ chối là một tín hiệu bị vứt đi |
| US-028 | Báo cáo khoảng trống | knowledge-gap | Should | Biến sản phẩm từ tra cứu thụ động thành vòng lặp cải thiện |
| US-029 | Bộ câu hỏi và đáp án chuẩn | evaluation | Must | Tài sản đắt nhất của chương thực nghiệm |
| US-030 | Bộ đo tầng truy hồi | evaluation | Must | Vừa là bằng chứng, vừa là **công cụ ra quyết định** chọn chiến lược tách đoạn |
| US-031 | Bộ đo tầng agent | evaluation | Must | Trả lời câu hỏi hội đồng chắc chắn sẽ hỏi |
| US-032 | Chấm và đối chiếu tay 20% | evaluation | Must | Con số đồng thuận máy-người là thứ bảo vệ cách chấm |
| US-033 | Xuất bảng kết quả | evaluation | Must | Chép số bằng tay là nguồn sai sót không ai phát hiện được |
| US-034 | Khởi động bằng một lệnh | deployment | Must | Mô hình bán hàng là mỗi doanh nghiệp một bản; buổi bảo vệ cũng là một lần cài trên máy lạ |
| US-035 | Chặn khởi động khi thiếu GPU | deployment | Must | Thà không chạy còn hơn chạy chậm rồi bị đánh giá là sản phẩm tồi |
| US-036 | Sao lưu và phục hồi | deployment | Could | Hạng mục vận hành, không chạm luận điểm nào của đồ án |

**Cộng: 27 Must · 8 Should · 1 Could.**

## 2. Bản đồ phụ thuộc

Bốn chuỗi phụ thuộc thật, phần còn lại làm song song được:

```
US-001 ─→ US-002 ─→ US-004 ─→ US-005 ─┬─→ US-006 ─┐
                                       └─→ US-007 ─┴─→ US-008 ──┐
                                                                 ↓
US-009 ─→ US-011 ─→ US-012 ────────────────────────────────→ US-017 ─→ US-018
   └────→ US-010                                                 │
   └────→ US-013 ─┬─→ US-014 ─→ US-025                           ├─→ US-019
                  └─→ US-015                                     ├─→ US-020 ─→ US-021
                                                                 ├─→ US-022
                                                                 ├─→ US-023 ─→ US-024 ─→ US-027 ─→ US-028
                                                                 └─→ US-026

US-029 ─→ US-030 (cần US-011, US-012)
      └─→ US-031 (cần US-022, US-023, US-024) ─→ US-032 ─→ US-033

US-034 ─→ US-035
      └─→ US-036
```

Ba điều bản đồ này nói ra mà bảng MoSCoW không nói:

- **US-008 là nút cổ chai**. Bảy story phân quyền hội tụ vào nó, và nó lại là điều kiện của mọi
  story hỏi đáp. Làm muộn thì phải sửa lại đường truy hồi đã viết.
- **US-030 phải có trước khi chốt cách tách đoạn**, không phải sau. Nó là công cụ để chọn, không
  chỉ là công cụ để báo cáo. Đây là lý do nó nằm ở sprint 2 chứ không phải sprint cuối.
- **US-029 không phụ thuộc dòng code nào** — soạn được từ tháng 9. Đây là ~25 giờ có thể rút ra
  khỏi quỹ lập trình mà không cắt gì.

## 3. Quỹ thời gian — số học trung thực

| Khối | Ước lượng |
|---|---|
| Nền giao diện: hệ thị giác, bộ thành phần, khung ứng dụng một trang | 20h |
| `access-control` (8 story, gồm 6 màn) | 24h |
| `doc-ingestion` (8 story, gồm 6 màn) | 34h |
| `qa-chat` (10 story, gồm 2 màn) | 40h |
| `knowledge-gap` (2 story, 1 màn) | 8h |
| `evaluation` (5 story, chạy bằng lệnh) | 22h |
| `deployment` (3 story) | 14h |
| Tích hợp, gỡ lỗi, đệm | 20h |
| **Cộng khối lập trình** | **~182h** |
| Chạy thực nghiệm, hiệu chỉnh ngưỡng, chấm và đối chiếu tay | ~30h |
| Bộ 150 câu ground truth (**việc tài liệu, không phải lập trình**) | ~25h |

Từ 05/10 đến 20/12 là **11 tuần**. Ở nhịp 14h/tuần thì quỹ là **154h**, trong khi nhu cầu là
**~212h** — thiếu khoảng **58h**.

**Bảng trên là nguồn duy nhất cho quỹ giờ** (`ADR-0004`). `prd.md` Mục 9 trỏ về đây thay vì nêu số
riêng — trước 2026-09-24 nó nêu một con số khác, viết cùng ngày, kèm một chiến lược ngược lại.

**Ba chỗ lệch trong chính bảng này, chưa sửa xong — đừng dùng số sprint để lập kế hoạch cho tới khi
sửa:** sáu sprint ở Mục 4 cộng lại **198h** chứ không phải 212h · khối *tích hợp, gỡ lỗi, đệm 20h*
không nằm trong sprint nào · phần việc của sprint 6 không nằm trong 212h. Nghĩa là tải thật mỗi
sprint **cao hơn con số đang ghi**.

Khoảng cách này đóng bằng hai đòn bẩy, cộng với việc cắt phạm vi theo `ADR-0004`:

1. **Rút ~25h ra khỏi quỹ lập trình** bằng cách soạn bộ 150 câu trong khoảng 20/09–04/10, song song
   lúc hoàn thiện đề cương. Nó là việc đọc và viết, không cần một dòng code nào tồn tại.
2. **Nâng nhịp lên khoảng 18–19h/tuần** trong 11 tuần đó. Đây là con số thật, nói ra để anh quyết
   chứ không để phát hiện vào tháng 11.

Nếu cả hai đòn bẩy đều không khả thi thì đòn bẩy thứ ba là chuyển vài hạng mục sang **sau bảo vệ** —
xem mục 5. Đó là lựa chọn của anh, không phải của tôi, nhưng nó tồn tại và tôi không giấu nó đi.

## 4. Sáu sprint

Ba nguyên tắc xếp sprint, theo đúng yêu cầu đã đặt: sprint đầu ra được đường chạy xuyên suốt thô ·
tầng truy hồi hoàn thiện trước vòng agent · hai tuần cuối không tính năng mới.

### Sprint 1 — Đường chạy xuyên suốt thô · 05/10 – 18/10 · ~28h

Mục tiêu: **thả một tệp vào, hỏi một câu, ra câu trả lời có trích dẫn.** Xấu cũng được, thiếu cũng
được, nhưng phải chạy hết chuỗi.

Làm: khung dự án và tệp cấu hình triển khai bốn dịch vụ (US-034 mức thô) · khung ứng dụng một trang
và nền hệ thị giác · US-001 đăng nhập · US-009 nạp một định dạng docx · US-011 tách đoạn mức cơ bản
· US-017 hỏi một lượt trả lời kèm trích dẫn, chưa lọc quyền.

**Xong nghĩa là:** trên máy sạch, một lệnh khởi động được hệ thống; nạp một tệp docx; đăng nhập; hỏi
một câu; nhận câu trả lời có ít nhất một trích dẫn trỏ tới đúng đoạn. Đo được thời gian tới chữ đầu
tiên, dù chưa cần đạt `NFR-system-001`.

**Rủi ro sprint này gánh:** đây là lúc `ASM-project-004` được trả lời — mô hình có chạy nổi trong
ngưỡng độ trễ trên GPU 6GB không. Sai thì mọi con số ở mục 8 của PRD phải đặt lại, và biết ở tuần 2
còn xoay được.

### Sprint 2 — Tầng truy hồi và bộ đo · 19/10 – 01/11 · ~34h

Mục tiêu: **truy hồi đạt chất lượng đo được, và có công cụ để đo.**

Làm: US-011 đầy đủ (bám cấu trúc, đường dẫn phân cấp) · US-012 bảng biểu · tách từ tiếng Việt chạy
trong máy ảo Java · ba nhánh tìm kiếm bật tắt bằng cấu hình · US-029 nạp bộ câu hỏi · US-030 bộ đo
tầng truy hồi · US-010 báo lỗi nạp.

**Xong nghĩa là:** một lệnh chạy ra bảng so sánh ba phương pháp tìm kiếm nhân ít nhất hai chiến
lược tách đoạn trên bộ 100 câu, có độ chính xác@k, độ phủ@k và thứ hạng đúng trung bình. Chiến lược
tách đoạn được **chọn bằng số liệu**, không bằng cảm tính.

### Sprint 3 — Phân quyền và vòng đời tài liệu · 02/11 – 15/11 · ~40h

Mục tiêu: **hệ thống an toàn để đưa cho người thật dùng.**

Làm: US-002 đến US-008 (nút cổ chai của bản đồ phụ thuộc) · US-013 xác nhận metadata · US-015 thay
thế và gỡ · US-016 danh sách và bộ lọc · các màn quản trị tương ứng.

**Xong nghĩa là:** hai người khác quyền hỏi cùng một câu nhận kết quả khác nhau, và không đoạn nào
ngoài quyền lọt vào câu trả lời hay vào trích dẫn. Chạy lại bộ đo truy hồi với tài khoản có phạm vi
quyền hẹp dưới 5% kho, độ phủ@10 **không thấp hơn** khi chạy toàn kho — đây là kiểm chứng của
`AC-006` trong US-008, và là chỗ đánh đổi kỹ thuật của pre-filter được xác nhận hay bị bác.

### Sprint 4 — Vòng agent đầy đủ và giao diện hoàn chỉnh · 16/11 – 29/11 · ~52h

Mục tiêu: **khoá tính năng.** Sprint nặng nhất, và là sprint cuối cùng có tính năng mới.

Làm: US-018 hiện dần · US-019 mở tài liệu gốc · US-020 hỏi tiếp nối · US-021 tóm tắt cuộn · US-022
phân rã · US-023 và US-024 hai tầng từ chối · US-025 cảnh báo · US-026 lịch sử phiên · US-014 chồng
lấn · US-027 và US-028 khoảng trống · US-031 và US-032 bộ đo tầng agent · US-035 chặn thiếu GPU ·
US-036 sao lưu · hoàn thiện toàn bộ giao diện theo hệ thị giác.

**Xong nghĩa là:** cả 36 story đạt acceptance criteria; ma trận thí nghiệm chạy được cả ba cấu hình
bóc tách; mọi màn phủ đủ trạng thái theo `NFR-system-039`. Sau ngày 29/11 **không thêm tính năng**.

**Cảnh báo sẵn:** sprint này ước lượng 52h trong 2 tuần. Ở nhịp 14h/tuần thì nó cần gần 4 tuần. Đây
là chỗ khoảng cách 58h ở mục 3 sẽ lộ ra rõ nhất, và là chỗ cần quyết sớm nhất.

### Sprint 5 — Thực nghiệm và số liệu · 30/11 – 13/12 · ~30h

**Không tính năng mới.** Chỉ chạy, đo, hiệu chỉnh, phân tích.

Làm: chạy toàn bộ ma trận trên cả hai kho thử nghiệm · hiệu chỉnh ngưỡng từ chối (OQ-2) bằng cách
quét dải giá trị · hiệu chỉnh ngưỡng chồng lấn (OQ-3) · chấm bộ 50 câu ba cấu hình · chấm tay 20%
mẫu và tính mức đồng thuận · US-033 xuất bảng · phân tích các câu trả lời sai.

**Xong nghĩa là:** đủ bảng số liệu cho chương thực nghiệm, mỗi bảng mang theo điều kiện sinh ra nó
theo `NFR-system-046`. Hai câu hỏi mở OQ-2 và OQ-3 được đóng bằng số đo, và số đó ghi ngược vào
`docs/_shared/nfr.md`.

### Sprint 6 — Báo cáo và đóng gói bàn giao · 14/12 – 20/12 · ~14h

Làm: viết chương thực nghiệm từ bảng đã có · tài liệu cài đặt và vận hành · kiểm lần cuối điều kiện
`NFR-system-024` bằng cách ngắt mạng chạy trọn một vòng · đóng gói bản bàn giao.

**Xong nghĩa là:** báo cáo nộp được ngày 21/12.

## 5. Nếu buộc phải hoãn — thứ tự và lý do

Danh sách này **không phải kế hoạch**, nó là phương án dự phòng để không phải quyết vội vào tháng 11.
Nếu tới cuối sprint 3 mà tiến độ lệch quá hai tuần, chuyển sang sau bảo vệ theo đúng thứ tự dưới:

| Thứ tự | Hoãn gì | Tiết kiệm | Vì sao hoãn được |
|---|---|---|---|
| 1 | US-036 sao lưu | 5h | Hạng mục vận hành, không chạm luận điểm nào |
| 2 | US-003 nhập hàng loạt tài khoản | 4h | Tạo tay vẫn dùng được ở quy mô demo |
| 3 | US-016 bộ lọc danh sách tài liệu | 3h | Kho demo vài trăm tài liệu vẫn xem được |
| 4 | US-021 tóm tắt cuộn | 5h | Hội thoại demo hiếm khi đủ dài để vượt cửa sổ ngữ cảnh |
| 5 | US-027 và US-028 báo cáo khoảng trống | 7h | Không nằm trong hai hạng mục đánh giá của đồ án |
| 6 | US-025 cảnh báo chồng lấn khi trả lời | 3h | Phụ thuộc mục 7 |
| 7 | US-014 phát hiện chồng lấn lúc nạp | 8h | Product Owner chọn tường minh — chỉ đụng khi đã cạn mọi mục trên |

**Không bao giờ hoãn:** nhóm `evaluation` (5 story) và US-023, US-024. Nhóm đầu là chương thực
nghiệm; hai story sau là luận điểm chống bịa đáp án. Hoãn chúng là hoãn chính thứ được chấm.

**Đã gỡ khỏi phạm vi, không phải hoãn — bốn thao tác hàng loạt** (chốt 2026-09-23, thu hẹp
2026-09-24). Đổi phòng ban, đổi mức mật, vô hiệu hoá nhiều tài khoản cùng lúc; gỡ và đổi phân loại
nhiều tài liệu cùng lúc. Bốn thao tác này **chưa bao giờ có tiêu chí chấp nhận** — chúng sinh ra ở
khâu thiết kế giao diện, không từ yêu cầu nào được duyệt, và bị phát hiện khi đối chiếu hai chiều
story với màn hình lúc vẽ wireframe.

> **Đính chính 2026-09-24.** Bản ngày 23/09 gộp nhầm *gán quyền hàng loạt trên tài liệu* vào danh
> sách này. `AC-005` của US-005 đòi thao tác ấy tường minh — chọn 20 tài liệu vừa nạp rồi cấp quyền
> cho một phòng ban trong một lần. Nó **ở lại trong phạm vi** và đã được vẽ lại. Thao tác hàng loạt cũng là loại dễ sai quyền nhất và không chứng minh thêm điều gì cho
báo cáo. Sửa từng bản ghi vẫn dùng được ở quy mô demo. Muốn đưa lại vào phạm vi thì mở change
request bổ sung tiêu chí trước, đừng vẽ trước rồi hợp thức hoá sau.

## 6. Cảnh báo sớm

Ba dấu hiệu nghĩa là mốc 29/11 đang bị đe doạ, kèm việc phải làm ngay khi thấy:

| Dấu hiệu | Thấy khi nào | Làm gì ngay |
|---|---|---|
| Cuối sprint 1 chưa chạy được hết chuỗi thả-file-tới-câu-trả-lời | 18/10 | Dừng mọi việc khác, đây là rủi ro kiến trúc chứ không phải chậm tiến độ |
| Cuối sprint 2 chưa có bảng so sánh ba phương pháp tìm kiếm | 01/11 | Chiến lược tách đoạn sẽ phải chọn bằng cảm tính; báo GVHD và cân nhắc thu hẹp ma trận thí nghiệm |
| Cuối sprint 3 lệch quá hai tuần | 15/11 | Kích hoạt danh sách hoãn ở mục 5, cắt từ trên xuống, quyết một lần thay vì cắt dần |

Ngoài ba dấu hiệu trên, hai giả định `ASM-project-001` và `ASM-project-002` có hạn **20/09** — trước
cả sprint 1. Chúng không phải rủi ro tiến độ mà là rủi ro **toàn bộ chương thực nghiệm bị chấm theo
tiêu chí khác với tiêu chí đã lập kế hoạch**.
