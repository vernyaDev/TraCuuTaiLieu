---
type: decision
decision_id: D-2026-09-09-technical-architecture
feature: _project
status: active
decided_at: 2026-09-09
decided_by: "@longdt"
source: "Phiên thiết kế kiến trúc 2026-09-09"
updated: 2026-09-09
links:
  - docs/decisions/0002-scope-and-internal-design.md
  - docs/_shared/nfr.md
  - docs/_product/roadmap.md
---

# Decision: Kiến trúc kỹ thuật và cách đóng gói

> Decided: 2026-09-09 | By: @longdt | Source: Phiên thiết kế kiến trúc 2026-09-09

## Context

Kiến trúc bảy module agent và stack kỹ thuật do GVHD chỉ định là bất biến. Nhưng cách hiện thực
hoá chúng thành dịch vụ, tiến trình, gói mã nguồn và lược đồ dữ liệu thì chưa quyết. Ba câu hỏi
phải trả lời trước khi viết dòng code đầu tiên, vì cả ba đều đắt khi sửa muộn: hệ thống gồm mấy
tiến trình, mã nguồn chia thành những khối nào, và ranh giới giữa các khối nằm ở đâu.

Một ràng buộc chi phối mọi quyết định dưới đây: `NFR-system-045` đòi mọi biến thí nghiệm — chiến
lược tách đoạn, phương pháp tìm kiếm, bật tắt Planner, bật tắt tầng kiểm gắn nguồn — đổi được bằng
cấu hình mà không sửa mã nguồn. Yêu cầu này không phải tiện nghi; nó quyết định cách chia gói.

## Decision

### 1. Bốn dịch vụ, không phải năm

| Dịch vụ | Vai trò | Ghi chú |
|---|---|---|
| `app` | Spring Boot 3 trên Java 21 — toàn bộ nghiệp vụ, giao diện lập trình, luồng sự kiện một chiều, **và phục vụ luôn tệp tĩnh của giao diện** | Một tiến trình |
| `db` | PostgreSQL 16 kèm phần mở rộng vector | Lưu tất cả: tài liệu, đoạn, biểu diễn ngữ nghĩa, chỉ mục toàn văn, người dùng, quyền, hội thoại, nhật ký |
| `llm` | Ollama, có quyền dùng GPU | Chỉ chạy mô hình ngôn ngữ |
| `cache` | Redis | Phiên đăng nhập, hàng đợi câu hỏi, kết quả tra cứu quyền dùng lại trong lượt |

**Giao diện không có container riêng.** Ứng dụng một trang dựng bằng Vite và React ở **thời điểm
dựng ảnh**, kết quả là tệp tĩnh được chép vào ảnh của `app` và phục vụ từ đó.

### 2. Sinh biểu diễn ngữ nghĩa chạy trong chính tiến trình `app`

Dùng thời gian chạy suy luận ONNX nhúng trong máy ảo Java, không dựng dịch vụ riêng cho việc này.

### 3. Tách từ tiếng Việt chạy trong chính tiến trình `app`

Thư viện Java thuần, không có tiến trình phụ và không có container thứ năm.

### 4. Xếp hạng từ khoá là BM25 hiện thực trong tầng ứng dụng

PostgreSQL dùng để **sinh tập ứng viên** bằng chỉ mục toàn văn và để cung cấp thống kê tần suất
từ. Điểm BM25 tính trong tầng ứng dụng theo đúng công thức.

### 5. Bảy module agent là bảy giao diện lập trình, ghép bằng một bộ điều phối tường minh

Mỗi module là một giao diện với ít nhất một hiện thực. Bộ điều phối chạy chúng theo đúng thứ tự
GVHD chỉ định, truyền một đối tượng ngữ cảnh có kiểu. Bật tắt hay thay thế một module là việc của
cấu hình, không phải của mã nguồn.

### 6. Bố cục kho mã nguồn

