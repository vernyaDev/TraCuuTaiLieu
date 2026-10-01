---
type: srs-userflow
feature: qa-chat
updated: 2026-09-23
primary_device: desktop
stage: flow-approved
flow_approved_at: 2026-09-23
flow_hash: "2fab63bc"
links:
  - docs/qa-chat/ascii-wireframe/qa-chat-wireframe-index.md
  - docs/qa-chat/usecases/qa-chat-usecase-index.md
  - docs/_shared/design-system.md
---

# Hỏi đáp có trích dẫn — User Flow

> Nguồn chia luồng duy nhất của tính năng này. `/wireframe-ascii` và `/wireframe-html` đọc file này
> để biết luồng nào gồm màn nào — không tự chia lại.

Khung vẽ: **1024 điểm ảnh** cho mọi luồng, theo `NFR-system-042` và mục 8 của hệ thống thị giác.

## 1. User Flow (tổng)

> Phủ happy, lỗi và biên. `[n]` là số màn hình, đối chiếu Mục 2. Nút bo tròn là điểm ra khỏi phạm vi
> tính năng này — chúng thuộc tính năng khác, không vẽ wireframe trong đợt này.

```mermaid
flowchart TD
    n1["[1] Hỏi đáp — rỗng lần đầu<br/>lời chào, 3 câu hỏi mẫu lấy từ kho của tổ chức"]
    g1{"Gửi được câu hỏi?"}
    e1["Nút gửi vô hiệu hoá<br/>ô rỗng hoặc đang có lượt chạy dở"]
    x2["Hệ thống bận<br/>vào hàng đợi, báo vị trí"]
    g2{"Hệ thống hiểu được câu hỏi?"}
    e2["Mời diễn đạt rõ hơn<br/>KHÔNG ghi bản ghi khoảng trống"]
    e3["Hỏi lại một câu làm rõ<br/>không trích dẫn, không khối nguồn"]
    g3{"Loại câu hỏi"}
    p2["Viết lại thành câu độc lập<br/>khối thu gọn Hệ thống đã hiểu câu hỏi của bạn là"]
    p3["Phân rã truy vấn con theo thứ tự phụ thuộc<br/>khối thu gọn Các bước đã thực hiện"]
    w1["Viết lại thất bại: chạy trên câu gốc<br/>nói rõ không chắc đã hiểu đúng ngữ cảnh"]
    w2["Tóm tắt hội thoại thất bại<br/>lùi về các lượt gần nhất, mời mở phiên mới"]
    p1["Chỉ báo tiến trình 3 bước<br/>hiểu câu hỏi, tìm tài liệu, soạn trả lời"]
    g4{"Đủ căn cứ?"}
    r1["Từ chối tầng một<br/>dừng trước khi gọi mô hình ngôn ngữ"]
    r2["Từ chối tầng hai<br/>loại bỏ hoàn toàn câu trả lời đã sinh"]
    r3["Khối từ chối, giọng bình thường<br/>nêu các khả năng, KHÔNG khẳng định khả năng nào"]
    r4["Khối riêng: ngoài phạm vi kho tri thức<br/>được nói thẳng, không ghi bản ghi khoảng trống"]
    a1["Câu trả lời hiện dần từng chữ<br/>nút gửi chuyển thành nút dừng"]
    a2["Trả lời một phần<br/>nêu rõ vế nào thiếu căn cứ, hoặc căn cứ còn mỏng"]
    a3["Khối cảnh báo<br/>hết hiệu lực · đã bị thay thế · chồng lấn"]
    a4["Khối nguồn: tên tài liệu + đường dẫn phân cấp tới đoạn"]
    s1["Lượt bị dừng<br/>giữ phần đã hiện, gắn dấu chưa hoàn tất"]
    x1["Mất kết nối giữa chừng<br/>giữ phần đã nhận"]
    x3["Phiên hết hạn sau 8 giờ không thao tác"]
    n2["[2] Xem tài liệu gốc<br/>ngăn kéo trượt từ phải, hội thoại vẫn thấy mờ phía sau"]
    l1["Đang tải: khung xương giữ hình dạng trang tài liệu"]
    g5{"Còn quyền đọc và tài liệu còn trong kho?"}
    y1["Nội dung tài liệu, tự cuộn tới đoạn trích<br/>nút tải tệp gốc kiểm quyền lần nữa"]
    z1["Không còn quyền truy cập<br/>không hiện nội dung nào"]
    z2["Tài liệu đã gỡ, không còn dùng làm căn cứ"]
    z3["Tài liệu đã xoá vĩnh viễn"]
    z4["Lỗi tải: không đọc được tệp<br/>nút thử lại tại chỗ"]
    ketthuc_dangnhap["Kết thúc luồng — sang Đăng nhập, thuộc access-control"]
    ketthuc_quantri["Kết thúc luồng — sang Danh sách tài liệu, thuộc doc-ingestion"]

    n1 --> g1
    g1 -->|"ô rỗng hoặc đang chạy dở"| e1
    e1 -.->|"gõ tiếp"| n1
    g1 -->|"đã có 5 câu đang xử lý"| x2
    x2 -.->|"tới lượt"| p1
    g1 -->|"gửi"| g2
    g2 -->|"gửi được nhưng không đủ nghĩa"| e2
    e2 -.->|"gõ lại"| n1
    g2 -->|"hiểu được hai cách, hoặc lượt đầu chưa có gì để bám"| e3
    e3 -.->|"nhân viên trả lời"| g2
    g2 -->|"rõ"| g3
    g3 -->|"một lượt"| p1
    g3 -->|"tiếp nối"| p2
    g3 -->|"phức tạp nhiều bước"| p3
    p2 -->|"viết lại được"| p1
    p2 -->|"viết lại thất bại"| w1
    p2 -->|"hội thoại quá dài"| w2
    w1 --> p1
    w2 --> p1
    p3 -->|"phân rã được"| p1
    p3 -->|"phân rã thất bại, hoặc vượt giới hạn truy vấn con"| p1
    p1 --> g4
    g4 -->|"không đoạn nào vượt ngưỡng liên quan"| r1
    g4 -->|"mọi ý không gắn được về nguồn"| r2
    r1 --> r3
    r2 --> r3
    g2 -->|"ngoài phạm vi kho tri thức"| r4
    r3 -.->|"hỏi lại theo cách khác"| n1
    r4 -.->|"hỏi việc khác"| n1
    g4 -->|"đủ"| a1
    g4 -->|"một số vế thiếu căn cứ, hoặc một vế quá hạn chờ"| a2
    a1 -->|"bấm dừng"| s1
    a1 -->|"mất kết nối"| x1
    a1 --> a3
    a2 --> a3
    a3 --> a4
    s1 -.->|"hỏi tiếp"| n1
    x1 -.->|"hỏi tiếp"| n1
    a4 -->|"bấm một dòng nguồn"| n2
    n2 --> l1
    l1 --> g5
    l1 -->|"không đọc được tệp"| z4
    z4 -.->|"thử lại"| l1
    g5 -->|"còn quyền, tài liệu còn"| y1
    g5 -->|"quyền đã bị thu hồi"| z1
    g5 -->|"tài liệu đã gỡ"| z2
    g5 -->|"đã xoá vĩnh viễn"| z3
    y1 -.->|"đóng ngăn kéo"| n1
    z1 -.->|"đóng ngăn kéo"| n1
    z2 -.->|"đóng ngăn kéo"| n1
    z3 -.->|"đóng ngăn kéo"| n1
    n1 -->|"phiên hết hạn"| x3
    x3 --> ketthuc_dangnhap
    n1 -->|"nút Khu quản trị, chỉ hiện với người có quyền"| ketthuc_quantri

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class n1,p1,p2,p3,a1,a3,a4,n2,y1 happy
    class e1,x1,z1,z4 error
    class e2,e3,w1,w2,r1,r2,r3,r4,a2,s1,x2,x3,z2,z3 edge
```

