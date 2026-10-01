---
type: architecture
status: draft
updated: 2026-09-24
links:
  - docs/decisions/0003-technical-architecture.md
  - docs/decisions/0002-scope-and-internal-design.md
  - docs/_shared/nfr.md
---

# Kiến trúc kỹ thuật

> Quyết định và lý do ở `docs/decisions/0003-technical-architecture.md`. File này mô tả **kết quả**:
> hệ thống gồm gì, mã nguồn chia thế nào, và ranh giới nào không được vượt.

## 1. Bốn dịch vụ

```
                    máy của nhân viên
                           │  trình duyệt
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  app  —  Spring Boot 3 / Java 21                             │
│                                                              │
│   tệp tĩnh giao diện  ·  giao diện lập trình  ·  luồng SSE   │
│   nghiệp vụ  ·  bảy module agent  ·  nạp tài liệu            │
│   sinh biểu diễn ngữ nghĩa (ONNX trong JVM)                  │
│   tách từ tiếng Việt (thư viện Java)                         │
│   tính điểm BM25                                             │
└───────┬──────────────────────┬───────────────────┬───────────┘
        │                      │                   │
        ▼                      ▼                   ▼
┌───────────────┐      ┌──────────────┐    ┌──────────────┐
│ db            │      │ llm          │    │ cache        │
│ PostgreSQL    │      │ Ollama       │    │ Redis        │
│ + vector      │      │ + GPU        │    │              │
│               │      │              │    │ phiên        │
│ tài liệu      │      │ sinh chữ     │    │ hàng đợi     │
│ đoạn + vector │      │ gợi ý        │    │ tra cứu      │
│ chỉ mục toàn  │      │ metadata     │    │ quyền dùng   │
│ văn + thống kê│      │ chấm điểm    │    │ lại trong    │
│ người dùng    │      │              │    │ lượt         │
│ quyền         │      │              │    │              │
│ hội thoại     │      │              │    │              │
│ nhật ký       │      │              │    │              │
└───────────────┘      └──────────────┘    └──────────────┘
```

**Giao diện không có tiến trình riêng.** Vite dựng ra tệp tĩnh lúc dựng ảnh, `app` phục vụ chúng.

**Chỉ `llm` cần GPU.** Sinh biểu diễn ngữ nghĩa chạy trên CPU trong `app` — đây là lý do
`NFR-system-022` đòi tối thiểu 4 nhân, và là lý do việc nạp tài liệu tranh tài nguyên với việc trả
lời câu hỏi (`NFR-system-011` đặt ngưỡng để đo chuyện đó).

## 2. Bố cục gói mã nguồn

```
app/src/main/java/vn/utc/tracuu/
│
├── platform/
│   ├── config/       ràng buộc cấu hình thành bản ghi bất biến
│   ├── security/     xác thực, ngữ cảnh người dùng hiện tại
│   ├── web/          xử lý lỗi chung, luồng sự kiện một chiều, hàng đợi câu hỏi
│   └── startup/      kiểm điều kiện khởi động
│
├── access/           người dùng · phòng ban · danh sách kiểm soát · nhãn mật
│   └── PermissionResolver          ◄── cửa DUY NHẤT quyết định ai đọc được gì
│
├── knowledge/        tài liệu · đoạn · vòng đời · cụm chủ đề
│
├── ingestion/        bóc văn bản · tách đoạn · xử lý bảng · sinh biểu diễn ·
│                     gợi ý metadata · phát hiện chồng lấn
│
├── agent/
│   ├── AgentPipeline               bộ điều phối tường minh
│   ├── AgentContext                ngữ cảnh có kiểu, đi qua cả bảy bước
│   ├── prompt/       chuẩn hoá câu hỏi, nhận diện ý định
│   ├── reasoning/    phân loại đơn giản / phức tạp
│   ├── memory/       lịch sử lượt · viết lại câu hỏi tiếp nối · tóm tắt cuộn
│   ├── planner/      phân rã thành truy vấn con có thứ tự
│   ├── retriever/    BM25 · vector · hợp nhất thứ hạng · lọc quyền trước xếp hạng
│   ├── tool/         tra metadata · hiệu lực · chồng lấn
│   └── action/       tổng hợp · trích dẫn · kiểm gắn nguồn · từ chối
│
├── qa/               phiên hội thoại · điểm vào giao diện lập trình
├── gap/              nhật ký từ chối · báo cáo khoảng trống
└── evaluation/       bộ chạy đo · chỉ số · chấm điểm · xuất bảng
```

