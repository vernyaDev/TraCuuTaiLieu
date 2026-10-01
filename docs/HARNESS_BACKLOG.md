# Harness Backlog — vướng mắc về BỘ KHUNG

> Chỗ ghi mọi thứ làm bạn khó chịu khi **dùng** bộ khung: skill, rule, khuôn, thông báo,
> tài liệu hướng dẫn. **Không** phải chỗ ghi vướng mắc nghiệp vụ của dự án (cái đó là câu
> hỏi mở trong chính tài liệu feature).
>
> Đây là **sổ ghi quản trị**, không phải artifact BA — cố ý không có frontmatter, không
> tham gia truy vết. Chủ ghi: `/ba-architecture` (bảng ownership) — nhưng **trong tuần
> pilot thì ai vướng người đó tự ghi**, đừng chờ.
>
> Chỉ **append**, không sửa dòng cũ. Đã xử lý thì đổi cột Trạng thái, giữ nguyên nội dung.

## Cách ghi — một dòng là đủ

Đừng chờ viết cho hay. Một dòng lúc đang bực có giá trị hơn một đoạn viết lại sau ba ngày.

| Cột | Điền gì |
|---|---|
| **Ngày** | ISO `YYYY-MM-DD` |
| **Ai** | @handle |
| **Ở đâu** | Skill / rule / file cụ thể. Không biết thì ghi việc đang làm |
| **Vướng gì** | Bạn định làm gì, và điều gì cản. **Một câu** |
| **Bạn làm gì để đi tiếp** | Hỏi người khác · đoán · bỏ qua · làm tay. Cột này quan trọng nhất — nó cho biết mức thiệt hại thật |
| **Mức** | `chặn` (không đi tiếp được) · `chậm` (mất thời gian nhưng qua được) · `khó chịu` (qua được, thấy sai sai) |
| **Trạng thái** | `mở` · `đang sửa` · `đã sửa` · `không sửa (lý do)` |

> Cột **"Bạn làm gì để đi tiếp"** đừng bỏ trống. *"Phải đi hỏi anh B"* và *"đoán đại rồi
> chạy tiếp"* là hai mức thiệt hại rất khác nhau, dù cùng là một chỗ vướng.

---

## Vướng mắc từ pilot

