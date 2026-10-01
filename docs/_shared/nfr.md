---
type: nfr
status: draft
updated: 2026-09-24
links:
  - docs/_product/prd.md
  - docs/_shared/design-system.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Yêu cầu phi chức năng

> Nguồn duy nhất cho mọi ngưỡng đo được của hệ thống. Story và màn hình tham chiếu mã `NFR-system-NNN`,
> không phát biểu lại con số.

## 0. Cách đọc

**Điều kiện đo chuẩn**, áp cho mọi ngưỡng ở mục 1 trừ khi ghi khác: máy có GPU 6GB VRAM, kho 1.000
tài liệu tương đương khoảng 50.000 đoạn, **một người dùng đang hỏi**, mô hình đã nạp sẵn vào bộ nhớ
đồ hoạ, **ngữ cảnh đưa vào mô hình bằng đúng giá trị mặc định của `context.max_tokens`**, và **phạm
vi quyền của người hỏi phủ toàn kho**. Ngưỡng khi nhiều người hỏi cùng lúc ở mục 2.

> **Hai điều kiện sau thêm 2026-09-24, và chúng quan trọng hơn vẻ ngoài.** Thời gian xử lý lời nhắc
> tỉ lệ thuận với số token đưa vào, nên đo với ngữ cảnh ngắn sẽ cho một con số đẹp mà vận hành thật
> không bao giờ đạt. Còn phạm vi quyền: truy vấn truy hồi chọn chế độ quét lặp để cứu độ phủ khi
> phạm vi hẹp, mà quét lặp đổi độ trễ lấy độ phủ — đo ở phạm vi toàn kho là đo ở điều kiện thuận lợi
> nhất, trong khi trường hợp đáng lo là phạm vi dưới 5% mà `AC-006` của US-008 đang nhắm tới. Khi
> chạy kịch bản ấy ở sprint 3 thì đo luôn độ trễ ở ba mức phạm vi 5%, 50% và 100%: cùng một lần dựng
> dữ liệu, cùng một lần chạy, mà đóng được cả hai ẩn số.

**p95** nghĩa là 95 lần trong 100 lần đo phải đạt. Dùng p95 thay vì trung bình vì trung bình che mất
đúng những lần chậm mà người dùng nhớ.

Con số ở đây là **đề xuất của phân tích**, chưa đối chiếu với GVHD — xem `ASM-project-001`. Con số
nào phụ thuộc mô hình chạy được tới đâu trên phần cứng thật thì còn phụ thuộc `ASM-project-004`.

