---
type: srs-userflow
feature: doc-ingestion
updated: 2026-09-09
primary_device: desktop
stage: flow-approved
flow_approved_at: 2026-09-09
flow_hash: "25512846"
links:
  - docs/doc-ingestion/ascii-wireframe/doc-ingestion-wireframe-index.md
  - docs/doc-ingestion/usecases/doc-ingestion-usecase-index.md
  - docs/_shared/design-system.md
---

# Nạp và quản lý vòng đời tài liệu — User Flow

> Nguồn chia luồng duy nhất của tính năng này. Khung vẽ **1024 điểm ảnh**.

Ba luồng dùng chung hai màn: *Danh sách tài liệu* là điểm khởi phát của cả ba, *Chi tiết tài liệu*
vừa là nơi cổng thứ ba của luồng nạp diễn ra vừa là màn trung tâm của luồng quản lý kho. Mỗi màn
vẫn chỉ được vẽ một lần, ở luồng nhà của nó — xem Mục 2.

## 1. User Flow (tổng)

### Flow: nap-tai-lieu

> Ba cổng chặn nằm trên đúng luồng này. Không có đường nào vòng qua chúng.

```mermaid
flowchart TD
    n1["[1] Danh sách tài liệu<br/>dải việc đang chờ + bảng"]
    n2["[2] Tải tài liệu lên<br/>hộp thoại kéo thả"]
    g1{"Tệp hợp lệ?"}
    e1["Vượt 50MB — đánh dấu đỏ tại dòng<br/>không tính vào đợt nạp"]
    e2["Định dạng không hỗ trợ<br/>nêu bốn định dạng nhận được"]
    g2{"CỔNG 1 — đã chọn nhãn mật?"}
    e3["Nút xác nhận vô hiệu hoá<br/>trỏ vào hiện lý do. Không có nhãn mặc định"]
    g3{"Trùng tài liệu đã có?"}
    e4["Hỏi rõ: bỏ qua hay coi là bản thay thế"]
    p1["Đang xử lý — bóc văn bản"]
    g4{"Bóc được văn bản?"}
    f1["Trạng thái lỗi, vào thẻ Nạp lỗi<br/>bản scan · tệp đặt mật khẩu · hỏng cấu trúc"]
    p2["Tách đoạn theo cấu trúc<br/>giữ đường dẫn phân cấp và bảng biểu"]
    g5{"Tách được?"}
    f2["Trạng thái lỗi: tài liệu rỗng hoặc quá ngắn<br/>nêu số ký tự bóc được"]
    w1["Không có cấu trúc rõ — lùi về tách theo đoạn văn<br/>trích dẫn chỉ tới vị trí, không tới điều khoản"]
    w2["Bảng không bóc được cấu trúc — về văn bản phẳng<br/>cảnh báo nguy cơ trả lời sai"]
    p3["Điền sẵn metadata vòng đời<br/>kèm trích đoạn nguyên văn làm căn cứ"]
    n3["[3] Xác nhận metadata — chế độ xác nhận lần đầu<br/>hai cột đối chiếu với văn bản thật"]
    g6{"CỔNG 2 — metadata hợp lệ và đã xác nhận?"}
    e5["Từ chối lưu: ngày hết hiệu lực sớm hơn ngày hiệu lực"]
    e6["Từ chối lưu: quan hệ thay thế tạo vòng lặp"]
    h1["Dừng ở trạng thái chờ xác nhận<br/>KHÔNG vào phạm vi truy hồi"]
    p4["So chồng lấn với tài liệu còn hiệu lực cùng phân loại"]
    p5["Sinh biểu diễn ngữ nghĩa và lập chỉ mục"]
    g7{"Lập chỉ mục xong?"}
    f3["Lỗi lập chỉ mục — xoá sạch, không để mảnh vụn<br/>vào thẻ Nạp lỗi, thử lại không cần xác nhận metadata lại"]
    p6["Trạng thái sẵn sàng"]
    n6["[6] Xem trước các đoạn đã tách"]
    n4["[4] Chi tiết tài liệu — khối Quyền truy cập"]
    g8{"CỔNG 3 — đã gán quyền cho ai chưa?"}
    h2["Sẵn sàng nhưng không ai đọc được<br/>mặc định là đóng, không phải mở"]
    ketthuc_ok["Kết thúc luồng — nhân viên có quyền hỏi được nội dung ngay"]
    sum["Đợt nhiều tệp: dải tổng kết<br/>số thành công · số lỗi · số bỏ qua do trùng"]

    n1 -->|"nút Tải tài liệu lên"| n2
    n2 --> g1
    g1 -->|"vượt dung lượng"| e1
    g1 -->|"sai định dạng"| e2
    e1 -.->|"gỡ dòng, chọn tệp khác"| n2
    e2 -.->|"gỡ dòng, chọn tệp khác"| n2
    g1 -->|"hợp lệ"| g2
    g2 -->|"chưa chọn"| e3
    e3 -.->|"chọn nhãn mật"| g2
    g2 -->|"đã chọn"| g3
    g3 -->|"trùng"| e4
    e4 -.->|"bỏ qua"| sum
    e4 -->|"coi là bản thay thế"| p1
    g3 -->|"không trùng"| p1
    p1 --> g4
    g4 -->|"không bóc được"| f1
    f1 -.->|"sửa tệp rồi nạp lại"| n2
    g4 -->|"bóc được"| p2
    p2 --> g5
    g5 -->|"rỗng hoặc quá ngắn"| f2
    f2 -.->|"nạp lại bản khác"| n2
    g5 -->|"không có cấu trúc rõ"| w1
    g5 -->|"bảng hỏng cấu trúc"| w2
    g5 -->|"tách được"| p3
    w1 --> p3
    w2 --> p3
    p3 --> n3
    n3 --> g6
    g6 -->|"ngày mâu thuẫn"| e5
    g6 -->|"vòng lặp thay thế"| e6
    e5 -.->|"sửa lại"| n3
    e6 -.->|"sửa lại"| n3
    g6 -->|"chưa xác nhận"| h1
    h1 -.->|"mở lại từ thẻ chờ xác nhận metadata"| n3
    g6 -->|"đã xác nhận"| p4
    p4 --> p5
    p5 --> g7
    g7 -->|"lỗi"| f3
    f3 -.->|"thử lại lập chỉ mục"| p5
    g7 -->|"xong"| p6
    p6 --> sum
    p6 -->|"có cờ suy giảm chất lượng"| n6
    p6 --> n4
    n4 --> g8
    g8 -->|"chưa gán"| h2
    h2 -.->|"gán quyền"| g8
    g8 -->|"đã gán"| ketthuc_ok
    sum -.->|"bấm vào phần lỗi"| n1
    n6 -.->|"quay lại danh sách"| n1

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class n1,n2,p1,p2,p3,n3,p4,p5,p6,n4,ketthuc_ok happy
    class e1,e2,e5,e6,f1,f2,f3 error
    class e3,e4,w1,w2,h1,h2,n6,sum edge
```