Bảy thư mục con của `agent/` **trùng tên và trùng thứ tự** với bảy module GVHD chỉ định. Đây là chủ
đích: sơ đồ trong báo cáo và cây thư mục thật là một, không phải hai thứ phải giải thích cho nhau.

## 3. Đường đi một câu hỏi

```
trình duyệt
   │  POST câu hỏi
   ▼
qa/          mở hoặc lấy phiên, ghi lượt hỏi
   ▼
platform/web hàng đợi: quá 5 câu đang xử lý thì xếp hàng, trả về vị trí
   ▼
agent/AgentPipeline  ─────────────────────────────────────────────┐
   │                                                              │
   ├─ 1. prompt      chuẩn hoá, nhận diện ý định                  │
   ├─ 2. reasoning   đơn giản hay phức tạp?                       │
   ├─ 3. memory      câu rút gọn → viết lại thành câu độc lập     │  mỗi bước
   ├─ 4. planner     phức tạp → tách truy vấn con có thứ tự       │  trả về một
   ├─ 5. retriever   ┌ access/PermissionResolver → tập được phép  │  AgentContext
   │                 ├ BM25 trên văn bản đã tách từ               │  MỚI, không
   │                 ├ tìm theo vector trong tập được phép        │  sửa tại chỗ
   │                 └ hợp nhất thứ hạng                          │
   ├─ 6. tool        hiệu lực · chuỗi thay thế · cụm chồng lấn    │
   └─ 7. action      ┌ không đủ căn cứ → từ chối (tầng một)       │
                     ├ sinh chữ, đẩy dần qua luồng sự kiện        │
                     ├ kiểm mọi ý gắn được về đoạn (tầng hai)     │
                     └ gắn trích dẫn + cảnh báo                   │
   ▼                                                              │
gap/         nếu từ chối: ghi nhật ký khoảng trống ◄──────────────┘
   ▼
trình duyệt  nhận chữ hiện dần, rồi cảnh báo, rồi khối nguồn
```

## 4. Bảy luật kiến trúc

Bảy luật này là thứ giữ cho hệ thống không trôi. Mỗi luật kèm lý do; luật không có lý do thì sớm
muộn sẽ bị vi phạm vì "lần này thì tiện hơn".

**R1 — Mọi truy vấn chạm bảng đoạn đều mang một phạm vi quyền tường minh.** Không hàm nào đọc bảng
đoạn mà không nhận tham số `PermissionScope`, và tham số ấy chỉ có **đúng ba giá trị hợp lệ**:

| Giá trị | Ai dùng | Ghi nhật ký kiểm toán |
|---|---|---|
| `user(id)` | Mọi đường đi từ giao diện — hỏi đáp, xem tài liệu gốc, tải tệp | không |
| `evaluation(principal)` | Bộ đo, chạy dưới danh tính một tài khoản thật có phạm vi quyền xác định | không |
| `unfiltered(lý do)` | **Chỉ** khâu phân loại lý do khoảng trống, và chỉ sinh từ gói `gap/` | **có, mọi lần** |

*Vì sao:* `AC-008` của US-008 đòi mọi đường dẫn tới nội dung đều áp cùng một luật đọc. Có hai chỗ
tính quyền thì sớm muộn hai chỗ đó lệch nhau, và chỗ lệch chính là chỗ rò rỉ.