## 1. Thời gian phản hồi

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-001` | Thời gian tới chữ đầu tiên của câu trả lời, câu hỏi đơn giản | ≤ 5 giây (p95) | Đo từ lúc bấm gửi tới lúc ký tự đầu hiện trên màn hình, trên 100 câu hỏi đơn giản của bộ đánh giá | `qa-chat` |
| `NFR-system-002` | Thời gian tới câu trả lời đầy đủ, câu hỏi đơn giản | ≤ 20 giây (p95) | Như trên, tính tới ký tự cuối | `qa-chat` |
| `NFR-system-003` | Thời gian tới câu trả lời đầy đủ, câu hỏi phức tạp ba truy vấn con | ≤ 60 giây (p95) | Đo trên bộ 40 câu phức tạp có đáp án | `qa-chat` |
| `NFR-system-004` | Thời gian tới thông báo từ chối ở tầng một | ≤ 3 giây (p95) | Tầng một không gọi mô hình ngôn ngữ nên phải nhanh hơn hẳn đường trả lời; chậm bằng đường trả lời là dấu hiệu cài sai chỗ chặn | `qa-chat` |
| `NFR-system-005` | Thời gian truy hồi thuần, không tính sinh chữ | ≤ 800 mili giây (p95) | Đo riêng bước truy hồi lai trên kho 50.000 đoạn, trong bộ đo tầng truy hồi | `qa-chat` |
| `NFR-system-006` | Mở tài liệu gốc từ một trích dẫn | ≤ 2 giây (p95) | Từ lúc bấm tới lúc đoạn được làm nổi bật hiện ra | `qa-chat` |
| `NFR-system-007` | Mọi màn quản trị hiện xong dữ liệu | ≤ 2 giây (p95) | Đo trên kho đầy: 1.000 tài liệu, 500 tài khoản, 50 phòng ban | `doc-ingestion`, `access-control`, `knowledge-gap` |

## 2. Sức chịu tải

Số người dùng đồng thời là câu hỏi mở OQ-1 của PRD; chốt tại đây.

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-008` | Số câu hỏi được xử lý cùng lúc | 5 | Câu thứ sáu vào hàng đợi và người hỏi **thấy vị trí trong hàng đợi**, không phải chờ trắng màn hình | `qa-chat` |
| `NFR-system-009` | Thời gian phản hồi khi đủ 5 câu hỏi đồng thời | Chữ đầu ≤ 15 giây, đầy đủ ≤ 60 giây (p95) | Chạy 5 câu hỏi song song lặp 20 lượt | `qa-chat` |
| `NFR-system-010` | Số phiên đăng nhập mở đồng thời | 50 | Phiên đang mở nhưng không hỏi thì không tiêu tài nguyên đáng kể; đo bằng cách mở 50 phiên rồi đo lại NFR-system-001 | `access-control` |
| `NFR-system-011` | Ảnh hưởng của việc nạp tài liệu lên việc hỏi đáp | Thời gian trả lời không quá 1,5 lần ngưỡng thường | Nạp 50 tài liệu ở chế độ nền trong lúc đo NFR-system-002 | `doc-ingestion` |

**Vì sao là 5.** Mô hình sinh chữ dùng chung một GPU; các câu hỏi đồng thời chia nhau băng thông
bộ nhớ đồ hoạ chứ không chạy song song thật. Quá 5 thì thời gian của mọi người cùng xấu đi thay vì
người đến sau phải chờ — hàng đợi có thông báo trung thực hơn là để tất cả cùng chậm. Với doanh
nghiệp 100–300 nhân viên, 5 câu hỏi cùng lúc là mức đỉnh hợp lý.

## 3. Quy mô dữ liệu

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-012` | Số tài liệu trong kho không làm suy giảm quá ngưỡng | 1.000 tài liệu, khoảng 50.000 đoạn | Đo lại NFR-system-005 ở kho đầy so với kho 100 tài liệu | `doc-ingestion` |
| `NFR-system-013` | Kích thước một tệp | ≤ 50 MB | Từ chối trước khi tải lên, nêu giới hạn và kích thước thật | `doc-ingestion` |
| `NFR-system-014` | Số tài khoản và phòng ban | 500 tài khoản, 50 phòng ban | Đo lại NFR-system-007 ở mức này | `access-control` |
| `NFR-system-015` | Dung lượng đĩa cho kho đầy | ≤ 20 GB, gồm cả tệp gốc | Đo sau khi nạp 1.000 tài liệu thật | `deployment` |

## 4. Thời gian nạp tài liệu

Sinh biểu diễn ngữ nghĩa chạy trên CPU chứ không trên GPU — GPU dành cho mô hình ngôn ngữ. Đây là
lý do các ngưỡng dưới đây tính bằng phút chứ không bằng giây.

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-016` | Nạp xong một tài liệu 100 trang | ≤ 3 phút | Từ lúc xác nhận tới lúc trạng thái sẵn sàng | `doc-ingestion` |
| `NFR-system-017` | Nạp xong một tệp 50 MB | ≤ 15 phút | Như trên | `doc-ingestion` |
| `NFR-system-018` | Nạp xong cả kho 1.000 tài liệu | ≤ 8 giờ | Chạy được qua một đêm là đủ cho việc triển khai ban đầu | `doc-ingestion` |
| `NFR-system-019` | Nạp tài liệu không chặn hỏi đáp | Không có khoảng thời gian nào hệ thống từ chối nhận câu hỏi vì đang nạp | Đo cùng NFR-system-011 | `doc-ingestion` |

