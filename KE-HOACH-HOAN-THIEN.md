# Kế hoạch hoàn thiện — Hệ thống tra cứu tri thức nội bộ

> Lập ngày 30/09/2026, sau một đợt rà soát ba góc nhìn (Kiến trúc · Kiểm thử · Sản phẩm) cộng
> với kiểm chứng chạy thật trên kho 1.003 tài liệu / 45.705 đoạn.
>
> **Cách đọc tài liệu này.** Mỗi mục được gắn một trong hai nhãn:
> **`[đo được]`** — đã dựng lại được lỗi trên hệ thống đang chạy, có số liệu hoặc mã trả về kèm
> theo; **`[đọc mã]`** — suy ra từ mã nguồn, chưa dựng lại. Phân biệt này quan trọng: một lỗi
> `[đọc mã]` có thể không xảy ra vì một lý do nào đó chưa nhìn ra, còn một lỗi `[đo được]` thì
> chắc chắn đang xảy ra.

---

## 1. Trạng thái hiện tại

Phần lõi kỹ thuật đứng vững và đã chạy thật đầu-cuối: truy hồi lai BM25 + vector hợp nhất RRF,
trích dẫn có breadcrumb chương/điều, hai tầng từ chối, phân quyền từ-chối-mặc-định, đóng gói
Docker bốn dịch vụ. Mọi ngưỡng độ trễ đều đạt trên kho đầy (xem `HUONG-DAN-CHAY.md`).

Đợt rà soát tìm ra **tám lỗi nghiêm trọng đang xảy ra thật** và một khoảng trống chức năng đủ
lớn để chặn triển khai. **Tất cả đã sửa** (Mục 2 và Mục 3).

Điều đáng ghi lại nhất không phải từng lỗi, mà là **cái mẫu chung của chúng**: phần lớn không
phải lỗi logic — chúng là chỗ **lời hứa trong tài liệu và chú thích không khớp mã thi hành**.
Mã chạy đúng như nó được viết; chỉ có điều nó không làm điều mà mọi người, kể cả người viết,
tin là nó làm:

| Điều tài liệu hoặc chú thích khẳng định | Điều mã thực sự làm |
|---|---|
| "Giao dịch RIÊNG cho từng tệp — là sự thật chứ không phải lời hứa" | Không có giao dịch nào được mở |
| "Tài liệu chưa xác nhận vẫn không ai đọc được cho tới khi có người cấp quyền" | Tài khoản thường đọc được trọn nội dung tài liệu không có một dòng cấp quyền nào |
| "Cấp quyền hàng loạt ở màn Tài liệu" | Giao diện không có màn cấp quyền nào |
| "Lý do máy-đọc-được không bao giờ đi ra màn hình người hỏi" | Nó đi qua luồng sự kiện và qua lịch sử hội thoại |
| "Chữ ký mô hình được ghi kèm mỗi tập đoạn" | Bảng không có cột nào cho nó |
| "Sẽ kiểm lại ở câu hỏi đầu tiên" (phép kiểm GPU) | Không có dòng mã nào kiểm lại |
| "Không có ngưỡng nào là hằng số trong mã" | Bốn ngưỡng là hằng số trong mã |

Bài học vận hành rút ra: **mỗi câu khẳng định trong tài liệu nên trỏ được tới một dòng mã hoặc
một phép kiểm**. Câu nào không trỏ được thì hoặc viết mã cho nó, hoặc sửa câu — đừng để nó đứng
một mình, vì nó sẽ được tin.

---

## 2. Đã sửa trong đợt này

Mỗi mục dưới đây đã được kiểm chứng bằng cách chạy lại chính phép thử đã phát hiện ra nó.

### 2.1 Bảo mật

| # | Lỗi | Bằng chứng trước khi sửa | Sau khi sửa |
|---|---|---|---|
| S1 | **Tài liệu chưa cấp quyền cho ai vẫn đọc được** `[đo được]` | `GET /api/documents/1003/chunks` → **200** kèm toàn văn, trong khi tài liệu có 0 dòng ACL | → **404**, giống hệt phản hồi "không tồn tại"; tài liệu đã cấp quyền vẫn 200 |
| S2 | **Vô hiệu hoá tài khoản không có tác dụng** `[đo được]` | Sau `deactivate`, `is_active=f` trong CSDL nhưng phiên cũ vẫn gọi được `/me` và `/api/qa/ask`, cả hai trả 200 | Lớp lọc mới cắt phiên ngay yêu cầu kế tiếp; đổi vai cũng vậy |
| S3 | **Cảnh báo thay thế lộ tên tài liệu ngoài quyền** `[đọc mã]` | Câu "Đã được thay thế bởi: {tên}" lấy tên không qua bộ lọc quyền nào | Chỉ nêu tên khi người hỏi đọc được bản mới; nếu không thì vẫn cảnh báo nhưng giấu tên |

