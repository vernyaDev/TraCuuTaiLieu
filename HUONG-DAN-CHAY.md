# Hệ thống tra cứu tri thức nội bộ — hướng dẫn chạy

> Phần mềm nằm ở `app/` (máy chủ) và `web/` (giao diện). Hồ sơ phân tích nằm ở `docs/`.
> Tài liệu này chỉ nói về **cách chạy**; vì sao thiết kế như vậy thì đọc
> `docs/_shared/architecture.md`.

## Hệ thống gồm gì

Bốn dịch vụ, một cổng ra ngoài:

| Dịch vụ | Là gì | Cần GPU |
|---|---|---|
| `app` | Máy chủ ứng dụng. Phục vụ luôn giao diện — giao diện **không có tiến trình riêng**. | không |
| `db` | PostgreSQL kèm phần mở rộng vector | không |
| `llm` | Ollama, chạy mô hình sinh câu trả lời | **có** |
| `cache` | Redis: phiên đăng nhập và hàng đợi câu hỏi | không |

Giao diện dựng ra tệp tĩnh lúc dựng ảnh và được `app` phục vụ. Vì vậy chỉ có một cổng,
một phiên bản, và không có tầng phiên bản hoá giao diện lập trình.

## Đã chạy thật — kết quả đo ngày 29/09/2026

Hệ thống đã được chạy đầy-cuối trên phần cứng thật, không phải chỉ dựng được.

**Phần cứng:** NVIDIA RTX 3060 Laptop, 6 GB VRAM — đúng cấu hình mà kiến trúc giả định.
**Hạ tầng:** PostgreSQL 18 + pgvector 0.8.1, Redis, Ollama trên Windows.

Chuỗi thả-tệp → hỏi → trả-lời-có-trích-dẫn chạy trọn vẹn. Trích dẫn chỉ tới **đúng đoạn**:

```
Câu trả lời : Người lao động làm việc đủ mười hai tháng được nghỉ hằng năm
              mười hai ngày làm việc...
Trích dẫn 1 : Quy chế Nghỉ phép năm — Chương I › Điều 2. Số ngày nghỉ hằng năm
              BM25 4,15 · vector 0,828 · hợp nhất 0,0328
```

**Độ trễ đo được trên KHO ĐẦY** — 996 tài liệu, 45.703 đoạn, đúng điều kiện mà `nfr.md` khai
kèm ngưỡng (mô hình `qwen2.5:3b-instruct-q4_K_M`, 100% nằm trên GPU, phạm vi quyền 100% kho,
20 lượt mỗi mức):

| Ngưỡng | Đo được (p95) | Cam kết | |
|---|---|---|---|
| NFR-001 chữ đầu, câu đơn giản | 1,7 s | 5 s | đạt |
| NFR-002 đầy đủ, câu đơn giản | 2,5 s | 20 s | đạt |
| NFR-009 chữ đầu, 5 câu đồng thời | 3,8 s | 15 s | đạt |
| NFR-009 đầy đủ, 5 câu đồng thời | 4,6 s | 60 s | đạt |
| NFR-003 đầy đủ, câu phức tạp | 12,6 s | 60 s | đạt |

**Mô hình nào chạy được trên phần cứng này — đã đo CẢ HAI.** Đây là điều đóng giả định
`ASM-project-004` bằng số liệu thay vì phỏng đoán:

| | nằm trên GPU | chữ đầu, 1 người | chữ đầu, 5 câu đồng thời | ngưỡng |
|---|---|---|---|---|
| `qwen2.5:3b-instruct-q4_K_M` | **100%** | 1,7 s | **3,8 s** | 15 s |
| `qwen2.5:7b-instruct-q4_K_M` | **84%** | 3,0 s | **12,0 s** | 15 s |

Bản 7b vẫn đạt mọi ngưỡng, nhưng chỉ nằm 84% trên GPU — 16% còn lại chạy trên CPU — và ăn
**80% ngân sách** của ngưỡng chặt nhất. Vì vậy mặc định là 3b: đó là cấu hình mà mọi số đo ở
trên được đo trên nó, và là cấu hình duy nhất qua được sàn `tracuu.llm.min-gpu-ratio`. Muốn
dùng 7b thì cần GPU lớn hơn, hoặc hạ sàn một cách có ý thức.

