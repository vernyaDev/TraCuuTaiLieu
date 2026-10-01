---
type: data-model
status: draft
updated: 2026-09-24
links:
  - docs/_shared/architecture.md
  - docs/decisions/0003-technical-architecture.md
  - docs/_shared/nfr.md
---

# Mô hình dữ liệu

> Lược đồ là phần sửa đắt nhất của hệ thống này: nó đỡ cả ba chiều phân quyền, cả hai nhánh truy
> hồi, và toàn bộ dữ liệu thực nghiệm. File này mô tả **cấu trúc và lý do**, không mô tả cú pháp
> tạo bảng — cú pháp thuộc về mã nguồn.

## 1. Năm nhóm dữ liệu

```
  access                knowledge                   qa
  ├ users               ├ categories                ├ conversations
  ├ departments         ├ documents ──┐             ├ turns
  ├ user_departments    ├ chunk_sets  │             ├ turn_subqueries
  ├ document_acl ───────┤             │             ├ turn_citations
  └ permission_audit    └ chunks ─────┘             └ turn_warnings
                             │
                             │            gap                 evaluation
                             │            ├ refusal_log       ├ eval_runs
                             │            ├ gap_clusters      ├ eval_results
                             │            └ gap_members       ├ eval_metrics
                             │                                ├ ground_truth_questions
                             └────────────────────────────────┤ ground_truth_positions
                                                              └ human_grades
```

## 2. Nhóm `access` — ba chiều phân quyền

| Bảng | Cột chính | Ghi chú thiết kế |
|---|---|---|
| `users` | `username` duy nhất · `password_hash` · `clearance_level` số nguyên · `is_active` · `must_change_password` · `last_login_at` | `clearance_level` là **số thứ tự** chứ không phải chuỗi. Lý do ghi ở US-007: thêm mức mật thứ tư về sau là thay đổi rẻ nếu thang là số, và đắt nếu thang là chuỗi liệt kê |
| `departments` | `name` duy nhất | Danh sách **phẳng**, không có cột cha. Quyết định ở US-004: kế thừa quyền theo cây là nguồn lỗi phân quyền phổ biến nhất |
| `user_departments` | `(user_id, department_id)` làm khoá | Một người thuộc nhiều phòng ban là hợp lệ |
| `document_acl` | `document_id` · `subject_type` (người dùng hoặc phòng ban) · `subject_id` · `effect` (cho phép hoặc từ chối) | **Một bảng cho cả cho phép lẫn từ chối.** Tách hai bảng thì luật "từ chối thắng mọi cho phép" phải viết ở hai chỗ, và hai chỗ sẽ lệch |
| `permission_audit` | `actor_id` · `action` · `target_type` · `target_id` · `before` · `after` · `at` | `NFR-system-029` đòi giữ ≥12 tháng. Đây là thứ trả lời được câu "ai đã mở quyền tài liệu này" khi có sự cố |

## 3. Nhóm `knowledge` — tài liệu và đoạn

### `documents`

Cột nghiệp vụ: `title` · `original_filename` · `storage_path` · `sensitivity` (số thứ tự, cùng
thang với `clearance_level`) · `category_id` · `status` · `effective_from` · `effective_to` ·
`has_no_expiry` · `supersedes_document_id` · `topic_cluster_id` · `uploaded_by` · `indexed_at` ·
`error_reason`.

Ba chi tiết không hiển nhiên:

- **`has_no_expiry` là cột riêng, không dùng `effective_to` rỗng để biểu diễn.** US-013 đòi phân
  biệt *"không có thời hạn"* với *"chưa xác định"*. Gộp hai thứ đó vào một ô rỗng là mất thông tin,
  và mất đúng thông tin quyết định có cảnh báo hết hiệu lực hay không.
- **`supersedes_document_id` trỏ về bản bị thay thế**, tạo chuỗi một chiều. Vòng lặp bị chặn khi ghi
  (US-015 nhánh AC-004), không chặn bằng ràng buộc của cơ sở dữ liệu vì ràng buộc không phát hiện
  được chu trình nhiều bước.
