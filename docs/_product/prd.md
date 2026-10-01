---
type: prd-product
status: draft
updated: 2026-09-24
links:
  - docs/_shared/project-profile.md
  - docs/decisions/0001-methodology-shape.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Hệ thống tra cứu tri thức nội bộ doanh nghiệp — Product Requirements Document

## 1. One-line Pitch

Nhân viên hỏi bằng tiếng Việt tự nhiên trên kho tài liệu nội bộ và nhận câu trả lời kèm trích dẫn
kiểm chứng được, toàn bộ chạy trong mạng của tổ chức, không câu nào được bịa.

## 2. Problem & Why Now

- **Vấn đề cốt lõi:** kho tài liệu nội bộ nằm rải rác dạng docx/pdf/txt/Markdown; tìm kiếm từ khoá
  thất bại vì lệch từ vựng ("nghỉ phép" trong câu hỏi, "nghỉ hằng năm" trong quy định). Câu hỏi
  thực tế lại thường nhiều bước: "phép năm chưa dùng có chuyển sang năm sau không, nghỉ việc giữa
  năm thì tính sao?".
- **Ai đang đau:** nhân viên mất thời gian hỏi vòng qua đồng nghiệp hoặc bộ phận nhân sự; người
  giữ tri thức trả lời lặp lại cùng một câu hỏi.
- **Giải pháp thay thế hiện tại:** tìm kiếm từ khoá của hệ thống lưu trữ, hỏi người, hoặc mở từng
  tài liệu đọc tay.
- **Why now:** mô hình ngôn ngữ đủ tốt đã chạy được trên một GPU phổ thông, nên lần đầu tiên có
  thể đặt toàn bộ năng lực hỏi đáp **bên trong mạng nội bộ** — điều mà mọi dịch vụ đám mây không
  đáp ứng được với tài liệu nhạy cảm.

## 3. Target Users

| Nhóm người dùng | Vai trò | Job-to-be-done | Bối cảnh sử dụng |
|---|---|---|---|
| Nhân viên | primary | Tìm câu trả lời đúng cho một tình huống công việc cụ thể, và biết nó dựa trên văn bản nào | Trong giờ làm, xen giữa việc khác, cần kết quả trong vài chục giây |
| Quản trị tri thức | primary | Đưa tài liệu vào kho, giữ cho nó đúng hiệu lực, biết kho đang thiếu gì | Theo đợt, khi có văn bản mới ban hành |
| Quản trị hệ thống | secondary | Cài đặt, cấu hình, sao lưu bản triển khai | Một lần lúc cài, sau đó định kỳ |

Ai KHÔNG phải người dùng: khách hàng bên ngoài doanh nghiệp; người dùng ẩn danh không có tài khoản.

## 4. Value Proposition & Differentiator

- **Giá trị cốt lõi:** trả lời đúng, có nguồn kiểm chứng được, và thà im lặng còn hơn bịa.
- **Vì sao chọn mình thay vì giải pháp cũ:** tìm kiếm từ khoá không hiểu câu hỏi; hỏi người thì
  chậm và không nhất quán; công cụ đám mây thì dữ liệu rời khỏi tổ chức.
- **Lợi thế khác biệt:** mỗi doanh nghiệp một bản độc lập, toàn bộ kể cả mô hình ngôn ngữ chạy
  trong mạng nội bộ, chi phí hạ tầng bằng không sau khi cài.

## 5. Goals & Non-Goals

**Mục tiêu:**
- Trả lời được câu hỏi tiếng Việt một lượt, tiếp nối, và phức tạp nhiều bước, luôn kèm trích dẫn.
- Từ chối có kiểm soát khi không đủ căn cứ, và đo được tỉ lệ từ chối đúng và từ chối sai.
- Phân quyền đọc theo từng tài liệu, áp ngay tại bước truy hồi.
- Đóng gói cài được bằng một lệnh trên máy có GPU.
- Có bộ đánh giá định lượng hai tầng chạy được bằng lệnh, không chấm tay.

