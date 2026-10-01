package vn.utc.tracuu.gap;

import vn.utc.tracuu.platform.web.Trang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.ingestion.EmbeddingService;
import vn.utc.tracuu.ingestion.ChunkWriter;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.web.BusinessException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Nhật ký khoảng trống và báo cáo gom nhóm.
 *
 * <p><strong>Ràng buộc quan trọng nhất của gói này là một điều nó KHÔNG có: không đường nào lọc
 * theo người dùng.</strong> Ràng buộc ấy thuộc về hợp đồng chứ không chỉ thuộc về giao diện —
 * thiếu nó ở tầng máy chủ thì bất kỳ ai sửa giao diện sau này cũng dựng được một công cụ giám
 * sát nhân viên trên nền hệ thống này. Báo cáo trả lời câu <em>"kho thiếu gì"</em>, không trả
 * lời câu <em>"ai hỏi gì"</em>.
 *
 * <p>Vì vậy {@code user_id} vẫn được lưu — cần để chụp phòng ban lúc hỏi — nhưng không có truy
 * vấn nào nhận nó làm điều kiện lọc, và không có đường mạng nào trả nó ra.
 */
@Service
public class GapService {

    private static final Logger log = LoggerFactory.getLogger(GapService.class);

    private final RefusalLogRepository refusals;
    private final GapClusterRepository clusters;
    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddings;
    private final TracuuProperties props;

    public GapService(RefusalLogRepository refusals, GapClusterRepository clusters,
                      JdbcTemplate jdbc, EmbeddingService embeddings, TracuuProperties props) {
        this.refusals = refusals;
        this.clusters = clusters;
        this.jdbc = jdbc;
        this.embeddings = embeddings;
        this.props = props;
    }

    /**
     * Ghi một lần từ chối và gom nó vào nhóm.
     *
     * <p>Chạy ngoài luồng trả lời để không làm chậm người dùng — họ đã nhận thông báo từ chối
     * rồi, việc ghi nhật ký không được giữ kết nối của họ thêm giây nào.
     */
    @Transactional
    public void record(Long turnId, Long userId, String question, String rewritten,
                       String reason, String departmentSnapshot, Integer clearanceSnapshot) {
        RefusalLog entry = new RefusalLog();
        entry.setTurnId(turnId);
        entry.setUserId(userId);
        entry.setQuestion(question);
        entry.setRewritten(rewritten);
        entry.setReason(reason);
        entry.setDepartmentSnapshot(departmentSnapshot);
        entry.setClearanceSnapshot(clearanceSnapshot);
        refusals.save(entry);

        try {
            attachToCluster(entry);
        } catch (RuntimeException e) {
            // Gom nhóm hỏng thì bản ghi vẫn còn — mất nhóm còn hơn mất cả bản ghi.
            log.warn("Không gom được nhóm khoảng trống: {}", e.getMessage());
        }
    }

    /**
     * Gắn một lần từ chối vào nhóm gần nhất, hoặc mở nhóm mới.
     *
     * <p>Gom theo vector câu hỏi: hai cách hỏi khác nhau về cùng một chuyện phải rơi vào cùng
     * một nhóm, mà so chuỗi thì không làm được điều đó.
     *
     * <p>Ngưỡng gom chưa hiệu chỉnh trên dữ liệu thật, nên nó nằm ở cấu hình. Báo cáo hiện tại
     * trả lời được <em>"có gom hay không"</em>, chưa trả lời được <em>"gom đúng mức nào"</em> —
     * nói rõ ra thay vì để người đọc tưởng con số đã được kiểm.
     */
    private void attachToCluster(RefusalLog entry) {
        String text = entry.getRewritten() != null && !entry.getRewritten().isBlank()
                ? entry.getRewritten() : entry.getQuestion();

        float[] vector = embeddings.embed(text);
        String literal = ChunkWriter.toVectorLiteral(vector);

        jdbc.update("UPDATE refusal_log SET embedding = ?::vector WHERE id = ?",
                literal, entry.getId());

        Long clusterId = jdbc.query("""
                SELECT id FROM gap_clusters
                WHERE reason = ? AND status = 'CHO' AND centroid IS NOT NULL
                  AND 1 - (centroid <=> ?::vector) >= ?
                ORDER BY centroid <=> ?::vector
                LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null,
                entry.getReason(), literal,
                props.gap().clusterSimilarityThreshold(), literal);

        if (clusterId == null) {
            GapCluster cluster = new GapCluster();
            cluster.setLabel(shorten(text));
            cluster.setReason(entry.getReason());
            cluster.setMemberCount(0);
            clusters.save(cluster);
            clusterId = cluster.getId();
            jdbc.update("UPDATE gap_clusters SET centroid = ?::vector WHERE id = ?",
                    literal, clusterId);
        }

        jdbc.update("""
                INSERT INTO gap_members (cluster_id, refusal_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, clusterId, entry.getId());

        // Tâm nhóm tính lại từ toàn bộ thành viên, không cộng dồn — cộng dồn sai một lần thì
        // sai vĩnh viễn và nhóm trôi dần khỏi chủ đề gốc mà không ai thấy.
        jdbc.update("""
                UPDATE gap_clusters SET
                    centroid = (SELECT avg(r.embedding)::vector FROM refusal_log r
                                JOIN gap_members m ON m.refusal_id = r.id
                                WHERE m.cluster_id = ? AND r.embedding IS NOT NULL),
                    member_count = (SELECT count(*) FROM gap_members WHERE cluster_id = ?),
                    last_seen = now()
                WHERE id = ?
                """, clusterId, clusterId, clusterId);
    }

    /**
     * Danh sách nhóm khoảng trống.
     *
     * <p>Không có tham số người dùng. Cố ý, và đây là chỗ dễ bị "thêm cho tiện" nhất.
     */
    @Transactional(readOnly = true)
    public Page<GapCluster> list(String status, String reason, int page, int size) {
        return clusters.search(status, reason, Trang.cua(page, size));
    }

    /** Các câu hỏi cụ thể trong một nhóm — hiện câu hỏi, KHÔNG hiện ai hỏi. */
    @Transactional(readOnly = true)
    public List<RefusalLog> questionsIn(long clusterId) {
        return refusals.findByCluster(clusterId);
    }

    @Transactional
    public void resolve(long clusterId, long actorId, String note, boolean ignore) {
        GapCluster cluster = clusters.findById(clusterId)
                .orElseThrow(BusinessException::khongTimThay);
        cluster.setStatus(ignore ? "BO_QUA" : "DA_XU_LY");
        cluster.setResolvedBy(actorId);
        cluster.setResolvedAt(Instant.now());
        cluster.setResolveNote(note);
        clusters.save(cluster);
        log.info("Nhóm khoảng trống {} được đánh dấu {}", clusterId, cluster.getStatus());
    }

    /**
     * Dọn nhật ký quá hạn giữ.
     *
     * <p>Câu hỏi của người dùng là dữ liệu nhạy cảm; giữ vô thời hạn không có lý do chính đáng.
     * Thời hạn ở cấu hình vì mỗi tổ chức có chính sách riêng.
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(props.gap().retentionMonths() * 30L, ChronoUnit.DAYS);
        int removed = refusals.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("Dọn {} bản ghi khoảng trống quá {} tháng", removed, props.gap().retentionMonths());
        }
    }

    private String shorten(String text) {
        String t = text.replaceAll("\\s+", " ").strip();
        return t.length() <= 200 ? t : t.substring(0, 197) + "…";
    }
}