- **`topic_cluster_id` được gán lúc nạp**, không tính lúc trả lời. Nó chỉ làm **bộ lọc thu hẹp** —
  hai tài liệu khác cụm thì không cần so nữa. Nó **không** mang được quyết định của quản trị; cái đó
  ở bảng dưới.

### `document_overlap_pairs` — chồng lấn là quan hệ theo CẶP, không phải thuộc tính của một tài liệu

`doc_a_id` · `doc_b_id` · `similarity` · `detected_at` · `decision` · `decided_by` · `decided_at`

Khoá chính là cặp `(doc_a_id, doc_b_id)` với quy ước **id nhỏ hơn đứng trước**, để cùng một cặp
không bao giờ sinh hai bản ghi ngược chiều nhau.

`decision` nhận bốn giá trị: `chờ` · `thay-thế` · `cùng-tồn-tại` · `báo-động-giả`.

**Vì sao phải có bảng này, thêm 2026-09-23.** Lược đồ trước chỉ có `topic_cluster_id` — một số
nguyên trên mỗi tài liệu. Số nguyên đó không mang nổi bốn thứ mà US-014 đòi: *mức độ giống nhau* để
xếp các cặp giảm dần (`AC-006`), *quyết định của quản trị* cho từng cặp riêng (`AC-003`), *trạng
thái đã xử lý* để không hiện lại đúng cặp ấy (`AC-005`), và *danh sách đã xử lý xem lại được*. Màn
`SCR-doc-ingestion-05` vẽ **mỗi cặp một thẻ** — không có bảng này thì màn ấy không có nguồn dữ liệu.

Và `AC-003` của US-025 đòi lúc **trả lời** phải biết cặp nào đã được xác nhận *cùng tồn tại hợp lệ*
để kèm cảnh báo — tức quyết định theo cặp phải đọc được ngay trong đường sinh câu trả lời, không chỉ
ở màn quản trị.

Chi phí lúc này khoảng một giờ. Sau sprint 4 thì nó là một lần đổi lược đồ cộng với nạp lại cả kho.

### `chunk_sets` — quyết định đáng chú ý nhất của lược đồ

Một bản ghi cho mỗi **chiến lược tách đoạn**: `strategy_name` · `params` · `chunk_count` ·
`created_at`. Mỗi đoạn thuộc đúng một `chunk_set`.

Cách hiển nhiên là mỗi lần đổi chiến lược thì xoá hết đoạn cũ và tách lại. Nhưng ma trận thí nghiệm
ở US-030 chạy **nhiều lần** trên **nhiều chiến lược**, và mỗi lần nạp lại kho 1.000 tài liệu tốn
tới 8 giờ theo `NFR-system-018`. Ba chiến lược nhân với vài lần chạy lại là vài ngày chỉ để chờ nạp.

Giữ nhiều tập đoạn song song thì nạp mỗi chiến lược **đúng một lần**, sau đó chạy toàn bộ ma trận
bao nhiêu lần cũng được. Giá phải trả là dung lượng: ba tập đoạn khoảng 150.000 đoạn, phần biểu
diễn ngữ nghĩa khoảng 450 MB — nằm gọn trong ngưỡng 20 GB của `NFR-system-015`.

Đây cũng là thứ làm `AC-010` của US-029 chạy được: cùng một bộ 100 câu ánh xạ sang từng tập đoạn,
không phải soạn lại đáp án.

**Bản triển khai cho doanh nghiệp chỉ có một tập đoạn đang hoạt động.** Nhiều tập là công cụ của
tác giả, không phải tính năng của người dùng.