**Phạm vi quyền đổi kết quả rất mạnh — và ngược với phỏng đoán ban đầu.** Bản trước của tài
liệu này viết rằng phạm vi hẹp là ca đáng lo. Đo thật cho kết quả ngược lại:

| Phạm vi quyền của người hỏi | Chữ đầu p95, một người hỏi |
|---|---|
| 1 tài liệu (0,1% kho) | 933 ms |
| 39 tài liệu (4% kho) | 1,4 s |
| 996 tài liệu (100% kho) | 8,2 s (gộp chung hai loại câu — xem dưới) |

Càng nhiều đoạn được phép đọc thì càng nhiều ứng viên phải chấm điểm, nên **phạm vi rộng mới
là ca chậm**. Khi trích con số độ trễ ra ngoài, luôn kèm phạm vi quyền của tài khoản đo.

**Một lỗi của BỘ ĐO, không phải của sản phẩm.** Lần đo đầu trên kho đầy báo `NFR-001 KHÔNG
ĐẠT` — 8,2 giây so với ngưỡng 5 giây. Nhưng `nfr.md` khai `NFR-001` là ngưỡng của **câu hỏi
đơn giản**, còn câu phức tạp có ngưỡng riêng `NFR-003` là 60 giây, vì câu phức tạp cố ý trả
thêm một lượt gọi mô hình để phân rã truy vấn con — riêng lượt đó tốn khoảng 6 giây. Bộ đo
trộn hai nhóm vào một rổ rồi chấm tất cả bằng ngưỡng 5 giây. Đo từng câu một mới thấy rõ:
bốn câu đơn giản 1,6–2,1 giây, một câu phức tạp 8,2 giây.

Bộ đo nay **tách hai nhóm và chấm mỗi nhóm bằng đúng ngưỡng của nó**, và in thẳng "CHƯA
KIỂM" nếu bộ câu hỏi thiếu hẳn một nhóm — một ngưỡng không được kiểm mà không ai biết thì
tệ hơn một ngưỡng báo hỏng.

**Kho dùng để đo là kho TỔNG HỢP, và điều đó giới hạn kết luận rút ra được.** 1.000 quy chế
nội bộ tiếng Việt sinh bằng máy, mười lĩnh vực (nghỉ phép, chấm công, công tác, chi phí, mua
sắm, bảo mật, đào tạo, tuyển dụng, lương thưởng, an toàn), mỗi lĩnh vực một bộ từ vựng riêng.
Nó **đủ để đo độ trễ** — thứ phụ thuộc số đoạn và kích thước chỉ mục. Nó **không đủ để đo
chất lượng trả lời**: văn phong sinh ra lặp đều hơn văn bản thật nhiều nên các tài liệu cạnh
tranh nhau mạnh bất thường, và tỷ lệ từ chối đo được (5 trong 20 lượt) là tính chất của dữ
liệu thử chứ không phải của sản phẩm. Đo chất lượng cần kho thật kèm đáp án đối chiếu.

Chạy lại phép đo bất cứ lúc nào: xem mục *Đo độ trễ trên phần cứng thật*.

## Chạy nhanh bằng Docker

```bash
cp .env.example .env          # rồi sửa DB_PASSWORD
docker compose up -d --build

# Lần đầu: tải mô hình về (vài GB, chạy một lần)
docker exec tracuu-llm ollama pull qwen2.5:3b-instruct-q4_K_M
```

Mở `http://localhost:8080`.

### Máy đã có Ollama chạy sẵn

Tải lại vài gigabyte mô hình vào trong ảnh là lãng phí khi máy đã có một bản chạy được:

```bash
docker compose -f docker-compose.yml -f docker-compose.host-llm.yml up -d --build
```

Điều kiện: máy chủ mô hình trên máy phải nghe ở `0.0.0.0` chứ không chỉ `127.0.0.1`
(`OLLAMA_HOST=0.0.0.0:11434`), nếu không tiến trình trong ảnh không gọi tới được.

### Hai biến phải khai, nếu không sẽ khó hiểu