**Gốc của S1 đáng ghi lại.** Phạm vi quyền của người đọc nhiều được lưu dạng "đọc được mọi thứ
TRỪ danh sách này". Danh sách chặn là một *ảnh chụp*. Tài liệu nạp sau ảnh chụp không nằm trong
đó, nên câu hỏi "có bị chặn không" trả lời "không" — và từ-chối-mặc-định lặng lẽ đảo thành
cho-phép-mặc-định cho **đúng những tài liệu mới nhất**. Bản ghi quyền đúng, truy vấn đúng, cache
đúng; chỉ cách biểu diễn sai, và nó sai theo chiều mở. Bản sửa gắn thêm **mốc kho** vào ảnh
chụp: id vượt mốc thì từ chối, vì chưa ai nói gì về nó.

### 2.2 Chất lượng trả lời

| # | Lỗi | Trước | Sau |
|---|---|---|---|
| Q1 | **Câu hỏi ngoài kho vẫn được trả lời** `[đo được]` | 5/10 câu đúng: 4 trong 5 câu hoàn toàn ngoài kho ("Cách nấu phở bò", "Ai là tác giả Truyện Kiều", "Cài Windows 11", "Đội tuyển đá với ai") đều **được trả lời** | **10/10 đúng**: 5 câu hợp lệ trả lời đủ, 5 câu ngoài kho đều bị từ chối, không chối oan câu nào |

Hai nguyên nhân, sửa cả hai: (a) tầng chặn thứ hai **bỏ qua hoàn toàn** câu trả lời ngắn — nên
mô hình trả "Không đủ căn cứ" (bốn từ) là đi thẳng qua cửa, và hệ thống phát nó ra như một câu
trả lời kèm tám trích dẫn; (b) khi mô hình **tự khai** không đủ căn cứ — đúng điều lời nhắc yêu
cầu — hệ thống không chuyển lời khai đó thành một lời từ chối.

### 2.3 Mất dữ liệu khi nạp

| # | Lỗi | Bằng chứng |
|---|---|---|
| N1 | **Tài liệu kẹt vĩnh viễn ở "đang xử lý"** `[đo được]` | Lô nạp kẹt `DANG_CHAY`, mục kẹt `CHO`, tài liệu `DANG_XU_LY` 0 đoạn — sau 90 giây vẫn nguyên, **không lỗi, không nhật ký**. Bước khôi phục mới tìm ra **6 tệp** đang kẹt như vậy, tức đợt nạp 1.000 tài liệu đã âm thầm đánh rơi 5 tệp mà không ai biết |

Việc nạp được giao cho luồng nền **trong lúc giao dịch còn mở**, nên luồng nền có thể đi tìm
mục nạp trước khi dòng đó tồn tại, không thấy, rồi `return` im lặng. Đây là cuộc đua nên nó
không xảy ra mọi lần — 919 tệp trước đó đều qua, và chính điều đó làm nó khó thấy. Bản sửa:
giao việc **sau khi commit**, gọi qua proxy để giao dịch riêng có thật, ghi nhật ký mức lỗi
thay vì im lặng, và thêm bước **nhặt lại việc dang dở khi khởi động** để một lần khởi động lại
không giết việc đang chạy.

### 2.4 Giao diện và giao thức