**Lược đồ cố ý KHÔNG có chiều kho** (chốt 2026-09-24). Bộ đánh giá chạy trên hai kho thử nghiệm,
nhưng hai kho ấy là **hai bản cài độc lập**, mỗi bản một cơ sở dữ liệu riêng — không phải hai tập dữ
liệu trong cùng một lược đồ. Thêm cột kho vào `documents`, `chunk_sets`, hai bảng thống kê, bộ câu
hỏi chuẩn và lượt chạy đo là sáu chỗ đổi cộng với việc tính lại ước lượng dung lượng ở Mục 9, đổi
lấy một tiện lợi mà một lệnh chạy mỗi kho đã giải quyết xong. `AC-001` và `AC-007` của US-030 đã sửa
theo. Đối chiếu kết quả giữa hai kho là việc ghép hai tệp kết quả, không phải một truy vấn.

### `chunks`

`chunk_set_id` · `document_id` · `ordinal` · `breadcrumb` · `content` · `content_segmented` ·
`tsv` · `embedding` · `token_count` · `is_table` · `table_part` / `table_parts_total` ·
`char_start` / `char_end`.

- **`breadcrumb`** giữ đường dẫn phân cấp (*Chương VII › Điều 113 › Khoản 2*) — đây là thứ làm
  trích dẫn ở US-017 chỉ được đúng chỗ thay vì chỉ tên tài liệu.
- **`content_segmented`** là văn bản đã tách từ tiếng Việt; `tsv` sinh từ cột này chứ không phải từ
  `content`. Tách hai cột để đối chiếu được khi nhánh từ khoá cho kết quả lạ.
- **`char_start` / `char_end`** trỏ về vị trí trong tài liệu gốc. Đây là cầu nối cho `AC-010` của
  US-029 — đáp án chuẩn khai theo vị trí trong tài liệu, ánh xạ sang đoạn theo từng tập.
- **`table_part` / `table_parts_total`** cho phép hiển thị *"mảnh 2/4, đã lặp dòng tiêu đề"* ở
  `SCR-doc-ingestion-06`, và cho phép kiểm chất lượng tách bảng mà không phải mở tài liệu gốc.

### Thống kê cho BM25

`bm25_term_df` (từ, số tài liệu chứa từ, `chunk_set_id`) và `bm25_corpus` (`chunk_set_id`, tổng số
đoạn, độ dài đoạn trung bình).

Hai bảng này tồn tại vì quyết định ở ADR-0003 mục 4: BM25 tính trong tầng ứng dụng nên công thức
cần tần suất tài liệu và độ dài trung bình. **Thống kê gắn với từng tập đoạn** — cùng một kho tách
theo hai chiến lược cho hai bộ thống kê khác nhau, và dùng nhầm bộ là làm sai chính thí nghiệm.

Cập nhật theo đợt sau khi nạp xong một tài liệu, không cập nhật theo từng đoạn.

## 4. Nhóm `qa` — hội thoại

| Bảng | Vì sao tồn tại |
|---|---|
| `conversations` | `user_id` · `started_at` · `summary` — cột tóm tắt phục vụ US-021 |
| `turns` | `raw_question` **và** `rewritten_question` lưu riêng. `AC-002` của US-020 đòi người dùng xem được câu đã viết lại; và khi một câu bị từ chối, `AC-004` của US-027 đòi biết việc viết lại có phải nguyên nhân thật không |
| `turn_subqueries` | `ordinal` · `depends_on_ordinal` — chuỗi phụ thuộc của US-022, và là nguồn cho khối "các bước đã thực hiện" |
| `turn_citations` | `chunk_id` · `rank` · `score_bm25` · `score_vector` · `score_rrf` — **ba điểm lưu riêng**, không lưu mỗi điểm cuối. Không tách ba thì không phân tích được vì sao nhánh lai thắng hay thua |
| `turn_warnings` | `type` · `document_id` — cảnh báo hết hiệu lực hoặc chồng lấn của US-025 |

`turns` cũng lưu `latency_first_token_ms` và `latency_total_ms`. Hai cột này là nguồn dữ liệu thật
cho `NFR-system-001` và `002` — đo trên chính lưu lượng dùng thật, không chỉ đo trong phòng thí
nghiệm.

## 5. Nhóm `gap` và `evaluation`

