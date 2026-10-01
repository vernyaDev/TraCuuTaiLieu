package vn.utc.tracuu.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mọi đường GHI quyền đi qua đây.
 *
 * <p>Lớp này tồn tại vì một bất biến: <strong>mọi thay đổi quyền phải tăng số hiệu phiên bản
 * quyền</strong>. Quên một đường là tạo ra một lỗ rò rỉ chỉ xuất hiện <em>sau khi</em> cấu hình
 * thay đổi — người bị thu hồi quyền vẫn đọc được tới khi đệm hết hạn, và lỗi ấy không tái hiện
 * được trong kiểm thử chức năng thông thường vì nó cần đúng một chuỗi thao tác theo thời gian.
 *
 * <p>Gom mọi đường ghi vào một lớp là cách duy nhất biến bất biến ấy thành thứ kiểm được bằng
 * mắt: chỉ cần đọc lớp này và xác nhận mỗi phương thức công khai đều kết thúc bằng một lời gọi
 * {@code bumpAndAudit}.
 */
@Service
public class PermissionAdminService {

    private static final Logger log = LoggerFactory.getLogger(PermissionAdminService.class);

    private final DocumentAclRepository acl;
    private final PermissionAuditRepository audits;
    private final PermissionResolver permissions;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public PermissionAdminService(DocumentAclRepository acl, PermissionAuditRepository audits,
                                  PermissionResolver permissions, UserRepository users,
                                  DepartmentRepository departments, JdbcTemplate jdbc,
                                  ObjectMapper json) {
        this.acl = acl;
        this.audits = audits;
        this.permissions = permissions;
        this.users = users;
        this.departments = departments;
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * Quyền trên một tài liệu, <strong>tách bạch quyền trực tiếp với quyền đến từ phòng ban</strong>.
     *
     * <p>Tách bạch là yêu cầu chứ không phải tiện ích hiển thị: người quản trị gỡ một quyền mà
     * không biết nó đến từ đâu sẽ gỡ nhầm chỗ, rồi tưởng hệ thống hỏng khi người dùng vẫn đọc
     * được.
     */
    @Transactional(readOnly = true)
    public DocumentPermissions ofDocument(long documentId) {
        List<DocumentAcl> entries = acl.findByDocumentId(documentId);
        List<Entry> direct = new ArrayList<>();
        List<Entry> viaDepartment = new ArrayList<>();

        for (DocumentAcl e : entries) {
            // Tên hiển thị của người dùng phải kèm TÊN ĐĂNG NHẬP.
            //
            // Họ tên không phải định danh: hai người trùng tên là chuyện thường trong một công
            // ty, và khi đó bảng quyền hiện hai dòng y hệt nhau — người quản trị không biết
            // dòng nào là ai, nên không dám gỡ dòng nào. Thấy đúng cảnh đó trên màn hình
            // ngày 30/09 với hai tài khoản khác nhau cùng họ tên.
            String name = e.getSubjectType() == SubjectType.NGUOI_DUNG
                    ? users.findById(e.getSubjectId())
                            .map(u -> u.getFullName() + " (" + u.getUsername() + ")")
                            .orElse("(tài khoản đã xoá)")
                    : departments.findById(e.getSubjectId()).map(Department::getName)
                            .orElse("(phòng ban đã xoá)");
            Entry row = new Entry(e.getId(), e.getSubjectType().name(), e.getSubjectId(),
                    name, e.getEffect().name(), e.getGrantedAt());
            if (e.getSubjectType() == SubjectType.NGUOI_DUNG) direct.add(row); else viaDepartment.add(row);
        }
        return new DocumentPermissions(documentId, direct, viaDepartment);
    }

    public record Entry(Long id, String subjectType, Long subjectId, String subjectName,
                        String effect, java.time.Instant grantedAt) {}

    public record DocumentPermissions(Long documentId, List<Entry> direct, List<Entry> viaDepartment) {}

    /** Thêm một mục cho phép hoặc từ chối. */
    @Transactional
    public void grant(long documentId, SubjectType subjectType, long subjectId,
                      AclEffect effect, long actorId) {
        validateSubject(subjectType, subjectId);

        boolean exists = acl.findByDocumentId(documentId).stream()
                .anyMatch(e -> e.getSubjectType() == subjectType
                            && e.getSubjectId().equals(subjectId)
                            && e.getEffect() == effect);
        if (exists) return;

        acl.save(new DocumentAcl(documentId, subjectType, subjectId, effect, actorId));
        bumpAndAudit(actorId, "CAP_QUYEN", "TAI_LIEU", documentId, null,
                Map.of("chuThe", subjectType.name(), "maChuThe", subjectId, "hieuLuc", effect.name()));
    }

    /**
     * Gán quyền cho <strong>nhiều tài liệu trong một thao tác</strong>.
     *
     * <p>Cấp quyền cho một phòng ban trên hai mươi tài liệu bằng hai mươi thao tác riêng là chỗ
     * người quản trị bỏ sót và làm sai nhiều nhất. Trả về số tài liệu đã áp dụng để họ đối chiếu
     * được với số mình đã chọn.
     *
     * <p>Lưu ý: đây là thao tác <em>ghi</em> hàng loạt, khác hẳn <em>xoá</em> hàng loạt — hệ
     * thống cố ý không có đường xoá hàng loạt tài liệu, vì xoá chỉ cần sai một lần là mất cả kho.
     */
    @Transactional
    public int grantBulk(List<Long> documentIds, SubjectType subjectType, long subjectId,
                         AclEffect effect, long actorId) {
        validateSubject(subjectType, subjectId);
        if (documentIds == null || documentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "documentIds",
                    "Chưa chọn tài liệu nào");
        }

        int applied = 0;
        for (Long docId : documentIds) {
            boolean exists = acl.findByDocumentId(docId).stream()
                    .anyMatch(e -> e.getSubjectType() == subjectType
                                && e.getSubjectId().equals(subjectId)
                                && e.getEffect() == effect);
            if (exists) continue;
            acl.save(new DocumentAcl(docId, subjectType, subjectId, effect, actorId));
            applied++;
        }

        bumpAndAudit(actorId, "CAP_QUYEN_HANG_LOAT", "TAI_LIEU", null, null,
                Map.of("soTaiLieuChon", documentIds.size(), "soTaiLieuApDung", applied,
                       "chuThe", subjectType.name(), "maChuThe", subjectId, "hieuLuc", effect.name()));
        log.info("Gán quyền hàng loạt: {} trên {} tài liệu, áp dụng {} tài liệu",
                subjectType, documentIds.size(), applied);
        return applied;
    }