### Flow: quan-ly-kho-tai-lieu

```mermaid
flowchart TD
    m1["[1] Danh sách tài liệu<br/>4 thẻ việc đang chờ · lọc · bảng phân trang"]
    s0["Rỗng lần đầu: giải thích kho trống<br/>nút tải tài liệu lên làm hành động chính"]
    s1["Rỗng do bộ lọc — nút xoá lọc ngay tại chỗ"]
    s2["Lỗi tải danh sách — thử lại tại chỗ<br/>không mất bộ lọc đang đặt"]
    c1["Thẻ: chờ xác nhận metadata"]
    c2["Thẻ: cảnh báo chồng lấn chưa xử lý"]
    c3["Thẻ: nạp lỗi"]
    c4["Thẻ: sắp hết hiệu lực trong 30 ngày<br/>kèm số ngày còn lại, sắp theo ngày gần nhất"]
    m4["[4] Chi tiết tài liệu — ngăn kéo trượt từ phải"]
    b1["Thông tin chung"]
    b2["Vòng đời + chuỗi thay thế nhiều đời"]
    b3["Quyền truy cập: được phép · bị từ chối · ô tra cứu một người"]
    b4["Cảnh báo chồng lấn, dẫn sang luồng xử lý chồng lấn"]
    m6["[6] Xem trước các đoạn đã tách<br/>hai cột, chọn đoạn thì bên trái nổi bật"]
    q1["Dải cảnh báo suy giảm chất lượng<br/>bảng không bóc được · tài liệu không có cấu trúc rõ"]
    m3["[3] Xác nhận metadata — chế độ chỉnh sửa<br/>có hiệu lực ngay ở câu hỏi kế tiếp"]
    m3r["[3] Xác nhận metadata — chế độ chỉ đọc<br/>khi người xem không có quyền sửa"]
    g1{"Thao tác trên tài liệu"}
    d1["Gỡ tài liệu — thân chuyển chỉ đọc<br/>kèm dải thông báo và nút khôi phục"]
    d2["Xoá vĩnh viễn — hộp thoại nêu số người sẽ mất quyền đọc<br/>và số trích dẫn đang trỏ tới, rồi mới bật nút"]
    d3["Tài liệu đang xử lý: khối quyền vô hiệu hoá kèm lý do"]
    ketthuc_chonglap["Kết thúc luồng — sang Xử lý cảnh báo chồng lấn"]

    m1 --> s0
    m1 --> s1
    m1 --> s2
    s1 -.->|"xoá lọc"| m1
    s2 -.->|"thử lại"| m1
    m1 --> c1
    m1 --> c2
    m1 --> c3
    m1 --> c4
    c1 -.->|"lọc bảng theo nhóm"| m1
    c2 -.->|"lọc bảng theo nhóm"| m1
    c3 -.->|"lọc bảng theo nhóm"| m1
    c4 -.->|"lọc bảng theo nhóm"| m1
    m1 -->|"bấm một dòng"| m4
    m4 --> b1
    m4 --> b2
    m4 --> b3
    m4 --> b4
    b1 --> g1
    b2 --> g1
    b3 --> g1
    b4 -->|"bấm khối cảnh báo"| ketthuc_chonglap
    s0 -.->|"nạp tài liệu đầu tiên"| m1
    d3 -.->|"xử lý xong"| m4
    m4 -->|"nút xem trước các đoạn"| m6
    m6 --> q1
    q1 -.->|"quay lại"| m4
    m4 --> g1
    g1 -->|"nút sửa metadata, người có quyền sửa"| m3
    g1 -->|"xem lại, không có quyền sửa"| m3r
    m3 -.->|"lưu xong"| m4
    m3r -.->|"đóng"| m4
    g1 -->|"gỡ"| d1
    d1 -.->|"khôi phục"| m4
    g1 -->|"xoá vĩnh viễn"| d2
    d2 -.->|"xác nhận, ngăn kéo đóng"| m1
    g1 -->|"tài liệu đang xử lý"| d3
    m4 -.->|"đóng ngăn kéo"| m1

    classDef happy fill:#d4edda,stroke:#28a745
    classDef error fill:#f8d7da,stroke:#dc3545
    classDef edge fill:#fff3cd,stroke:#ffc107

    class m1,m4,m6,m3,b1,b2,b3 happy
    class s2,d2 error
    class s0,s1,c1,c2,c3,c4,b4,q1,m3r,d1,d3 edge
```