**`refusal_log`** lưu `question` · `rewritten` · `reason` · `department_snapshot`. Chụp lại phòng
ban tại thời điểm hỏi vì người dùng có thể đổi phòng ban sau đó, và báo cáo ở US-028 cần biết phòng
ban **lúc hỏi** để trả lời đúng câu "nên cấp quyền cho phòng nào".

**`ground_truth_questions`** và **`ground_truth_positions`** tách làm hai: một câu hỏi có nhiều vị
trí đáp án đều chấp nhận được (`AC-004` của US-029). Vị trí khai bằng `document_id` cộng khoảng ký
tự, **không** khai bằng mã đoạn — đây là quyết định đã chốt ngày 2026-09-09.

**`eval_runs`** lưu `config` đầy đủ, `chunk_set_id`, `question_set_version`, `model_name` và tham số
sinh. Đây là hiện thực trực tiếp của `NFR-system-046`: kết quả tự mang theo điều kiện sinh ra nó.
**`eval_results`** một dòng cho mỗi câu hỏi trong mỗi lượt chạy, nên `AC-009` của US-030 (chạy tiếp
phần dở dang) chỉ là truy vấn tìm câu chưa có kết quả.

**`human_grades`** tách khỏi `eval_results` chứ không ghi đè lên nó. `AC-008` của US-032: giữ cả hai
kết quả, vì **so sánh giữa máy chấm và người chấm mới là thứ có giá trị**, không phải kết quả cuối.

## 6. Chỉ mục và lý do

| Chỉ mục | Trên bảng | Vì sao |
|---|---|---|
| Chỉ mục xấp xỉ trên `embedding` | `chunks` | Nhánh truy hồi ngữ nghĩa. **Phải chạy được ở chế độ quét lặp** — xem mục 7 |
| Chỉ mục toàn văn trên `tsv` | `chunks` | Sinh tập ứng viên cho BM25 |
| `(chunk_set_id, document_id)` | `chunks` | Mọi truy vấn truy hồi đều lọc theo hai cột này trước |
| `(document_id)` và `(subject_type, subject_id)` | `document_acl` | Hai chiều tra: "tài liệu này ai đọc được" và "người này đọc được gì" |
| `(status)`, `(category_id)`, `(effective_to)` | `documents` | Bộ lọc của `SCR-doc-ingestion-01`, gồm cả nhóm sắp hết hiệu lực |
| `(run_id, question_id)` | `eval_results` | Chạy tiếp phần dở dang |

## 7. Truy vấn truy hồi — chỗ mọi thứ gặp nhau

Đây là truy vấn quan trọng nhất của hệ thống. Trình tự:

1. **Tính tập tài liệu được phép** cho người hỏi, theo luật đọc duy nhất ở US-008.
2. **Chọn chiều lọc theo độ chọn lọc.** Nếu tập được phép nhỏ hơn khoảng một nửa kho thì lọc theo
   danh sách **được phép**; nếu lớn hơn thì lọc theo danh sách **bị chặn**. Truyền một mảng vài
   trăm mã vào truy vấn thì nhanh; truyền một mảng chín trăm mã thì chậm hơn cả việc loại trừ vài
   chục mã còn lại.
3. **Nhánh ngữ nghĩa:** tìm láng giềng gần nhất trong phạm vi đã lọc, **bật chế độ quét lặp** của
   chỉ mục xấp xỉ.
4. **Nhánh từ khoá:** lấy tập ứng viên bằng chỉ mục toàn văn trong cùng phạm vi, rồi tính điểm BM25
   trong tầng ứng dụng từ `bm25_term_df` và `bm25_corpus`.
5. **Hợp nhất thứ hạng** hai nhánh.

**Bước 3 là chỗ rủi ro kỹ thuật lớn nhất của dự án.** Chỉ mục xấp xỉ tìm láng giềng gần nhất
*trước* rồi mới áp bộ lọc; nếu phần lớn láng giềng tìm được nằm ngoài phạm vi cho phép thì kết quả
trả về ít hơn số cần, và độ phủ tụt mà không có dấu hiệu gì. Chế độ quét lặp giải đúng chuyện này
bằng cách quét tiếp cho tới khi đủ. Thiếu tính năng đó thì phải chuyển sang quét chính xác cho tập
hẹp — chậm hơn nhưng đúng.