| # | Lỗi | Trước | Sau |
|---|---|---|---|
| G1 | Màn **Tài khoản** trả 500 mọi lần mở `[đo được]` | `LazyInitializationException` | 200 kèm phòng ban, một truy vấn cho cả trang |
| G2 | Yêu cầu sai dạng bị báo là lỗi máy chủ `[đo được]` | `?page=abc`, `/documents/khong-phai-so`, thiếu tham số bắt buộc, `?page=-5` — cả bốn trả **500** | 400 (hoặc kẹp về khoảng hợp lệ) |
| G3 | Lỗi giữa luồng SSE sinh ngoại lệ **thứ hai** che ngoại lệ gốc `[đo được]` | Bộ xử lý ghi JSON vào phản hồi `text/event-stream` | Nhận ra luồng đã phát, dừng; ngắt kết nối của người dùng không còn bị ghi mức lỗi |
| G4 | Mô hình chép nhãn `[Nguồn N]` vào đầu câu trả lời `[đo được]` | Hiện ra màn hình như nội dung | Lọc trên luồng chữ, giữ nguyên dấu trích dẫn thật |
| G5 | Ảnh Docker của compose khác ảnh dựng tay `[đo được]` | `up -d` chạy ảnh cũ sau khi đã dựng lại xong | Tên ảnh khai tường minh |

### 2.5 Phần cứng — giả định `ASM-project-004` đã đóng bằng số liệu

Mặc định trong compose là `qwen2.5:7b` trong khi **mọi số đo là `3b`** — tức tài liệu mô tả một
cấu hình khác cấu hình sẽ bàn giao. Đã tải 7B về và đo cả hai `[đo được]`:

| | nằm trên GPU | chữ đầu, 1 người | chữ đầu, 5 câu đồng thời (ngưỡng 15 s) |
|---|---|---|---|
| `qwen2.5:3b` | **100%** | 1,7 s | **3,8 s** |
| `qwen2.5:7b` | **84%** | 3,0 s | **12,0 s** |

7B vẫn đạt mọi ngưỡng nhưng ăn 80% ngân sách của ngưỡng chặt nhất, với 16% mô hình chạy trên
CPU. Mặc định đã chuyển về 3b — cấu hình mà các số đo được đo trên nó.

Đi kèm là hai lỗi của chính cửa kiểm này:

- **Phép kiểm khởi động số 4 cho qua mô hình nạp một phần** `[đo được]` — nó chỉ hỏi "có byte nào
  trên GPU không", nên in ra "84% mô hình" rồi báo ĐẠT. Nay có sàn `tracuu.llm.min-gpu-ratio`
  (mặc định 1.0) và chặn khi dưới sàn.
- **Lời hứa "sẽ kiểm lại ở câu hỏi đầu tiên" không có mã đứng sau** `[đo được]` — máy chủ mô hình
  nạp lười nên lúc khởi động gần như luôn là "chưa nạp", phép kiểm bỏ qua, và không có gì kiểm
  lại. Tức phép kiểm đáng giá thứ hai của hệ thống bị bỏ qua **vĩnh viễn trong ca thường gặp
  nhất**. Nay đã hiện thực: lượt sinh chữ đầu tiên kiểm lại và ghi nhật ký mức lỗi nếu dưới sàn.

### 2.6 Bộ đo

| # | Lỗi | Hệ quả |
|---|---|---|
| D1 | Chấm câu phức tạp bằng ngưỡng của câu đơn giản `[đo được]` | Báo `NFR-001 KHÔNG ĐẠT` (8,2 s / ngưỡng 5 s) cho một ngưỡng **sản phẩm không hề vi phạm**. Tách đúng nhóm: câu đơn giản 1,7 s |
| D2 | Bộ kiểm tích hợp **hỏng cứng** thay vì bỏ qua khi thiếu CSDL | Lời hứa "`mvn test` vẫn xanh trên máy chưa dựng hạ tầng" là sai |
| D3 | Mật khẩu CSDL ghi cứng trong mã kiểm thử | Không chạy được kiểm thử trên đúng hạ tầng vừa dựng |

### 2.7 Ba màn quản trị đang chặn triển khai — đã dựng xong

Máy chủ vốn có đủ đường, giao diện thì không gọi cái nào. Nay đã nối, và quan trọng hơn là đã
**kiểm chứng bằng đúng chuỗi việc người quản trị làm trong ngày đầu triển khai**:

| Màn | Làm được gì |
|---|---|
| **Cấp quyền trên tài liệu** | Xem quyền đang có (tách *cấp trực tiếp* khỏi *cấp qua phòng ban*, vì gỡ hai loại này ở hai chỗ khác nhau) · thêm/gỡ quyền cho một người hoặc cả phòng ban · cho phép và từ chối · chọn nhiều tài liệu rồi cấp một lần · tra *"vì sao người này đọc được / không đọc được"* |
| **Tài khoản và phòng ban** | Tạo tài khoản, sửa, đặt lại mật khẩu · gán phòng ban và mức mật · tạo phòng ban · nhập hàng loạt từ danh sách **có xem trước rồi mới ghi** |
| **Cấp quyền ngay lúc nạp** | Chọn tệp thì hỏi ngay "phòng ban nào được đọc lô này" + nhãn mật, rồi tự áp sau khi đợt nạp xong — chỉ áp cho tệp nạp thành công |