**Ngoài phạm vi (đã chốt, không đưa vào backlog):** huấn luyện hay tinh chỉnh mô hình · nhận dạng
ký tự cho tài liệu scan hoặc ảnh · audio và video · nhiều tổ chức trên một bản cài · đăng nhập
một lần qua SSO/LDAP · kết nối đồng bộ tự động với SharePoint/Drive · thanh toán · hồ sơ người
dùng bền vững qua nhiều phiên.

## 6. Capability Themes

| Theme | Mô tả | Features thuộc theme |
|---|---|---|
| Tra cứu | Mọi thứ nhân viên chạm vào khi đi tìm câu trả lời | `qa-chat` |
| Quản trị tri thức | Đưa tài liệu vào kho và giữ nó đúng | `doc-ingestion`, `knowledge-gap` |
| Kiểm soát truy cập | Ai được đọc gì | `access-control` |
| Nghiên cứu và đánh giá | Bằng chứng định lượng cho chương thực nghiệm | `evaluation` |
| Vận hành | Cài đặt và duy trì bản triển khai | `deployment` |

## 7. Feature Map

### Luồng người dùng tổng quan

1. Quản trị hệ thống cài bản triển khai bằng một lệnh → `deployment`
2. Quản trị tri thức tạo tài khoản, phòng ban, nạp tài liệu và xác nhận metadata → `access-control`, `doc-ingestion`
3. Nhân viên đặt câu hỏi, nhận câu trả lời có trích dẫn, mở tài liệu gốc → `qa-chat`
4. Nhân viên hỏi tiếp nối hoặc hỏi câu phức tạp nhiều bước → `qa-chat`
5. Hệ thống từ chối khi không đủ căn cứ và ghi lại câu hỏi đó → `qa-chat`, `knowledge-gap`
6. Quản trị tri thức đọc báo cáo khoảng trống và bổ sung tài liệu → `knowledge-gap`
7. Tác giả chạy bộ đo và lấy số liệu cho báo cáo → `evaluation`

### Bảng Feature Map

| # | Tính năng | Slug | Theme | Persona | Ưu tiên | Phụ thuộc | Chi tiết hoá |
|---|---|---|---|---|---|---|---|
| 1 | Hỏi đáp có trích dẫn | `qa-chat` | Tra cứu | Nhân viên | Must | 2, 3 | ⬜ chưa |
| 2 | Nạp và quản lý vòng đời tài liệu | `doc-ingestion` | Quản trị tri thức | Quản trị tri thức | Must | 3 | ⬜ chưa |
| 3 | Tài khoản và phân quyền | `access-control` | Kiểm soát truy cập | Quản trị tri thức | Must | — | ⬜ chưa |
| 4 | Bộ đánh giá định lượng | `evaluation` | Nghiên cứu và đánh giá | Tác giả đồ án | Must | 1, 2 | ⬜ chưa |
| 5 | Đóng gói on-premise | `deployment` | Vận hành | Quản trị hệ thống | Must | — | ⬜ chưa |
| 6 | Báo cáo khoảng trống tri thức | `knowledge-gap` | Quản trị tri thức | Quản trị tri thức | Should | 1 | ⬜ chưa |

### 7.1 Hỏi đáp có trích dẫn — `qa-chat`