```
app/      máy chủ Spring Boot
web/      giao diện Vite + React
deploy/   tệp cấu hình triển khai, biến môi trường mẫu, kịch bản khởi tạo
eval/     bộ câu hỏi và đáp án chuẩn (dữ liệu, không phải mã)
docs/     tài liệu phân tích thiết kế (đã có)
```

Công cụ dựng của `app` là Maven.

## Rationale

**Vì sao bốn dịch vụ chứ không năm.** ADR-0002 khi chốt ứng dụng một trang có ghi cái giá là "thêm
một container và một chuỗi công cụ dựng". Điều đó **đúng với Next.js nhưng sai với Vite**: Next.js
cần một tiến trình Node chạy thường trực, còn Vite chỉ chạy lúc dựng và cho ra tệp tĩnh. Chọn Vite
thì giao diện có trần trải nghiệm của một ứng dụng một trang thật, mà bản triển khai vẫn giữ nguyên
bốn dịch vụ. Hệ thống không cần hiển thị phía máy chủ, không cần tối ưu công cụ tìm kiếm — hai lý
do chính để dùng Next.js đều không áp dụng cho một sản phẩm chạy sau tường lửa doanh nghiệp.

**Vì sao sinh biểu diễn ngữ nghĩa và tách từ đều nằm trong tiến trình `app`.** Mỗi container thêm
vào là một thứ nữa phải khởi động đúng thứ tự, phải kiểm tình trạng, phải sao lưu, và phải giải
thích cho người quản trị hệ thống của doanh nghiệp. Luận điểm bán hàng là cài bằng một lệnh; mỗi
dịch vụ thêm vào làm luận điểm ấy yếu đi một chút. Cả hai việc này đều có thư viện chạy trong máy
ảo Java nên không có lý do gì tách ra.

**Vì sao BM25 phải tự hiện thực — đây là điểm quan trọng nhất của ADR này.** Đề tài tuyên bố so
sánh **BM25** với truy hồi ngữ nghĩa. PostgreSQL không có BM25; hàm xếp hạng sẵn có của nó dựa trên
tần suất từ nhưng **không phải BM25** — nó thiếu hai thành phần cốt lõi là chuẩn hoá theo độ dài
văn bản và tham số bão hoà tần suất. Dùng hàm sẵn có rồi gọi nó là BM25 trong báo cáo là sai về mặt
học thuật, và là loại sai mà một người phản biện đọc kỹ sẽ phát hiện. Ba đường đi:

1. Dùng hàm xếp hạng sẵn có và **gọi đúng tên** là "xếp hạng từ khoá theo tần suất". Rẻ nhất, nhưng
   mất chính cái tên trong đề tài.
2. Cài phần mở rộng BM25 của bên thứ ba vào PostgreSQL. Đúng tên, nhưng thêm một phần mở rộng phải
   đóng gói và bảo trì trong ảnh cơ sở dữ liệu.
3. **Tự tính BM25 trong tầng ứng dụng** trên tập ứng viên do chỉ mục toàn văn sinh ra, dùng thống kê
   tần suất tài liệu lấy từ chính cơ sở dữ liệu.

Chọn đường 3. Công thức BM25 ngắn, tham số minh bạch, và **có thể trình bày nguyên văn trong báo
cáo** — điều này biến một rủi ro học thuật thành một điểm cộng, vì hội đồng thấy được đúng cái được
tính. Chi phí ước lượng 6–8 giờ, đổi lấy việc bảo vệ được tên gọi của một trong ba nhánh thí nghiệm.

**Vì sao bảy module là bảy giao diện lập trình.** Đây là chỗ `NFR-system-045` gặp kiến trúc GVHD
chỉ định. Nếu bảy module chỉ là bảy nhóm hàm gọi lẫn nhau thì thí nghiệm bóc tách "agent bỏ Planner"
buộc phải sửa mã và dựng lại — vi phạm `NFR-system-045`, và tệ hơn là hai lần chạy không so được
với nhau vì chúng chạy trên hai bản mã khác nhau. Khi mỗi module là một giao diện, "bỏ Planner"
nghĩa là thay hiện thực bằng một hiện thực trả về đúng một truy vấn con — cùng một bản mã, khác một
dòng cấu hình. Kiến trúc này cũng làm sơ đồ trong báo cáo khớp một-một với gói mã nguồn thật, chứ
không phải một sơ đồ vẽ cho đẹp.