**Phép kiểm quan trọng nhất là phép kiểm cuối**: một tài khoản tạo mới hoàn toàn qua giao diện,
gán phòng ban qua giao diện, tài liệu nạp và cấp quyền qua giao diện — rồi chính người đó đăng
nhập, bị bắt đổi mật khẩu, và **hỏi ra được câu trả lời kèm trích dẫn** từ tài liệu vừa nạp.
Ba màn chạy đẹp mà quyền không có hiệu lực thật thì vô nghĩa, nên phép kiểm không dừng ở chỗ
"bấm nút không lỗi". **15/15 đạt.**

Hai thứ nhỏ tìm ra khi nhìn ảnh chụp màn hình:

- Bảng quyền hiện **hai dòng y hệt nhau** khi hai tài khoản trùng họ tên — quản trị không biết
  dòng nào là ai nên không dám gỡ dòng nào. Nay tên hiển thị kèm tên đăng nhập.
- Thang mức mật trước đây không có đường nào cho giao diện biết. Ghi cứng "1 Công khai, 2 Nội
  bộ, 3 Mật" vào giao diện thì ngày ai đó thêm mức thứ tư vào cấu hình, giao diện sẽ im lặng bỏ
  sót nó. Nay có `GET /api/admin/clearance-levels`.

### 2.8 Lưới kiểm thử bổ sung

- `AdminScreensIT` — quét mọi đường GET của màn quản trị, cộng ca yêu cầu sai dạng và tham số
  phân trang ngoài khoảng. **Đã kiểm ngược**: trả mã về bản hỏng thì nó đỏ đúng
  `LazyInitializationException`.
- `CoreLogicTest` — thêm ca mốc kho cho danh sách chặn (chính lỗ hổng S1).
- `GroundingCheckerTest` — viết lại phép kiểm câu ngắn. Bản cũ khẳng định "câu ngắn thì không
  kết luận là bịa", tức là **bảo vệ đúng lỗ hổng Q1**; nay tách thành hai ca: câu ngắn trùng
  nguồn thì đạt, câu ngắn không trùng nguồn thì không.
- Một bộ kiểm giao diện lái Chrome qua giao thức gỡ lỗi (không cài thêm gói): 15/15 đạt.

**Tổng: 52 phép kiểm đơn vị + 18 phép kiểm tích hợp, xanh toàn bộ.**

---

## 3. Danh sách CHẶN — đã trống

Cả tám hạng mục CHẶN đã xong. Ba hạng mục giao diện ở Mục 2.7; bốn hạng mục nền dưới đây, mỗi
cái đều được **dựng lại lỗi trước khi sửa và kiểm chứng lại sau khi sửa**:

| # | Việc | Bằng chứng |
|---|---|---|
| **B5** | **Lý do từ chối máy-đọc-được lọt ra dây** — trường `reason` phân biệt `THIEU_QUYEN` với `THIEU_TAI_LIEU`, tức đúng hai ca mà cả hệ thống được dựng để không phân biệt được từ bên ngoài. Giao diện không hiển thị nó, nhưng ai mở bảng mạng của trình duyệt đều đọc được | Đã bỏ khỏi cả sự kiện luồng lẫn lịch sử hội thoại. Phép kiểm mới soi thành phần của kiểu bản ghi, nên một trường `reason` thêm vào sau này sẽ làm nó đỏ — kể cả khi không ai nhớ vì sao nó từng bị bỏ |
| **B6** | **Trần quét chỉ mục 40.000 ghi cứng, nhỏ hơn kho 45.938 đoạn**, và **dải mù 25–50%**: người có phạm vi quyền trong khoảng đó vừa không được quét lặp vừa không được lợi thế danh sách cho-phép | Cả hai thành cấu hình. Ngưỡng hẹp đặt bằng ngưỡng đổi chiều lọc nên không còn dải mù; trần nâng lên 60.000 và hệ thống **kêu lên** nếu trần thấp hơn số đoạn thay vì im lặng |
| **B7** | **Chữ ký mô hình sinh biểu diễn không được lưu** — chú thích khẳng định "ghi kèm mỗi tập đoạn" nhưng bảng không có cột. Nạp nửa kho bằng mô hình này, nửa kia bằng mô hình khác thì khoảng cách vector vẫn tính ra số đẹp mà vô nghĩa | Thêm cột + đóng dấu lúc nạp. **Hai cửa chặn đều đã dựng lại được**: chạy với một mô hình khác thì nạp bị từ chối kèm câu nêu đúng nguyên nhân, và đường hỏi bỏ nhánh ngữ nghĩa kèm cảnh báo mức lỗi thay vì trả kết quả sai |
| **B8** | **Đường từ chối trả giá một lượt truy hồi không-lọc toàn kho, đồng bộ** — `@Async` và `@Transactional` không có tác dụng vì lời gọi trong cùng lớp không đi qua proxy | Đo trước: máy chủ báo xong sau 1,4–2,4 s nhưng người dùng **còn chờ thêm tới 1,9 giây**. Đo sau: chênh còn **95–180 ms**. Việc phân loại khoảng trống nay chạy trên bể luồng riêng, bể nhỏ có trần — lúc bận thì bỏ một bản ghi thống kê tốt hơn làm chậm một câu trả lời |

Một khiếm khuyết đi kèm, tìm ra khi kiểm chứng B7: cửa chặn nổ đúng nhưng **báo sai nguyên nhân**
— *"Thao tác không thực hiện được ở trạng thái hiện tại của tài liệu. Làm mới danh sách"*. Người
vận hành sẽ đi làm mới danh sách và thấy không có gì đổi, trong khi việc cần làm nằm ở cấu hình
mô hình. Một thông báo lạc đề còn tệ hơn không có thông báo, vì nó dẫn người ta đi sai hướng một
cách tự tin. Nay có mã lỗi riêng với câu chữ trỏ đúng việc phải làm.

## 4. Còn lại — NÊN SỬA

### 4.1 Số liệu trong mã lệch số liệu trong `nfr.md`

`nfr.md` tự khai là nguồn duy nhất, nhưng ba con số đang lệch `[đọc mã]`:

| Khái niệm | `nfr.md` | Mã |
|---|---|---|
| Độ dài mật khẩu tối thiểu | ≥ 12 ký tự, có danh sách mật khẩu phổ biến | ≥ 8 ký tự, bắt buộc trộn chữ và số, không có danh sách |
| Khoá tài khoản sau số lần sai | 10 | 5 |
| Thời gian chờ mô hình | ≤ 30 giây | 180 giây |

Phải chốt một bên. Người hiện thực hiện không có cách nào biết cái nào là luật.

### 4.2 Lời hứa khác chưa khớp mã

- **Luật R5** ("`ingestion/` không gọi `agent/` và ngược lại") bị vi phạm ở **năm chỗ** `[đọc mã]`.
  Hình dạng thật: `ingestion/` đã thành tầng tiện ích văn bản/vector dùng chung. Hoặc chuyển
  `VietnameseSegmenter` / `EmbeddingService` / `Bm25StatsService` sang gói chung, hoặc phát
  biểu lại R5 theo chiều thật.
- **`maximumSessions(3)`** được chú thích là hiện thực `NFR-010` (50 phiên toàn hệ thống) — hai
  thứ không liên quan `[đọc mã]`.
- **Hàng đợi câu hỏi** nằm trong bộ nhớ tiến trình, không nằm ở Redis như cả ba tài liệu vẽ
  `[đọc mã]`. Chạy hai bản `app` sau cân tải làm số lượt sinh đồng thời trên cùng một GPU âm
  thầm thành 10.
- **Thứ tự sự kiện** trong hợp đồng giao diện khác thứ tự mã phát (cảnh báo trước/sau mẩu chữ
  đầu) `[đọc mã]`.
- **Ảnh CSDL** là `pg16` thẻ trôi trong khi tài liệu khai PostgreSQL 18 `[đo được]` — phép kiểm
  khởi động số 2 chặn theo một phiên bản đến từ thẻ không ghim.

### 4.3 Hiệu năng sẽ gãy khi lên quy mô `[đọc mã]`