## 5. Phần cứng tối thiểu

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-020` | Bộ nhớ đồ hoạ | ≥ 6 GB — **bắt buộc**, thiếu thì chặn khởi động | Kiểm ngay lúc khởi động, trước khi tải mô hình | `deployment` |
| `NFR-system-021` | Bộ nhớ máy | 16 GB khuyến nghị · 8 GB tối thiểu | Ở mức 8 GB, số câu hỏi đồng thời hạ xuống 2 và điều này phải được nói rõ trong tài liệu cài đặt, không để người vận hành tự phát hiện | `deployment` |
| `NFR-system-022` | Số nhân xử lý | ≥ 4 | Sinh biểu diễn ngữ nghĩa chạy trên CPU | `deployment` |
| `NFR-system-023` | Dung lượng đĩa trống | ≥ 50 GB | Gồm kho, mô hình, và chỗ cho một bản sao lưu | `deployment` |

**Lệch so với đề cương ban đầu, nói rõ để không ai hiểu nhầm:** đề cương ghi "1 máy ảo 8 GB RAM,
GPU tuỳ chọn". Nay GPU là **bắt buộc** (`NFR-system-020`), và 8 GB RAM là mức tối thiểu chạy được
chứ không phải mức khuyến nghị.

## 6. Bảo mật

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-024` | Không lời gọi nào ra ngoài mạng tổ chức sau lần cài đầu | Không có ngoại lệ | **Kiểm được**: ngắt kết nối ra ngoài rồi chạy trọn một vòng nạp tài liệu và hỏi đáp. Đây là kiểm chứng của luận điểm cốt lõi, phải nằm trong kịch bản kiểm thử chứ không phải trong lời cam kết | toàn hệ thống |
| `NFR-system-025` | Mật khẩu lưu dưới dạng băm có muối | Không lưu dạng đọc lại được ở bất kỳ đâu, kể cả nhật ký và bản sao lưu | Rà mã nguồn và rà bản sao lưu | `access-control` |
| `NFR-system-026` | Chính sách mật khẩu | ≥ 12 ký tự; chặn danh sách mật khẩu phổ biến đóng gói kèm; **không** bắt buộc trộn loại ký tự | Độ dài bảo vệ tốt hơn quy tắc trộn ký tự, vốn chỉ đẩy người dùng tới những biến thể dễ đoán | `access-control` |
| `NFR-system-027` | Thời hạn phiên | Hết hạn sau 8 giờ không thao tác | Một ngày làm việc; hết hạn giữa ca là phiền không cần thiết | `access-control` |
| `NFR-system-028` | Chống dò mật khẩu | Khoá tài khoản 15 phút sau 10 lần đăng nhập sai liên tiếp | Thông báo khi bị khoá **không** cho biết tên đăng nhập có tồn tại hay không | `access-control` |
| `NFR-system-029` | Nhật ký thay đổi quyền | Mọi thay đổi quyền, nhãn mật, mức mật ghi lại người thực hiện, thời điểm, giá trị trước và sau; giữ ≥ 12 tháng | Đây là thứ trả lời được câu "ai đã mở quyền tài liệu này" khi có sự cố | `access-control` |
| `NFR-system-030` | Bản sao lưu là dữ liệu nhạy cảm | Tài liệu vận hành nêu rõ bản sao lưu phải bảo vệ ngang hệ thống và không đưa ra ngoài mạng tổ chức | Rà tài liệu | `deployment` |
| `NFR-system-031` | Thông báo lỗi không lộ dấu vết kỹ thuật | Không hiện vết gọi hàm, câu truy vấn, đường dẫn tệp máy chủ cho người dùng cuối | Rà mọi nhánh lỗi trong danh sách màn hình | toàn hệ thống |

