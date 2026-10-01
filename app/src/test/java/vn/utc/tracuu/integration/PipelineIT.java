package vn.utc.tracuu.integration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import vn.utc.tracuu.access.*;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.AgentEvent;
import vn.utc.tracuu.agent.AgentPipeline;
import vn.utc.tracuu.agent.retriever.Retriever;
import vn.utc.tracuu.ingestion.*;
import vn.utc.tracuu.knowledge.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp trên cơ sở dữ liệu THẬT.
 *
 * <p>Đây là lớp kiểm chứng mà kiểm thử đơn vị không thay được, vì phần rủi ro nhất của hệ thống
 * này nằm trong <strong>SQL</strong> chứ không nằm trong logic Java: truy vấn truy hồi lai có
 * mệnh đề lọc quyền ghép động, phép trải vị trí từ để lấy tần suất, toán tử khoảng cách vector,
 * và 29 bảng phải khớp với các thực thể. Không thứ nào trong số đó kiểm được bằng mock.
 *
 * <p><strong>Tự bỏ qua khi không có cơ sở dữ liệu</strong>, nên {@code mvn test} vẫn xanh trên
 * máy chưa dựng hạ tầng. Chạy có hạ tầng thì truyền địa chỉ vào:
 *
 * <pre>
 *   mvn verify -Dtracuu.it.enabled=true \
 *              -Dtracuu.it.db=jdbc:postgresql://localhost:5432/tracuu_it \
 *              -Dtracuu.it.user=tracuu_it -Dtracuu.it.password=... \
 *              -Dtracuu.it.redis=localhost
 * </pre>
 *
 * <p>Dùng một cơ sở dữ liệu RIÊNG cho kiểm thử. Bộ kiểm này ghi tài liệu, người dùng
 * và quyền thật rồi tự dọn; chĩa nó vào cơ sở dữ liệu đang phục vụ là trộn dữ liệu thử
 * vào dữ liệu vận hành, và một lần dọn lỗi sẽ để rác lại đúng nơi không nên có rác.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestDoubles.class)