- **Nhánh BM25 lấy không giới hạn rồi cắt trong JVM** — không có `LIMIT` trong câu lệnh, mỗi
  bản ghi giữ toàn văn đoạn. Đây là cơ chế đứng sau con số 8,2 giây đã đo, và kho lớn hơn một
  bậc là hết bộ nhớ chứ không chỉ chậm.
- **Thống kê BM25 tính lại toàn bộ sau MỖI tài liệu** → một lần nạp kho là O(n²). Con số 29 phút
  cho 1.000 tài liệu **không ngoại suy được**.
- **Số hiệu phiên bản quyền là bộ đếm toàn cục** — một lần cấp quyền hàng loạt buộc mọi phiên
  đang mở tính lại cùng lúc.

### 4.4 Phép kiểm cần bổ sung (chọn lọc từ 30 kịch bản)

Ưu tiên cao nhất, theo thứ tự:

1. Hai người khác quyền, kho **nhiều tài liệu** — hiện bộ kiểm chỉ có một tài liệu nên nhánh lọc
   quyền trong SQL **chưa từng loại một đoạn nào**; đảo dấu `NOT` phép kiểm vẫn xanh.
2. Người dùng thường và người chưa đăng nhập gọi `/api/admin/**` → 403 / 401.
3. Thu hồi quyền giữa phiên rồi mở lại trích dẫn cũ và tải tệp gốc.
4. Nạp một lô có tệp hỏng + tệp rỗng — kiểm cả trạng thái lỗi lẫn việc không để lại đoạn mồ côi.
5. Lỗi giữa luồng SSE (hồi quy cho G3).
6. Áp **kiểm đột biến** (PIT) lên gói `access/` và `agent/action/`: bỏ mệnh đề nhãn mật, đảo
   `NOT` của danh sách chặn, đổi điều kiện đếm đoạn hỗ trợ — mỗi đột biến phải làm ít nhất một
   phép kiểm đỏ.

---

## 5. Chức năng còn thiếu để đóng vòng giá trị

| Mức | Việc | Vì sao |
|---|---|---|
| ~~P0~~ | ~~B1–B3 (cấp quyền, tạo tài khoản, quyền theo lô nạp)~~ — **đã xong**, xem Mục 2.7 | — |
| P1 | **Nút phản hồi hai trạng thái** trên mỗi câu trả lời | Tín hiệu chất lượng **duy nhất** thu được sau khi triển khai. Hiện chỉ biết khi từ chối, không biết khi trả lời sai |
| P1 | **Câu từ chối có đường ra** — thêm một câu chung "nếu bạn tin mình cần tài liệu này, gửi yêu cầu cho quản trị", không nêu tên tài liệu | Người bị từ chối 2–3 lần sẽ quay lại hỏi đồng nghiệp, đúng hành vi sản phẩm sinh ra để thay thế |
| P1 | **Lịch sử hội thoại của chính mình** — máy chủ đã có `GET /conversations`, giao diện chưa gọi | Tra lại câu trả lời hôm qua là hành vi thường gặp nhất |
| P1 | **Khai tài liệu thay thế trên giao diện** — đường `/{id}/supersedes` đã có, không có nút | Không có thao tác này thì cảnh báo "đã bị thay thế" không bao giờ có dữ liệu thật |
| P2 | Quyền theo **danh mục** (tài liệu mới vào danh mục tự thừa hưởng) | Giảm tiếp khối lượng cấp quyền, nhưng chỉ đáng làm sau khi P0 chạy |
| P2 | Sao lưu và phục hồi | Hoãn được, không bỏ được — là câu hỏi đầu tiên của khách hàng thật |

**Không nên đầu tư thêm:** đường sinh biểu diễn ONNX trong JVM (đường dự phòng đã chạy được),
ngưỡng phát hiện chồng lấn, và bộ đo độ trễ (đã đủ dùng).

---

## 6. Ẩn số phải đo sớm

