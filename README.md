# Hệ thống tra cứu tri thức nội bộ

Đồ án: hệ thống hỏi–đáp tiếng Việt trên kho tài liệu nội bộ của doanh nghiệp. Nhân viên đặt
câu hỏi bằng ngôn ngữ tự nhiên, hệ thống trả lời **kèm trích dẫn tới đúng đoạn tài liệu gốc**
và **từ chối trả lời khi không đủ căn cứ**.

Toàn bộ hệ thống, kể cả mô hình ngôn ngữ, chạy trong mạng nội bộ qua Docker Compose. Dữ liệu
không rời khỏi tổ chức và không gọi dịch vụ AI bên ngoài.

## Chức năng chính

| Nhóm chức năng | Mô tả | Hồ sơ phân tích |
|---|---|---|
| Hỏi–đáp | Trả lời có trích dẫn, giữ ngữ cảnh hội thoại, từ chối khi thiếu căn cứ | [docs/qa-chat/](docs/qa-chat/) |
| Nạp tài liệu | Nhận docx / pdf / txt / Markdown, tách đoạn, xử lý bảng, gợi ý metadata, phát hiện chồng lấn | [docs/doc-ingestion/](docs/doc-ingestion/) |
| Phân quyền | Người dùng, phòng ban, nhãn mật; lọc quyền **trước** khi xếp hạng kết quả | [docs/access-control/](docs/access-control/) |
| Khoảng trống tri thức | Ghi lại câu hỏi hệ thống không trả lời được để bổ sung kho | [docs/knowledge-gap/](docs/knowledge-gap/) |
| Đánh giá | Bộ chạy đo độ trễ và chất lượng trả lời | [docs/evaluation/](docs/evaluation/) |

## Kiến trúc

Bốn dịch vụ, một cổng ra ngoài:

| Dịch vụ | Công nghệ | Vai trò |
|---|---|---|
| `app` | Spring Boot 3.3 · Java 21 | Nghiệp vụ, bảy module agent, nạp tài liệu, phục vụ luôn giao diện |
| `db` | PostgreSQL + pgvector | Tài liệu, đoạn, vector, chỉ mục toàn văn, người dùng, quyền |
| `llm` | Ollama (Qwen2.5) — cần GPU | Sinh câu trả lời |
| `cache` | Redis | Phiên đăng nhập, hàng đợi câu hỏi |

Giao diện viết bằng React 18 + TypeScript + Vite, được dựng thành tệp tĩnh và do `app` phục vụ.

Mỗi câu hỏi đi qua bảy module theo thứ tự cố định:

```
Prompt → Reasoning → Memory → Planner → Retriever → Tool → Action
```

Truy hồi kết hợp BM25 trên văn bản đã tách từ tiếng Việt với tìm kiếm vector, rồi hợp nhất thứ
hạng. Chi tiết: [docs/_shared/architecture.md](docs/_shared/architecture.md).

## Cấu trúc thư mục

```
app/                          máy chủ ứng dụng (Maven)
└── src/main/java/vn/utc/tracuu/
    ├── platform/             cấu hình, bảo mật, xử lý lỗi, kiểm điều kiện khởi động
    ├── access/               người dùng, phòng ban, quyền đọc tài liệu
    ├── knowledge/            tài liệu, đoạn, vòng đời
    ├── ingestion/            bóc văn bản, tách đoạn, sinh biểu diễn ngữ nghĩa
    ├── agent/                bảy module agent và bộ điều phối
    ├── qa/                   phiên hội thoại, điểm vào hỏi–đáp
    ├── gap/                  nhật ký từ chối, báo cáo khoảng trống
    └── evaluation/           bộ chạy đo, chỉ số, chấm điểm
web/                          giao diện (React + Vite)
docs/                         hồ sơ phân tích thiết kế
├── _product/                 PRD và lộ trình
├── _shared/                  kiến trúc, mô hình dữ liệu, hợp đồng API, yêu cầu phi chức năng
├── decisions/                các quyết định kiến trúc (ADR)
└── <chức năng>/              SRS, use case, user story, wireframe, kiểm thử
Dockerfile, docker-compose.yml
HUONG-DAN-CHAY.md             hướng dẫn chạy và kết quả đo trên phần cứng thật
KE-HOACH-HOAN-THIEN.md        kế hoạch hoàn thiện
```

## Chạy nhanh

Yêu cầu: Docker, và GPU NVIDIA từ 6 GB VRAM cho dịch vụ `llm`.

```bash
cp .env.example .env          # rồi sửa DB_PASSWORD
docker compose up -d --build

# Lần đầu: tải mô hình về (vài GB, chạy một lần)
docker exec tracuu-llm ollama pull qwen2.5:3b-instruct-q4_K_M
```

Mở `http://localhost:8080`.

Chạy ở chế độ phát triển, dùng Ollama có sẵn trên máy, chạy kiểm thử và đo độ trễ: xem
[HUONG-DAN-CHAY.md](HUONG-DAN-CHAY.md).

## Tài liệu nên đọc trước

| Muốn biết | Đọc |
|---|---|
| Sản phẩm giải quyết bài toán gì | [docs/_product/prd.md](docs/_product/prd.md) |
| Hệ thống được thiết kế thế nào | [docs/_shared/architecture.md](docs/_shared/architecture.md) |
| Vì sao chọn thiết kế đó | [docs/decisions/](docs/decisions/) |
| Mô hình dữ liệu | [docs/_shared/data-model.md](docs/_shared/data-model.md) |
| Ngưỡng hiệu năng cam kết | [docs/_shared/nfr.md](docs/_shared/nfr.md) |
| Cách chạy và số liệu đo thật | [HUONG-DAN-CHAY.md](HUONG-DAN-CHAY.md) |