| Biến trong `.env` | Vì sao cần khai |
|---|---|
| `TRACUU_LLM_MODEL` | Phép kiểm khởi động số 3 **chặn** nếu máy chủ mô hình không có đúng mô hình đang cấu hình. Đó là hành vi đúng, nhưng một giá trị mặc định không khớp biến nó thành một lần "không khởi động được" không rõ nguyên nhân. |
| `TRACUU_ADMIN_PASSWORD` | Bỏ trống thì hệ thống sinh mật khẩu ngẫu nhiên và in ra nhật ký **đúng một lần**. Hợp lý ở nơi cài thật, bất tiện trên máy phát triển. |

### Bản dựng chịu được mạng chập chờn

Bước đóng gói trong ảnh **chạy lại tối đa tám lần**, và kho Maven nằm trong bộ nhớ đệm của
BuildKit. Đây không phải cẩn thận thừa: qua proxy doanh nghiệp, các artifact lớn bị cắt giữa
chừng với *"Premature end of Content-Length"*, và Maven mặc định không thử lại. Lần dựng đầu
trên máy này **cần bốn lượt** mới xong — mỗi lượt giữ lại phần đã tải nên lượt sau chỉ lấy phần
còn thiếu. Không có vòng lặp thì mỗi lần dựng là một lần tung đồng xu.

### Một ảnh, một tên — đừng để hai

Dịch vụ `app` khai `image: tracuu-app:0.1.0` tường minh. Bỏ dòng đó đi thì Compose tự đặt tên
ảnh theo tên thư mục dự án, còn lệnh `docker build -t tracuu-app:0.1.0` trong tài liệu gắn một
thẻ khác — **hai ảnh khác nhau**, và `docker compose up -d` vẫn khởi động ảnh cũ sau khi bạn
vừa dựng lại xong.

Đây là loại sai tệ nhất vì mọi tín hiệu đều xanh: bản dựng báo thành công, container báo khoẻ,
chỉ có mã đang chạy là mã cũ. Nó đã xảy ra thật ngày 29/09 và chỉ lộ ra khi một bản vá vừa dựng
xong lại không có tác dụng. Cách kiểm trong ba mươi giây:

```bash
docker compose ps -q app | xargs docker inspect --format '{{.Image}}'
docker image inspect tracuu-app:0.1.0 --format '{{.Id}}'
```

Hai mã băm phải trùng nhau. Lệch nhau thì container đang chạy thứ khác với thứ bạn vừa dựng.

**Tài khoản quản trị đầu tiên** được tạo tự động lúc khởi động nếu chưa có quản trị nào.
Mật khẩu in ra nhật ký **đúng một lần**:

```bash
docker logs tracuu-app | grep -A4 "tài khoản quản trị"
```

Muốn tự chọn mật khẩu thì đặt `TRACUU_ADMIN_PASSWORD` trước khi khởi động lần đầu.
Dù thế nào, lần đăng nhập đầu tiên hệ thống cũng bắt đổi mật khẩu.

### Quản trị KHÔNG đọc được tài liệu — và đó là cố ý

Điều dễ tưởng là hỏng nhất khi mở hệ thống lần đầu: đăng nhập bằng tài khoản quản trị rồi hỏi
một câu, và nhận về *"Tôi không tìm thấy đủ căn cứ trong kho tài liệu"* — dù kho có cả nghìn
tài liệu.

Đó là mô hình quyền **từ chối mặc định** làm đúng việc của nó: vai quản trị cho phép *quản lý*
tài liệu và tài khoản, **không** tự động cho phép *đọc nội dung*. Muốn đọc thì phải được cấp
quyền trên từng tài liệu như mọi người khác. Không có đường vòng cho quản trị, kể cả ở màn hỏi
đáp — vì một đường vòng như thế sẽ biến mọi câu trả lời cho quản trị thành một lỗ rò tiềm ẩn.

Nên khi thử màn hỏi đáp, dùng một tài khoản đã được cấp quyền đọc.

Cấp quyền làm ngay trên giao diện, ở ba chỗ:

| Chỗ | Dùng khi |
|---|---|
| **Tài liệu → nút `Quyền`** trên từng dòng | Mở quyền cho một tài liệu; xem ai đang đọc được và vì sao |
| **Tài liệu → tích nhiều dòng → `Cấp quyền cho N tài liệu`** | Mở quyền cho cả một nhóm tài liệu một lần |
| **Tài liệu → `Nạp tài liệu`** | Chọn luôn "phòng ban nào được đọc lô này" *trước khi* nạp — cách ít quên nhất |

Tài khoản, phòng ban và nhập danh sách hàng loạt nằm ở màn **Tài khoản**. Cấp cho **cả phòng
ban** là đường nhanh nhất: mọi thành viên được hưởng ngay, và người vào sau cũng tự có.

## Chạy khi đang phát triển

Hai tiến trình cho nhanh, thay vì dựng lại ảnh mỗi lần sửa giao diện:

```bash
# Hạ tầng
docker compose up -d db cache llm

# Máy chủ
cd app && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Giao diện, cổng 5173, tự chuyển tiếp /api sang cổng 8080
cd web && npm install && npm run dev
```

## Sáu phép kiểm lúc khởi động

`app` chạy sáu phép kiểm **theo thứ tự** trước khi nhận yêu cầu đầu tiên. Thất bại ở bất kỳ
phép nào thì dừng hẳn kèm thông báo nêu rõ thiếu gì và làm gì tiếp — **không khởi động ở chế
độ suy giảm**.

1. Cơ sở dữ liệu kết nối được.
2. Phần mở rộng vector có mặt **và đủ mới để quét chỉ mục lặp** (tối thiểu 0.8.0).
3. Máy chủ mô hình kết nối được và mô hình đã tải về.
4. Mô hình đang chạy trên GPU, không phải CPU.
5. Mô hình sinh biểu diễn ngữ nghĩa nạp được.
6. Bộ nhớ đệm kết nối được.

Phép 2 và phép 4 là hai phép đáng giá nhất: cả hai bắt đúng loại lỗi mà nếu không kiểm thì
hệ thống **vẫn chạy**, chỉ là chạy sai hoặc chạy chậm, và người vận hành không có cách nào
biết.

### Mô hình sinh biểu diễn ngữ nghĩa

Thiết kế chạy ONNX ngay trong máy ảo Java, **trên CPU** — bộ nhớ đồ hoạ dành trọn cho mô hình
sinh chữ. Đặt hai tệp vào thư mục `models/`:

```
models/model.onnx        mô hình sinh biểu diễn, 768 chiều
models/tokenizer.json    bộ tách token đi kèm mô hình
```

Chưa có hai tệp ấy thì hệ thống **vẫn chạy** bằng đường dự phòng (sinh biểu diễn qua máy chủ
mô hình), và nói rõ điều đó trong nhật ký. Hệ quả của đường dự phòng: việc nạp tài liệu tranh
GPU với việc trả lời câu hỏi. Chấp nhận được lúc thử nghiệm, không phải hình dạng đã thiết kế.

## Bộ đo — chạy bằng lệnh, không có đường mạng

Bộ đo là công cụ của tác giả, không phải tính năng của người dùng. Mở nó ra thành đường mạng
nghĩa là phải phân quyền, phải giới hạn tần suất, và phải xử lý một lượt chạy kéo dài hàng giờ
qua kết nối HTTP. Chạy bằng lệnh không có vấn đề nào trong số đó.

```bash
# Nạp bộ câu hỏi chuẩn
java -jar app/target/tracuu.jar --import-questions=bo-cau-hoi.json --questions=v1

# Chạy đo
java -jar app/target/tracuu.jar --eval \
     --questions=v1 --user=quantri --mode=hybrid --label="lai-cau-truc"

# Chạy tiếp lượt dở dang
java -jar app/target/tracuu.jar --eval --resume=3 --user=quantri

# Nhập điểm do NGƯỜI chấm, để đối chiếu với điểm máy
java -jar app/target/tracuu.jar --import-grades=diem.json      --grader="Nguyen Van A" --run=3
```