## 2. Danh sách màn hình

> Cột `Slug` là định danh máy đọc, khớp `## Screen: {slug}` bên wireframe. Cột `Luồng nhà` là nơi
> màn được vẽ; cột `Dùng chung ở` là các luồng khác đi qua nó mà không vẽ lại.

| [#] | Slug | Mã màn | Màn hình | Mục đích | Luồng nhà | Dùng chung ở |
|-----|------|--------|----------|----------|-----------|--------------|
| 1 | `hoi-dap` | `SCR-qa-chat-01` | Hỏi đáp | Đặt câu hỏi và nhận câu trả lời có trích dẫn | `hoi-dap` | — |
| 2 | `xem-tai-lieu-goc` | `SCR-qa-chat-02` | Xem tài liệu gốc | Kiểm chứng trích dẫn tại đúng đoạn rồi quay lại | `hoi-dap` | — |

## 3. Danh sách flow

| Flow-slug | Tên flow | Màn hình gồm | Cases phủ |
|-----------|-----------|--------------|-----------|
| `hoi-dap` | Hỏi đáp có trích dẫn | `hoi-dap` → `xem-tai-lieu-goc` | happy (một lượt, tiếp nối, phức tạp nhiều bước) · từ chối hai tầng, khối nêu các khả năng không khẳng định · ngoài phạm vi kho là khối riêng · trả lời một phần · hỏi lại làm rõ · lỗi (mất kết nối, hệ thống bận, phiên hết hạn, lỗi tải tài liệu) · biên (dừng lượt, viết lại thất bại, tóm tắt thất bại, quyền bị thu hồi, tài liệu đã gỡ hoặc đã xoá) |

Hai màn nằm chung một luồng vì quan hệ mở-từ-trích-dẫn rất chặt: mạch *gõ câu hỏi → thấy nguồn →
xác minh tại chỗ → quay lại hội thoại* sẽ gãy nếu tách qua hai tệp.

## 3.5. Chuyển màn

> Nguồn duy nhất cho việc nối điều hướng. Một dòng là một chuyển.

| Từ màn [#] | Đến màn [#] | Trigger | Điều kiện |
|-----------|------------|---------|-----------|
| Hỏi đáp [1] | Xem tài liệu gốc [2] | Bấm một dòng trong khối nguồn | Lượt đã hiện xong khối nguồn |
| Xem tài liệu gốc [2] | Hỏi đáp [1] | Đóng ngăn kéo | Mọi trạng thái, kể cả khi không còn quyền hoặc tài liệu đã gỡ |
| Hỏi đáp [1] | (giữ nguyên) [1] | Gửi câu hỏi | Không đủ nghĩa → mời diễn đạt lại; hoặc hệ thống hỏi lại làm rõ |

**Lối ra khỏi phạm vi tính năng** — không nằm trong bảng trên vì đích thuộc tính năng khác:

- Hỏi đáp [1] → màn **Đăng nhập** của `access-control`, khi phiên hết hạn 8 giờ không thao tác.
  Ghi nhớ trang đang mở để quay lại sau khi đăng nhập.
- Hỏi đáp [1] → màn **Danh sách tài liệu** của `doc-ingestion`, khi bấm nút *Khu quản trị* trên
  thanh trên. Nút chỉ hiện với người có quyền quản trị.

## 4. Điều phải giữ khi vẽ wireframe và viết spec

- **Khối từ chối nêu các khả năng, không khẳng định khả năng nào.** Giữ lối nói *"có thể chưa được
  cấp quyền"* **và cho nó hiện y hệt ở mọi lượt từ chối vì thiếu căn cứ** — lối nói mơ hồ chỉ bảo vệ
  được khi nó không đổi theo dữ liệu thật. Chỉ hiện nó khi thật sự có tài liệu bị chặn thì nó thôi
  là một khả năng và thành một câu khẳng định, lộ đúng thứ `AC-001` của US-008 cấm. Đổi thành câu
  khẳng định kiểu *"bạn không có quyền xem tài liệu này"* thì càng vi phạm. Lý do thật vẫn ghi chính
  xác vào dữ liệu khoảng trống tri thức, nơi chỉ quản trị đọc. Sửa 2026-09-23 theo `AC-003` của
  US-023; câu hỏi **ngoài phạm vi kho** là khối riêng, được nói thẳng (`AC-003b`).
- **Hỏi lại làm rõ là kết cục lượt thứ tư**, không phải lỗi và không phải từ chối. Nó không có khối
  nguồn, nên khung bốn phần của một lượt trả lời không áp cho nó.
- **Từ chối vì ngoài phạm vi kho không ghi bản ghi khoảng trống**; hai lý do còn lại thì có. Câu
  không đủ nghĩa cũng không ghi.
- **Bắt đầu phiên mới** đưa vùng hội thoại về trạng thái rỗng lần đầu; bối cảnh cũ không theo sang
  và không ảnh hưởng kết quả truy hồi của phiên mới (`AC-006` của US-021 và US-026).

## 5. Việc phải đồng bộ ngược sang wireframe index

> Chủ ghi của `ascii-wireframe/qa-chat-wireframe-index.md` là `/wireframe-ascii`. Áp khi chạy skill
> đó, qua L2 diff.

1. **Thêm cột `Slug` vào bảng Screens**, lấy đúng slug ở Mục 2 trên — đồ thị tri thức cần định danh
   máy đọc, hiện nó phải tự suy từ tên tiếng Việt.
2. **Thêm nút *Khu quản trị*** vào mô tả thanh trên của màn Hỏi đáp, kèm điều kiện chỉ hiện với
   người có quyền.
3. **Bổ sung ba trạng thái còn thiếu**: kết cục *hỏi lại làm rõ* ở màn Hỏi đáp; *đang tải* và *lỗi
   tải* ở ngăn kéo Xem tài liệu gốc.

## 6. Open Questions

- [ ] OQ-1: Hội thoại của phiên cũ có mở lại được không? Hiện không có màn lịch sử phiên nào trong
      danh sách 15 màn, nên mặc định là không. Nếu cần thì đây là màn thứ 16, quyết trước khi vẽ.