| Ngày | Ai | Ở đâu | Vướng gì | Làm gì để đi tiếp | Mức | Trạng thái |
|---|---|---|---|---|---|---|
| 2026-09-09 | @longdt | `scripts/trace-check.mjs` hàm `defines()` | `naming-conventions.md` bảng ID conventions ghi SCR định nghĩa "trong `srs/{feature}-spec.md` Mục 10 **+ wireframe index**", nhưng code chỉ nhận nhánh SRS. Ở hình thái `prd-stories` (không sinh SRS) thì SCR không có chỗ nào là định nghĩa, nên 15 màn hình đều thành BROKEN dù tài liệu đúng | Sửa `defines()` nhận thêm `*/ascii-wireframe/*wireframe-index.md` cho kind `SCR`. Chạy lại 3 bộ test: approval-guard 6/6, trace-check-bridge 3/3, cli-guard 14/15 (case fail là lỗi hạ tầng mermaid có sẵn, không liên quan) | chặn | đã sửa |
| 2026-09-09 | @longdt | `scripts/trace-check.mjs` phạm vi mã US | Mã `US-NNN` là phạm vi thư mục feature, nên tài liệu cấp dự án (`_product/prd.md`) tham chiếu mã trần thì thành BROKEN. Đúng theo canon nhưng thông báo không nói ra điều đó, mất thời gian đoán | Ban đầu né bằng cách trỏ đường dẫn. Nhưng roadmap phải nhắc cả 36 story trong một bảng ưu tiên — trỏ đường dẫn thì bảng không đọc nổi, nên đã sửa tận gốc: tham chiếu từ tài liệu cấp dự án (khoá `_::`) khớp với định nghĩa ở **bất kỳ** feature nào. Kiểm hai chiều bằng cách chèn tạm một mã story và một mã NFR không tồn tại vào roadmap — cả hai vẫn bị bắt BROKEN, nên gate không bị nới lỏng | chậm | đã sửa |
| 2026-09-23 | @longdt | `kg/engine/kg-build.mjs` `parseScreenIndex()` | Hàm này lấy định danh màn từ `row.values.id`, mà bảng canon có **cả cột `ID` lẫn cột `Slug`** cùng ánh xạ về khoá `id` — cột đứng trước thắng, nên mã `SCR-*` thành định danh còn `Slug` bị bỏ qua. Cộng thêm: heading H3 trong mục `## Descriptions` sinh ra **một node màn thứ hai** lấy slug từ TÊN tiếng Việt. Hệ quả: mỗi màn có tới ba node (`scr-...`, tên-slug-hoá, slug thật), và 15 màn thành 35 node — đồ thị báo 20 màn "không thuộc flow nào" trong khi tài liệu đúng | Đổi tiêu đề cột đầu từ `ID` sang `Mã màn` (chuỗi này không nằm trong bảng ánh xạ) để cột `Slug` thành định danh. Kết quả: 35 node còn 20, phát hiện coverage 36 còn **5**. Năm cái còn lại là node sinh từ heading H3 có tên dài hơn slug (vd *Nhập hàng loạt tài khoản* so với `nhap-hang-loat`) — **không sửa**, vì bẻ tên màn tiếng Việt cho vừa parser là hỏng tài liệu để làm đẹp công cụ. Sửa đúng: `parseScreenIndex` ưu tiên cột `Slug`, và heading H3 không sinh node màn mới mà chỉ gắn mô tả vào node đã có | chậm | mở |
| 2026-09-23 | @longdt | `scripts/trace-check.mjs` phạm vi mã `US-NNN` | Tệp wireframe của `doc-ingestion` trích tiêu chí của một story thuộc `access-control` bằng mã trần dạng `US-NNN` → BROKEN, vì mã `US-NNN` có phạm vi theo thư mục feature. Đáng nói: viết thành wikilink `[[đường/dẫn\|mã story của access-control]]` **vẫn gãy**, do phần hiển thị của liên kết cũng chứa chính mã ấy và bị quét như tham chiếu | Bỏ hẳn mã trần khỏi văn bản, đổi thành *story "Đặt quyền từ chối"* kèm wikilink không mang mã. Cùng lớp lỗi với liên kết sai trong `CR-20260923-001` cùng ngày. Cần cân nhắc: quét tham chiếu nên bỏ qua phần hiển thị của wikilink, vì đó là văn bản cho người đọc chứ không phải tham chiếu máy | chậm | mở |
| 2026-09-23 | @longdt | `scripts/trace-check.mjs` dòng ~176 `isComponentRegistry` vs `rules/kg-governance.md` G5 | Lệnh mà **G5 quy định làm gate thăng status** — `trace-check.mjs --feature <feature> --strict` — báo **9 DANGLING oan** cho `qa-chat`, trong khi quét toàn vault thì sạch. Nguyên nhân: `--feature` loại mọi file không thuộc feature, và danh sách ngoại lệ "định nghĩa cấp dự án phải luôn nạp" mới chỉ có `_shared/spec-components.md` + `_shared/nfr.md`. **`_product/prd.md` — nơi định nghĩa toàn bộ 25 mã `CAP-*` — không nằm trong đó**, nên mọi tham chiếu CAP từ trong feature thành mồ côi. Với `--strict` thì lệnh này exit khác 0 và **chặn oan việc thăng status của bất kỳ feature nào có trỏ tới CAP** | Dùng bản quét toàn vault để xác nhận sạch, chưa chạy gate theo đúng câu lệnh G5. Sửa đề xuất: đổi `isComponentRegistry` thành danh sách định-nghĩa-cấp-dự-án gồm cả `_product/prd.md`. Đây là **khuôn lỗi thứ năm lần thứ tư**: luật liệt kê nhiều nơi định nghĩa hợp lệ, code chỉ cài một phần — ba lần trước là SCR ở wireframe index, mã US từ tài liệu cấp dự án, và `_shared/nfr.md` | chặn (gate sai) | mở |
| 2026-09-09 | @longdt | `kg/engine/kg-build.mjs` `parseUserFlow()` vs `rules/ba-conventions.md` S1 | Tiêu đề mục trong `userflow.md` phải khớp đúng chuỗi `danh sach flow`. Viết `## 3. Danh sách luồng` — thuần Việt hơn, đúng tinh thần S1 — thì parser **im lặng không nhận**: không cạnh CONTAINS nào được tạo, không cảnh báo nào, `trace-check` vẫn báo sạch. Chỉ lộ ra khi đối chiếu `kg counts` thấy không có node loại `flow` | Đổi lại thành `Danh sách flow` cho khớp parser, và đổi cột `Tên luồng` → `Tên flow` (mapping `ten flow` → title). Đây là khuôn mẫu lỗi số 2 của chính tài liệu này: luật viết ra mà cơ chế chỉ cài một nửa — parser nên nhận cả hai cách viết, hoặc `kg-build` nên cảnh báo khi file `srs-userflow` không sinh được flow nào | chậm | mở |
| 2026-09-09 | @longdt | `scripts/mermaid-verify.mjs` | `mmdc` chưa cài trên máy này. Script báo `Cú pháp compile: FAIL` cho cả 9 khối sơ đồ, kèm dòng `'mmdc.cmd' is not recognized` — tức **lỗi hạ tầng bị hiển thị y như lỗi nội dung**. Đây đúng bài học đã ghi ở `diagram-correctness.md` Mục 5 (tách `res.error` khỏi `res.status != 0`), nhưng nhánh `shell: true` của Windows không rơi vào `res.error` nên vẫn lọt | Đọc kỹ stderr để tự phân biệt; hai tầng còn lại (nhãn an toàn renderer, ngữ nghĩa luồng) đều PASS nên vẫn ghi file. **Chưa xác nhận được sơ đồ compile được** — cần cài `@mermaid-js/mermaid-cli` rồi chạy lại. Đề xuất sửa script: nhận diện chuỗi `is not recognized`/`command not found` trong stderr và in `Cú pháp compile: SKIPPED (thiếu mmdc)` thay vì FAIL | chậm | mở |
| 2026-09-09 | @longdt | `kg/engine/kg-query.mjs` lazy rebuild | `kg-usage.md` nói lazy-rebuild <1s tự lo khi query. Nhưng sau khi tạo 4 file `userflow.md` mới, `kg-query coverage` vẫn trả kết quả cũ; phải chạy tay `kg-build.mjs` mới thấy 9 flow | Chạy `kg-build.mjs` tay sau mỗi đợt tạo file mới. Cần kiểm lại điều kiện kích hoạt rebuild — nếu nó dựa trên mtime của thư mục thì file mới trong thư mục mới tạo có thể không đổi mtime gốc | chậm | mở |
| 2026-09-09 | @longdt | `scripts/trace-check.mjs` `walk()` · `defines()` · `featureOf()` | Thư mục `_shared` bị loại khỏi quét (vì chứa log máy sinh), chỉ `spec-components.md` được nạp riêng bằng một ngoại lệ đóng cứng. Đặt `nfr.md` vào đó thì 48 mã NFR không được coi là định nghĩa, mọi tham chiếu thành BROKEN/DANGLING | Mở rộng ngoại lệ của `walk()` thành danh sách tên tệp, và cho `defines()` nhận `_shared/nfr.md` cho kind NFR. Gốc vấn đề giống hệt mục SCR ở trên | chặn | đã sửa |