Điểm người ghi vào bảng riêng, **không ghi đè điểm máy**. Thứ có giá trị không phải điểm cuối
cùng mà là *độ lệch giữa hai bên*: một bộ chấm tự động lệch xa người chấm thì mọi con số nó sinh
ra đều phải đọc kèm cảnh báo, và ghi đè là xoá mất đúng phép so ấy. Lệnh in ra ba con số — độ
lệch trung bình (máy chấm chặt hay lỏng hơn), độ lệch tuyệt đối trung bình (sai số điển hình),
và độ lệch lớn nhất (ca tệ nhất, thường mới là ca đáng đi xem).

`--user` **bắt buộc**: bộ đo chạy dưới danh tính một tài khoản thật, vì phạm vi quyền quyết
định tập tài liệu mà truy hồi nhìn thấy. Một lượt đo không nói rõ chạy dưới quyền ai là một
con số không diễn giải được.

`--mode` nhận `hybrid`, `vector` hoặc `bm25` — đây là cách so ba phương pháp tìm kiếm bằng số
liệu thay vì bằng cảm tính.

**Mọi lượt chạy ghi kèm cấu hình đầy đủ.** Hai lượt khác cấu hình thì không so được với nhau
mà trông vẫn như so được; cột cấu hình tồn tại để chặn đúng chuyện đó.

## Đo độ trễ trên phần cứng thật

Lệnh riêng, vì nó trả lời câu khác với bộ đo chất lượng: bộ đo trả lời *"câu trả lời có đúng
không"*, lệnh này trả lời *"hệ thống có nhanh đủ dùng không"*. Hai câu ấy cần hai điều kiện chạy
khác nhau — đo chất lượng chạy tuần tự cho tất định, đo độ trễ phải chạy **đồng thời** để thấy
chỗ suy giảm.

```bash
java -jar app/target/tracuu.jar --measure --user=quantri

# Đo nhiều mức đồng thời, lặp 10 lượt mỗi mức
java -jar app/target/tracuu.jar --measure --user=quantri --concurrency=1,2,5 --repeat=10
```

Kết quả in kèm **ba điều kiện đo mà thiếu một là số vô nghĩa**:

1. **Mô hình có nằm trên bộ nhớ đồ hoạ không.** Cùng một mô hình chạy CPU chậm hơn hàng chục
   lần; bảng số không nói rõ điều này thì vô dụng.
2. **Ngữ cảnh dài đúng bằng giá trị cấu hình.** Thời gian xử lý lời nhắc tỉ lệ thuận với số
   token, nên đo với ngữ cảnh ngắn cho con số đẹp mà vận hành thật không bao giờ đạt.
3. **Phạm vi quyền của người đo.** Truy vấn bật quét chỉ mục lặp khi phạm vi hẹp, và quét lặp
   đổi độ trễ lấy độ phủ — đo ở phạm vi toàn kho là đo ở điều kiện thuận lợi nhất.

Lượt đầu tiên là **lượt làm nóng** và không tính vào kết quả: nó gánh cả việc nạp mô hình vào
bộ nhớ đồ hoạ, một chi phí chỉ xảy ra một lần.

Lệnh **không tự kết luận đạt hay không đạt toàn cục**, và đó là chủ đích. Ngưỡng ở tài liệu khai
kèm điều kiện *kho 1.000 tài liệu, khoảng 50.000 đoạn*; đo trên kho nhỏ hơn mà đạt thì chưa
chứng minh được sẽ đạt ở kho đầy. Kết quả in ra nói rõ hai giới hạn ấy thay vì để người đọc
tưởng đã xong.

### Dạng tệp bộ câu hỏi

```json
[
  { "ordinal": 1,
    "text": "Nghỉ phép năm được bao nhiêu ngày?",
    "complexity": "DON_GIAN",
    "expectedAnswer": "12 ngày làm việc",
    "positions": [ { "documentId": 3, "charStart": 1200, "charEnd": 1480 } ] }
]
```

Vị trí đáp án khai bằng **khoảng ký tự trong tài liệu**, không khai bằng mã đoạn. Nhờ vậy cùng
một bộ câu hỏi dùng lại được cho mọi chiến lược tách đoạn mà không phải soạn lại đáp án.

## Metadata hệ thống đề xuất

