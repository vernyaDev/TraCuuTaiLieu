package vn.utc.tracuu.knowledge;

import vn.utc.tracuu.platform.web.Trang;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.utc.tracuu.access.PermissionAdminService;
import vn.utc.tracuu.ingestion.IngestionService;
import vn.utc.tracuu.platform.config.TracuuProperties;
import vn.utc.tracuu.platform.security.CurrentUser;
import vn.utc.tracuu.platform.web.BusinessException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Khu quản trị: nạp, xem xét và quản vòng đời tài liệu — hợp đồng giao diện Mục 5. */
@RestController
@RequestMapping("/api/admin/documents")
public class AdminDocumentController {

    private final DocumentRepository documents;
    private final CategoryRepository categories;
    private final IngestionService ingestion;
    private final DocumentLifecycleService lifecycle;
    private final PermissionAdminService permissionAdmin;
    private final TracuuProperties props;

    public AdminDocumentController(DocumentRepository documents, CategoryRepository categories,
                                   IngestionService ingestion, DocumentLifecycleService lifecycle,
                                   PermissionAdminService permissionAdmin, TracuuProperties props) {
        this.documents = documents;
        this.categories = categories;
        this.ingestion = ingestion;
        this.lifecycle = lifecycle;
        this.permissionAdmin = permissionAdmin;
        this.props = props;
    }

    public record DocumentRow(Long id, String title, String originalFilename, String status,
                              String category, int sensitivity, String sensitivityLabel,
                              LocalDate effectiveFrom, LocalDate effectiveTo, boolean hasNoExpiry,
                              boolean expired, boolean expiringSoon, boolean unknownExpiry,
                              Long supersedesDocumentId, boolean metadataConfirmed,
                              String errorReason, java.time.Instant uploadedAt) {}

    /**
     * Danh sách tài liệu, có lọc và phân trang.
     *
     * <p>{@code expiringInDays} phục vụ nhóm <em>sắp hết hiệu lực</em>. Ba nhóm ngày tháng được
     * tách riêng trong kết quả — đã hết hạn, sắp hết hạn, và <strong>chưa khai thời hạn</strong>
     * — vì nhóm thứ ba là thiếu thông tin cần bổ sung, không phải việc cần xử lý gấp.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public Page<DocumentRow> list(@RequestParam(required = false) String q,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) Integer expiringInDays,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {

        DocumentStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = DocumentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(
                        vn.utc.tracuu.platform.web.ErrorCode.DU_LIEU_KHONG_HOP_LE, "status");
            }
        }
        LocalDate expiringBefore = expiringInDays == null
                ? null : LocalDate.now().plusDays(expiringInDays);

        return documents.search(q, parsedStatus, categoryId, expiringBefore,
                        Trang.cua(page, size, Sort.by(Sort.Direction.DESC, "uploadedAt")))
                .map(this::toRow);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public DocumentRow detail(@PathVariable Long id) {
        return toRow(documents.findById(id).orElseThrow(BusinessException::khongTimThay));
    }

    // ── Nạp ───────────────────────────────────────────────────────────────────────────────

    public record UploadAccepted(UUID trackingId, int fileCount) {}

    /** Nhận tệp và trả mã theo dõi NGAY — việc bóc chữ chạy nền. */
    @PostMapping("/upload")
    public UploadAccepted upload(@RequestParam("files") List<MultipartFile> files) {
        UUID tracking = ingestion.startBatch(files, CurrentUser.requireId());
        return new UploadAccepted(tracking, files.size());
    }

    @GetMapping("/batches/{trackingId}")
    public IngestionService.BatchProgress progress(@PathVariable UUID trackingId) {
        return ingestion.progress(trackingId);
    }

    @PostMapping("/batches/{trackingId}/cancel")
    public void cancelBatch(@PathVariable UUID trackingId) {
        ingestion.cancel(trackingId);
    }

    // ── Metadata ──────────────────────────────────────────────────────────────────────────

    public record MetadataRequest(
            String title, Long categoryId,
            LocalDate effectiveFrom, LocalDate effectiveTo, boolean hasNoExpiry) {}

    /**
     * Sửa metadata.
     *
     * <p>Không cho vừa khai "không có thời hạn" vừa đặt ngày hết hiệu lực — hai điều đó mâu
     * thuẫn, và lược đồ cũng có ràng buộc chặn. Bắt ở đây để người dùng nhận câu lỗi nghiệp vụ
     * thay vì một lỗi cơ sở dữ liệu.
     */
    @PutMapping("/{id}/metadata")
    @Transactional
    public DocumentRow updateMetadata(@PathVariable Long id, @Valid @RequestBody MetadataRequest body) {
        Document doc = documents.findById(id).orElseThrow(BusinessException::khongTimThay);

        if (body.hasNoExpiry() && body.effectiveTo() != null) {
            throw new BusinessException(
                    vn.utc.tracuu.platform.web.ErrorCode.DU_LIEU_KHONG_HOP_LE, "effectiveTo",
                    "Đã khai không có thời hạn thì không đặt ngày hết hiệu lực");
        }
        if (body.title() != null && !body.title().isBlank()) doc.setTitle(body.title().strip());
        if (body.categoryId() != null) {
            doc.setCategory(categories.findById(body.categoryId())
                    .orElseThrow(BusinessException::khongTimThay));
        }
        doc.setEffectiveFrom(body.effectiveFrom());
        doc.setEffectiveTo(body.effectiveTo());
        doc.setHasNoExpiry(body.hasNoExpiry());
        documents.save(doc);
        return toRow(doc);
    }