## 7. Tin cậy và phục hồi

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-032` | Thời gian khởi động lại tới lúc nhận được câu hỏi | ≤ 3 phút | Gồm cả thời gian nạp mô hình vào bộ nhớ đồ hoạ | `deployment` |
| `NFR-system-033` | Khởi động lại không mất dữ liệu | Không mất tài liệu, tài khoản, cấu hình quyền | Dừng và khởi động lại với kho đầy | `deployment` |
| `NFR-system-034` | Mô hình không phản hồi | Người dùng nhận thông báo trong ≤ 30 giây, không treo vô hạn | Dừng dịch vụ mô hình rồi gửi câu hỏi | `qa-chat` |
| `NFR-system-035` | Tài liệu nạp lỗi không để lại mảnh vụn | Không đoạn nào của tài liệu hỏng nằm trong phạm vi truy hồi | Nạp tệp hỏng rồi kiểm kho | `doc-ingestion` |

## 8. Khả dụng và tiếp cận được

Chi tiết cách làm ở `docs/_shared/design-system.md`; đây là phần đo được.

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-036` | Tương phản chữ với nền | ≥ 4,5:1, kể cả chữ phụ và chữ trong huy hiệu trạng thái | Đo trên từng màn | toàn hệ thống |
| `NFR-system-037` | Thao tác bằng bàn phím | Mọi thao tác làm được không cần chuột; tiêu điểm luôn nhìn thấy | Đi hết một lượt mỗi màn chỉ bằng bàn phím | toàn hệ thống |
| `NFR-system-038` | Chiều cao dòng của nội dung | ≥ 1,6 lần cỡ chữ | Ràng buộc của tiếng Việt có dấu, không phải sở thích thẩm mỹ | toàn hệ thống |
| `NFR-system-039` | Phủ trạng thái | Mọi màn khai đủ các trạng thái ở mục 6 của hệ thống thị giác, hoặc ghi rõ "không áp dụng vì…" | Đối chiếu screen index với danh sách trạng thái | toàn hệ thống |
| `NFR-system-040` | Ký tự tiếng Việt | Phông đóng gói kèm phải hiển thị đủ mọi ký tự tiếng Việt có dấu | Kiểm bằng một trang mẫu chứa đủ tổ hợp dấu | toàn hệ thống |

## 9. Tương thích

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-041` | Trình duyệt | Hai phiên bản gần nhất của các trình duyệt phổ biến | Chạy thử luồng chính trên từng trình duyệt | toàn hệ thống |
| `NFR-system-042` | Bề ngang màn hình | Tối ưu từ 1280 điểm ảnh; không vỡ bố cục từ 1024 điểm ảnh | Thu hẹp cửa sổ và kiểm từng màn | toàn hệ thống |

## 10. Tính lặp lại của thực nghiệm

Nhóm này là yêu cầu **đặc thù của đồ án**, không có trong sản phẩm thương mại thông thường. Không
đạt nhóm này thì mọi con số ở chương thực nghiệm đều không bảo vệ được trước hội đồng.

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-043` | Truy hồi cho kết quả lặp lại được | Cùng câu hỏi, cùng kho, cùng cấu hình thì cùng kết quả | Chạy hai lần và so từng đoạn trả về | `evaluation` |
| `NFR-system-044` | Yếu tố ngẫu nhiên của mô hình cố định được | Tham số điều khiển ngẫu nhiên đặt được và **ghi cùng kết quả đo** | Không ghi lại thì hai lần chạy không so được với nhau mà trông vẫn như so được | `evaluation` |
| `NFR-system-045` | Đổi cấu hình thí nghiệm không phải sửa mã nguồn | Chiến lược tách đoạn, phương pháp tìm kiếm, bật tắt bước phân rã, bật tắt tầng kiểm gắn nguồn, ngưỡng từ chối — tất cả qua cấu hình | Chạy hết ma trận thí nghiệm mà không dịch lại lần nào | `evaluation` |
| `NFR-system-046` | Kết quả đo tự mang theo điều kiện sinh ra nó | Mỗi lượt chạy ghi kèm cấu hình đầy đủ, phiên bản bộ câu hỏi, kho nào, mô hình nào, thời điểm | Người phản biện đọc bảng phải biết nó sinh ra trong điều kiện gì | `evaluation` |