Sau mỗi lần nạp, hệ thống đề xuất tên tài liệu, danh mục, ngày hiệu lực và nhãn mật —
**mỗi giá trị kèm trích đoạn làm căn cứ**, lấy nguyên văn từ tài liệu.

Mỗi trích đoạn được **tìm lại trong văn bản gốc** trước khi tin. Không tìm thấy thì giá trị vẫn
được lưu để quản trị xem, nhưng đánh dấu *chưa đối chiếu* và **không tự áp** lên tài liệu. Lý
do: mô hình trả về căn cứ rất thuyết phục cho những câu không hề có trong tài liệu, và một ngày
hiệu lực bịa mà tự động áp sẽ làm mọi cảnh báo hết hiệu lực về sau tính trên con số sai.

Hai thứ **không bao giờ tự áp**, kể cả khi có căn cứ:

- **Nhãn mật** — nâng nhãn mật lấy quyền đọc của người khác; đó là quyết định của người.
- **Danh mục ngoài danh sách** — danh mục là danh sách đóng do quản trị đặt.

Ngày tháng thì **không hỏi mô hình**: dò bằng biểu thức trên chính văn bản, theo cụm chỉ dấu
(*có hiệu lực từ*, *hết hiệu lực*). Văn bản hành chính luôn mở đầu bằng ngày ban hành, mà ngày
ban hành khác ngày có hiệu lực — lấy bừa ngày đầu tiên là gán sai cho gần như mọi văn bản.

Xem đề xuất của một tài liệu: `GET /api/admin/documents/{id}/metadata-suggestion`

Muốn bắt xác nhận trước khi tài liệu vào chỉ mục thì bật
`tracuu.ingestion.require-metadata-confirmation`. Mặc định tắt, vì gate thật đã nằm ở tầng
quyền: nhãn mật mặc định là mức thấp nhất và quyền đọc là từ-chối-mặc-định, nên tài liệu chưa
xác nhận vẫn không ai đọc được cho tới khi có người cấp quyền.

## Hiệu chỉnh

Mọi ngưỡng nằm trong `app/src/main/resources/application.yml`, không có ngưỡng nào là hằng số
trong mã. Sửa tệp rồi khởi động lại là đủ; không phải dịch lại.

Những tham số hay phải đổi nhất:

| Tham số | Nghĩa |
|---|---|
| `tracuu.retrieval.top-k` | Số đoạn đưa vào ngữ cảnh |
| `tracuu.context.max-tokens` | **Mọi ngưỡng độ trễ treo lên con số này.** Đo với ngữ cảnh ngắn hơn là đo sai điều kiện |
| `tracuu.retrieval.mode` | `hybrid` · `vector` · `bm25` |
| `tracuu.chunking.default-strategy` | `structural` · `fixed` |
| `tracuu.segmentation.strategy` | `longest-match` · `whitespace` |
| `tracuu.agent.refusal.min-top-score` | Ngưỡng từ chối ở tầng một |
| `tracuu.qa.max-concurrent-questions` | Số câu xử lý cùng lúc, quá thì xếp hàng |
| `tracuu.ingestion.thread-pool-size` | Số luồng nạp tài liệu |

## Kiểm thử

Hai tầng, tách bằng quy ước đặt tên, để không ai phải chọn giữa *chạy nhanh* và *chạy đủ*.

### Tầng một: không cần hạ tầng

```bash
cd app && mvn test
```

Nhắm vào phần logic thuần: luật đọc quyền, nhận diện tiêu đề văn bản pháp quy, tách từ tiếng
Việt, tách đoạn và lặp dòng tiêu đề của bảng, tầng chặn gắn nguồn, và **bất biến về câu từ
chối**.

Bài đáng chú ý nhất là `RefusalDeciderTest`: nó chốt rằng hai ca *kho không có tài liệu* và
*tài liệu nằm ngoài quyền* phải cho ra **cùng một câu chữ**. Khác nhau dù một chữ là đủ để
người hỏi so hai lần từ chối và suy ra tài liệu nào tồn tại ngoài quyền mình.

### Tầng hai: trên cơ sở dữ liệu thật

