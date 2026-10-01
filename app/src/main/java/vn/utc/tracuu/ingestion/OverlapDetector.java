package vn.utc.tracuu.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.knowledge.Document;
import vn.utc.tracuu.knowledge.DocumentRepository;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.util.List;

/**
 * Phát hiện tài liệu chồng lấn sau khi nạp, và gán cụm chủ đề.
 *
 * <p>Hai việc này khác nhau và hay bị gộp nhầm:
 * <ul>
 *   <li><strong>Cụm chủ đề</strong> là bộ lọc thu hẹp, gán một lần lúc nạp. Hai tài liệu khác
 *       cụm thì không cần so nữa. Nó <em>không</em> mang được quyết định của quản trị.</li>
 *   <li><strong>Cặp chồng lấn</strong> mang mức độ giống nhau và quyết định cho từng cặp riêng.
 *       Một tài liệu có thể chồng lấn với tài liệu này và không với tài liệu kia, nên đây bắt
 *       buộc là quan hệ theo cặp chứ không phải một thuộc tính trên tài liệu.</li>
 * </ul>
 *
 * <p>So theo <strong>tâm vector</strong> của tài liệu — trung bình vector các đoạn. Cách này
 * bắt được "hai văn bản cùng nói về một chuyện" mà không bắt nhầm "hai văn bản có chung vài
 * đoạn bảng biểu giống hệt nhau", vì một vài đoạn giống không kéo nổi tâm của cả tài liệu.
 *
 * <p>Ngưỡng ở cấu hình vì nó phải hiệu chỉnh trên kho thật: đặt thấp thì mỗi lần nạp sinh hàng
 * chục cảnh báo giả và quản trị sẽ bỏ qua cả những cảnh báo đúng.
 */
@Component
public class OverlapDetector {

    private static final Logger log = LoggerFactory.getLogger(OverlapDetector.class);
    private static final int MAX_CANDIDATES = 20;

    private final JdbcTemplate jdbc;
    private final DocumentRepository documents;
    private final TracuuProperties props;

    public OverlapDetector(JdbcTemplate jdbc, DocumentRepository documents, TracuuProperties props) {
        this.jdbc = jdbc;
        this.documents = documents;
        this.props = props;
    }

    /** Một tài liệu khác kèm mức độ giống nhau với tài liệu vừa nạp. */
    private record Neighbour(long documentId, double similarity, Long clusterId) {}

    @Transactional
    public void detectFor(Document doc, long chunkSetId) {
        if (!props.ingestion().overlap().enabled()) return;

        List<Neighbour> neighbours;
        try {
            neighbours = nearestDocuments(doc.getId(), chunkSetId);
        } catch (RuntimeException e) {
            // Không được để việc dò chồng lấn làm hỏng việc nạp: tài liệu đã vào chỉ mục rồi.
            log.warn("Bỏ qua bước dò chồng lấn cho tài liệu {}: {}", doc.getId(), e.getMessage());
            return;
        }
        if (neighbours.isEmpty()) {
            assignNewCluster(doc);
            return;
        }

        assignCluster(doc, neighbours);

        double threshold = props.ingestion().overlap().similarityThreshold();
        int recorded = 0;
        for (Neighbour n : neighbours) {
            if (n.similarity() < threshold) break;   // danh sách đã xếp giảm dần
            recordPair(doc.getId(), n.documentId(), (float) n.similarity());
            recorded++;
        }
        if (recorded > 0) {
            log.info("Tài liệu {} có {} cặp nghi chồng lấn chờ quản trị xử lý.", doc.getId(), recorded);
        }
    }

    /**
     * Các tài liệu gần nhất theo tâm vector.
     *
     * <p>{@code avg(embedding)} gộp các đoạn thành một vector đại diện cho cả tài liệu, còn
     * toán tử khoảng cách cosin cho giá trị trong khoảng 0–2 nên lấy {@code 1 - khoảng cách}
     * để ra mức độ giống nhau quen thuộc.
     */
    private List<Neighbour> nearestDocuments(long documentId, long chunkSetId) {
        return jdbc.query("""
                WITH me AS (
                    SELECT avg(embedding)::vector AS v
                    FROM chunks WHERE document_id = ? AND chunk_set_id = ? AND embedding IS NOT NULL
                ),
                others AS (
                    SELECT c.document_id, avg(c.embedding)::vector AS v
                    FROM chunks c
                    JOIN documents d ON d.id = c.document_id
                    WHERE c.chunk_set_id = ? AND c.document_id <> ?
                      AND c.embedding IS NOT NULL
                      AND d.status = 'DA_LAP_CHI_MUC'
                    GROUP BY c.document_id
                )
                SELECT o.document_id, 1 - (o.v <=> me.v) AS similarity, d.topic_cluster_id
                FROM others o
                CROSS JOIN me
                JOIN documents d ON d.id = o.document_id
                WHERE me.v IS NOT NULL
                ORDER BY similarity DESC
                LIMIT ?
                """,
                (rs, i) -> new Neighbour(rs.getLong(1), rs.getDouble(2), (Long) rs.getObject(3)),
                documentId, chunkSetId, chunkSetId, documentId, MAX_CANDIDATES);
    }

    /**
     * Gán cụm chủ đề: theo tài liệu gần nhất nếu đủ giống, ngược lại mở cụm mới.
     *
     * <p>Đây là cách gom cụm đơn giản nhất chạy được theo thời gian thực lúc nạp. Nó không cho
     * cụm tối ưu toàn cục, và không cần — vai trò duy nhất của cụm là <em>thu hẹp</em> số cặp
     * phải so, còn quyết định chồng lấn thì đã nằm ở bảng cặp.
     */
    private void assignCluster(Document doc, List<Neighbour> neighbours) {
        Neighbour best = neighbours.get(0);
        if (best.similarity() >= props.ingestion().cluster().similarityThreshold()
                && best.clusterId() != null) {
            doc.setTopicClusterId(best.clusterId());
        } else {
            assignNewCluster(doc);
        }
        documents.save(doc);
    }

    private void assignNewCluster(Document doc) {
        Long next = jdbc.queryForObject(
                "SELECT coalesce(max(topic_cluster_id), 0) + 1 FROM documents", Long.class);
        doc.setTopicClusterId(next == null ? 1L : next);
        documents.save(doc);
    }

    /**
     * Ghi một cặp, theo quy ước <strong>id nhỏ hơn đứng trước</strong>.
     *
     * <p>Không ghi đè quyết định đã có: quản trị đã đánh dấu "báo động giả" cho một cặp thì lần
     * nạp lại không được làm nó hiện ra lần nữa — đó chính là {@code AC-005} của US-014, và là
     * khác biệt giữa một danh sách dùng được với một danh sách bị bỏ qua.
     */
    private void recordPair(long one, long other, float similarity) {
        long a = Math.min(one, other);
        long b = Math.max(one, other);
        jdbc.update("""
                INSERT INTO document_overlap_pairs (doc_a_id, doc_b_id, similarity, decision)
                VALUES (?, ?, ?, 'CHO')
                ON CONFLICT (doc_a_id, doc_b_id) DO UPDATE
                SET similarity = excluded.similarity
                WHERE document_overlap_pairs.decision = 'CHO'
                """, a, b, similarity);
    }
}