---

## Việc đã biết trước pilot

Ghi sẵn để không bị báo trùng, và để không quên. Nguồn: đợt rà soát 2026-08-10.

| # | Việc | Mức | Trạng thái | Ghi chú |
|---|---|---|---|---|
| H-01 | **Chưa chạy end-to-end lần nào.** Phase 0 và vòng `/cr` chưa ai đi qua | chặn (rủi ro) | **đang sửa** | Chính là mục tiêu của pilot này |
| H-02 | **Gate G5 không tự chạy** — chỉ chặn thăng status khi có người nhớ gõ lệnh. L0 có hook `PreToolUse`, G5 không có gì | chậm | mở — **hoãn có chủ đích** | Chờ pilot trả lời: quy ước này có thật sự bị trượt không. Trượt thật thì viết hook `.mjs` (chỉ cần Node, không vướng dependency như 6 hook kia) |
| H-03 | **Localization · accessibility · 6 state · hai-chiều FR↔screen là quy ước, không có gate** | khó chịu | không sửa (có chủ đích) | Cơ giới hoá hết sẽ làm gate ồn, mà gate ồn thì bị vô hiệu hoá. Việc cần làm là **biết** mình đang dựa vào kỷ luật người |
| H-04 | **31 tham chiếu `@` viết kiểu root-relative** trong khi 530 cái còn lại viết file-relative | khó chịu | mở | Cả hai đều trỏ tới file có thật, không gãy. Sửa gộp một lượt khi tiện, đừng sửa lẻ |
| H-05 | **6 hook chưa đăng ký** trong `settings.json` | khó chịu | không sửa (có chủ đích) | Phụ thuộc môi trường (bash + jq). Mỗi hook đã có cột "ai làm thay" ở `.claude/hooks/README.md` |
| H-06 | **`token-track` chưa bật** (statusline + hook `Skill`/`Stop`) | khó chịu | mở | Tuỳ chọn. Bật ở Phase 0 hoặc theo `.claude/scripts/token-track/README.md` |
| H-07 | **`legacy/harness-graph.json` + `harness-map.html` còn tên 4 skill đã xoá** | khó chịu | không sửa | Là ảnh chụp lịch sử, để nguyên mới đúng |