Màn hình nhân viên dùng hằng ngày: gõ câu hỏi tiếng Việt, nhận câu trả lời hiện dần từng chữ kèm
danh sách nguồn bấm mở được. Đây là nơi bảy module agent thể hiện ra ngoài.

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-qa-chat-01` | Hỏi một lượt, nhận câu trả lời kèm trích dẫn về đúng đoạn trong tài liệu | Must |
| `CAP-qa-chat-02` | Hỏi tiếp nối trong cùng phiên; câu hỏi được viết lại thành câu độc lập trước khi truy hồi | Must |
| `CAP-qa-chat-03` | Câu hỏi phức tạp được phân rã thành các truy vấn con có thứ tự rồi tổng hợp | Must |
| `CAP-qa-chat-04` | Từ chối hai tầng khi không đủ căn cứ, nói rõ lý do thay vì trả lời bừa | Must |
| `CAP-qa-chat-05` | Cảnh báo khi nguồn trích dẫn đã hết hiệu lực hoặc có văn bản chồng lấn còn hiệu lực | Should |
| `CAP-qa-chat-06` | Mở tài liệu gốc từ trích dẫn, nhảy tới đúng vị trí đoạn | Should |

**Phạm vi v1:** giao diện đầy đủ cho mọi thao tác của ba nhóm người dùng, câu trả lời hiện dần,
lịch sử hội thoại trong phiên. Chất lượng giao diện là **yêu cầu**, không phải phần dư — xem mục 9.
**Chưa làm:** người dùng đánh giá câu trả lời; chia sẻ hội thoại; xuất câu trả lời ra tệp.

**Rủi ro chính:** độ trễ trên GPU 6GB khi có nhiều người hỏi cùng lúc.
**Đo thành công:** `CAP-evaluation-02`.

### 7.2 Nạp và quản lý vòng đời tài liệu — `doc-ingestion`

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-doc-ingestion-01` | Nạp docx, pdf, txt, Markdown; báo tiến độ và lỗi từng tệp | Must |
| `CAP-doc-ingestion-02` | Tách đoạn theo cấu trúc tài liệu, giữ nguyên bảng biểu, lặp dòng tiêu đề khi phải chia bảng | Must |
| `CAP-doc-ingestion-03` | Metadata vòng đời (hiệu lực từ, hiệu lực đến, thay thế tài liệu nào, phân loại) — hệ thống điền sẵn, người xác nhận | Must |
| `CAP-doc-ingestion-04` | Gắn nhãn nghi ngờ chồng lấn chủ đề lúc nạp, chờ người xác nhận | Should |
| `CAP-doc-ingestion-05` | Gỡ tài liệu hoặc thay bằng bản mới, giữ nguyên chuỗi thay thế | Must |

**Phạm vi v1:** tải tệp lên qua giao diện, tối đa 50MB một tệp, mục tiêu kho khoảng 1.000 tài liệu.
**Chưa làm:** đồng bộ tự động từ thư mục mạng; nhận dạng ký tự cho tài liệu scan.

**Rủi ro chính:** 30% kho là tài liệu nhiều bảng biểu — tách sai làm hỏng đúng nhóm quan trọng nhất.
**Đo thành công:** `CAP-evaluation-01` so sánh các chiến lược tách đoạn.

### 7.3 Tài khoản và phân quyền — `access-control`

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-access-control-01` | Tạo tài khoản bằng tay và nhập hàng loạt từ tệp danh sách | Must |
| `CAP-access-control-02` | Quản lý phòng ban và thành viên | Must |
| `CAP-access-control-03` | Gán quyền đọc theo tài liệu cho người dùng hoặc phòng ban, có hiệu lực từ chối | Must |
| `CAP-access-control-04` | Áp quyền ngay tại bước truy hồi, không lọc sau khi đã xếp hạng | Must |
| `CAP-access-control-05` | Nhãn mật của tài liệu và mức mật của người dùng | Must |

**Phạm vi v1:** tài khoản cục bộ, mật khẩu đặt trong hệ thống.
**Chưa làm:** đăng nhập một lần; đồng bộ danh bạ người dùng của doanh nghiệp.

**Rủi ro chính:** lọc quyền trước khi xếp hạng làm tụt độ phủ của truy hồi xấp xỉ.
**Đo thành công:** kịch bản kiểm thử riêng — cùng một câu hỏi, hai người khác quyền, kết quả khác
nhau và không rò đoạn nào ngoài quyền.

### 7.4 Bộ đánh giá định lượng — `evaluation`

Hạng mục quyết định điểm số của đồ án, ưu tiên ngang tính năng.

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-evaluation-01` | Đo tầng truy hồi trên 2 kho thử nghiệm với 100 câu: độ chính xác@k, độ phủ@k, thứ hạng đúng trung bình; so sánh các chiến lược tách đoạn nhân với 3 phương pháp tìm kiếm | Must |
| `CAP-evaluation-02` | Đo tầng agent trên 50 câu phức tạp (gồm 10 câu không có đáp án trong kho); thí nghiệm bóc tách: agent đầy đủ, một lượt, bỏ Planner | Must |
| `CAP-evaluation-03` | Quản lý bộ câu hỏi và đáp án chuẩn dưới dạng tệp nạp được, sửa được, có phiên bản | Must |
| `CAP-evaluation-04` | Chạy bằng một lệnh, xuất kết quả dạng bảng dùng thẳng cho báo cáo | Must |