### Flow: xu-ly-chong-lap

```mermaid
flowchart TD
    k1["[1] Danh sách tài liệu — thẻ cảnh báo chồng lấn"]
    k5["[5] Xử lý cảnh báo chồng lấn<br/>mỗi cặp một thẻ, hai tài liệu đặt cạnh nhau"]
    k0["Rỗng: không có cảnh báo nào<br/>nói rõ đây là trạng thái tốt, không phải lỗi"]
    kl["Đang tải"]
    g1{"Quyết định của quản trị — không có nút mặc định"}
    r1["Xác nhận đây là bản thay thế<br/>chuyển sang luồng thay thế, ghi chuỗi"]
    r2["Hai văn bản cùng tồn tại hợp lệ<br/>câu trả lời dùng cả hai sẽ kèm cảnh báo"]
    r3["Bỏ qua cảnh báo — báo động giả<br/>không hiện lại cho đúng cặp này"]
    done["Chuyển sang danh sách đã xử lý, xem lại được"]
    multi["Một tài liệu nghi chồng lấn với nhiều tài liệu<br/>nhiều thẻ, xếp theo mức giống nhau giảm dần"]

    k1 -->|"bấm thẻ cảnh báo chồng lấn"| k5
    k5 --> kl
    kl --> k0
    kl --> multi
    multi --> g1
    kl --> g1
    g1 -->|"là bản thay thế"| r1
    g1 -->|"cùng tồn tại hợp lệ"| r2
    g1 -->|"báo động giả"| r3
    r1 --> done
    r2 --> done
    r3 --> done
    done -.->|"còn cặp khác"| g1
    done -.->|"hết"| k1
    k0 -.->|"quay lại"| k1

    classDef happy fill:#d4edda,stroke:#28a745
    classDef edge fill:#fff3cd,stroke:#ffc107

    class k1,k5,g1,r1,r2,done happy
    class k0,kl,r3,multi edge
```