Tầng này ghi tài liệu, người dùng và quyền **thật** rồi tự dọn, nên nó cần một cơ sở dữ liệu
riêng — chĩa vào cơ sở dữ liệu đang phục vụ là trộn dữ liệu thử vào dữ liệu vận hành. Dựng một
lần:

```bash
docker compose exec -T db psql -U tracuu -d postgres   -c "CREATE ROLE tracuu_it LOGIN PASSWORD 'kiemthu_cucbo';"   -c "CREATE DATABASE tracuu_it OWNER tracuu_it;"
docker compose exec -T db psql -U tracuu -d tracuu_it   -c "CREATE EXTENSION IF NOT EXISTS vector;" -c "GRANT ALL ON SCHEMA public TO tracuu_it;"
```

Rồi chạy:

```bash
cd app && mvn verify -Dtracuu.it.enabled=true   -Dtracuu.it.db=jdbc:postgresql://localhost:5432/tracuu_it   -Dtracuu.it.user=tracuu_it -Dtracuu.it.password=kiemthu_cucbo   -Dtracuu.it.redis=localhost
```

Mật khẩu phải truyền được từ ngoài: cơ sở dữ liệu trong bản Docker lấy mật khẩu từ `.env`, nên
một giá trị ghi cứng trong mã kiểm thử biến *chạy kiểm thử trên đúng hạ tầng vừa dựng* thành
một lỗi xác thực khó hiểu.

Thiếu cờ `-Dtracuu.it.enabled=true` thì phần này **tự bỏ qua** (14 bài báo *Skipped*, bản dựng
vẫn xanh) trên máy chưa dựng hạ tầng.

Tầng này tồn tại vì phần rủi ro nhất của hệ thống nằm trong **SQL**, không nằm trong logic
Java: truy vấn truy hồi lai có mệnh đề lọc quyền ghép động, phép trải vị trí từ để lấy tần
suất, toán tử khoảng cách vector, và 29 bảng phải khớp với các thực thể. Không thứ nào trong số
đó kiểm được bằng mock.

Dịch vụ mô hình được thay bằng bản giả tất định — phần sinh biểu diễn ngữ nghĩa là một phép
nhúng túi-từ thật, nên nhánh tìm theo vector <em>thật sự được kiểm</em>. Ngưỡng từ chối
**không bị ghi đè**: phép kiểm chạy trên đúng con số của sản phẩm.

**Ba lỗi mà chỉ tầng này bắt được** (29/09), cả ba đều biên dịch sạch và qua hết kiểm thử đơn vị:

| Lỗi | Vì sao không tầng nào khác thấy |
|---|---|
| Năm kho dữ liệu của bộ đo khai lồng trong một lớp | Spring chỉ dựng bean cho interface cấp cao nhất; interface lồng bị bỏ qua **im lặng** tới lúc khởi động |
| Bốn cột `jsonb` ánh xạ từ trường `String` không khai kiểu | PostgreSQL không tự ép chuỗi sang `jsonb`; đổ ở **lần ghi thật đầu tiên** |
| Nhật ký kiểm toán ghi trong giao dịch chỉ-đọc | Làm hỏng cả giao dịch, nên đường đọc-không-lọc **vừa mất kiểm toán vừa không chạy** — mọi lý do *thiếu quyền* của báo cáo khoảng trống bị xếp nhầm, không lỗi nào báo |

### Tầng ba: giao diện chạy thật trong trình duyệt

```bash
node kiem-giao-dien.mjs        # cần Chrome trên máy, không cần cài gói nào
```

Mở Chrome ở chế độ không giao diện, nối vào giao thức gỡ lỗi của nó (Node 24 có sẵn
WebSocket, nên không phải tải bộ trình duyệt 130 MB qua proxy), rồi đi đúng đường người dùng
đi: tải trang, thử sai mật khẩu, đăng nhập, mở lần lượt bốn màn quản trị, đổi sang tài khoản
đọc được, hỏi một câu và chờ tới khi luồng chữ kết thúc. Nó cũng thu lỗi JavaScript và mọi
đường gọi trả mã ≥ 400.

