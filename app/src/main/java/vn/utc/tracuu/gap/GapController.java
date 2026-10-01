package vn.utc.tracuu.gap;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.utc.tracuu.platform.security.CurrentUser;

import java.time.Instant;
import java.util.List;

/**
 * Báo cáo khoảng trống tri thức — hợp đồng giao diện Mục 7.
 *
 * <p><strong>Điều quan trọng nhất của lớp này là một đường KHÔNG có: không đường nào lọc theo
 * người dùng.</strong> Ràng buộc ấy thuộc về hợp đồng chứ không chỉ thuộc về giao diện — thiếu
 * nó ở tầng máy chủ thì bất kỳ ai sửa giao diện về sau cũng dựng được một công cụ giám sát nhân
 * viên trên nền hệ thống này, và người đó thậm chí không cần có ý đồ xấu, chỉ cần thấy dữ liệu
 * đã sẵn ở đó.
 *
 * <p>Báo cáo trả lời câu <em>"kho đang thiếu gì"</em>. Nó không trả lời câu <em>"ai hỏi
 * gì"</em>, và các bản ghi trả ra không mang định danh người hỏi.
 */
@RestController
@RequestMapping("/api/admin/gaps")
public class GapController {

    private final GapService gaps;

    public GapController(GapService gaps) {
        this.gaps = gaps;
    }

    public record ClusterView(Long id, String label, String reason, String reasonLabel,
                              int memberCount, Instant firstSeen, Instant lastSeen,
                              String status, String resolveNote) {}

    /** Câu hỏi trong một nhóm — hiện CÂU HỎI, không hiện ai hỏi. */
    public record QuestionView(Long id, String question, String rewritten,
                               String departmentSnapshot, Instant askedAt) {}

    @GetMapping
    public Page<ClusterView> list(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) String reason,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return gaps.list(status, reason, page, size).map(this::toView);
    }

    /**
     * Các câu hỏi cụ thể trong một nhóm.
     *
     * <p>Trả về phòng ban <em>tại thời điểm hỏi</em> chứ không phải phòng ban hiện tại, vì câu
     * hỏi quản trị đang cần trả lời là <em>"nên cấp quyền cho phòng nào"</em> — và người hỏi có
     * thể đã chuyển phòng từ lúc ấy tới giờ. Phòng ban là thông tin <strong>nhóm</strong>, không
     * phải định danh cá nhân.
     */
    @GetMapping("/{clusterId}/questions")
    public List<QuestionView> questions(@PathVariable Long clusterId) {
        return gaps.questionsIn(clusterId).stream()
                .map(r -> new QuestionView(r.getId(), r.getQuestion(), r.getRewritten(),
                        r.getDepartmentSnapshot(), r.getCreatedAt()))
                .toList();
    }

    public record ResolveRequest(String note, boolean ignore) {}

    @PostMapping("/{clusterId}/resolve")
    public void resolve(@PathVariable Long clusterId, @Valid @RequestBody ResolveRequest body) {
        gaps.resolve(clusterId, CurrentUser.requireId(), body.note(), body.ignore());
    }

    private ClusterView toView(GapCluster c) {
        return new ClusterView(c.getId(), c.getLabel(), c.getReason(), reasonLabel(c.getReason()),
                c.getMemberCount(), c.getFirstSeen(), c.getLastSeen(),
                c.getStatus(), c.getResolveNote());
    }

    /**
     * Nhãn tiếng Việt cho từng lý do.
     *
     * <p>Bốn lý do dẫn tới bốn hành động khác nhau, nên nhãn nói luôn hành động: thiếu tài liệu
     * thì đi bổ sung, thiếu quyền thì đi cấp quyền, căn cứ mỏng thì tài liệu có nhưng chưa đủ
     * chi tiết.
     */
    private String reasonLabel(String reason) {
        return switch (reason) {
            case RefusalClassifier.THIEU_TAI_LIEU -> "Kho chưa có tài liệu về chủ đề này";
            case RefusalClassifier.THIEU_QUYEN -> "Có tài liệu nhưng người hỏi chưa được cấp quyền";
            case RefusalClassifier.CAN_CU_MONG -> "Có tài liệu liên quan nhưng chưa đủ chi tiết";
            case RefusalClassifier.NGOAI_PHAM_VI -> "Câu hỏi ngoài phạm vi kho tài liệu";
            default -> reason;
        };
    }
}