| Ẩn số | Cách đo | Vì sao gấp |
|---|---|---|
| Bộ nhớ đồ hoạ còn lại với mô hình **mặc định** ở 5 lượt đồng thời | Chạy `--measure` với `TRACUU_LLM_MODEL` đúng mô hình bàn giao | Phải biết **trước khi** hứa 7B với bất kỳ ai. 6 GB phải chứa trọng số + 5 × ngữ cảnh 3072 token |
| Độ trễ **truy hồi thuần** p95 ở phạm vi 100% kho (`NFR-005`, 800 ms) | Đã ghi sẵn ở `turns.latency_retrieval_ms` — chỉ cần in phân vị | Rất có thể đang **không đạt** mà không ai biết, vì bảng đối chiếu bỏ hẳn ngưỡng này |
| Độ trễ **đường từ chối** (`NFR-004`, 3 giây) | Báo riêng nhóm từ chối thay vì loại khỏi mẫu | Hiện bị loại khỏi mọi báo cáo, mà 25% lượt là từ chối |
| Độ phủ ở phạm vi quyền **25–50% kho** | Dựng tài khoản ~35% kho, so độ phủ@k với 5% và 100% | Dải mà cả quét lặp lẫn danh sách cho-phép đều không áp |
| Chất lượng trả lời trên kho **thật** | Xem Mục 7 | Chưa có số nào nói câu trả lời đúng bao nhiêu phần trăm |

---

## 7. Đo chất lượng cho đúng

Kho 1.000 tài liệu hiện tại là kho **tổng hợp**: đủ để đo độ trễ, **không** đủ để đo chất lượng.
Cần ba lớp:

1. **Kho thật hoặc gần thật** — 150–300 tài liệu tiếng Việt có cấu trúc thật (quy chế nội bộ đã
   ẩn danh, hoặc văn bản công khai). Giữ nguyên dữ liệu bẩn: bảng vỡ, tiêu đề không chuẩn. Thêm
   nhóm khó có chủ đích: cặp bản cũ/bản thay thế, điều khoản mâu thuẫn, bảng biểu.
2. **Bộ câu hỏi có đáp án**, khoảng 150–200 câu chia tầng — trong đó **bắt buộc** có nhóm *không
   có đáp án trong kho* và nhóm *đáp án nằm ngoài quyền*, vì đó là hai nhóm đo đúng lời hứa cốt
   lõi của sản phẩm. Chia phần hiệu chỉnh ngưỡng và phần giữ lại; báo kết quả cuối trên phần
   giữ lại, nếu không con số là quá khớp.
3. **Kim gài trong kho tổng hợp** — 50–100 điều khoản có con số riêng, tiêm vào kho lớn. Đo truy
   hồi dưới cạnh tranh mà không cần chấm tay.

---

## 8. Thứ tự đề nghị

1. ~~**Tuần 1** — B1, B2, B3 (giao diện quản trị).~~ **Đã xong** — xem Mục 2.7.
2. ~~**Tuần 1** — B5, B6.~~ **Đã xong** — xem Mục 3.
3. **Tuần 2** — ba ẩn số đo được ngay ở Mục 6; chốt số liệu trong `nfr.md` (Mục 4.1).
4. ~~**Tuần 2** — B7, B8.~~ **Đã xong** — xem Mục 3.
5. **Tuần 3** — bổ sung phép kiểm theo Mục 4.4, đặc biệt kho nhiều tài liệu cho phép kiểm phân
   quyền và kiểm đột biến.
6. **Tuần 3–4** — dựng bộ đối chiếu chất lượng (Mục 7), rồi mới hiệu chỉnh ngưỡng từ chối.
7. **Sau đó** — P1 ở Mục 5 (phản hồi, đường ra khi từ chối, lịch sử hội thoại).

---

## 9. Điều máy không kiểm được — phải có người quyết

- **Mô hình quyền ba chiều có khớp luật truy cập thật của tổ chức không** (phòng ban phẳng không
  kế thừa, mức mật là một số nguyên, từ chối thắng cho phép). Mã hiện thực nó đúng và nhất quán;
  câu hỏi còn lại là nó có phải luật đúng.
- **Việc quản trị không đọc được tài liệu có sống nổi trong vận hành thật không.** Thiết kế nhất
  quán và lý do viết rõ, nhưng chỉ tổ chức dùng thật mới trả lời được.
- **Câu từ chối duy nhất cho mọi ca** là một đánh đổi an ninh/trải nghiệm, không phải một thuộc
  tính đúng-sai. Hiện có một mâu thuẫn phải gỡ ở tầng yêu cầu: `AC-004` của US-008 đòi thông báo
  *phân biệt được* với trường hợp kho không có thông tin, trong khi `AC-001`/`AC-002` đòi không
  tiết lộ gì. Hai điều này không thể cùng đúng — phải sửa đặc tả trước, nếu không phép kiểm nào
  cũng có thể bị coi là đúng.