## Alternatives Considered

| Lựa chọn | Ưu | Nhược | Vì sao loại |
|---|---|---|---|
| Next.js cho giao diện | Hệ sinh thái lớn hơn | Cần tiến trình Node chạy thường trực, thành container thứ năm | Không dùng tới hiển thị phía máy chủ; giá phải trả không đổi lấy gì |
| Dịch vụ riêng cho sinh biểu diễn ngữ nghĩa | Đổi mô hình nhúng dễ hơn | Thêm container, thêm điểm hỏng, thêm việc cho người cài | Thư viện chạy trong máy ảo Java làm được việc này |
| Dùng hàm xếp hạng sẵn có và gọi là BM25 | Rẻ nhất | **Sai về mặt học thuật** | Một trong ba nhánh thí nghiệm mất tên gọi |
| Phần mở rộng BM25 của bên thứ ba | Đúng tên, ít mã tự viết | Ảnh cơ sở dữ liệu không còn là ảnh chuẩn; thêm thứ phải bảo trì khi nâng cấp | Đường 3 rẻ tương đương mà không ràng buộc gì thêm |
| Bảy module là bảy nhóm hàm | Ít mã khung hơn | Thí nghiệm bóc tách phải sửa mã, vi phạm `NFR-system-045` | Chính nhóm yêu cầu chấm điểm bị hỏng |
| Kiến trúc phân tán nhiều tiến trình theo module | Mở rộng theo chiều ngang | Không có nhu cầu mở rộng: một doanh nghiệp một bản, 5 câu hỏi đồng thời | Phức tạp không đổi lấy gì |

## Consequences

### Positive
- Bản triển khai giữ đúng bốn dịch vụ; luận điểm "cài bằng một lệnh" không bị làm yếu đi.
- Thí nghiệm bóc tách chạy bằng cấu hình, thoả `NFR-system-045` và đảm bảo các lần chạy so sánh
  được với nhau vì cùng một bản mã.
- Sơ đồ kiến trúc trong báo cáo khớp một-một với gói mã nguồn thật.
- Công thức BM25 trình bày được trong báo cáo, biến rủi ro học thuật thành điểm cộng.

### Negative / Trade-offs
- Tự tính BM25 tốn thêm 6–8 giờ so với dùng hàm sẵn có.
- Sinh biểu diễn ngữ nghĩa trong cùng tiến trình `app` nghĩa là việc nạp tài liệu tranh CPU với
  việc phục vụ câu hỏi. Đây chính là rủi ro mà `NFR-system-011` đặt ngưỡng để đo.
- Bảy giao diện lập trình cộng một bộ điều phối là nhiều mã khung hơn so với gọi hàm trực tiếp.

### Risks
- **Phần mở rộng vector phải đủ mới để hỗ trợ quét chỉ mục lặp.** Thiếu tính năng đó thì lọc quyền
  trước khi xếp hạng sẽ làm tụt độ phủ đúng như rủi ro đã ghi ở ADR-0002, và `AC-006` của US-008 sẽ
  không đạt. Phải kiểm ngay ở sprint 1, không đợi sprint 3.
- Thời gian sinh biểu diễn ngữ nghĩa trên CPU quyết định `NFR-system-016` đến `NFR-system-018`.
  Chưa đo thật nên đây vẫn là ẩn số; sprint 2 phải trả lời.

## Affected Artifacts

* `docs/decisions/0002-scope-and-internal-design.md` — mục 7 ghi cái giá "thêm một container", nay
  không còn đúng
* `docs/_shared/nfr.md` — `NFR-system-045`, `NFR-system-011`
* `docs/_product/roadmap.md` — sprint 1 phải thêm việc kiểm quét chỉ mục lặp

## Review / Supersede

* Status: `active`
* Replacement: —