> **Vì sao phát biểu lại luật này (2026-09-24).** Bản trước viết tuyệt đối — *"không truy vấn nào
> chạm bảng đoạn mà không đi qua nó"* — nhưng hệ thống **có ba đường cần đọc không lọc quyền**, và
> luật tuyệt đối không xoá được chúng, chỉ đẩy chúng xuống thành ngoại lệ không ai theo dõi.
>
> Ba đường ấy: **một**, phân loại lý do *thiếu quyền* ở `AC-002` của US-027 — muốn biết người hỏi bị
> chặn vì thiếu quyền chứ không phải vì kho trống thì buộc phải so kết quả có lọc với kết quả không
> lọc, và cả tab *thiếu quyền* của báo cáo khoảng trống đứng trên năng lực đó. **Hai**, chính mục 9
> của hợp đồng giao diện lập trình tự cho phép *"cần xem kết quả không lọc thì chạy bằng lệnh"* —
> ngay dưới câu cấm, nghĩa là R1 thật ra vẫn luôn là "không có đường HTTP nào". **Ba**, bộ đo chạy
> dưới danh tính gì thì trước đây không tài liệu nào nói, trong khi `AC-004` của US-030 lại đòi chạy
> được với một tài khoản có phạm vi quyền hẹp.
>
> Ba giá trị ở bảng trên biến cả ba đường thành **hợp pháp và đếm được**. Nếu để luật tuyệt đối,
> người hiện thực gặp bế tắc sẽ tự cài một đường vòng, và đường ấy nằm im trong bản bàn giao mà
> không ai biết để kiểm.

**R2 — Gói `agent/` không biết gì về giao thức web.** Không lớp nào trong đó nhận đối tượng yêu cầu
HTTP hay trả về đối tượng phản hồi HTTP. *Vì sao:* bộ đo ở `evaluation/` gọi thẳng `AgentPipeline`
không qua tầng web. Nếu agent dính vào web thì bộ đo phải dựng máy chủ giả, và nó sẽ đo một thứ
khác với thứ người dùng dùng.

**R3 — `AgentContext` bất biến; mỗi bước trả về một bản mới.** *Vì sao:* `AC-005` của US-031 đòi ghi
lại được toàn bộ quá trình từng câu — câu đã viết lại, các truy vấn con, các đoạn đã truy hồi. Với
ngữ cảnh bất biến, chuỗi các bản ghi **chính là** bản ghi quá trình, không cần rải mã ghi nhật ký
khắp nơi. Đây cũng là thứ làm phần phân tích lỗi trong báo cáo viết được.

**R4 — Mọi tham số thí nghiệm nằm trong đúng một bản ghi cấu hình, và bản ghi đó được ghi kèm mọi
kết quả đo.** *Vì sao:* `NFR-system-045` và `NFR-system-046`. Hai lần chạy không kèm cấu hình thì
không so được với nhau mà trông vẫn như so được.

**R5 — `ingestion/` không gọi `agent/`, và `agent/` không gọi `ingestion/`.** Chúng gặp nhau qua
`knowledge/`. *Vì sao:* nạp tài liệu là việc theo đợt, trả lời là việc theo yêu cầu; trộn hai vòng
đời đó vào nhau là cách nhanh nhất để việc nạp làm treo việc trả lời.

**R6 — `evaluation/` gọi `AgentPipeline` đúng như một người dùng gọi.** Cùng đường mã, chỉ khác đầu
vào và khác cách ghi kết quả. *Vì sao:* bộ đo đi đường riêng thì nó đo một hệ thống khác với hệ
thống được bàn giao — và mọi con số trong chương thực nghiệm nói về hệ thống không tồn tại.

**R7 — Việc gợi ý metadata và việc chấm điểm dùng chung `llm` nhưng khác hoàn toàn đường mã trả
lời.** *Vì sao:* chấm điểm mà đi qua chính đường sinh câu trả lời thì máy chấm và máy bị chấm chia
sẻ trạng thái, và kết quả chấm mất giá trị.

## 5. Thí nghiệm bóc tách hoạt động thế nào

