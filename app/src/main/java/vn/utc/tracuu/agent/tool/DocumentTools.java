package vn.utc.tracuu.agent.tool;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.agent.AgentContext;
import vn.utc.tracuu.agent.retriever.RetrievedChunk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bước 6 — tra metadata của chính những tài liệu vừa truy hồi.
 *
 * <p>Truy hồi tìm đoạn <em>liên quan nhất</em>, không tìm đoạn <em>còn hiệu lực nhất</em>. Một
 * quy định đã bị thay thế vẫn nói về đúng chủ đề người dùng hỏi, nên nó vẫn được xếp hạng cao.
 * Bước này là thứ ngăn hệ thống trả lời bằng văn bản đã hết hiệu lực mà không nói gì.
 *
 * <p>Ba loại cảnh báo, và chúng <strong>khác nhau về hành động người đọc phải làm</strong>:
 * <ul>
 *   <li><em>Hết hiệu lực</em> — thông tin có thể đã sai, phải tra bản mới.</li>
 *   <li><em>Đã bị thay thế</em> — có bản mới cụ thể, hệ thống chỉ thẳng ra.</li>
 *   <li><em>Chồng lấn</em> — hai văn bản cùng tồn tại hợp lệ nhưng nói về cùng một việc; người
 *       đọc cần biết để không tưởng chỉ có một nguồn.</li>
 * </ul>
 *
 * <p>Cảnh báo chồng lấn chỉ phát khi quản trị đã xác nhận cặp ấy <em>cùng tồn tại hợp lệ</em> —
 * tức quyết định theo cặp phải đọc được ngay trong đường sinh câu trả lời, không chỉ ở màn
 * quản trị. Cặp còn ở trạng thái chờ thì im lặng, vì cảnh báo dựa trên nghi ngờ chưa ai xác
 * nhận chỉ làm nhiễu.
 */
@Component
public class DocumentTools {

    /** Ngưỡng "sắp hết hiệu lực" tính theo ngày. */
    private static final int EXPIRING_SOON_DAYS = 30;

    private final JdbcTemplate jdbc;
    private final vn.utc.tracuu.access.PermissionResolver permissions;

