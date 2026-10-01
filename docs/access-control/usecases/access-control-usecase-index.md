---
type: usecase-index
feature: access-control
status: draft
updated: 2026-09-09
links:
  - docs/access-control/userstories/access-control-story-index.md
  - docs/_product/prd.md
---

# Tài khoản và phân quyền — Use Case Index

## Use cases

| UC | Actor chính | Mức | Story | Năng lực | Màn hình | Trạng thái |
|---|---|---|---|---|---|---|
| `UC-grant-document-access` | Quản trị tri thức | sea-level | US-005, US-006, US-007, US-008, US-004, US-002 | `CAP-access-control-03`, `CAP-access-control-04`, `CAP-access-control-05` | Quản trị tài liệu (khối quyền truy cập) | draft |

Mã màn hình và mã lỗi được cấp ở bước danh sách màn hình.

## Ba cơ chế, một luật

Phân quyền của hệ thống gồm ba cơ chế độc lập. Chúng **không** thay thế nhau, và kết hợp theo đúng
một luật phát biểu tại US-008:

| Cơ chế | Vai trò | Đặc tả ở |
|---|---|---|
| Cấp quyền theo tài liệu | Cơ chế chính, chi tiết tới từng tài liệu | US-005 |
| Từ chối | Xử lý ngoại lệ mà không phải phá cách gán theo phòng ban | US-006 |
| Nhãn mật và mức mật | Tầng chặn chung, không phụ thuộc việc có nhớ gán quyền hay không | US-007 |

> Đọc được khi và chỉ khi `(cấp qua phòng ban HOẶC cấp trực tiếp)` **và** `nhãn mật ≤ mức mật`
> **và** `không có mục từ chối nào áp lên người đó hoặc phòng ban của người đó`.

Hai nhầm lẫn phải chặn từ đặc tả, vì cả hai đều "nghe hợp lý" nhưng sai:

- Mức mật cao **không** tự sinh ra quyền đọc. Người mức mật cao nhất mà chưa được cấp quyền vẫn
  không đọc được gì.
- Quyền gán trực tiếp **không** vượt được nhãn mật. Cấp quyền cho một người trên tài liệu có nhãn
  cao hơn mức của họ thì họ vẫn không đọc được — hệ thống phải cảnh báo ngay lúc gán, chứ không để
  người gán tưởng đã xong việc.

## Nơi luật được thi hành

Luật trên **không** thi hành ở màn hình quản trị. Nó thi hành ở bước truy hồi, và ở mọi đường khác
dẫn tới nội dung tài liệu: câu trả lời, trích dẫn, mở tài liệu gốc, tải tệp. Không đường nào được
bỏ qua bước kiểm — điều này là AC-008 của US-008.

Lọc là **pre-filter**: tập tài liệu được phép xác định trước khi xếp hạng, không phải lọc bỏ sau
khi đã lấy kết quả. Đánh đổi kỹ thuật đi kèm — lọc trước cộng chỉ mục xấp xỉ có thể làm tụt độ phủ
— được kiểm bằng kịch bản riêng ở AC-006 của US-008, chạy ngay trong đợt làm truy hồi chứ không
để đến kỳ đo cuối.

## Không rò rỉ gián tiếp

Sự tồn tại của tài liệu cũng là thông tin cần giữ. Khi người dùng hỏi một câu mà đáp án nằm trong
tài liệu ngoài quyền của họ, hệ thống trả lời bằng các nguồn khác hoặc từ chối vì không đủ căn cứ
— nó **không** báo rằng có tài liệu phù hợp mà họ không được xem.

Điều này ràng buộc cả thông báo từ chối ở `UC-refuse-answer` nhánh 2a: lời lẽ phải đủ để người
dùng biết nên liên hệ quản trị, nhưng không đủ để suy ra có tồn tại tài liệu nào.

## Quan hệ với use case khác

- `UC-ingest-document` bước 12 gọi sang use case này.
- Kết quả cấu hình quyền được thi hành ở `UC-single-turn-qa` bước 3.
- Nhánh 6a (thu hồi quyền) ảnh hưởng trực tiếp `UC-single-turn-qa` nhánh 10a.
