---
type: srs-userflow
feature: knowledge-gap
updated: 2026-09-09
primary_device: desktop
stage: flow-approved
flow_approved_at: 2026-09-09
flow_hash: "abb41e15"
links:
  - docs/knowledge-gap/ascii-wireframe/knowledge-gap-wireframe-index.md
  - docs/knowledge-gap/userstories/knowledge-gap-story-index.md
  - docs/qa-chat/usecases/uc-refuse-answer.md
---

# Báo cáo khoảng trống tri thức — User Flow

> Nguồn chia luồng duy nhất của tính năng này. Khung vẽ **1024 điểm ảnh**.

Một màn, một luồng. Nhưng dữ liệu của nó sinh ra ở tính năng khác: mỗi lần `qa-chat` từ chối trả
lời, hoặc trả lời được nhưng căn cứ mỏng, là một bản ghi rơi vào đây.

## 1. User Flow (tổng)

### Flow: bao-cao-khoang-trong

```mermaid
flowchart TD
    src["Nguồn dữ liệu: mỗi lượt hỏi ở qa-chat<br/>ghi bản ghi khi từ chối, hoặc khi căn cứ mỏng"]
    batdau_thanhben["Thanh bên trái khu quản trị"]
    n1["[1] Báo cáo khoảng trống tri thức<br/>bộ lọc thời gian + ba tab + danh sách nhóm"]
    perm{"Người xem có quyền quản trị tri thức?"}
    ketthuc_khongduquyen["Kết thúc luồng — không đủ quyền, không hiện dữ liệu nào<br/>kèm liên kết quay về màn hỏi đáp"]
    load["Đang tải: khung xương giữ hình dạng danh sách thẻ"]
    err["Lỗi tải — thử lại tại chỗ"]
    t1["Tab: thiếu tài liệu<br/>kho không có thông tin — việc cần làm là nạp thêm tài liệu"]
    t2["Tab: thiếu quyền<br/>tài liệu đã có trong kho — việc cần làm là gán quyền"]
    t3["Tab: căn cứ mỏng<br/>đã trả lời được nhưng chỉ dựa trên một đoạn liên quan thấp"]
    sub{"Bộ lọc phụ theo trạng thái"}
    e0["Rỗng lần đầu: chưa có câu hỏi nào bị từ chối<br/>nói rõ đây là trạng thái tốt và màn này sẽ hiện gì khi có dữ liệu"]
    e1["Rỗng do khoảng thời gian đang chọn<br/>kèm nút mở rộng khoảng"]
    list["Danh sách nhóm, xếp theo số lần được hỏi giảm dần<br/>con số lớn · câu hỏi đại diện · khoảng thời gian từ lần đầu tới lần gần nhất"]
    tag["Thẻ nhỏ: đã xử lý nhưng chưa hết"]
    exp["Mở rộng nhóm: các câu cụ thể kèm thời điểm<br/>câu đã viết lại là câu chính, câu gốc thu gọn để đối chiếu"]
    dept["Nhóm thiếu quyền kèm phòng ban của người hỏi<br/>KHÔNG có bộ lọc theo người dùng"]
    ketthuc_naptailieu["Kết thúc luồng — sang Danh sách tài liệu, thuộc doc-ingestion"]
    ketthuc_ganquyen["Kết thúc luồng — sang Chi tiết tài liệu khối quyền, thuộc doc-ingestion"]
    mark["Đánh dấu đã xử lý — đang gửi"]
    done["Chuyển sang bộ lọc đã xử lý"]
    again["Sau đó vẫn có người hỏi và vẫn bị từ chối<br/>quay lại chờ xử lý, KHÔNG lặng lẽ tạo nhóm mới"]

    src --> n1
    batdau_thanhben --> perm
    perm -->|"không"| ketthuc_khongduquyen
    perm -->|"có"| n1
    n1 --> load
    load -->|"lỗi"| err
    err -.->|"thử lại"| load
    load --> t1
    load --> t2
    load --> t3
    t1 --> sub
    t2 --> sub
    t3 --> sub
    sub -->|"chưa có dữ liệu nào"| e0
    sub -->|"khoảng thời gian không có nhóm nào"| e1
    e1 -.->|"mở rộng khoảng"| sub
    sub -->|"chờ xử lý, hoặc đã xử lý"| list
    list --> tag
    list -->|"bấm mũi tên mở rộng"| exp
    exp --> dept
    exp -->|"nhóm thiếu tài liệu"| ketthuc_naptailieu
    dept -->|"nhóm thiếu quyền"| ketthuc_ganquyen
    e0 -.->|"khi bắt đầu có dữ liệu"| list
    tag -.->|"mở nhóm xem chi tiết"| exp
    list -->|"nút đánh dấu đã xử lý"| mark
    exp -->|"nút đánh dấu đã xử lý"| mark
    mark --> done
    done -.->|"xem lại"| sub
    done --> again
    again -.->|"gắn thẻ và quay lại chờ xử lý"| list

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class batdau_thanhben,n1,t1,t2,list,exp,mark,done,ketthuc_naptailieu,ketthuc_ganquyen happy
    class ketthuc_khongduquyen,err error
    class src,load,t3,e0,e1,tag,dept,again edge
```