    public DocumentTools(JdbcTemplate jdbc, vn.utc.tracuu.access.PermissionResolver permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public AgentContext apply(AgentContext ctx) {
        if (ctx.chunks().isEmpty()) return ctx;

        List<Long> docIds = ctx.chunks().stream()
                .map(RetrievedChunk::documentId)
                .distinct()
                .toList();

        List<AgentContext.Warning> warnings = new ArrayList<>();
        warnings.addAll(validityWarnings(docIds));
        warnings.addAll(supersessionWarnings(docIds, permissions.resolve(ctx.scope())));
        warnings.addAll(overlapWarnings(docIds));

        return ctx.withWarnings(warnings);
    }

    /**
     * Hết hiệu lực và sắp hết hiệu lực.
     *
     * <p>Điều kiện {@code has_no_expiry = false} là cốt yếu: tài liệu khai "không có thời hạn"
     * thì không bao giờ hết hạn, và tài liệu <em>chưa khai</em> thời hạn là thiếu thông tin chứ
     * không phải sắp hết hạn. Gộp hai thứ đó lại là cách sinh cảnh báo giả hàng loạt, và cảnh
     * báo giả hàng loạt thì người dùng sẽ bỏ qua cả cảnh báo đúng.
     */
    private List<AgentContext.Warning> validityWarnings(List<Long> docIds) {
        LocalDate today = LocalDate.now();
        return jdbc.query("""
                SELECT id, title, effective_to,
                       CASE WHEN effective_to < ? THEN 'HET_HIEU_LUC' ELSE 'SAP_HET_HIEU_LUC' END AS kind
                FROM documents
                WHERE id = ANY(?)
                  AND has_no_expiry = false
                  AND effective_to IS NOT NULL
                  AND effective_to <= ?
                """,
                (rs, i) -> {
                    LocalDate to = rs.getObject("effective_to", LocalDate.class);
                    String kind = rs.getString("kind");
                    String detail = "HET_HIEU_LUC".equals(kind)
                            ? "Văn bản đã hết hiệu lực từ ngày " + to
                            : "Văn bản hết hiệu lực vào ngày " + to;
                    return new AgentContext.Warning(kind, rs.getLong("id"), rs.getString("title"), detail);
                },
                today, docIds.toArray(new Long[0]), today.plusDays(EXPIRING_SOON_DAYS));
    }

    /**
     * Tài liệu nguồn đã bị một văn bản khác thay thế — chỉ thẳng bản thay thế cho người đọc.
     *
     * <p><strong>Tên bản thay thế chỉ được nêu khi người hỏi được phép đọc chính nó.</strong>
     * Bản trước lấy {@code new_doc.title} không qua bộ lọc quyền nào, nên câu cảnh báo
     * "Đã được thay thế bởi: {tên}" tiết lộ tên một tài liệu ngoài quyền của người hỏi — đi
     * vòng qua đúng bất biến mà cả hệ thống được dựng để giữ, và đi vòng ở một đường phụ mà
     * không ai nghĩ là đường truy hồi.
     *
     * <p>Không phải cứ giấu là xong: việc "văn bản anh đang đọc đã bị thay thế" là thông tin
     * an toàn, và nó nói về tài liệu người hỏi ĐÃ được phép đọc. Nên cảnh báo vẫn phát, chỉ
     * bỏ phần tên. Bỏ hẳn cảnh báo sẽ để người đọc hành động theo một văn bản đã hết dùng —
     * một cái hại khác, không phải một cái an toàn hơn.
     */
    private List<AgentContext.Warning> supersessionWarnings(
            List<Long> docIds, vn.utc.tracuu.access.ReadableDocuments doDuoc) {
        return jdbc.query("""
                SELECT old.id, old.title, new_doc.id AS successor_id,
                       new_doc.title AS successor_title
                FROM documents old
                JOIN documents new_doc ON new_doc.supersedes_document_id = old.id
                WHERE old.id = ANY(?)
                  AND new_doc.status = 'DA_LAP_CHI_MUC'
                """,
                (rs, i) -> {
                    boolean thayDuocBanMoi = doDuoc.permits(rs.getLong("successor_id"));
                    String chiTiet = thayDuocBanMoi
                            ? "Đã được thay thế bởi: " + rs.getString("successor_title")
                            : "Đã được thay thế bởi một văn bản bạn chưa được cấp quyền đọc. "
                              + "Liên hệ quản trị nếu cần bản mới.";
                    return new AgentContext.Warning(
                            "DA_BI_THAY_THE", rs.getLong("id"), rs.getString("title"), chiTiet);
                },
                // Bọc thành Object[] một phần tử: truyền thẳng Long[] vào tham số biến-độ-dài
                // sẽ bị TRẢI thành nhiều tham số, trong khi câu lệnh chỉ có một dấu hỏi.
                new Object[]{docIds.toArray(new Long[0])});
    }

    /**
     * Hai tài liệu nguồn đã được xác nhận cùng tồn tại hợp lệ mà lại nói về cùng một việc.
     *
     * <p>Chỉ cảnh báo khi <strong>cả hai</strong> tài liệu của cặp đều nằm trong kết quả truy
     * hồi. Một tài liệu chồng lấn với một tài liệu không được trích dẫn thì người đọc không có
     * gì để nhầm lẫn, và cảnh báo lúc ấy chỉ là tiếng ồn.
     */
    private List<AgentContext.Warning> overlapWarnings(List<Long> docIds) {
        if (docIds.size() < 2) return List.of();

        Long[] ids = docIds.toArray(new Long[0]);
        List<AgentContext.Warning> out = new ArrayList<>();
        Set<Long> reported = new LinkedHashSet<>();

        jdbc.query("""
                SELECT p.doc_a_id, p.doc_b_id, p.similarity, a.title AS title_a, b.title AS title_b
                FROM document_overlap_pairs p
                JOIN documents a ON a.id = p.doc_a_id
                JOIN documents b ON b.id = p.doc_b_id
                WHERE p.decision = 'CUNG_TON_TAI'
                  AND p.doc_a_id = ANY(?) AND p.doc_b_id = ANY(?)
                """,
                rs -> {
                    long a = rs.getLong("doc_a_id");
                    long b = rs.getLong("doc_b_id");
                    String titleA = rs.getString("title_a");
                    String titleB = rs.getString("title_b");
                    if (reported.add(a)) {
                        out.add(new AgentContext.Warning("CHONG_LAN", a, titleA,
                                "Nội dung chồng lấn với: " + titleB));
                    }
                    if (reported.add(b)) {
                        out.add(new AgentContext.Warning("CHONG_LAN", b, titleB,
                                "Nội dung chồng lấn với: " + titleA));
                    }
                }, ids, ids);

        return out;
    }
}