    @PostMapping("/{id}/confirm-metadata")
    public void confirmMetadata(@PathVariable Long id) {
        lifecycle.confirmMetadata(id, CurrentUser.requireId());
    }

    /**
     * Metadata hệ thống đề xuất, <strong>kèm trích đoạn làm căn cứ từng giá trị</strong>.
     *
     * <p>Trả về nguyên dạng JSON đã lưu lúc nạp. Mỗi giá trị mang cờ {@code verified} cho biết
     * trích đoạn đã được đối chiếu là có thật trong tài liệu hay chưa — giao diện phải hiện hai
     * ca đó khác nhau, vì một căn cứ bịa còn tệ hơn không có căn cứ.
     */
    @GetMapping("/{id}/metadata-suggestion")
    @Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<String> metadataSuggestion(@PathVariable Long id) {
        Document doc = documents.findById(id).orElseThrow(BusinessException::khongTimThay);
        String suggestion = doc.getMetadataSuggestion();
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(suggestion == null
                        ? "{\"notes\":[\"Tài liệu này chưa qua bước đề xuất metadata.\"]}"
                        : suggestion);
    }

    // ── Nhãn mật ──────────────────────────────────────────────────────────────────────────

    public record SensitivityRequest(@Min(1) int sensitivity) {}

    public record SensitivityImpact(int current, int proposed, long usersLosingAccess) {}

    /** Con số hậu quả của việc nâng nhãn mật — hiện TRƯỚC khi hỏi xác nhận. */
    @GetMapping("/{id}/sensitivity-impact")
    @Transactional(readOnly = true)
    public SensitivityImpact sensitivityImpact(@PathVariable Long id, @RequestParam int sensitivity) {
        Document doc = documents.findById(id).orElseThrow(BusinessException::khongTimThay);
        long losing = sensitivity > doc.getSensitivity()
                ? permissionAdmin.countUsersLosingAccess(id, sensitivity) : 0;
        return new SensitivityImpact(doc.getSensitivity(), sensitivity, losing);
    }

    @PutMapping("/{id}/sensitivity")
    public void setSensitivity(@PathVariable Long id, @Valid @RequestBody SensitivityRequest body) {
        permissionAdmin.setSensitivity(id, body.sensitivity(), CurrentUser.requireId());
    }

    // ── Vòng đời ──────────────────────────────────────────────────────────────────────────

    public record SupersedeRequest(@NotNull Long supersedesDocumentId) {}

    @PostMapping("/{id}/supersedes")
    public void supersede(@PathVariable Long id, @Valid @RequestBody SupersedeRequest body) {
        lifecycle.declareSupersession(id, body.supersedesDocumentId(), CurrentUser.requireId());
    }

    @GetMapping("/{id}/chain")
    public List<DocumentRow> chain(@PathVariable Long id) {
        return lifecycle.supersessionChain(id).stream().map(this::toRow).toList();
    }

    @PostMapping("/{id}/withdraw")
    public void withdraw(@PathVariable Long id) {
        lifecycle.withdraw(id, CurrentUser.requireId());
    }

    @PostMapping("/{id}/restore")
    public void restore(@PathVariable Long id) {
        lifecycle.restore(id, CurrentUser.requireId());
    }

    @GetMapping("/{id}/delete-impact")
    public DocumentLifecycleService.DeleteImpact deleteImpact(@PathVariable Long id) {
        return lifecycle.deleteImpact(id);
    }

    /** Xoá vĩnh viễn — từng tài liệu một, không có đường xoá hàng loạt. Cố ý. */
    @DeleteMapping("/{id}")
    public void deletePermanently(@PathVariable Long id) {
        lifecycle.deletePermanently(id, CurrentUser.requireId());
    }

    // ── Danh mục ──────────────────────────────────────────────────────────────────────────

    @GetMapping("/categories")
    public List<Category> categories() {
        return categories.findAll(Sort.by("name"));
    }

    // ── Tiện ích ──────────────────────────────────────────────────────────────────────────

    private DocumentRow toRow(Document d) {
        LocalDate today = LocalDate.now();
        return new DocumentRow(
                d.getId(), d.getTitle(), d.getOriginalFilename(), d.getStatus().name(),
                d.getCategory() == null ? null : d.getCategory().getName(),
                d.getSensitivity(), props.security().labelOf(d.getSensitivity()),
                d.getEffectiveFrom(), d.getEffectiveTo(), d.isHasNoExpiry(),
                d.isExpired(today), d.isExpiringSoon(today, 30), d.hasUnknownExpiry(),
                d.getSupersedesDocumentId(), d.isMetadataConfirmed(),
                d.getErrorReason(), d.getUploadedAt());
    }
}