Đây là lý do phép kiểm khởi động số 2 tồn tại, và là lý do `AC-006` của US-008 phải chạy ngay ở
sprint 3 chứ không đợi kỳ đo cuối.

## 8. Bộ nhớ đệm quyền

Tính tập tài liệu được phép cho mỗi câu hỏi là tốn kém khi kho có 1.000 tài liệu và người dùng
thuộc nhiều phòng ban. Cách làm:

- Lưu tập được phép trong `cache`, khoá theo `user_id` kèm một **số hiệu phiên bản quyền**.
- Số hiệu tăng lên khi **bất kỳ** thứ nào sau đây đổi: danh sách kiểm soát truy cập, thành viên
  phòng ban, nhãn mật của tài liệu, mức mật của người dùng, hoặc trạng thái tài liệu.
- Đọc thấy số hiệu khác thì tính lại.

Điều này thoả `AC-005` của US-008 (quyền đổi có hiệu lực ngay ở câu hỏi kế tiếp, không cần đăng
nhập lại) mà không phải tính lại mỗi lượt. **Tăng số hiệu là việc bắt buộc ở mọi đường ghi quyền** —
quên một đường là tạo ra một lỗ rò rỉ chỉ xuất hiện sau khi cấu hình thay đổi, tức là loại lỗi khó
tái hiện nhất.

## 9. Dung lượng ước tính

| Thành phần | Ước tính |
|---|---|
| Tệp gốc, 1.000 tài liệu | 5–10 GB |
| Văn bản đoạn, một tập | ~150 MB |
| Biểu diễn ngữ nghĩa, một tập 50.000 đoạn | ~150 MB |
| Ba tập đoạn song song | ~900 MB |
| Chỉ mục toàn văn và chỉ mục xấp xỉ | ~500 MB |
| Hội thoại, nhật ký, dữ liệu đo | < 1 GB |
| **Cộng** | **~13 GB** — trong ngưỡng 20 GB của `NFR-system-015` |

Mô hình ngôn ngữ nằm ngoài con số này; nó sống trong `llm` và chiếm thêm vài GB đĩa.

## 10. Vòng đời dữ liệu

| Dữ liệu | Giữ bao lâu | Vì sao |
|---|---|---|
| Tài liệu đã gỡ | Vô thời hạn tới khi xoá vĩnh viễn | US-015: văn bản không biến mất, chúng bị thay thế |
| Tài liệu xoá vĩnh viễn | Xoá thật, cả tệp gốc lẫn đoạn | US-015 `AC-007` |
| Hội thoại | Theo phiên; không giữ qua phiên | Ranh giới phạm vi ở ADR-0002 |
| `refusal_log` | 12 tháng, cấu hình được | Câu hỏi của người dùng là dữ liệu nhạy cảm; giữ vô thời hạn không có lý do chính đáng |
| `permission_audit` | ≥ 12 tháng | `NFR-system-029` |
| Dữ liệu đo | Vô thời hạn | Là bằng chứng của báo cáo, không được mất |

## 11. Ba điều lược đồ này cố ý KHÔNG làm

- **Không có cột `tenant_id`.** Mỗi doanh nghiệp một bản cài độc lập; thêm cột đó là mở cửa cho một
  kiến trúc nhiều tổ chức mà phạm vi đã loại bỏ.
- **Không lưu nội dung tài liệu vào nhật ký vận hành.** `NFR-system-048` — nhật ký ghi lượt hỏi nào
  lỗi ở bước nào, không ghi người dùng đọc được gì.
- **Không có bảng cấu hình.** Mọi tham số sống trong tệp cấu hình, không trong cơ sở dữ liệu. Lý do:
  `NFR-system-046` đòi kết quả đo mang theo cấu hình; cấu hình nằm trong tệp thì chụp lại nguyên vẹn
  được, còn nằm trong bảng thì phải tự dựng cơ chế phiên bản cho nó.