    @Transactional
    public void revoke(long documentId, SubjectType subjectType, long subjectId,
                       AclEffect effect, long actorId) {
        int removed = acl.deleteEntry(documentId, subjectType, subjectId, effect);
        if (removed == 0) return;
        bumpAndAudit(actorId, "GO_QUYEN", "TAI_LIEU", documentId,
                Map.of("chuThe", subjectType.name(), "maChuThe", subjectId, "hieuLuc", effect.name()),
                null);
    }

    /** Thành viên phòng ban đổi cũng là thay đổi quyền — đây là đường hay bị quên nhất. */
    @Transactional
    public void setDepartments(long userId, List<Long> departmentIds, long actorId) {
        User user = users.findByIdWithDepartments(userId)
                .orElseThrow(BusinessException::khongTimThay);

        List<String> before = user.getDepartments().stream().map(Department::getName).toList();
        user.getDepartments().clear();
        for (Long id : departmentIds) {
            user.getDepartments().add(departments.findById(id)
                    .orElseThrow(BusinessException::khongTimThay));
        }
        users.save(user);

        bumpAndAudit(actorId, "DOI_PHONG_BAN", "NGUOI_DUNG", userId,
                Map.of("phongBan", before),
                Map.of("phongBan", user.getDepartments().stream().map(Department::getName).toList()));
    }

    /** Mức mật của người dùng đổi cũng là thay đổi quyền. */
    @Transactional
    public void setClearance(long userId, int clearanceLevel, long actorId) {
        User user = users.findById(userId).orElseThrow(BusinessException::khongTimThay);
        int before = user.getClearanceLevel();
        if (before == clearanceLevel) return;

        user.setClearanceLevel(clearanceLevel);
        users.save(user);
        bumpAndAudit(actorId, "DOI_MUC_MAT", "NGUOI_DUNG", userId,
                Map.of("mucMat", before), Map.of("mucMat", clearanceLevel));
    }

