package vn.utc.tracuu.access;

import vn.utc.tracuu.platform.web.Trang;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.security.CurrentUser;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Khu quản trị: tài khoản, phòng ban, quyền.
 *
 * <p>Toàn bộ đường dưới {@code /api/admin/**} yêu cầu vai trò quản trị — kiểm ở tầng cấu hình
 * an ninh, không kiểm lặp ở từng phương thức. Ở đây quyền là quyền trên <strong>chức năng</strong>
 * nên trả <em>không đủ quyền</em> là đúng: lộ sự tồn tại của một chức năng thì không lộ gì về
 * dữ liệu.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAccessController {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PermissionAdminService permissionAdmin;
    private final AuthService auth;
    private final PasswordEncoder encoder;
    private final TracuuProperties props;

    public AdminAccessController(UserRepository users, DepartmentRepository departments,
                                 PermissionAdminService permissionAdmin, AuthService auth,
                                 PasswordEncoder encoder, TracuuProperties props) {
        this.users = users;
        this.departments = departments;
        this.permissionAdmin = permissionAdmin;
        this.auth = auth;
        this.encoder = encoder;
        this.props = props;
    }

    // ── Tài khoản ─────────────────────────────────────────────────────────────────────────

    public record UserView(Long id, String username, String fullName, String email,
                           String role, int clearanceLevel, String clearanceLabel,
                           boolean active, boolean mustChangePassword,
                           List<String> departments, java.time.Instant lastLoginAt) {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(max = 200) String fullName,
            @Email String email,
            @NotBlank String password,
            @Min(1) int clearanceLevel,
            @NotBlank String role,
            List<Long> departmentIds) {}

    public record UpdateUserRequest(
            @NotBlank @Size(max = 200) String fullName,
            @Email String email,
            @Min(1) int clearanceLevel,
            @NotBlank String role,
            List<Long> departmentIds) {}

    /**
     * Danh sách tài khoản.
     *
     * <p>Tên phòng ban được lấy riêng cho cả trang bằng MỘT truy vấn, không đi qua
     * {@code u.getDepartments()}. Quan hệ đó là LAZY, và ánh xạ sang bản hiển thị xảy ra sau
     * khi phiên Hibernate đã đóng — bản trước làm đúng như vậy và màn Tài khoản trả 500 cho
     * mọi lần mở. Bọc phương thức trong một giao dịch cũng chữa được lỗi đó, nhưng đổi nó lấy
     * một truy vấn phụ cho mỗi dòng; cách này không phải chọn giữa hai điều đó.
     */
    @GetMapping("/users")
    public Page<UserView> listUsers(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Boolean active,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Page<User> trang = users.search(q, active,
                Trang.cua(page, size, Sort.by("fullName")));
        if (trang.isEmpty()) return trang.map(u -> toView(u, List.of()));

        Map<Long, List<String>> theoNguoi = new HashMap<>();
        for (Object[] hang : users.findDepartmentNamesFor(
                trang.getContent().stream().map(User::getId).toList())) {
            theoNguoi.computeIfAbsent((Long) hang[0], k -> new ArrayList<>()).add((String) hang[1]);
        }
        return trang.map(u -> toView(u, theoNguoi.getOrDefault(u.getId(), List.of())));
    }

    @PostMapping("/users")
    @Transactional
    public UserView createUser(@Valid @RequestBody CreateUserRequest body) {
        if (users.existsByUsernameIgnoreCase(body.username())) {
            throw new BusinessException(ErrorCode.TRUNG_DU_LIEU, "username");
        }
        User user = new User();
        user.setUsername(body.username().strip());
        user.setFullName(body.fullName().strip());
        user.setEmail(body.email());
        user.setPasswordHash(encoder.encode(body.password()));
        user.setClearanceLevel(body.clearanceLevel());
        user.setRole(parseRole(body.role()));
        // Tài khoản mới LUÔN phải đổi mật khẩu lần đầu: mật khẩu do quản trị đặt thì quản trị
        // cũng biết nó, nên nó không còn là bí mật của riêng người dùng.
        user.setMustChangePassword(true);
        users.save(user);

        if (body.departmentIds() != null && !body.departmentIds().isEmpty()) {
            permissionAdmin.setDepartments(user.getId(), body.departmentIds(), CurrentUser.requireId());
        }
        return toView(users.findByIdWithDepartments(user.getId()).orElseThrow());
    }

    @PutMapping("/users/{id}")
    @Transactional
    public UserView updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest body) {
        User user = users.findById(id).orElseThrow(BusinessException::khongTimThay);
        user.setFullName(body.fullName().strip());
        user.setEmail(body.email());
        user.setRole(parseRole(body.role()));
        users.save(user);

        long actor = CurrentUser.requireId();
        // Hai thứ dưới đây là THAY ĐỔI QUYỀN nên phải đi qua lớp chuyên trách — nó tăng số hiệu
        // phiên bản quyền và ghi kiểm toán, việc mà lưu thẳng ở đây sẽ bỏ sót.
        permissionAdmin.setClearance(id, body.clearanceLevel(), actor);
        permissionAdmin.setDepartments(id, body.departmentIds() == null ? List.of() : body.departmentIds(), actor);

        return toView(users.findByIdWithDepartments(id).orElseThrow());
    }

    /**
     * Vô hiệu hoá tài khoản.
     *
     * <p>Không xoá: bản ghi hội thoại và nhật ký kiểm toán trỏ tới tài khoản này, và xoá đi là
     * mất luôn khả năng trả lời câu "ai đã làm việc đó".
     */
    @PostMapping("/users/{id}/deactivate")
    @Transactional
    public UserView deactivate(@PathVariable Long id) {
        User user = users.findById(id).orElseThrow(BusinessException::khongTimThay);
        if (user.getId().equals(CurrentUser.requireId())) {
            throw new BusinessException(ErrorCode.TRANG_THAI_KHONG_HOP_LE, null,
                    "Không tự vô hiệu hoá tài khoản đang đăng nhập");
        }
        user.setActive(false);
        users.save(user);
        return toView(user);
    }

    @PostMapping("/users/{id}/activate")
    @Transactional
    public UserView activate(@PathVariable Long id) {
        User user = users.findById(id).orElseThrow(BusinessException::khongTimThay);
        user.setActive(true);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        users.save(user);
        return toView(user);
    }

    public record ResetPasswordRequest(@NotBlank String newPassword) {}

    @PostMapping("/users/{id}/reset-password")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest body) {
        auth.resetPassword(id, body.newPassword(), CurrentUser.requireId());
    }

    // ── Nhập hàng loạt: HAI BƯỚC, xem trước rồi mới ghi ───────────────────────────────────

    public record ImportRow(String username, String fullName, String email,
                            Integer clearanceLevel, String departments) {}

    public record ImportPreviewRow(int line, String username, String fullName,
                                   String status, String message) {}

    public record ImportPreview(int total, int valid, int invalid, List<ImportPreviewRow> rows) {}

    /**
     * Bước một: xem trước.
     *
     * <p>Ghi thẳng một tệp trăm dòng rồi báo "có 7 dòng lỗi" là đẩy người quản trị vào thế phải
     * dọn dẹp thủ công. Xem trước tách việc kiểm tra khỏi việc ghi, và đó là khác biệt giữa một
     * thao tác sửa được với một thao tác phải sửa hậu quả.
     */
    @PostMapping("/users/import/preview")
    @Transactional(readOnly = true)
    public ImportPreview previewImport(@RequestBody List<ImportRow> rows) {
        List<ImportPreviewRow> out = new ArrayList<>();
        int valid = 0;

        for (int i = 0; i < rows.size(); i++) {
            ImportRow r = rows.get(i);
            int line = i + 1;

            if (r.username() == null || r.username().isBlank()) {
                out.add(new ImportPreviewRow(line, r.username(), r.fullName(), "LOI",
                        "Thiếu tên đăng nhập"));
            } else if (r.fullName() == null || r.fullName().isBlank()) {
                out.add(new ImportPreviewRow(line, r.username(), r.fullName(), "LOI",
                        "Thiếu họ tên"));
            } else if (users.existsByUsernameIgnoreCase(r.username().strip())) {
                out.add(new ImportPreviewRow(line, r.username(), r.fullName(), "TRUNG",
                        "Tên đăng nhập đã tồn tại, dòng này sẽ bị bỏ qua"));
            } else if (r.clearanceLevel() != null
                    && (r.clearanceLevel() < 1 || r.clearanceLevel() > props.security().maxClearance())) {
                out.add(new ImportPreviewRow(line, r.username(), r.fullName(), "LOI",
                        "Mức mật ngoài thang 1–" + props.security().maxClearance()));
            } else {
                out.add(new ImportPreviewRow(line, r.username(), r.fullName(), "HOP_LE",
                        "Sẽ tạo mới"));
                valid++;
            }
        }
        return new ImportPreview(rows.size(), valid, rows.size() - valid, out);
    }

    public record ImportResult(int created, int skipped, List<String> messages) {}

    /** Bước hai: ghi thật. Chỉ ghi những dòng hợp lệ; dòng lỗi bỏ qua chứ không dừng cả tệp. */
    @PostMapping("/users/import/commit")
    @Transactional
    public ImportResult commitImport(@RequestBody List<ImportRow> rows,
                                     @RequestParam(defaultValue = "Doimatkhau@1") String defaultPassword) {
        long actor = CurrentUser.requireId();
        int created = 0, skipped = 0;
        List<String> messages = new ArrayList<>();

        for (ImportRow r : rows) {
            if (r.username() == null || r.username().isBlank()
                    || r.fullName() == null || r.fullName().isBlank()
                    || users.existsByUsernameIgnoreCase(r.username().strip())) {
                skipped++;
                continue;
            }
            User user = new User();
            user.setUsername(r.username().strip());
            user.setFullName(r.fullName().strip());
            user.setEmail(r.email());
            user.setPasswordHash(encoder.encode(defaultPassword));
            user.setClearanceLevel(r.clearanceLevel() == null ? 1 : r.clearanceLevel());
            user.setRole(Role.NHAN_VIEN);
            user.setMustChangePassword(true);
            users.save(user);
            created++;

            if (r.departments() != null && !r.departments().isBlank()) {
                List<Long> ids = new ArrayList<>();
                for (String name : r.departments().split(";")) {
                    departments.findByNameIgnoreCase(name.strip())
                            .ifPresentOrElse(d -> ids.add(d.getId()),
                                    () -> messages.add("Không có phòng ban: " + name.strip()));
                }
                if (!ids.isEmpty()) permissionAdmin.setDepartments(user.getId(), ids, actor);
            }
        }
        return new ImportResult(created, skipped, messages);
    }

    // ── Phòng ban ─────────────────────────────────────────────────────────────────────────

    public record ClearanceLevel(int level, String label) {}

    /**
     * Thang mức mật đang dùng — giao diện cần nó để dựng ô chọn.
     *
     * <p>Thang là <strong>số thứ tự có nhãn cấu hình được</strong> (mô hình dữ liệu Mục 2), nên
     * giao diện không được đoán. Thiếu đường này thì màn tạo tài khoản phải ghi cứng "1 Công
     * khai, 2 Nội bộ, 3 Mật" — và ngày ai đó thêm mức thứ tư vào cấu hình, giao diện sẽ im lặng
     * bỏ sót nó.
     */
    @GetMapping("/clearance-levels")
    public List<ClearanceLevel> clearanceLevels() {
        return props.security().clearanceLabels().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ClearanceLevel(e.getKey(), e.getValue()))
                .toList();
    }

    public record DepartmentView(Long id, String name, String description,
                                 long memberCount, long aclCount) {}

    public record DepartmentRequest(@NotBlank @Size(max = 150) String name,
                                    @Size(max = 500) String description) {}

    @GetMapping("/departments")
    public List<DepartmentView> listDepartments() {
        return departments.findAll(Sort.by("name")).stream()
                .map(d -> new DepartmentView(d.getId(), d.getName(), d.getDescription(),
                        departments.countMembers(d.getId()), departments.countAclEntries(d.getId())))
                .toList();
    }

    @PostMapping("/departments")
    public DepartmentView createDepartment(@Valid @RequestBody DepartmentRequest body) {
        if (departments.existsByNameIgnoreCase(body.name())) {
            throw new BusinessException(ErrorCode.TRUNG_DU_LIEU, "name");
        }
        Department d = departments.save(new Department(body.name().strip(), body.description()));
        return new DepartmentView(d.getId(), d.getName(), d.getDescription(), 0, 0);
    }

    /**
     * Xoá phòng ban — trả về con số hậu quả TRƯỚC khi làm.
     *
     * <p>Xoá một phòng ban gỡ luôn mọi quyền cấp qua nó. Con số người mất quyền phải hiện ra
     * trước, vì "bạn có chắc không" mà không kèm số thì người quản trị sẽ bấm đồng ý theo phản xạ.
     */
    @GetMapping("/departments/{id}/impact")
    public DepartmentView impact(@PathVariable Long id) {
        Department d = departments.findById(id).orElseThrow(BusinessException::khongTimThay);
        return new DepartmentView(d.getId(), d.getName(), d.getDescription(),
                departments.countMembers(id), departments.countAclEntries(id));
    }

    public record DeleteDepartmentResult(int removedPermissions) {}

    @DeleteMapping("/departments/{id}")
    public DeleteDepartmentResult deleteDepartment(@PathVariable Long id) {
        // Đi qua lớp chuyên trách: xoá phòng ban gỡ luôn mọi quyền cấp qua nó, nên nó là một
        // thay đổi quyền và phải tăng số hiệu phiên bản quyền.
        return new DeleteDepartmentResult(
                permissionAdmin.deleteDepartment(id, CurrentUser.requireId()));
    }

    // ── Quyền trên tài liệu ───────────────────────────────────────────────────────────────

    public record GrantRequest(@NotBlank String subjectType, @NotNull Long subjectId,
                               @NotBlank String effect) {}

    public record BulkGrantRequest(@NotEmpty List<Long> documentIds,
                                   @NotBlank String subjectType, @NotNull Long subjectId,
                                   @NotBlank String effect) {}

    @GetMapping("/documents/{id}/permissions")
    public PermissionAdminService.DocumentPermissions documentPermissions(@PathVariable Long id) {
        return permissionAdmin.ofDocument(id);
    }

    @PostMapping("/documents/{id}/permissions")
    public void grant(@PathVariable Long id, @Valid @RequestBody GrantRequest body) {
        permissionAdmin.grant(id, parseSubject(body.subjectType()), body.subjectId(),
                parseEffect(body.effect()), CurrentUser.requireId());
    }

    @DeleteMapping("/documents/{id}/permissions")
    public void revoke(@PathVariable Long id, @Valid @RequestBody GrantRequest body) {
        permissionAdmin.revoke(id, parseSubject(body.subjectType()), body.subjectId(),
                parseEffect(body.effect()), CurrentUser.requireId());
    }

    public record BulkGrantResult(int applied, int selected) {}

    @PostMapping("/permissions/bulk-grant")
    public BulkGrantResult bulkGrant(@Valid @RequestBody BulkGrantRequest body) {
        int applied = permissionAdmin.grantBulk(body.documentIds(), parseSubject(body.subjectType()),
                body.subjectId(), parseEffect(body.effect()), CurrentUser.requireId());
        return new BulkGrantResult(applied, body.documentIds().size());
    }

    /** "Người này đọc được tài liệu này không, và vì sao." */
    @GetMapping("/permissions/explain")
    public PermissionResolver.AccessExplanation explain(@RequestParam Long userId,
                                                        @RequestParam Long documentId) {
        return permissionAdmin.explain(userId, documentId);
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────────────────

    /** Dùng khi quan hệ phòng ban ĐÃ được nạp (trong giao dịch, hoặc qua truy vấn nạp kèm). */
    private UserView toView(User u) {
        return toView(u, u.getDepartments() == null ? List.of()
                : u.getDepartments().stream().map(Department::getName).toList());
    }

    /** Dùng khi tên phòng ban đã được lấy sẵn ở nơi khác — không chạm quan hệ lười. */
    private UserView toView(User u, List<String> depts) {
        return new UserView(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(),
                u.getRole().name(), u.getClearanceLevel(),
                props.security().labelOf(u.getClearanceLevel()),
                u.isActive(), u.isMustChangePassword(), depts, u.getLastLoginAt());
    }

    private Role parseRole(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "role");
        }
    }

    private SubjectType parseSubject(String value) {
        try {
            return SubjectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "subjectType");
        }
    }

    private AclEffect parseEffect(String value) {
        try {
            return AclEffect.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "effect");
        }
    }
}
