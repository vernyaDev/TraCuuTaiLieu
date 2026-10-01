package vn.utc.tracuu.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.platform.config.TracuuProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cửa DUY NHẤT quyết định ai đọc được gì — luật R1 của kiến trúc.
 *
 * <p>Luật đọc, phát biểu một lần duy nhất ở US-008 và hiện thực một lần duy nhất ở đây:
 * <blockquote>
 * Người dùng đọc được một tài liệu khi và chỉ khi
 * {@code (được cấp quyền qua phòng ban HOẶC được cấp quyền trực tiếp)}
 * <strong>và</strong> {@code nhãn mật ≤ mức mật của người dùng}
 * <strong>và</strong> {@code không có mục từ chối nào áp lên người đó hoặc phòng ban của người đó}.
 * </blockquote>
 *
 * <p>Hệ quả quan trọng và dễ quên: <strong>từ chối mặc định</strong>. Tài liệu không có mục cấp
 * quyền nào thì không ai đọc được, kể cả khi nhãn mật thấp — {@code AC-004} của US-008 nói thẳng
 * điều đó cho tài khoản mới tạo.
 *
 * <p>Có hai chỗ tính quyền thì sớm muộn hai chỗ lệch nhau, và chỗ lệch chính là chỗ rò rỉ.
 */
@Component
public class PermissionResolver {

    private static final Logger log = LoggerFactory.getLogger(PermissionResolver.class);

    /** Đệm quyền sống lâu; thứ làm nó hết hạn là SỐ HIỆU PHIÊN BẢN, không phải đồng hồ. */
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final PermissionAuditWriter auditWriter;
    private final TracuuProperties props;

