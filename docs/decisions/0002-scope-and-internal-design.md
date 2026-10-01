---
type: decision
decision_id: D-2026-09-09-scope-internal-design
feature: _project
status: active
decided_at: 2026-09-09
decided_by: "@longdt"
source: "Phiên brainstorming 2026-09-09"
updated: 2026-09-09
links:
  - docs/decisions/0001-methodology-shape.md
  - docs/_shared/project-profile.md
---

# Decision: Phạm vi Phương án B và 8 quyết định thiết kế bên trong kiến trúc GVHD chốt

> Decided: 2026-09-09 | By: @longdt | Source: Phiên brainstorming 2026-09-09

## Context

Kiến trúc 7 module (Prompt → Reasoning → Memory → Planner → Retriever → Tool → Action) và stack
kỹ thuật do GVHD chỉ định, không được thay đổi. Nhưng bên trong nó còn tám điểm chưa quyết, và
ba lựa chọn ban đầu của Product Owner (sidecar tách từ tiếng Việt, SPA riêng cho màn hỏi đáp, hồ
sơ người dùng bền vững) cộng lại cần ~194h trong khi quỹ đến mốc khoá tính năng 01/12/2026 chỉ
có ~118h.

## Decision

Chốt **Phương án B** — giữ hạng mục có giá trị nghiên cứu, cắt hạng mục chỉ có giá trị trình
diễn — cùng tám quyết định thiết kế dưới đây.

### Phạm vi: ba điều chỉnh so với lựa chọn ban đầu

| Hạng mục | Ban đầu | Chốt | Công |
|---|---|---|---|
| Tách từ tiếng Việt cho BM25 | Sidecar Python | **Thư viện Java chạy trong JVM** | 14h → 5h |
| Giao diện | Thymeleaf quản trị + SPA hỏi đáp | ~~Một artefact Thymeleaf + HTMX~~ → **SPA cho toàn bộ giao diện** (sửa 2026-09-09, xem mục 7) | — |
| Bộ nhớ | Hồ sơ người dùng bền vững qua nhiều phiên | **Lịch sử hội thoại + tóm tắt cuộn** | 20h → 8h |

Hồ sơ người dùng bền vững chuyển sang hướng phát triển sau đồ án.

### 1. Mô hình phân quyền

Bốn thực thể: người dùng (có mức mật), phòng ban và quan hệ thành viên, tài liệu (có nhãn mật),
danh sách kiểm soát truy cập theo tài liệu với chủ thể là người dùng hoặc phòng ban và hiệu lực
cho phép hoặc từ chối.

Luật đọc: `(được gán qua phòng ban HOẶC được gán trực tiếp) VÀ nhãn tài liệu ≤ mức mật của người
dùng`; **từ chối thắng mọi cho phép**.

Lọc quyền là **pre-filter** — tính trước tập tài liệu được phép rồi mới xếp hạng.

### 2. Nạp tài liệu và tách đoạn

Tách đoạn theo cấu trúc tài liệu (heading của docx, outline của pdf, cấp tiêu đề Markdown), mỗi
đoạn mang theo đường dẫn phân cấp để trích dẫn chỉ đúng chỗ. Bảng biểu **không bị cắt giữa
chừng**; bảng vượt kích thước đoạn thì chia theo hàng và lặp lại dòng tiêu đề ở mỗi mảnh.

### 3. Truy hồi lai

Ba nhánh bật tắt được bằng cấu hình: chỉ từ khoá, chỉ ngữ nghĩa, và lai hợp nhất bằng Reciprocal
Rank Fusion. Nhánh từ khoá chạy trên văn bản đã tách từ tiếng Việt.

### 4. Vòng đời và xung đột tài liệu

Metadata bắt buộc khi nạp: hiệu lực từ, hiệu lực đến, thay thế tài liệu nào, phân loại. Hệ thống
đọc tài liệu và điền sẵn, **quản trị tri thức xác nhận hoặc sửa** — không tự động chốt.

Phát hiện chủ đề trùng thực hiện **lúc nạp** (so tài liệu mới với các tài liệu còn hiệu lực cùng
phân loại; vượt ngưỡng thì gắn nhãn nghi ngờ chồng lấn chờ người xác nhận). Lúc trả lời chỉ tra
metadata đã có, không phát sinh thêm suy luận nào.

### 5. Từ chối trả lời, hai tầng

Tầng một: không có đoạn nào vượt ngưỡng điểm thì từ chối ngay. Tầng hai: câu trả lời tổng hợp
xong phải gắn được mọi ý về đoạn cụ thể; ý không gắn được thì bỏ, bỏ hết thì thành từ chối.

Bộ 50 câu đánh giá gồm **10 câu cố tình không có đáp án** trong kho, để đo tách bạch từ chối
đúng và từ chối sai.

### 6. Bộ nhớ

Lưu lượt hội thoại; nạp N lượt gần nhất; vượt cửa sổ ngữ cảnh thì tóm tắt cuộn. **Câu hỏi tiếp
nối được viết lại thành câu độc lập trước khi truy hồi.**

### 7. Giao diện

Một artefact duy nhất, câu trả lời stream từng token.