## 2. Danh sách màn hình

| [#] | Slug | Mã màn | Màn hình | Mục đích | Luồng nhà | Dùng chung ở |
|-----|------|--------|----------|----------|-----------|--------------|
| 1 | `danh-sach-tai-lieu` | `SCR-doc-ingestion-01` | Danh sách tài liệu | Trông coi kho ở quy mô 1.000 tài liệu, và trả lời "hôm nay phải làm gì" | `quan-ly-kho-tai-lieu` | `nap-tai-lieu`, `xu-ly-chong-lap` |
| 2 | `tai-tai-lieu-len` | `SCR-doc-ingestion-02` | Tải tài liệu lên | Chọn tệp, đặt nhãn mật và phân loại cho cả đợt | `nap-tai-lieu` | — |
| 3 | `xac-nhan-metadata` | `SCR-doc-ingestion-03` | Xác nhận metadata vòng đời | Đối chiếu metadata máy điền với văn bản thật rồi xác nhận | `nap-tai-lieu` | `quan-ly-kho-tai-lieu` (chế độ chỉnh sửa và chỉ đọc) |
| 4 | `chi-tiet-tai-lieu` | `SCR-doc-ingestion-04` | Chi tiết tài liệu | Xem vòng đời, gán quyền, gỡ hoặc xoá | `quan-ly-kho-tai-lieu` | `nap-tai-lieu` (cổng 3 gán quyền) |
| 5 | `xu-ly-chong-lap` | `SCR-doc-ingestion-05` | Xử lý cảnh báo chồng lấn | Quyết từng cặp tài liệu nghi chồng lấn | `xu-ly-chong-lap` | — |
| 6 | `xem-truoc-doan` | `SCR-doc-ingestion-06` | Xem trước các đoạn đã tách | Kiểm chất lượng tách đoạn trước khi tin vào kho | `quan-ly-kho-tai-lieu` | `nap-tai-lieu` (khi có cờ suy giảm chất lượng) |

## 3. Danh sách flow

| Flow-slug | Tên flow | Màn hình gồm | Cases phủ |
|-----------|----------|--------------|-----------|
| `nap-tai-lieu` | Nạp tài liệu vào kho | `danh-sach-tai-lieu` → `tai-tai-lieu-len` → `xac-nhan-metadata` → `chi-tiet-tai-lieu` | happy 12 bước · ba cổng chặn · lỗi trước khi tải (dung lượng, định dạng) · lỗi bóc (bản scan, đặt mật khẩu, hỏng cấu trúc) · lỗi tách (rỗng, quá ngắn) · suy giảm chất lượng (không cấu trúc, bảng hỏng) · trùng tài liệu · metadata mâu thuẫn · vòng lặp thay thế · lỗi lập chỉ mục · một phần thành công |
| `quan-ly-kho-tai-lieu` | Trông coi kho đã có | `danh-sach-tai-lieu` → `chi-tiet-tai-lieu` → `xem-truoc-doan` | happy (lọc, tìm, phân trang, thao tác hàng loạt) · bốn thẻ việc đang chờ · sửa metadata sau khi đã xác nhận · gỡ và khôi phục · xoá vĩnh viễn có nêu số liệu · rỗng lần đầu · rỗng do lọc · lỗi tải · tài liệu đang xử lý |
| `xu-ly-chong-lap` | Xử lý cảnh báo chồng lấn | `danh-sach-tai-lieu` → `xu-ly-chong-lap` | ba quyết định không có mặc định · một tài liệu nhiều cặp · rỗng là trạng thái tốt · đang tải · đang gửi · đã xử lý |

## 3.5. Chuyển màn

| Từ màn [#] | Đến màn [#] | Trigger | Điều kiện |
|-----------|------------|---------|-----------|
| Danh sách [1] | Tải lên [2] | Nút *Tải tài liệu lên* | — |
| Tải lên [2] | Danh sách [1] | Xác nhận đợt nạp | Đã chọn nhãn mật (cổng 1) |
| Danh sách [1] | Xác nhận metadata [3] | Bấm thẻ *chờ xác nhận metadata*, hoặc dòng tài liệu ở trạng thái chờ | Tài liệu đã bóc và tách xong |
| Xác nhận metadata [3] | Danh sách [1] | Xác nhận (cổng 2) | Ngày hợp lệ, quan hệ thay thế không tạo vòng lặp |
| Danh sách [1] | Chi tiết [4] | Bấm một dòng | — |
| Chi tiết [4] | Xác nhận metadata [3] | Nút *sửa metadata* | Người xem có quyền sửa → chế độ chỉnh sửa; không có quyền → chế độ chỉ đọc |
| Chi tiết [4] | Xem trước đoạn [6] | Nút *xem trước các đoạn* | — |
| Chi tiết [4] | Xử lý chồng lấn [5] | Bấm khối cảnh báo | Tài liệu đang mang nhãn nghi chồng lấn |
| Chi tiết [4] | Danh sách [1] | Đóng ngăn kéo, hoặc xác nhận xoá vĩnh viễn | — |
| Danh sách [1] | Xử lý chồng lấn [5] | Bấm thẻ *cảnh báo chồng lấn chưa xử lý* | — |
| Danh sách [1] | (giữ nguyên) [1] | Bấm một thẻ việc đang chờ | Lọc bảng theo đúng nhóm của thẻ |

## 4. Điều phải giữ khi vẽ wireframe và viết spec

- **Ba cổng chặn phải chặn ở giao diện, không chỉ ở máy chủ.** Nhãn mật không có giá trị mặc định;
  tài liệu chưa xác nhận metadata không vào phạm vi truy hồi; tài liệu chưa gán quyền thì không ai
  đọc được.
- **Cảnh báo chồng lấn không chặn luồng nạp** — tài liệu vẫn vào kho và vẫn trả lời được câu hỏi.
- **Lỗi lập chỉ mục không để lại mảnh vụn**, và đường ra là thử lại lập chỉ mục, không bắt xác nhận
  metadata lại vì metadata đã lưu.
- Hai giá trị *không có thời hạn* và *chưa xác định* của ngày hết hiệu lực là hai thứ khác nhau,
  không gộp.

## 5. Việc phải đồng bộ ngược sang wireframe index

> Ba mục dưới đây là thay đổi với `ascii-wireframe/doc-ingestion-wireframe-index.md`, file có chủ
> ghi riêng. Áp một lượt khi chạy `/wireframe-ascii`, qua L2 diff.

1. **Thêm thẻ thứ tư *sắp hết hiệu lực*** vào dải việc đang chờ của màn Danh sách, và thêm giá trị
   *sắp hết hiệu lực* vào ô lọc trạng thái hiệu lực — `AC-005` của US-016 đòi lọc nhóm sắp hết hiệu
   lực trong 30 ngày kèm số ngày còn lại, hiện không có chỗ nào hiển thị.
2. **Màn Xác nhận metadata có ba chế độ** thay vì hai: xác nhận lần đầu · **chỉnh sửa** (vào từ nút
   *sửa metadata*, có hiệu lực ngay ở câu hỏi kế tiếp theo `AC-009` của US-013) · chỉ đọc (khi người
   xem không có quyền sửa). Dòng hiện tại ghi mở lại từ Chi tiết tài liệu là chỉ đọc — nếu giữ
   nguyên thì nút *sửa metadata* hứa một hành vi mà màn không cho làm.
3. **Thêm cột `Slug` vào bảng Screens của wireframe index**, lấy đúng slug ở Mục 2 trên. Hiện index
   chỉ có mã màn và tên tiếng Việt, nên đồ thị tri thức tự suy slug từ tên và sinh ra node trùng
   (`xac-nhan-metadata-vong-doi` bên cạnh `xac-nhan-metadata`). Slug ở userflow là bản chuẩn.
4. **Tài liệu mang cờ suy giảm chất lượng phải nhìn thấy được ở mức danh sách** — một cột thẻ nhỏ
   hoặc một giá trị lọc. Hiện chỉ thấy khi mở đúng tài liệu đó ra xem trước đoạn, tức phải mở mù
   từng tài liệu trong kho 1.000 tài liệu.

## 6. Open Questions

- [ ] OQ-1: Tệp bị **bỏ qua vì trùng** tính vào cột nào của dải tổng kết đợt nạp? `AC-005` của
      US-010 chỉ có hai cột thành công và lỗi; tệp bỏ qua không thuộc cột nào.
- [ ] OQ-2: Đóng ngăn kéo Chi tiết tài liệu lúc đang có thay đổi chưa lưu thì cảnh báo hay bỏ
      luôn? Chưa nguồn nào chốt.
