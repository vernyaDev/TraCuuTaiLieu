package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

/**
 * Một CẶP tài liệu bị nghi chồng lấn.
 *
 * <p>Chồng lấn là quan hệ theo cặp, không phải thuộc tính của một tài liệu. Lược đồ trước chỉ
 * có một số nguyên cụm chủ đề trên mỗi tài liệu, và số nguyên đó không mang nổi bốn thứ US-014
 * đòi: mức độ giống nhau để xếp giảm dần, quyết định của quản trị cho từng cặp riêng, trạng
 * thái đã xử lý để không hiện lại đúng cặp ấy, và danh sách đã xử lý xem lại được.
 *
 * <p>Khoá chính là cặp {@code (docA, docB)} với quy ước <strong>id nhỏ hơn đứng trước</strong>,
 * để cùng một cặp không bao giờ sinh hai bản ghi ngược chiều nhau.
 */
@Entity
@Table(name = "document_overlap_pairs")
@IdClass(DocumentOverlapPair.Key.class)
@Getter @Setter @NoArgsConstructor
public class DocumentOverlapPair {

    @Id
    @Column(name = "doc_a_id")
    private Long docAId;

    @Id
    @Column(name = "doc_b_id")
    private Long docBId;

    @Column(nullable = false)
    private float similarity;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverlapDecision decision = OverlapDecision.CHO;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Dựng một cặp theo đúng quy ước thứ tự — đường DUY NHẤT nên dùng để tạo. */
    public static DocumentOverlapPair of(long one, long other, float similarity) {
        DocumentOverlapPair p = new DocumentOverlapPair();
        p.docAId = Math.min(one, other);
        p.docBId = Math.max(one, other);
        p.similarity = similarity;
        return p;
    }

    @Getter @Setter @NoArgsConstructor
    public static class Key implements Serializable {
        private Long docAId;
        private Long docBId;

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return java.util.Objects.equals(docAId, k.docAId)
                && java.util.Objects.equals(docBId, k.docBId);
        }
        @Override public int hashCode() { return java.util.Objects.hash(docAId, docBId); }
    }
}