    /**
     * Nhãn mật của tài liệu đổi cũng là thay đổi quyền.
     *
     * <p>Nâng nhãn mật lấy quyền đọc của một số người, nên nó trả về <strong>con số hậu quả</strong>
     * trước khi thực hiện — hộp thoại xác nhận cần nội dung thật, không phải một câu hỏi chung chung.
     */
    @Transactional
    public void setSensitivity(long documentId, int sensitivity, long actorId) {
        Integer before = jdbc.queryForObject(
                "SELECT sensitivity FROM documents WHERE id = ?", Integer.class, documentId);
        if (before == null) throw BusinessException.khongTimThay();
        if (before == sensitivity) return;

        jdbc.update("UPDATE documents SET sensitivity = ? WHERE id = ?", sensitivity, documentId);
        bumpAndAudit(actorId, "DOI_NHAN_MAT", "TAI_LIEU", documentId,
                Map.of("nhanMat", before), Map.of("nhanMat", sensitivity));
    }

    /**
     * Xoá một phòng ban.
     *
     * <p>Thuộc lớp này chứ không thuộc lớp quản trị chung, vì xoá phòng ban <strong>gỡ luôn mọi
     * quyền cấp qua nó</strong> — tức là một thay đổi quyền diện rộng, và nó phải tăng số hiệu
     * phiên bản quyền như mọi thay đổi quyền khác. Để việc này ở nơi khác là đúng kiểu bỏ sót
     * sinh ra lỗ rò rỉ chỉ hiện sau khi cấu hình đổi.
     *
     * @return số mục kiểm soát truy cập đã bị gỡ theo
     */
    @Transactional
    public int deleteDepartment(long departmentId, long actorId) {
        Department department = departments.findById(departmentId)
                .orElseThrow(BusinessException::khongTimThay);
        String name = department.getName();

        int removedAcl = acl.deleteByDepartment(departmentId);
        int members = (int) departments.countMembers(departmentId);
        departments.delete(department);

        bumpAndAudit(actorId, "XOA_PHONG_BAN", "PHONG_BAN", departmentId,
                Map.of("ten", name, "soThanhVien", members, "soMucQuyen", removedAcl), null);
        log.info("Xoá phòng ban {} — gỡ theo {} mục quyền, {} thành viên mất liên kết",
                name, removedAcl, members);
        return removedAcl;
    }

    /** Ai đọc được tài liệu này không, và VÌ SAO. */
    @Transactional(readOnly = true)
    public PermissionResolver.AccessExplanation explain(long userId, long documentId) {
        return permissions.explain(userId, documentId);
    }

    @Transactional(readOnly = true)
    public long countUsersLosingAccess(long documentId, int newSensitivity) {
        Long count = jdbc.queryForObject("""
                SELECT count(DISTINCT u.id) FROM users u
                WHERE u.clearance_level < ?
                  AND EXISTS (
                    SELECT 1 FROM document_acl a
                    WHERE a.document_id = ? AND a.effect = 'CHO_PHEP'
                      AND ( (a.subject_type = 'NGUOI_DUNG' AND a.subject_id = u.id)
                         OR (a.subject_type = 'PHONG_BAN' AND a.subject_id IN
                             (SELECT department_id FROM user_departments WHERE user_id = u.id)) ))
                """, Long.class, newSensitivity, documentId);
        return count == null ? 0 : count;
    }

    // ── Bất biến ──────────────────────────────────────────────────────────────────────────

    /**
     * Tăng số hiệu phiên bản quyền và ghi nhật ký kiểm toán.
     *
     * <p><strong>Mọi phương thức ghi ở lớp này đều kết thúc bằng một lời gọi tới đây.</strong>
     * Thêm một đường ghi quyền mới mà quên gọi là tạo lỗ rò rỉ — đó là lý do phương thức này để
     * riêng và mang tên nói rõ nó làm hai việc.
     */
    private void bumpAndAudit(long actorId, String action, String targetType, Long targetId,
                              Object before, Object after) {
        permissions.bumpVersion();
        try {
            audits.save(PermissionAudit.of(actorId, action, targetType, targetId,
                    before == null ? null : json.writeValueAsString(before),
                    after == null ? null : json.writeValueAsString(after)));
        } catch (Exception e) {
            log.error("Không ghi được nhật ký kiểm toán cho {} trên {} {}",
                    action, targetType, targetId, e);
        }
    }

    private void validateSubject(SubjectType type, long id) {
        boolean exists = type == SubjectType.NGUOI_DUNG
                ? users.existsById(id)
                : departments.existsById(id);
        if (!exists) throw BusinessException.khongTimThay();
    }
}