> **Sửa đổi 2026-09-09 — điểm một.** Bản đầu của quyết định này ghi "ba màn hình tối giản (hỏi đáp, quản trị
> tài liệu, báo cáo)". Product Owner đã thay yêu cầu: giao diện phải **phủ đủ** mọi thao tác của
> ba nhóm người dùng và đạt **chất lượng trình bày chuyên nghiệp**. Ba màn hình không đủ — riêng
> 36 story đã cần thêm đăng nhập, đổi mật khẩu lần đầu, quản lý người dùng, quản lý phòng ban,
> xác nhận metadata và xem tài liệu gốc. Danh sách màn hình thật nằm ở các tệp screen index của
> từng feature; hệ thống thị giác dùng chung ở `docs/_shared/design-system.md`. Ràng buộc đầy đủ
> ghi ở mục 9 của `docs/_product/prd.md`.
>
> **Sửa đổi 2026-09-09 — điểm hai.** Nền giao diện đổi từ "một artefact Thymeleaf + HTMX" sang
> **ứng dụng một trang riêng cho toàn bộ giao diện**, gọi sang máy chủ qua giao diện lập trình và
> luồng sự kiện một chiều để nhận câu trả lời hiện dần. Lý do đổi: lựa chọn cũ được chọn **vì mốc
> thời gian**, mà mốc đó không còn ràng buộc; đồng thời chất lượng giao diện nay là yêu cầu nên
> trần trải nghiệm mới là tiêu chí chọn. Phương án lai (quản trị một nền, hỏi đáp một nền) bị loại
> vì hai hệ thị giác song song sẽ trôi khác nhau theo thời gian — về mặt "nhìn như một sản phẩm"
> thì nó khó hơn cả hai phương án thuần. Đổi lại: thêm một container và một chuỗi công cụ dựng,
> chấp nhận được vì tệp cấu hình triển khai vốn đã có bốn dịch vụ.
>
> **Đính chính 2026-09-09 (ADR-0003 mục 1):** cái giá "thêm một container" **không còn đúng**. Nó
> đúng với Next.js — cần một tiến trình Node chạy thường trực — nhưng sai với Vite, vốn chỉ chạy
> lúc dựng và cho ra tệp tĩnh do chính `app` phục vụ. Bản triển khai giữ nguyên bốn dịch vụ. Chuỗi
> công cụ dựng thứ hai thì vẫn có thật, nhưng nó chỉ tồn tại trên máy của người dựng ảnh, không
> tồn tại ở nơi cài đặt.

### 8. Đóng gói và bộ đo

Bốn dịch vụ trong một tệp Docker Compose. **Kiểm tra GPU khi khởi động, thiếu GPU thì chặn hẳn
kèm thông báo rõ** thay vì chạy chậm.

Bộ đánh giá là **một phần của sản phẩm**: một lệnh nạp bộ câu hỏi, chạy hết các cấu hình cần so
sánh, xuất kết quả dạng bảng. Làm từ sprint 2, không để dồn sang tháng 12.

## Alternatives Considered

| Option | Pros | Cons | Why rejected |
|---|---|---|---|
| Phương án A (giữ nguyên lựa chọn ban đầu) | Giao diện đẹp hơn khi demo; bộ nhớ cá nhân hoá | ~194h so với ~118h khả dụng; cá nhân hoá truy hồi phá tính lặp lại của thực nghiệm | Vỡ mốc 01/12 và mất chương thực nghiệm |
| Phương án C (an toàn tối đa) | ~128h, có đệm thật | Nhánh từ khoá không tách từ tiếng Việt nên yếu; kết luận "lai thắng đơn lẻ" thành hiển nhiên | Mất giá trị học thuật của thí nghiệm chính |
| Phát hiện xung đột ngữ nghĩa lúc trả lời | Chính xác hơn | Thêm suy luận mỗi câu hỏi, tăng độ trễ, +17h | Đổi nhiều lấy ít |

## Consequences

### Positive
- Khối lượng còn ~147h; bộ ground truth 150 câu (~25h) là việc tài liệu nên đẩy sang tháng 9–10
  chạy song song lúc viết đề cương, không ăn vào quỹ lập trình.
- Ba nhánh truy hồi bật tắt bằng cấu hình nên thí nghiệm so sánh chạy được bằng một lệnh.

### Negative / Trade-offs
- Giao diện hỏi đáp giản dị hơn đáng kể so với một SPA.
- Không có cá nhân hoá; mọi người hỏi cùng câu nhận cùng kết quả (đây cũng là điều kiện để thực
  nghiệm lặp lại được).
- ~147h vẫn vượt ~118h khả dụng: cần một hàng đợi cắt phạm vi có thứ tự, chốt ở mục MoSCoW.

### Risks
- **Pre-filter cộng chỉ mục xấp xỉ làm tụt recall** — lọc xong có thể không còn đủ láng giềng.
  Giảm thiểu: truyền tập tài liệu được phép vào truy vấn, dùng quét lặp của chỉ mục, quét chính
  xác khi tập cho phép nhỏ. Phải có kịch bản kiểm thử riêng, không để phát hiện lúc chạy thực
  nghiệm tháng 12.
- Tách bảng biểu sai làm hỏng đúng nhóm tài liệu chiếm 30% kho.

## Affected Artifacts

* `docs/_product/prd.md`
* Toàn bộ user story và NFR sẽ sinh sau
* Kế hoạch sprint 05/10 – 20/12/2026

## Review / Supersede

* Status: `active`
* Replacement: —