    public PermissionResolver(JdbcTemplate jdbc,
                              StringRedisTemplate redis,
                              PermissionAuditWriter auditWriter,
                              TracuuProperties props) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.auditWriter = auditWriter;
        this.props = props;
    }

    // ── Số hiệu phiên bản quyền ────────────────────────────────────────────────────────────

    /**
     * Số hiệu phiên bản quyền hiện tại (mô hình dữ liệu Mục 8).
     *
     * <p>Đệm quyền khoá theo {@code userId} kèm số hiệu này. Đọc thấy số hiệu khác thì tính lại —
     * nhờ đó {@code AC-005} của US-008 (quyền đổi có hiệu lực ngay ở câu hỏi kế tiếp, không cần
     * đăng nhập lại) đạt được mà không phải tính lại mỗi lượt.
     */
    public long currentVersion() {
        Long v = jdbc.queryForObject("SELECT version FROM permission_version WHERE id = 1", Long.class);
        return v == null ? 1L : v;
    }

    /**
     * Tăng số hiệu phiên bản quyền. <strong>Bắt buộc gọi ở MỌI đường ghi quyền</strong>: mục kiểm
     * soát truy cập, thành viên phòng ban, nhãn mật tài liệu, mức mật người dùng, trạng thái tài liệu.
     *
     * <p>Quên một đường là tạo ra một lỗ rò rỉ chỉ xuất hiện <em>sau khi</em> cấu hình thay đổi —
     * tức là loại lỗi khó tái hiện nhất, và loại lỗi mà kiểm thử chức năng thông thường không thấy.
     */
    @Transactional
    public long bumpVersion() {
        Long v = jdbc.queryForObject(
                "UPDATE permission_version SET version = version + 1 WHERE id = 1 RETURNING version",
                Long.class);
        long next = v == null ? 1L : v;
        log.debug("Số hiệu phiên bản quyền tăng lên {}", next);
        return next;
    }

    // ── Giải quyết phạm vi ─────────────────────────────────────────────────────────────────

    /**
     * Tập tài liệu đọc được ứng với một phạm vi quyền.
     *
     * <p>Đây là hàm mà mọi truy vấn chạm bảng đoạn phải đi qua. Không có quá tải nào nhận
     * {@code long userId} trần — buộc phải dựng một {@link PermissionScope}, vì chính việc phải
     * gọi tên phạm vi là thứ làm đường đọc-không-lọc trở nên đếm được thay vì lặng lẽ.
     */
    public ReadableDocuments resolve(PermissionScope scope) {
        if (scope instanceof PermissionScope.Unfiltered u) {
            recordUnfilteredRead(u);
            return new ReadableDocuments.All();
        }

        Long userId = scope.effectiveUserId();
        if (userId == null) {
            throw new IllegalStateException("Phạm vi quyền không có danh tính: " + scope.describe());
        }

        long version = currentVersion();
        String key = "quyen:" + userId + ":v" + version;

        String cached = safeGet(key);
        if (cached != null) {
            return decode(cached, userId);
        }

        ReadableDocuments resolved = computeFromDatabase(userId);
        safeSet(key, encode(resolved));
        return resolved;
    }

    private ReadableDocuments computeFromDatabase(long userId) {
        Integer clearance = jdbc.query(
                "SELECT clearance_level FROM users WHERE id = ? AND is_active",
                rs -> rs.next() ? rs.getInt(1) : null, userId);

        if (clearance == null) {
            // Tài khoản không tồn tại hoặc đã bị vô hiệu hoá: không đọc được gì.
            return new ReadableDocuments.None();
        }

        // Luật đọc của US-008, viết đúng một lần, trong đúng một câu truy vấn.
        List<Long> allowed = jdbc.queryForList("""
                WITH my_depts AS (
                    SELECT department_id FROM user_departments WHERE user_id = ?
                ),
                denied AS (
                    SELECT document_id FROM document_acl
                    WHERE effect = 'TU_CHOI'
                      AND ( (subject_type = 'NGUOI_DUNG' AND subject_id = ?)
                         OR (subject_type = 'PHONG_BAN'  AND subject_id IN (SELECT department_id FROM my_depts)) )
                ),
                granted AS (
                    SELECT DISTINCT document_id FROM document_acl
                    WHERE effect = 'CHO_PHEP'
                      AND ( (subject_type = 'NGUOI_DUNG' AND subject_id = ?)
                         OR (subject_type = 'PHONG_BAN'  AND subject_id IN (SELECT department_id FROM my_depts)) )
                )
                SELECT d.id
                FROM documents d
                JOIN granted g ON g.document_id = d.id
                WHERE d.sensitivity <= ?
                  AND d.id NOT IN (SELECT document_id FROM denied)
                """, Long.class, userId, userId, userId, clearance);

        if (allowed.isEmpty()) {
            return new ReadableDocuments.None();
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM documents", Long.class);
        int corpusSize = total == null ? allowed.size() : total.intValue();

        // Đổi chiều lọc theo độ chọn lọc (mô hình dữ liệu Mục 7 bước 2).
        double ratio = corpusSize == 0 ? 1.0 : (double) allowed.size() / corpusSize;
        if (ratio > props.retrieval().allowListRatioThreshold()) {
            Set<Long> allowedSet = new LinkedHashSet<>(allowed);
            Set<Long> blocked = jdbc.queryForList("SELECT id FROM documents", Long.class).stream()
                    .filter(id -> !allowedSet.contains(id))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // Mốc kho tại thời điểm chụp danh sách chặn. Tài liệu có id lớn hơn mốc là tài liệu
            // ra đời SAU ảnh chụp, nên chưa ai nói gì về nó — và chưa nói gì nghĩa là không cho.
            Long mocCao = jdbc.queryForObject("SELECT coalesce(max(id), 0) FROM documents", Long.class);
            return new ReadableDocuments.DenyList(blocked, corpusSize, mocCao == null ? 0L : mocCao);
        }
        return new ReadableDocuments.AllowList(new LinkedHashSet<>(allowed));
    }

    // ── Giải thích cho người quản trị ──────────────────────────────────────────────────────

    /**
     * Trả lời "người này đọc được tài liệu này không, và VÌ SAO".
     *
     * <p>Hiện thực của {@code AC-005} trong US-006. Đường này không thêm năng lực nào cho hệ thống
     * nhưng là thứ duy nhất giúp người quản trị gỡ rối cấu hình quyền mà không phải đoán.
     */
    public AccessExplanation explain(long userId, long documentId) {
        try {
            Integer clearance = jdbc.queryForObject(
                    "SELECT clearance_level FROM users WHERE id = ?", Integer.class, userId);
            Integer sensitivity = jdbc.queryForObject(
                    "SELECT sensitivity FROM documents WHERE id = ?", Integer.class, documentId);
            if (clearance == null || sensitivity == null) {
                return AccessExplanation.denied("Không tìm thấy người dùng hoặc tài liệu.");
            }

            List<String> denials = jdbc.queryForList("""
                    SELECT CASE a.subject_type
                             WHEN 'NGUOI_DUNG' THEN 'mục từ chối đặt trực tiếp lên người dùng'
                             ELSE 'mục từ chối đặt lên phòng ban ' || coalesce(dep.name, '?')
                           END
                    FROM document_acl a
                    LEFT JOIN departments dep ON dep.id = a.subject_id AND a.subject_type = 'PHONG_BAN'
                    WHERE a.document_id = ? AND a.effect = 'TU_CHOI'
                      AND ( (a.subject_type = 'NGUOI_DUNG' AND a.subject_id = ?)
                         OR (a.subject_type = 'PHONG_BAN'  AND a.subject_id IN
                             (SELECT department_id FROM user_departments WHERE user_id = ?)) )
                    """, String.class, documentId, userId, userId);

            if (!denials.isEmpty()) {
                return AccessExplanation.denied("Bị chặn bởi " + String.join("; ", denials)
                        + ". Mục từ chối thắng mọi mục cho phép.");
            }

            if (sensitivity > clearance) {
                return AccessExplanation.denied(
                        "Nhãn mật của tài liệu là " + props.security().labelOf(sensitivity)
                        + ", cao hơn mức mật " + props.security().labelOf(clearance) + " của người dùng.");
            }

            List<String> grants = jdbc.queryForList("""
                    SELECT CASE a.subject_type
                             WHEN 'NGUOI_DUNG' THEN 'quyền cấp trực tiếp cho người dùng'
                             ELSE 'quyền đến từ phòng ban ' || coalesce(dep.name, '?')
                           END
                    FROM document_acl a
                    LEFT JOIN departments dep ON dep.id = a.subject_id AND a.subject_type = 'PHONG_BAN'
                    WHERE a.document_id = ? AND a.effect = 'CHO_PHEP'
                      AND ( (a.subject_type = 'NGUOI_DUNG' AND a.subject_id = ?)
                         OR (a.subject_type = 'PHONG_BAN'  AND a.subject_id IN
                             (SELECT department_id FROM user_departments WHERE user_id = ?)) )
                    """, String.class, documentId, userId, userId);

            if (grants.isEmpty()) {
                return AccessExplanation.denied(
                        "Chưa có mục cấp quyền nào cho người dùng này trên tài liệu, dù trực tiếp "
                        + "hay qua phòng ban. Hệ thống từ chối mặc định.");
            }
            return AccessExplanation.allowed("Được phép qua " + String.join("; ", grants) + ".");

        } catch (EmptyResultDataAccessException e) {
            return AccessExplanation.denied("Không tìm thấy người dùng hoặc tài liệu.");
        }
    }

    // ── Nhật ký kiểm toán cho đường đọc không lọc ──────────────────────────────────────────

    /**
     * Ghi nhật ký cho một lần đọc không lọc quyền.
     *
     * <p>Uỷ cho {@link PermissionAuditWriter} vì nó mở GIAO DỊCH RIÊNG. Truy hồi chạy trong
     * giao dịch chỉ-đọc, và ghi vào giao dịch chỉ-đọc làm hỏng cả giao dịch — xem ghi chú ở
     * lớp đó để biết lỗi ấy đã che mất những gì.
     */
    private void recordUnfilteredRead(PermissionScope.Unfiltered u) {
        auditWriter.recordUnfilteredRead(u.reason());
    }

    // ── Đệm ────────────────────────────────────────────────────────────────────────────────
    // Redis hỏng thì hệ thống chậm đi, KHÔNG được sai quyền. Nên mọi lỗi đệm đều rơi về tính lại.

    private String safeGet(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("Không đọc được đệm quyền, tính lại từ cơ sở dữ liệu: {}", e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, String value) {
        try {
            redis.opsForValue().set(key, value, CACHE_TTL);
        } catch (RuntimeException e) {
            log.warn("Không ghi được đệm quyền: {}", e.getMessage());
        }
    }

    private String encode(ReadableDocuments r) {
        return switch (r) {
            case ReadableDocuments.None ignored -> "N";
            case ReadableDocuments.All ignored -> "A";
            case ReadableDocuments.AllowList a -> "L:" + join(a.ids());
            case ReadableDocuments.DenyList d ->
                    "D:" + d.corpusSize() + ":" + d.maxDocumentId() + ":" + join(d.blockedIds());
        };
    }

    private ReadableDocuments decode(String s, long userId) {
        try {
            if ("N".equals(s)) return new ReadableDocuments.None();
            if ("A".equals(s)) return new ReadableDocuments.All();
            if (s.startsWith("L:")) return new ReadableDocuments.AllowList(parse(s.substring(2)));
            if (s.startsWith("D:")) {
                int sep = s.indexOf(':', 2);
                int sep2 = s.indexOf(':', sep + 1);
                // Dạng cũ "D:{corpus}:{ids}" không có mốc kho. Không đoán một mốc cho nó: đệm
                // theo dạng cũ chính là đệm đã sinh ra lỗ hổng, nên tính lại từ đầu.
                if (sep2 < 0) throw new IllegalStateException("đệm quyền theo dạng cũ, chưa có mốc kho");
                int corpus = Integer.parseInt(s.substring(2, sep));
                long moc = Long.parseLong(s.substring(sep + 1, sep2));
                return new ReadableDocuments.DenyList(parse(s.substring(sep2 + 1)), corpus, moc);
            }
        } catch (RuntimeException e) {
            log.warn("Đệm quyền hỏng dạng, tính lại: {}", e.getMessage());
        }
        return computeFromDatabase(userId);
    }

    private static String join(Set<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static Set<Long> parse(String csv) {
        if (csv.isEmpty()) return Set.of();
        Set<Long> out = new LinkedHashSet<>();
        for (String part : csv.split(",")) out.add(Long.valueOf(part));
        return out;
    }

    /** Kết quả tra cứu "đọc được không, vì sao". */
    public record AccessExplanation(boolean allowed, String reason) {
        static AccessExplanation allowed(String r) { return new AccessExplanation(true, r); }
        static AccessExplanation denied(String r) { return new AccessExplanation(false, r); }
    }
}