---

## Bốn khuôn lỗi — đọc trước khi vá bất cứ gì

Rút từ đợt rà soát 2026-08-10. Vá một lỗi mà không nhận ra nó thuộc khuôn nào thì lỗi cùng
khuôn sẽ quay lại.

1. **Dọn nửa chừng.** Xoá hoặc gộp một đơn vị mà chỉ sửa những chỗ bản ghi quyết định gọi
   tên. → **Việc CUỐI CÙNG của mọi lần xoá/gộp là `grep` tên nó toàn kho**, không phải việc
   đầu tiên.
2. **Luật viết ra mà không có cơ chế.** Chữ "chặn / bắt buộc" không trỏ được tới dòng code
   nào. Nguy hiểm **hơn** không có luật, vì người ta yên tâm sai.
3. **Một khái niệm định nghĩa hai chỗ** rồi lệch. Xem `rules/file-ownership.md` mục *"Áp cho
   `.claude/`"* — có bảng "định nghĩa gốc duy nhất ở đâu".
4. **Con số trong văn xuôi là dữ liệu không nguồn.** Đã cắn hai lần: số skill (75/73/**71**)
   và số assertion selftest (137/145/**158**). → Phát biểu bằng bất biến (`0 fail`), hoặc để
   đúng một chỗ kèm lệnh đếm lại.

**Khuôn thứ năm, phát hiện 2026-09-09:** *luật đúng nhưng code chỉ cài một nửa số nhánh của luật.*
Ba mục vướng đầu tiên của pilot đều cùng khuôn này: `naming-conventions.md` nói SCR sống ở
"spec **+ wireframe index**", code chỉ nhận spec; `_shared` bị loại khỏi quét với một ngoại lệ
đóng cứng đúng một tên tệp. Cả hai chỉ lộ ra ở hình thái `prd-stories` — hình thái mà bộ khung
tuyên bố hỗ trợ nhưng chưa ai chạy thật. → Khi luật liệt kê nhiều nơi hợp lệ, kiểm rằng code cài
**đủ** các nơi đó, đừng dừng ở nơi đầu tiên.