Đây là chỗ kiến trúc phục vụ trực tiếp chương thực nghiệm.

| Cấu hình | Thay đổi gì | Cách làm |
|---|---|---|
| Agent đầy đủ | — | Hiện thực mặc định của cả bảy module |
| Hỏi đáp một lượt | Bỏ Planner và Memory | Planner thay bằng hiện thực trả về đúng một truy vấn con là chính câu hỏi; Memory thay bằng hiện thực không mang ngữ cảnh |
| Agent bỏ Planner | Chỉ bỏ Planner | Như trên nhưng giữ nguyên Memory |
| Chỉ BM25 | Retriever dùng một nhánh | Tham số cấu hình của Retriever |
| Chỉ vector | Retriever dùng một nhánh | Tham số cấu hình |
| Không kiểm gắn nguồn | Bỏ tầng chặn thứ hai | Cờ cấu hình trong Action |
| Chiến lược tách đoạn | Đổi cách tách | Tham số cấu hình của Ingestion, cần nạp lại kho |

**Sáu trên bảy dòng không cần nạp lại dữ liệu và không cần dựng lại mã.** Chỉ dòng cuối cần nạp lại
kho, vì nó đổi chính cách dữ liệu được lưu.

## 6. Khởi động: kiểm trước, chạy sau

`platform/startup/` chạy các phép kiểm dưới đây **theo thứ tự** trước khi `app` nhận yêu cầu đầu
tiên. Thất bại ở bất kỳ phép nào thì dừng hẳn kèm thông báo nêu rõ thiếu gì và làm gì tiếp — không
khởi động ở chế độ suy giảm.

| # | Kiểm gì | Vì sao ở bước này | Thất bại thì |
|---|---|---|---|
| 1 | `db` kết nối được | Không có gì chạy được nếu thiếu | Dừng |
| 2 | Phần mở rộng vector có mặt **và đủ mới để quét chỉ mục lặp** | Thiếu tính năng này thì lọc quyền trước xếp hạng làm tụt độ phủ — `AC-006` của US-008 sẽ không đạt | Dừng, nêu phiên bản hiện có và phiên bản tối thiểu |
| 3 | `llm` kết nối được và mô hình đã có | Lần cài đầu tải mô hình ở đây | Dừng, hoặc bắt đầu tải nếu là lần cài đầu |
| 4 | Mô hình đang chạy trên GPU, không phải CPU | `NFR-system-020` — thà không chạy còn hơn chạy chậm | Dừng, nêu tình trạng máy và điều kiện yêu cầu |
| 5 | Mô hình sinh biểu diễn ngữ nghĩa nạp được | Nạp tài liệu sẽ hỏng nếu thiếu | Dừng |
| 6 | `cache` kết nối được | Phiên đăng nhập và hàng đợi | Dừng |

Bước 2 và bước 4 là hai bước đáng giá nhất: cả hai đều bắt đúng loại lỗi mà nếu không kiểm thì hệ
thống vẫn chạy, chỉ là chạy sai hoặc chạy chậm, và người vận hành không có cách nào biết.

## 7. Những ẩn số phải đo sớm, không đợi muộn hơn

Các con số dưới đây hiện là giả định. Tất cả đều rẻ để đo và đắt để phát hiện muộn.

**Hai phép đo về mô hình phải chạy TRƯỚC 05/10, không phải trong sprint 1** (sửa 2026-09-24). Chính
`Method` của `ASM-project-004` viết *"chạy thử đo thời gian trên chính máy đo, **trước khi viết dòng
code đầu tiên**"* và hạn xác nhận là 05/10 — đúng ngày sprint 1 bắt đầu. Nhưng roadmap lại xếp nó
vào sprint 1, tức tới 18/10 mới có câu trả lời: phép đo quan trọng nhất của dự án đang bị trượt hai
tuần ngay trên giấy. Phép đo dung lượng bộ nhớ đồ hoạ khi chạy đồng thời gộp chung buổi ấy, vì cả
hai dùng chung một lần nạp mô hình.