@TestPropertySource(properties = {
        // sslmode=disable: cơ sở dữ liệu kiểm thử nằm trên liên kết riêng của máy, và bản
        // triển khai thật cũng để app với db trong cùng một mạng nội bộ của Docker. Bật SSL
        // ở đây chỉ thêm một chứng thư tự tạo cần bảo trì mà không thêm bảo đảm nào.
        "spring.datasource.url=${tracuu.it.db:jdbc:postgresql://localhost:5432/tracuu}?sslmode=disable",
        "spring.datasource.username=${tracuu.it.user:tracuu}",
        // Mật khẩu PHẢI truyền được từ ngoài. Cơ sở dữ liệu trong bản triển khai Docker lấy
        // mật khẩu từ tệp .env, nên một giá trị ghi cứng ở đây biến "chạy kiểm thử trên đúng
        // hạ tầng vừa dựng" thành một lỗi xác thực khó hiểu.
        "spring.datasource.password=${tracuu.it.password:tracuu_dev}",
        "spring.data.redis.host=${tracuu.it.redis:localhost}",
        "tracuu.storage.root=./target/it-documents",
        "tracuu.retrieval.top-k=5",
        // KHÔNG ghi đè ngưỡng từ chối: phép kiểm phải chạy trên đúng con số của sản phẩm.
        // Hạ ngưỡng cho kiểm thử dễ qua là tự tay vô hiệu hoá chính thứ đang kiểm — lần trước
        // ngưỡng 0,05 làm một câu hỏi hoàn toàn không liên quan vẫn nhận được câu trả lời.
        "spring.liquibase.enabled=true",
        // Khớp với tên mô hình của bản giả. Phải khai: SÁU phép kiểm khởi động cũng chạy
        // trong kiểm thử này, và phép số 3 chặn nếu máy chủ mô hình không có đúng mô hình
        // đang cấu hình. Đó là hành vi ĐÚNG — nên ở đây ta khai cho khớp, không tắt phép kiểm.
        "tracuu.llm.model=stub-model",
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// Điều kiện bỏ qua phải ở CẤP LỚP, không phải một câu giả định trong @BeforeAll.
//
// Bản trước dùng Assumptions trong @BeforeAll và trông như đã bỏ qua đúng — nhưng @AfterAll
// nhận một JdbcTemplate, nên JUnit vẫn phải dựng ngữ cảnh Spring để gọi nó. Máy không có cơ
// sở dữ liệu vì thế nhận một lỗi kết nối đỏ ngay ở chỗ đáng lẽ là một dòng "đã bỏ qua". Chú
// giải này chặn từ trước khi lớp được khởi tạo, nên lời hứa "mvn test vẫn xanh trên máy chưa
// dựng hạ tầng" mới thành thật.
@EnabledIfSystemProperty(named = "tracuu.it.enabled", matches = "true",
        disabledReason = "Bỏ qua kiểm thử tích hợp: chưa bật -Dtracuu.it.enabled=true")
class PipelineIT {

    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired UserRepository users;
    @Autowired DepartmentRepository departments;
    @Autowired DocumentAclRepository acl;
    @Autowired PermissionResolver permissions;
    @Autowired PermissionAdminService permissionAdmin;
    @Autowired DocumentRepository documents;
    @Autowired ChunkSetRepository chunkSets;
    @Autowired ChunkRepository chunks;
    @Autowired IngestionPipeline ingestion;
    @Autowired Retriever retriever;
    @Autowired AgentPipeline agent;
    @Autowired Bm25StatsService bm25;

    private static Long chunkSetId;
    private static Long documentId;
    private static Long allowedUserId;
    private static Long blockedUserId;


    // ── 1. Lược đồ ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Lược đồ dựng đủ bảng và phần mở rộng vector đủ mới")
    void luocDoDungDu() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, Integer.class);
        assertNotNull(tables);
        assertTrue(tables >= 29, "Thiếu bảng: đếm được " + tables);

        String version = jdbc.queryForObject(
                "SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
        assertNotNull(version, "Chưa cài phần mở rộng vector");
        assertTrue(atLeastVersion(version, 0, 8),
                "Phần mở rộng vector " + version + " quá cũ: thiếu quét chỉ mục lặp thì lọc quyền "
                + "trước xếp hạng sẽ làm tụt độ phủ mà không có dấu hiệu");
    }

    @Test
    @Order(2)
    @DisplayName("Chỉ mục toàn văn và chỉ mục xấp xỉ đều có mặt")
    void chiMucCoMat() {
        List<String> indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'chunks'", String.class);
        assertTrue(indexes.contains("idx_chunks_tsv"), "Thiếu chỉ mục toàn văn");
        assertTrue(indexes.contains("idx_chunks_embedding"), "Thiếu chỉ mục xấp xỉ trên vector");
    }

    // ── 2. Nạp tài liệu ───────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("Nạp một tài liệu: bóc chữ, tách đoạn, ghi vector, cập nhật thống kê")
    void napTaiLieu() throws Exception {
        chunkSetId = chunkSets.findByActiveTrue().orElseThrow().getId();

        Path file = Path.of("./target/it-documents/quy-che-nghi-phep.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                QUY CHẾ NGHỈ PHÉP

                Chương I
                Điều 1. Phạm vi áp dụng
                Quy chế này áp dụng cho toàn bộ người lao động đang làm việc tại công ty
                theo hợp đồng lao động không xác định thời hạn.

                Điều 2. Số ngày nghỉ hằng năm
                Người lao động làm việc đủ mười hai tháng cho một người sử dụng lao động
                thì được nghỉ hằng năm mười hai ngày làm việc trong điều kiện bình thường.
                Người làm nghề nặng nhọc được nghỉ mười bốn ngày làm việc.

                Điều 3. Thủ tục xin nghỉ
                Người lao động gửi đơn xin nghỉ phép cho trưởng phòng trước ít nhất ba ngày
                làm việc. Trường hợp nghỉ liên tục từ năm ngày trở lên phải có thêm phê duyệt
                của ban giám đốc.
                """, StandardCharsets.UTF_8);

        Document doc = new Document();
        doc.setTitle("tạm");
        doc.setOriginalFilename("quy-che-nghi-phep.txt");
        doc.setStoragePath("quy-che-nghi-phep.txt");
        doc.setSensitivity(1);
        documents.save(doc);
        documentId = doc.getId();

        List<String> stages = new ArrayList<>();
        IngestionPipeline.Result result = ingestion.run(
                file, "quy-che-nghi-phep.txt", documentId,
                chunkSets.findById(chunkSetId).orElseThrow(), stages::add);

        assertTrue(result.chunkCount() > 0, "Không tách được đoạn nào");
        assertTrue(stages.size() >= 5, "Thiếu bước báo tiến độ: " + stages);

        doc.setStatus(DocumentStatus.DA_LAP_CHI_MUC);
        doc.setIndexedAt(java.time.Instant.now());
        documents.save(doc);

        // Vector phải ghi được và đọc lại đúng số chiều — chỗ dễ hỏng vì kiểu vector là kiểu
        // riêng của phần mở rộng, không phải kiểu SQL chuẩn.
        Integer dims = jdbc.queryForObject("""
                SELECT vector_dims(embedding) FROM chunks
                WHERE document_id = ? AND embedding IS NOT NULL LIMIT 1
                """, Integer.class, documentId);
        assertEquals(768, dims, "Số chiều vector trong cơ sở dữ liệu không khớp khai báo");

        // Đường dẫn phân cấp phải bám cấu trúc văn bản pháp quy.
        List<String> breadcrumbs = jdbc.queryForList(
                "SELECT breadcrumb FROM chunks WHERE document_id = ? AND breadcrumb <> ''",
                String.class, documentId);
        assertFalse(breadcrumbs.isEmpty(), "Không đoạn nào có đường dẫn phân cấp");
        assertTrue(breadcrumbs.stream().anyMatch(b -> b.contains("Điều")),
                "Đường dẫn phân cấp không nhận ra cấu trúc Điều: " + breadcrumbs);
    }

    @Test
    @Order(11)
    @DisplayName("Thống kê BM25 tính được từ chỉ mục toàn văn")
    void thongKeBm25() {
        bm25.recompute(chunkSetId);
        Bm25StatsService.Corpus corpus = bm25.corpus(chunkSetId);

        assertFalse(corpus.isEmpty(), "Thống kê rỗng: nhánh từ khoá sẽ không chạy");
        assertTrue(corpus.averageLength() > 0);

        Integer terms = jdbc.queryForObject(
                "SELECT count(*) FROM bm25_term_df WHERE chunk_set_id = ?", Integer.class, chunkSetId);
        assertNotNull(terms);
        assertTrue(terms > 20, "Quá ít từ trong thống kê: " + terms);
    }

    // ── 3. Phân quyền ─────────────────────────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("Tài khoản chưa được cấp quyền không đọc được gì")
    void chuaCapQuyenThiKhongDocDuocGi() {
        blockedUserId = createUser("it-bi-chan", 1);

        ReadableDocuments readable = permissions.resolve(new PermissionScope.ForUser(blockedUserId));
        assertTrue(readable.isEmpty(),
                "Từ chối mặc định bị vi phạm: tài khoản mới đọc được tài liệu chưa ai cấp quyền");
    }

    @Test
    @Order(21)
    @DisplayName("Cấp quyền qua phòng ban có hiệu lực ngay ở câu hỏi kế tiếp")
    void capQuyenCoHieuLucNgay() {
        allowedUserId = createUser("it-duoc-doc", 1);
        Department dept = departments.findByNameIgnoreCase("Nhan su")
                .orElseGet(() -> departments.save(new Department("Nhan su", null)));

        permissionAdmin.setDepartments(allowedUserId, List.of(dept.getId()), allowedUserId);
        permissionAdmin.grant(documentId, SubjectType.PHONG_BAN, dept.getId(),
                AclEffect.CHO_PHEP, allowedUserId);

        // Không đăng nhập lại, không đợi nạp lại kho: số hiệu phiên bản quyền đã tăng nên
        // đệm cũ bị bỏ và tập được phép tính lại ngay.
        ReadableDocuments readable = permissions.resolve(new PermissionScope.ForUser(allowedUserId));
        assertTrue(readable.permits(documentId), "Quyền vừa cấp chưa có hiệu lực");
    }

    @Test
    @Order(22)
    @DisplayName("Mục từ chối thắng mọi mục cho phép")
    void tuChoiThangChoPhep() {
        // Đi qua lớp quản trị quyền chứ không gọi thẳng kho dữ liệu: các truy vấn xoá cần
        // giao dịch, và chính lớp ấy mới tăng số hiệu phiên bản quyền. Gọi thẳng kho là bỏ
        // qua đúng cái bất biến mà phép kiểm này muốn xác nhận.
        permissionAdmin.grant(documentId, SubjectType.NGUOI_DUNG, allowedUserId,
                AclEffect.TU_CHOI, allowedUserId);
        try {
            assertFalse(permissions.resolve(new PermissionScope.ForUser(allowedUserId))
                            .permits(documentId),
                    "Mục từ chối không chặn được, dù chỉ cần một chỗ lệch là một lỗ rò rỉ");

            PermissionResolver.AccessExplanation why = permissions.explain(allowedUserId, documentId);
            assertFalse(why.allowed());
            assertTrue(why.reason().toLowerCase().contains("từ chối"),
                    "Lời giải thích không nêu đúng nguyên nhân: " + why.reason());
        } finally {
            // Dọn trong finally: một phép kiểm đỏ không được kéo theo các phép kiểm sau đỏ vì
            // dữ liệu sót. Đây chính là cái bẫy đã làm bốn phép kiểm phía sau đổ oan.
            permissionAdmin.revoke(documentId, SubjectType.NGUOI_DUNG, allowedUserId,
                    AclEffect.TU_CHOI, allowedUserId);
        }
    }

    @Test
    @Order(23)
    @DisplayName("Nhãn mật cao hơn mức mật thì chặn, dù đã cấp quyền")
    void nhanMatChanDuDaCapQuyen() {
        permissionAdmin.setSensitivity(documentId, 3, allowedUserId);
        assertFalse(permissions.resolve(new PermissionScope.ForUser(allowedUserId))
                .permits(documentId));

        permissionAdmin.setSensitivity(documentId, 1, allowedUserId);
        assertTrue(permissions.resolve(new PermissionScope.ForUser(allowedUserId))
                .permits(documentId));
    }

    // ── 4. Truy hồi ───────────────────────────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("Truy hồi lai trả về đoạn đúng, kèm cả hai điểm số")
    void truyHoiLai() {
        Retriever.Outcome outcome = retriever.retrieve(
                "nghỉ hằng năm bao nhiêu ngày",
                new PermissionScope.ForUser(allowedUserId), chunkSetId, 5);

        assertFalse(outcome.isEmpty(), "Truy hồi không trả về đoạn nào");
        assertTrue(outcome.chunks().stream()
                        .anyMatch(c -> c.content().contains("mười hai ngày làm việc")),
                "Không tìm được đoạn chứa câu trả lời");

        // Cả hai nhánh phải hoạt động: nhánh nào im lặng trả rỗng thì truy hồi lai chỉ còn
        // một nhánh mà vẫn trông như hai.
        assertTrue(outcome.chunks().stream().anyMatch(c -> c.scoreBm25() != null),
                "Nhánh từ khoá không trả về gì");
        assertTrue(outcome.chunks().stream().anyMatch(c -> c.scoreVector() != null),
                "Nhánh ngữ nghĩa không trả về gì");
    }

    @Test
    @Order(31)
    @DisplayName("Lọc quyền là TIỀN lọc: người không có quyền nhận về tập rỗng")
    void locQuyenLaTienLoc() {
        Retriever.Outcome outcome = retriever.retrieve(
                "nghỉ hằng năm bao nhiêu ngày",
                new PermissionScope.ForUser(blockedUserId), chunkSetId, 5);

        assertTrue(outcome.isEmpty(),
                "Người không có quyền vẫn nhận được đoạn — đây là rò rỉ nội dung");
        assertEquals(0, outcome.permittedDocumentCount());
    }

    @Test
    @Order(32)
    @DisplayName("Đọc không lọc quyền trả về kết quả VÀ ghi nhật ký kiểm toán")
    void docKhongLocGhiKiemToan() {
        Integer before = jdbc.queryForObject(
                "SELECT count(*) FROM permission_audit WHERE action = 'DOC_KHONG_LOC_QUYEN'",
                Integer.class);

        Retriever.Outcome outcome = retriever.retrieve(
                "nghỉ hằng năm bao nhiêu ngày",
                new PermissionScope.Unfiltered("kiem-thu-phan-loai-khoang-trong"), chunkSetId, 5);

        assertFalse(outcome.isEmpty(), "Đọc không lọc phải thấy tài liệu");

        Integer after = jdbc.queryForObject(
                "SELECT count(*) FROM permission_audit WHERE action = 'DOC_KHONG_LOC_QUYEN'",
                Integer.class);
        assertNotNull(before);
        assertNotNull(after);
        assertEquals(before + 1, after,
                "Mỗi lần đọc không lọc quyền phải để lại một dòng kiểm toán — thiếu nó thì "
                + "đường đặc quyền này trở thành đường không ai theo dõi");
    }

    // ── 5. Toàn chuỗi ─────────────────────────────────────────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("Toàn chuỗi: hỏi một câu, nhận câu trả lời kèm trích dẫn")
    void toanChuoiRaTrichDan() {
        List<AgentEvent> events = new ArrayList<>();
        AgentContext start = AgentContext.start(
                "Người lao động được nghỉ hằng năm bao nhiêu ngày?",
                new PermissionScope.ForUser(allowedUserId), chunkSetId, null, List.of());

        AgentPipeline.Outcome outcome = agent.run(start, events::add, () -> false);

        assertFalse(outcome.context().refused(),
                "Bị từ chối trong khi tài liệu có câu trả lời và người hỏi có quyền");
        assertNotNull(outcome.context().answer());
        assertFalse(outcome.citations().isEmpty(), "Câu trả lời không có trích dẫn nào");

        assertTrue(events.stream().anyMatch(e -> e instanceof AgentEvent.Token),
                "Không có sự kiện đẩy chữ nào");
        assertTrue(events.stream().anyMatch(e -> e instanceof AgentEvent.Citations),
                "Không phát sự kiện trích dẫn");

        // Trích dẫn phải chỉ tới ĐÚNG ĐOẠN, không chỉ tới tên tài liệu.
        AgentEvent.Citations citations = events.stream()
                .filter(AgentEvent.Citations.class::isInstance)
                .map(AgentEvent.Citations.class::cast)
                .findFirst().orElseThrow();
        assertTrue(citations.items().stream()
                        .anyMatch(c -> c.breadcrumb() != null && !c.breadcrumb().isBlank()),
                "Trích dẫn thiếu đường dẫn phân cấp");
    }

    @Test
    @Order(41)
    @DisplayName("Hai người khác quyền, cùng một câu hỏi, kết quả khác nhau")
    void haiNguoiKhacQuyen() {
        String question = "Người lao động được nghỉ hằng năm bao nhiêu ngày?";

        AgentPipeline.Outcome allowed = agent.run(AgentContext.start(
                question, new PermissionScope.ForUser(allowedUserId), chunkSetId, null, List.of()),
                e -> { }, () -> false);

        AgentPipeline.Outcome blocked = agent.run(AgentContext.start(
                question, new PermissionScope.ForUser(blockedUserId), chunkSetId, null, List.of()),
                e -> { }, () -> false);

        assertFalse(allowed.context().refused());
        assertTrue(blocked.context().refused(),
                "Người không có quyền vẫn nhận được câu trả lời");

        // Câu trả lời của người có quyền KHÔNG được lọt vào phản hồi của người không có quyền,
        // dưới bất kỳ hình thức nào.
        assertNull(blocked.context().answer());
        assertTrue(blocked.citations().isEmpty());
        assertFalse(blocked.context().refusal().message().contains("mười hai ngày"),
                "Nội dung tài liệu rò rỉ qua thông báo từ chối");
    }

    @Test
    @Order(42)
    @DisplayName("Thông báo từ chối GIỐNG HỆT nhau giữa thiếu quyền và kho trống")
    void thongBaoTuChoiGiongNhau() {
        AgentPipeline.Outcome thieuQuyen = agent.run(AgentContext.start(
                "Người lao động được nghỉ hằng năm bao nhiêu ngày?",
                new PermissionScope.ForUser(blockedUserId), chunkSetId, null, List.of()),
                e -> { }, () -> false);

        AgentPipeline.Outcome khoTrong = agent.run(AgentContext.start(
                // Cố ý không dùng chung TỪ NÀO với tài liệu thử: phép nhúng túi-từ của bản giả đo
                // độ giống về từ vựng, nên một câu hỏi có chữ "quy trình" vẫn khớp lỏng lẻo và
                // không rơi vào nhánh từ chối như ý định của phép kiểm.
                "Máy ép thủy lực xưởng đúc hiệu chỉnh áp suất bao nhiêu bar?",
                new PermissionScope.ForUser(allowedUserId), chunkSetId, null, List.of()),
                e -> { }, () -> false);

        assertTrue(thieuQuyen.context().refused(),
                "Người không có quyền phải bị từ chối");
        assertTrue(khoTrong.context().refused(),
                "Câu hỏi không có tài liệu nào trả lời được phải bị từ chối, nhưng lại ra: "
                + khoTrong.context().answer());
        assertEquals(thieuQuyen.context().refusal().message(),
                khoTrong.context().refusal().message(),
                """
                Hai ca này phải cho cùng một câu chữ. Khác nhau dù một chữ là đủ để người hỏi
                so hai lần từ chối và suy ra tài liệu nào tồn tại ngoài quyền của mình.""");
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────────────────

    /** So phiên bản tối giản. Phép so đầy đủ đã có bộ kiểm riêng ở gói khởi động. */
    private static boolean atLeastVersion(String actual, int major, int minor) {
        String[] parts = actual.split("[.-]");
        int a = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        int b = parts.length > 1 ? Integer.parseInt(parts[1].replaceAll("[^0-9]", "")) : 0;
        return a > major || (a == major && b >= minor);
    }

    private Long createUser(String username, int clearance) {
        return users.findByUsernameIgnoreCase(username).map(User::getId).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            u.setFullName("Kiểm thử " + username);
            u.setPasswordHash(encoder.encode("Kiemthu@123"));
            u.setClearanceLevel(clearance);
            u.setRole(Role.NHAN_VIEN);
            u.setActive(true);
            u.setMustChangePassword(false);
            return users.save(u).getId();
        });
    }

    @AfterAll
    static void cleanUp(@Autowired JdbcTemplate jdbc) {
        if (documentId == null) return;
        try {
            jdbc.update("DELETE FROM chunks WHERE document_id = ?", documentId);
            jdbc.update("DELETE FROM document_acl WHERE document_id = ?", documentId);
            jdbc.update("DELETE FROM documents WHERE id = ?", documentId);
            jdbc.update("DELETE FROM user_departments WHERE user_id = ANY(?)",
                    new Object[]{new Long[]{allowedUserId, blockedUserId}});
            jdbc.update("DELETE FROM users WHERE username LIKE 'it-%'");
        } catch (RuntimeException e) {
            // Dọn dẹp thất bại không được làm phép kiểm đỏ — dữ liệu thử còn lại là vô hại.
        }
    }
}