## 11. Khả năng cấu hình và vận hành

| Mã | Yêu cầu | Ngưỡng | Cách đo | Áp cho |
|---|---|---|---|---|
| `NFR-system-047` | Mọi ngưỡng hiệu chỉnh được không cần dịch lại | Ngưỡng từ chối, ngưỡng chồng lấn, ngưỡng gom nhóm câu hỏi ở báo cáo khoảng trống, số truy vấn con tối đa, số lượt hội thoại giữ lại, thời hạn phiên, **`retrieval.top_k`**, **`context.max_tokens`**, kích thước bể luồng của khâu nạp | Sửa tệp cấu hình rồi khởi động lại | toàn hệ thống |
| `NFR-system-048` | Nhật ký vận hành đủ để truy nguyên sự cố | Ghi được lượt hỏi nào lỗi ở bước nào, không ghi nội dung tài liệu vào nhật ký | Gây lỗi có chủ đích rồi truy lại | `deployment` |

> **Vì sao thêm `retrieval.top_k` và `context.max_tokens` vào danh sách (2026-09-24).** Mọi ngưỡng
> độ trễ ở mục 1 đều treo trên một biến mà trước đó **không tài liệu nào đặt ngưỡng**: số đoạn đưa
> vào ngữ cảnh của mô hình. `NFR-system-001` bằng thời gian truy hồi cộng thời gian xử lý lời nhắc
> cộng thời gian sinh chữ đầu — vế giữa tỉ lệ thuận với số token, nên khi biến ấy vô chủ thì cả nhóm
> ngưỡng độ trễ không kiểm chứng được. Nó cũng là biến thí nghiệm: `AC-002` của US-030 báo cáo độ
> chính xác@k và độ phủ@k, tức **k đã tồn tại trong bộ đo nhưng chưa ai khai nó là tham số**.

## 12. Điều cố ý KHÔNG cam kết

Ghi ra để người đọc không tự suy là có:

- **Không cam kết chạy khi không có GPU.** Không có chế độ suy giảm dùng CPU.
- **Không cam kết sẵn sàng cao.** Một máy, một bản chạy; máy hỏng thì hệ thống dừng cho tới khi
  phục hồi từ bản sao lưu.
- **Không cam kết trải nghiệm tối ưu trên điện thoại.** Không vỡ bố cục là đủ ở phiên bản này.
- **Không cam kết thời gian trả lời khi quá 5 câu hỏi đồng thời.** Quá ngưỡng thì vào hàng đợi.
- **Không cam kết nạp tài liệu scan.** Không có nhận dạng ký tự từ ảnh.
- **Không có chế độ tối.**

## 13. Câu hỏi mở còn lại

Hai ngưỡng dưới đây **không đặt trước** — đặt số bây giờ là đoán, và số đoán sai sẽ đi thẳng vào
chương thực nghiệm:

- **Ngưỡng điểm để từ chối ở tầng một** (OQ-2 của PRD). Xác định bằng cách quét dải ngưỡng trên bộ
  100 câu và chọn điểm cân bằng giữa tỉ lệ từ chối đúng và tỉ lệ từ chối sai. Giá trị chốt được sẽ
  ghi ngược vào đây.
- **Ngưỡng tương đồng để gắn nhãn nghi ngờ chồng lấn** (OQ-3 của PRD). Xác định bằng cách thử trên
  kho thật, cân giữa nhiễu và bỏ sót.

Cả hai đều thoả `NFR-system-047` nên hiệu chỉnh được mà không phải sửa mã nguồn — đó chính là lý do
để chúng ở dạng tham số thay vì hằng số.