**Phạm vi v1:** chấm bằng mô hình cục bộ, đối chiếu tay 20% mẫu.
**Chưa làm:** so sánh với hệ thống bên ngoài (đã chốt không cần).

**Rủi ro chính:** bộ 150 câu tự dựng tốn khoảng 25h; để đến tháng 12 mới làm thì không kịp.
**Giảm thiểu:** làm trong tháng 9 và 10 song song lúc viết đề cương — đây là việc tài liệu, không
phải việc lập trình, nên không cần đợi có code.

### 7.5 Đóng gói on-premise — `deployment`

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-deployment-01` | Bốn dịch vụ khởi động bằng một lệnh, cấu hình qua tệp môi trường | Must |
| `CAP-deployment-02` | Kiểm tra GPU khi khởi động; thiếu GPU thì dừng kèm thông báo rõ nguyên nhân | Must |
| `CAP-deployment-03` | Sao lưu và phục hồi dữ liệu kho tri thức | Could |

### 7.6 Báo cáo khoảng trống tri thức — `knowledge-gap`

| Mã | Năng lực | Ưu tiên |
|---|---|---|
| `CAP-knowledge-gap-01` | Ghi lại câu hỏi bị từ chối kèm lý do từ chối | Should |
| `CAP-knowledge-gap-02` | Báo cáo nhóm câu hỏi hệ thống không trả lời được, sắp theo tần suất | Should |

## 8. Success Metrics

| Vai trò | Chỉ số | Baseline | Target | Mốc |
|---|---|---|---|---|
| North Star | Tỉ lệ trả lời đúng trên 40 câu phức tạp có đáp án | 0 | ≥ 70% | 15/12/2026 |
| Input | Độ phủ@10 của truy hồi lai trên bộ 100 câu | 0 | ≥ 85% | 15/11/2026 |
| Input | Thứ hạng đúng trung bình của truy hồi lai | 0 | ≥ 0,60 | 15/11/2026 |
| Input | Mức cải thiện của truy hồi lai so với nhánh đơn lẻ tốt nhất | — | ≥ 8 điểm phần trăm độ phủ@10 | 15/11/2026 |
| Guardrail | Tỉ lệ từ chối đúng trên 10 câu không có đáp án | — | ≥ 80% | 15/12/2026 |
| Guardrail | Tỉ lệ từ chối sai trên 40 câu có đáp án | — | ≤ 10% | 15/12/2026 |
| Guardrail | Câu trả lời chứa ý không có trong nguồn đã trích dẫn | — | ≤ 2% | 15/12/2026 |
| Guardrail | Thời gian ra chữ đầu tiên, câu hỏi đơn giản, một người dùng | — | ≤ 5 giây | liên tục |
| Guardrail | Thời gian ra câu trả lời đầy đủ, câu hỏi đơn giản | — | ≤ 20 giây | liên tục |

Mọi con số target ở trên là đề xuất của phân tích, **chưa được GVHD xác nhận** — xem `ASM-project-001`.

## 9. Constraints

- **Nguồn lực:** một người, khoảng 14h mỗi tuần. Bắt đầu code 05/10/2026, khoá tính năng
  01/12/2026, nộp báo cáo 21/12/2026, bảo vệ 10/01/2027. **Quỹ giờ và khoảng cách so với khối lượng
  ước lượng: xem bảng bottom-up ở Mục 3 của `roadmap.md`** — đó là nguồn duy nhất, mục này không nêu
  số riêng nữa. Chiến lược ứng phó chốt ở `ADR-0004`: **có cắt phạm vi, cắt ngay chứ không đợi**.
  *(Sửa 2026-09-24. Bản trước nêu "thiếu khoảng 29h" trong khi roadmap viết cùng ngày nói 58h, và
  hai tài liệu kèm hai chiến lược ngược nhau — cắt theo MoSCoW so với không cắt phạm vi.)*
- **Kiến trúc:** bảy module theo workflow cố định do GVHD chỉ định, không thay đổi. Stack kỹ
  thuật đã chốt.
- **Phần cứng:** máy đo là máy có GPU 6GB VRAM; bản giao doanh nghiệp bắt buộc có GPU, không hỗ
  trợ chế độ chỉ dùng CPU.
- **Giao diện:** phủ **đủ** mọi thao tác của ba nhóm người dùng — không có thao tác nào chỉ làm
  được bằng dòng lệnh hay bằng cách sửa thẳng dữ liệu — và đạt **chất lượng trình bày chuyên
  nghiệp**: hệ thống thị giác nhất quán (màu, chữ, khoảng cách, thành phần dùng lại), mọi trạng
  thái đều được thiết kế (mặc định, đang tải, rỗng, lỗi, không đủ quyền, thao tác không hoàn tác
  được), và dùng được trên màn hình laptop lẫn màn hình lớn. Chốt 2026-09-09, thay cho định hướng
  "giao diện tối giản" ban đầu.
- **Bảo mật:** không dữ liệu nào của tổ chức rời khỏi mạng nội bộ. Ràng buộc này thắng mọi ràng
  buộc khác. Ngoại lệ duy nhất, chốt 2026-09-09: **lần cài đầu tải mô hình ngôn ngữ về** — một
  chiều, một lần, không mang dữ liệu tổ chức đi (xem `docs/deployment/userstories/us-034.md`). Khi trình bày sản phẩm phải phát
  biểu đúng như vậy, không phát biểu thành "máy chủ không bao giờ cần kết nối ra ngoài".

## 10. Risks & Assumptions

| Rủi ro | Loại | Tầm quan trọng | Khả năng | Hậu quả | Cách phòng |
|---|---|---|---|---|---|
| Khối lượng vượt quỹ giờ (con số ở Mục 3 của `roadmap.md`) | viability | Cao | Cao | Vỡ mốc 01/12, mất chương thực nghiệm | Cắt phạm vi ngay theo `ADR-0004`, không đợi khủng hoảng; đẩy bộ ground truth sang tháng 9 và 10 |
| Lọc quyền trước xếp hạng làm tụt độ phủ | feasibility | Cao | Trung bình | Số liệu truy hồi kém, kết luận sai | Kịch bản kiểm thử riêng ngay ở sprint truy hồi |
| Tách bảng biểu sai trên 30% kho | feasibility | Cao | Trung bình | Nhóm tài liệu quan trọng nhất trả lời sai | Đưa tài liệu nhiều bảng vào bộ 100 câu ngay từ đầu |
| Độ trễ không đạt khi nhiều người hỏi cùng lúc | feasibility | Trung bình | Cao | Không đạt yêu cầu phi chức năng | Đo sớm; hạ số người dùng đồng thời mục tiêu nếu cần |
| Bộ 150 câu dựng chậm | viability | Cao | Trung bình | Không có số liệu để viết chương thực nghiệm | Bắt đầu ngay tháng 9, không đợi có code |

### Giả định cần xác nhận

**ASM-project-001 — Các mốc số liệu ở mục 8 là mức GVHD chấp nhận**

- Owner: @longdt
- Method: triển khai theo mốc tự đặt trước, mang số liệu thật đi đối chiếu với GVHD sau
- Confirm-by: 2026-09-20
- Risk nếu sai: toàn bộ tiêu chí "xong" của các sprint đánh giá phải đặt lại; có thể phải thêm
  nhánh thí nghiệm.
- Status: in-validation

**ASM-project-002 — Chấm bằng mô hình cục bộ kèm đối chiếu tay 20% được hội đồng chấp nhận**

- Owner: @longdt
- Method: triển khai trước; chuẩn bị sẵn số liệu độ đồng thuận giữa máy chấm và người chấm làm
  bằng chứng khi trình bày
- Confirm-by: 2026-09-20
- Risk nếu sai: phải chấm tay toàn bộ 50 câu nhân 3 cấu hình, thêm khoảng 20h.
- Status: in-validation
- Ghi chú (2026-09-09): chủ động triển khai trước thay vì chờ xác nhận. Đánh đổi được chấp nhận có
  ý thức — chính con số đồng thuận máy-người ở `docs/evaluation/userstories/us-032.md` là thứ dùng để thuyết phục, nên làm trước
  còn có bằng chứng mang đi, chờ hỏi thì không có gì trong tay.

**ASM-project-003 — Kho văn bản pháp quy công khai đủ để dựng 150 câu có đáp án chuẩn**

- Owner: @longdt
- Method: thử dựng 20 câu đầu trên kho đã chọn và đánh giá độ khó thu được
- Confirm-by: 2026-10-05
- Risk nếu sai: phải đổi nguồn corpus, làm lại từ đầu phần chuẩn bị dữ liệu.
- Status: proposed

**ASM-project-004 — Mô hình 7B lượng tử hoá chạy được trên GPU 6GB trong ngưỡng độ trễ mục tiêu**

- Owner: @longdt
- Method: chạy thử đo thời gian trên chính máy đo, trước khi viết dòng code đầu tiên
- Confirm-by: 2026-10-05
- Risk nếu sai: phải hạ xuống mô hình nhỏ hơn, ảnh hưởng trực tiếp chất lượng câu trả lời và mọi
  con số ở mục 8.
- Status: proposed

## 11. Open Questions

- [x] OQ-1: Số người dùng đồng thời mục tiêu. **Chốt 2026-09-09: 5 câu hỏi xử lý cùng lúc, câu thứ
      sáu vào hàng đợi có thông báo vị trí; 50 phiên đăng nhập mở đồng thời.** Lý do và cách đo ở
      `NFR-system-008` đến `NFR-system-011`.
- [ ] OQ-2: Ngưỡng điểm để từ chối ở tầng một đặt bao nhiêu? Chỉ xác định được bằng thực nghiệm
      trên bộ 100 câu, không đặt trước. Giữ ở dạng tham số hiệu chỉnh được (`NFR-system-047`).
- [ ] OQ-3: Ngưỡng tương đồng để gắn nhãn nghi ngờ chồng lấn chủ đề? Như OQ-2.
- [x] OQ-4: Chính sách mật khẩu và thời hạn phiên đăng nhập. **Chốt 2026-09-09: mật khẩu tối thiểu
      12 ký tự, chặn danh sách mật khẩu phổ biến, không bắt buộc trộn loại ký tự; phiên hết hạn sau
      8 giờ không thao tác; khoá tài khoản 15 phút sau 10 lần sai liên tiếp.** Xem `NFR-system-026`
      đến `NFR-system-028`.