| Ẩn số | Đo thế nào | Sai thì hỏng gì |
|---|---|---|
| Phần mở rộng vector có quét chỉ mục lặp không | Một truy vấn thử với bộ lọc hẹp trên dữ liệu giả | `AC-006` của US-008 không đạt; phải đổi cách lọc quyền, ảnh hưởng cả nhánh phân quyền |
| Tốc độ sinh biểu diễn ngữ nghĩa trên CPU của máy thật | Nhúng 1.000 đoạn mẫu, đo thời gian | `NFR-system-016` đến `018` phải đặt lại; kế hoạch nạp kho 1.000 tài liệu qua đêm có thể không khả thi |
| Thời gian sinh chữ của mô hình trên GPU 6GB | **Với lời nhắc dài bằng đúng ngữ cảnh RAG thật** (`context.max_tokens` mặc định), sinh 200 chữ, đo thời gian tới chữ đầu và tới chữ cuối. Chạy thêm một lần với lời nhắc ngắn để biết chênh lệch | `ASM-project-004`; sai thì mọi con số ở mục 8 của PRD và mọi ngưỡng ở mục 1 của `nfr.md` phải đặt lại |
| **Dung lượng bộ nhớ đồ hoạ còn lại khi nhiều lượt sinh chạy đồng thời** | Nạp mô hình đã lượng tử hoá, chạy 1 / 2 / 5 lượt song song với lời nhắc dài bằng ngữ cảnh thật, ghi bộ nhớ còn lại và thời gian tới chữ đầu ở từng mức | `NFR-system-008` chốt 5 câu đồng thời nhưng chưa ai đối chiếu với 6GB. Trọng số mô hình và bộ đệm khoá-giá trị của các lượt song song nằm chung trong đúng 6GB ấy, mà bộ đệm tăng theo *số lượt × độ dài ngữ cảnh* — và ngữ cảnh của một lượt RAG là ngữ cảnh dài. Sai thì `NFR-system-008`, `009`, `021` phải đặt lại, và US-018 bị thiết kế quanh một con số sai |
| **Thư viện tách từ tiếng Việt chạy trong máy ảo Java** | Tách thử 20 đoạn có thuật ngữ, đối chiếu bằng mắt. Kiểm luôn giấy phép và khả năng chạy trên phiên bản Java đã chốt | ADR-0002 đổi từ tiến trình phụ Python sang thư viện Java thuần và cắt ước lượng 14 giờ xuống 5 giờ, nhưng quyết định ấy **chưa có một dòng chạy thử nào**. Nhánh tìm theo từ là một trong ba nhánh thí nghiệm; tách từ kém thì kết luận *"lai thắng đơn lẻ"* thành hiển nhiên vì lý do sai — đúng cái bẫy mà ADR-0002 loại Phương án C để tránh |

Các phép đo này gộp lại chưa tới một ngày công, và chúng là lý do sprint 1 tồn tại dưới dạng "đường
chạy xuyên suốt thô" chứ không phải "làm xong một tính năng".

**Một ẩn số nữa, muộn nhất phải biết cuối sprint 2 (01/11): tổng giờ MÁY của ma trận thực nghiệm.**
Sprint 5 cấp 30 giờ **công người**, nhưng giờ máy thì chưa ai ước lượng ở đâu — mà riêng
`NFR-system-018` đã là 8 giờ cho một lần nạp một kho. Nhân số kho với số chiến lược tách đoạn, cộng
sinh câu trả lời 50 câu nhân ba cấu hình, cộng các lượt chấm bằng mô hình, cộng quét dải ngưỡng để
hiệu chỉnh — tất cả nối tiếp nhau trên **một** máy, và đó cũng chính là máy đang dùng để lập trình.
Sai thì sprint 5 không đủ ngày lịch, bất kể đủ giờ người. Đo bằng cách nhân số tổ hợp với thời gian
một tổ hợp, ngay khi sprint 2 có bộ đo đầu tiên.
