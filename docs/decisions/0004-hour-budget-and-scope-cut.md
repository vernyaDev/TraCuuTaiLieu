---
type: decision
decision_id: D-2026-09-24-hour-budget-and-scope-cut
feature: _project
status: active
decided_at: 2026-09-24
decided_by: "@longdt"
source: "Rà soát của Solution Architect và Product Owner 2026-09-23"
updated: 2026-09-24
links:
  - docs/_product/roadmap.md
  - docs/_product/prd.md
  - docs/decisions/0002-scope-and-internal-design.md
---

# Decision: Một nguồn duy nhất cho quỹ giờ, và câu trả lời cho "cắt hay không cắt"

> Decided: 2026-09-24 | By: @longdt | Source: Rà soát của Solution Architect và Product Owner 2026-09-23

## Context

Ngày 2026-09-09, hai tài liệu chính thức được viết cùng một ngày và **nói ngược nhau về chính con số
quyết định cả dự án**:

| Nguồn | Khoảng cách giờ | Chiến lược kèm theo |
|---|---|---|
| `prd.md` Mục 9 và Mục 10 | thiếu **29 giờ** | *"phải cắt theo thứ tự MoSCoW"* |
| `roadmap.md` Mục 1 và Mục 3 | thiếu **58 giờ** | *"Product Owner đã chốt **không cắt phạm vi**"* |

Không tài liệu nào tuyên bố thay thế tài liệu kia. Người thực hiện — cũng chính là người viết cả hai
— phải chọn một, và hai lựa chọn dẫn tới hai sản phẩm khác nhau.

Đợt rà soát ngày 2026-09-23 tìm thêm ba lỗi số học **bên trong chính roadmap**: sáu sprint cộng lại
**198 giờ** trong khi bảng khối của nó nói nhu cầu **212 giờ**; khối *"Tích hợp, gỡ lỗi, đệm — 20
giờ"* không xuất hiện trong bất kỳ sprint nào; và phần việc của sprint 6 không nằm trong 212 giờ ấy.

Điều làm chuyện này nguy hiểm không phải bản thân con số. Chiến lược ứng phó ở roadmap — rút 25 giờ
bộ câu hỏi ra khỏi quỹ lập trình, nâng nhịp lên 18–19 giờ mỗi tuần — được tính để đóng khoảng cách
**58 giờ**. Ai đọc PRD trước rồi tin vào 29 giờ sẽ đánh giá thấp mức khẩn cấp đúng vào tuần cần nó
nhất, tức tuần đầu tháng 10.

## Decision

**Bảng ước lượng bottom-up ở `roadmap.md` Mục 3 là nguồn duy nhất cho quỹ giờ.** `prd.md` Mục 9 trỏ
sang đó thay vì tự nêu số. Con số nào xuất hiện ở hai nơi thì nơi thứ hai là bản chép sẽ lệch.

**Về câu "cắt hay không cắt": có cắt, và cắt ngay bây giờ chứ không đợi khủng hoảng.** Quyết định
ngày 2026-09-09 *"không cắt phạm vi"* được thay bằng quyết định này.

## Rationale

Bảng bottom-up đáng tin hơn ước lượng thô vì nó cộng từng khối việc có thể kiểm lại được, còn con số
147 giờ ở PRD không có bảng nào đứng sau.

Về việc cắt: danh sách hoãn hiện có tiết kiệm tổng cộng 35 giờ nếu thực hiện **trọn vẹn cả bảy hạng
mục** — vẫn không đủ bù khoảng cách thật. Nghĩa là đòn bẩy duy nhất còn lại của phương án
*không-cắt* là nâng nhịp lên 18–19 giờ mỗi tuần suốt 11 tuần liên tiếp. Đó là một **giả định về sức
người chưa từng được đưa vào khung giả định để theo dõi**, trong khi bốn giả định đang có đều là giả
định kỹ thuật. Đặt cả kế hoạch lên một biến không ai đo là cách trượt hạn mà không ai thấy trước.

Cắt sớm còn có một lợi thế mà cắt muộn không có: **giờ tiết kiệm được dùng lại cho việc khác**. Cắt
vào tháng 11 theo cơ chế cảnh báo hiện tại thì giờ ấy chỉ để "không trễ thêm".

## Consequences

**Tích cực.** Một con số, một chiến lược, không còn hai tài liệu chính thức nói ngược nhau. Việc cắt
chuyển từ phản-ứng-khi-khủng-hoảng sang chủ-động-ngay, lúc còn đủ thời gian để dùng lại phần tiết
kiệm được.

**Tiêu cực.** Bản demo mất một phần năng lực quản trị tri thức. Phải nói rõ trong báo cáo đây là lựa
chọn có chủ đích kèm lý do, không phải phần làm không kịp.

**Việc phải làm theo sau quyết định này:**

1. Sửa `prd.md` Mục 9 và Mục 10 để trỏ sang roadmap thay vì nêu số riêng.
2. Sửa `roadmap.md` Mục 4 cho phép cộng sáu sprint khớp bảng khối, và phân bổ khối *tích hợp, gỡ
   lỗi, đệm* vào các sprint thay vì để nó không thuộc sprint nào.
3. Chốt danh sách cắt cụ thể — chưa quyết trong bản ghi này, xem mục Open Questions.
4. Lập một giả định mới cho nhịp làm việc thật, có chủ, có phương thức đo, có hạn.

## Open Questions

- [ ] OQ-1: Cắt cụ thể những hạng mục nào? Đợt rà soát đề xuất ba: US-036 sao lưu (5 giờ) · US-014
      phát hiện chồng lấn lúc nạp (8 giờ) · US-006 đặt quyền từ chối hạ từ Must xuống Should. Cả ba
      đều không map vào chỉ số nào ở Mục 8 của PRD. Chưa chốt.
- [ ] OQ-2: Nhịp làm việc thật duy trì được bao nhiêu giờ mỗi tuần? Cần đo hai tuần đầu tháng 10 rồi
      mới đặt con số, không đặt trước.