## 2. Danh sách màn hình

| [#] | Slug | Mã màn | Màn hình | Mục đích | Luồng nhà | Dùng chung ở |
|-----|------|--------|----------|----------|-----------|--------------|
| 1 | `bao-cao-khoang-trong` | `SCR-knowledge-gap-01` | Báo cáo khoảng trống tri thức | Trả lời một câu hỏi: kho đang thiếu gì, và thiếu cái nào trước | `bao-cao-khoang-trong` | — |

## 3. Danh sách flow

| Flow-slug | Tên flow | Màn hình gồm | Cases phủ |
|-----------|----------|--------------|-----------|
| `bao-cao-khoang-trong` | Đọc và đóng khoảng trống tri thức | `bao-cao-khoang-trong` | happy (lọc, xếp theo tần suất, mở rộng nhóm, đánh dấu đã xử lý) · ba tab · đã xử lý nhưng chưa hết · rỗng lần đầu · rỗng do khoảng thời gian · đang tải · lỗi tải · không đủ quyền · đang gửi |

## 3.5. Chuyển màn

| Từ màn [#] | Đến màn [#] | Trigger | Điều kiện |
|-----------|------------|---------|-----------|
| Báo cáo [1] | (giữ nguyên) [1] | Đổi tab, đổi khoảng thời gian, đổi bộ lọc trạng thái | — |

**Lối vào và lối ra khỏi phạm vi tính năng:**

- Thanh bên khu quản trị → **Báo cáo [1]**, bấm mục *Khoảng trống tri thức*. Người không có quyền
  quản trị tri thức nhận thông báo, không thấy dữ liệu nào.
- Báo cáo [1] → màn **Danh sách tài liệu** của `doc-ingestion`, bấm liên kết trong một nhóm *thiếu
  tài liệu*. Đây là liên kết điều hướng, không phải nút hành động.
- Báo cáo [1] → màn **Chi tiết tài liệu** của `doc-ingestion`, bấm liên kết trong một nhóm *thiếu
  quyền*, khi xác định được tài liệu liên quan.

## 4. Điều phải giữ khi vẽ wireframe và viết spec

- **Không có bộ lọc theo người dùng, và không có đường nào liệt kê mọi câu một người đã hỏi.** Câu
  hỏi của nhân viên tiết lộ họ đang quan tâm việc gì; công cụ quản trị tri thức không được lặng lẽ
  biến thành công cụ giám sát. Đây là ranh giới cứng, không phải khuyến nghị.
- **Ba tab tách bạch vì việc cần làm khác hẳn nhau**, không phải vì dữ liệu khác nhau. Trộn chung
  thì con số tổng không nói lên điều gì hành động được.
- **Tab *căn cứ mỏng* không phải nhóm từ chối.** Những câu này hệ thống **đã trả lời được**; chúng
  là khoảng trống sắp thành vấn đề. Gộp vào tab *thiếu tài liệu* sẽ làm con số *số lần bị hỏi mà
  không trả lời được* trên mỗi thẻ sai nghĩa. Chốt 2026-09-09.
- **Từ chối vì câu hỏi ngoài phạm vi kho không tạo bản ghi** — đó không phải tín hiệu về kho tri
  thức. Câu hỏi rỗng hoặc quá ngắn bị chặn ngay ở khâu nhập cũng không tạo.
- **Nhóm đã xử lý mà vẫn bị hỏi thì quay lại chờ xử lý kèm thẻ riêng**, không tạo nhóm mới. Đây là
  tín hiệu tài liệu vừa nạp không trả lời được câu hỏi thật.
- Hai lý do kỹ thuật bên dưới *thiếu tài liệu* — không truy hồi được gì từ đầu, và mọi ý sinh ra bị
  loại vì không gắn được nguồn — **gộp hiển thị có chủ đích** vì việc khắc phục giống nhau. Chúng
  vẫn tách ở tầng dữ liệu theo `uc-refuse-answer` nhánh 1b2.

## 5. Việc phải đồng bộ ngược sang wireframe index

> Chủ ghi của `ascii-wireframe/knowledge-gap-wireframe-index.md` là `/wireframe-ascii`. Áp khi chạy
> skill đó, qua L2 diff.

1. **Thêm cột `Slug` vào bảng Screens**, lấy đúng slug ở Mục 2 trên.
2. **Ba tab thay vì hai** — thêm tab *căn cứ mỏng* vào mô tả màn, kèm giải thích vì sao nó không
   nằm chung với tab *thiếu tài liệu*.
3. **Thêm liên kết điều hướng** từ mỗi nhóm sang màn khắc phục thật, và ghi rõ mở rộng nhóm hiện
   câu đã viết lại làm câu chính.

## 6. Open Questions

- [ ] OQ-1: Phòng ban chỉ có một hai người thì hiển thị phòng ban ở nhóm *thiếu quyền* gần như bằng
      chỉ đích danh người hỏi. Có gộp phòng ban dưới N người vào cấp khối cha khi hiển thị không, và
      N bằng bao nhiêu? Chưa nguồn nào chốt; ở quy mô tổ chức hiện tại có thể chấp nhận được, nhưng
      phải quyết chứ không để trôi.
