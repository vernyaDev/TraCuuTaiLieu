package vn.utc.tracuu.knowledge;

import vn.utc.tracuu.platform.web.Trang;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.utc.tracuu.platform.security.CurrentUser;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * Xử lý các cặp tài liệu bị nghi chồng lấn.
 *
 * <p>Màn này hiện <strong>mỗi cặp một thẻ</strong>, xếp theo mức độ giống nhau giảm dần. Bốn
 * hướng xử lý khác nhau về hệ quả, nên chúng là bốn nút chứ không phải một nút "đã xem":
 *
 * <ul>
 *   <li><em>Thay thế</em> — một bản thay bản kia, hệ thống ghi luôn chuỗi thay thế.</li>
 *   <li><em>Cùng tồn tại</em> — cả hai hợp lệ; từ đây câu trả lời dựa trên một trong hai sẽ
 *       <strong>kèm cảnh báo</strong> để người đọc biết còn nguồn khác.</li>
 *   <li><em>Báo động giả</em> — hai văn bản chỉ tình cờ giống nhau; cặp này không hiện lại nữa.</li>
 *   <li><em>Chờ</em> — chưa quyết, vẫn nằm trong danh sách.</li>
 * </ul>
 *
 * <p>Quyết định lưu theo <strong>từng cặp</strong>, và lần nạp lại không ghi đè nó — đánh dấu
 * báo động giả mà lần sau vẫn hiện lại đúng cặp ấy là cách chắc chắn làm người quản trị bỏ qua
 * cả danh sách.
 */
@RestController
@RequestMapping("/api/admin/overlaps")
public class AdminOverlapController {

    private final OverlapPairRepository pairs;
    private final DocumentRepository documents;
    private final DocumentLifecycleService lifecycle;

    public AdminOverlapController(OverlapPairRepository pairs, DocumentRepository documents,
                                  DocumentLifecycleService lifecycle) {
        this.pairs = pairs;
        this.documents = documents;
        this.lifecycle = lifecycle;
    }

    public record PairView(Long docAId, String titleA, Long docBId, String titleB,
                           float similarity, int similarityPercent, String decision,
                           Instant detectedAt, Instant decidedAt) {}

    @GetMapping
    @Transactional(readOnly = true)
    public List<PairView> list(@RequestParam(defaultValue = "CHO") String decision,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        OverlapDecision parsed = parse(decision);
        return pairs.findByDecisionOrderBySimilarityDesc(parsed,
                        Trang.cua(page, size))
                .map(this::toView)
                .getContent();
    }

    public record DecisionRequest(
            @NotBlank String decision,
            /** Chỉ dùng khi chọn "thay thế": tài liệu nào là bản MỚI. */
            Long newerDocumentId) {}

    /**
     * Quyết định cho một cặp.
     *
     * <p>Chọn <em>thay thế</em> thì phải nói rõ bản nào mới — hệ thống không đoán bằng ngày tải
     * lên, vì tài liệu cũ hoàn toàn có thể được số hoá sau tài liệu mới.
     */
    @PostMapping("/{docAId}/{docBId}")
    @Transactional
    public PairView decide(@PathVariable Long docAId, @PathVariable Long docBId,
                           @Valid @RequestBody DecisionRequest body) {

        long a = Math.min(docAId, docBId);
        long b = Math.max(docAId, docBId);
        DocumentOverlapPair pair = pairs.findPair(a, b).orElseThrow(BusinessException::khongTimThay);

        OverlapDecision decision = parse(body.decision());
        if (decision == OverlapDecision.THAY_THE) {
            if (body.newerDocumentId() == null
                    || (!body.newerDocumentId().equals(a) && !body.newerDocumentId().equals(b))) {
                throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "newerDocumentId",
                        "Phải chỉ rõ tài liệu nào là bản mới, và nó phải thuộc cặp này");
            }
            long newer = body.newerDocumentId();
            long older = newer == a ? b : a;
            lifecycle.declareSupersession(newer, older, CurrentUser.requireId());
            lifecycle.withdraw(older, CurrentUser.requireId());
        }

        pair.setDecision(decision);
        pair.setDecidedBy(CurrentUser.requireId());
        pair.setDecidedAt(Instant.now());
        pairs.save(pair);
        return toView(pair);
    }

    private PairView toView(DocumentOverlapPair p) {
        String titleA = documents.findById(p.getDocAId()).map(Document::getTitle).orElse("(đã xoá)");
        String titleB = documents.findById(p.getDocBId()).map(Document::getTitle).orElse("(đã xoá)");
        return new PairView(p.getDocAId(), titleA, p.getDocBId(), titleB,
                p.getSimilarity(), Math.round(p.getSimilarity() * 100),
                p.getDecision().name(), p.getDetectedAt(), p.getDecidedAt());
    }

    private OverlapDecision parse(String value) {
        try {
            return OverlapDecision.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "decision");
        }
    }
}