**Vì sao tầng này cần tồn tại.** Ngày 30/09 nó tìm ra hai lỗi mà 51 phép kiểm đơn vị và 14
phép kiểm tích hợp đều không thấy, vì không phép kiểm nào trong số đó đi qua tầng HTTP:

| Lỗi | Triệu chứng | Vì sao các tầng khác mù |
|---|---|---|
| Màn **Tài khoản** trả 500 cho mọi lần mở | Danh sách người dùng ánh xạ sang bản hiển thị *sau khi* phiên Hibernate đã đóng, nên chạm quan hệ phòng ban (LAZY) là nổ | Kiểm thử gọi thẳng lớp Java, nơi phiên vẫn còn mở |
| Lỗi giữa luồng hỏi đáp sinh ra ngoại lệ **thứ hai** | Bộ xử lý lỗi cố ghi thân JSON vào một phản hồi đã đặt kiểu `text/event-stream`, che mất ngoại lệ gốc | Chỉ xảy ra trên phản hồi dạng luồng, mà không kiểm thử nào tạo ra phản hồi dạng luồng |

Lỗi thứ nhất nay có phép kiểm hồi quy riêng (`AdminScreensIT`) mở lần lượt mọi đường GET của
màn quản trị. Phép kiểm đó đã được **kiểm ngược**: trả mã về bản hỏng thì nó đỏ với đúng
`LazyInitializationException` — một phép kiểm chỉ đáng tin khi nó hỏng trên mã hỏng.

### Dựng hạ tầng cho kiểm thử khi không có Docker

Máy không chạy được Docker vẫn dựng được bằng WSL:

```bash
# trong WSL, dưới root
apt-get install -y postgresql postgresql-18-pgvector redis-server
# cho phép kết nối từ Windows
sed -i "s/^#\?listen_addresses.*/listen_addresses = '*'/" /etc/postgresql/18/main/postgresql.conf
echo "host all all 0.0.0.0/0 md5" >> /etc/postgresql/18/main/pg_hba.conf
pg_ctlcluster 18 main restart
su - postgres -c "psql -c \"CREATE ROLE tracuu LOGIN PASSWORD 'tracuu_dev'\"                        -c \"CREATE DATABASE tracuu OWNER tracuu\""
su - postgres -c "psql -d tracuu -c 'CREATE EXTENSION vector'"
```

**Phần mở rộng vector phải từ 0.8.0 trở lên** — bản cũ hơn không có chế độ quét chỉ mục lặp, và
phép kiểm khởi động số 2 sẽ chặn. Ubuntu 26.04 đóng gói 0.8.1.

Một lưu ý về WSL: máy ảo **tự tắt khi không phiên nào giữ**, nên giữ một phiên sống trong lúc
chạy kiểm thử (`wsl -d Ubuntu -- sleep 900 &`). Không có nó thì lỗi hiện ra dưới dạng *connection
refused* rất khó đoán nguyên nhân.

## Môi trường dựng

`app/.mvn/settings.xml` bỏ qua gương kho nội bộ và đi thẳng Maven Central. Khối `<proxies>`
trong tệp ấy là **của máy dựng hiện tại**, không phải của sản phẩm — máy nào ra thẳng internet
thì xoá cả khối đó đi, mọi thứ còn lại vẫn chạy.

## Điều hệ thống cố ý KHÔNG có

Ghi ra để người đọc không tự suy là có:

- **Không có đường mạng nào cho phép truy hồi mà bỏ qua lọc quyền.** Đọc không lọc tồn tại ở
  đúng một chỗ — khâu phân loại lý do khoảng trống — và mọi lần đều ghi nhật ký kiểm toán.
- **Không có đường xoá hàng loạt tài liệu.** Xoá vĩnh viễn là thao tác từng tài liệu một.
- **Không có đường nào lọc báo cáo khoảng trống theo người dùng.** Báo cáo trả lời *"kho thiếu
  gì"*, không trả lời *"ai hỏi gì"*.
- **Không cam kết chạy khi không có GPU.** Không có chế độ suy giảm dùng CPU.
- **Không có bảng cấu hình trong cơ sở dữ liệu.** Mọi tham số sống trong tệp cấu hình, để kết
  quả đo chụp lại được nguyên vẹn điều kiện sinh ra nó.
