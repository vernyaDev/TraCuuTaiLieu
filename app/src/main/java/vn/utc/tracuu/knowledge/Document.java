package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Một tài liệu trong kho.
 *
 * <p>Ba chi tiết không hiển nhiên, ghi lại ngay tại chỗ vì chúng hay bị "dọn gọn" cho sai:
 *
 * <ul>
 *   <li><strong>{@code hasNoExpiry} là cột riêng, KHÔNG dùng {@code effectiveTo} rỗng để biểu
 *       diễn.</strong> US-013 đòi phân biệt <em>"không có thời hạn"</em> với <em>"chưa xác
 *       định"</em>. Gộp hai thứ đó vào một ô rỗng là mất đúng thông tin quyết định có cảnh báo
 *       hết hiệu lực hay không.</li>
 *   <li><strong>{@code supersedesDocumentId} trỏ về bản BỊ thay thế</strong>, tạo chuỗi một
 *       chiều. Vòng lặp bị chặn khi ghi chứ không bằng ràng buộc của cơ sở dữ liệu — ràng buộc
 *       không phát hiện được chu trình nhiều bước.</li>
 *   <li><strong>{@code topicClusterId} chỉ là BỘ LỌC THU HẸP</strong>, gán lúc nạp. Nó không
 *       mang được quyết định của quản trị về chồng lấn; cái đó ở {@link DocumentOverlapPair},
 *       vì chồng lấn là quan hệ theo cặp chứ không phải thuộc tính của một tài liệu.</li>
 * </ul>
 */
@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "original_filename", nullable = false, length = 400)
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, length = 700)
    private String storagePath;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** Nhãn mật — cùng THANG SỐ với mức mật của người dùng, không phải chuỗi liệt kê. */
    @Column(nullable = false)
    private int sensitivity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.DANG_XU_LY;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "has_no_expiry", nullable = false)
    private boolean hasNoExpiry = false;

    @Column(name = "supersedes_document_id")
    private Long supersedesDocumentId;

    @Column(name = "topic_cluster_id")
    private Long topicClusterId;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "error_reason", length = 1000)
    private String errorReason;

    @Column(name = "metadata_confirmed", nullable = false)
    private boolean metadataConfirmed = false;

    /**
     * Metadata hệ thống đề xuất, dạng JSON, mỗi giá trị kèm trích đoạn làm căn cứ.
     *
     * <p>Giữ ở dạng chuỗi JSON thay vì ánh xạ thành thực thể: đây là dữ liệu để quản trị ĐỌC
     * rồi quyết, không phải dữ liệu hệ thống truy vấn theo từng trường. Dựng năm bảng con cho
     * một thứ chỉ hiện lên một lần rồi bị thay bằng giá trị người xác nhận là trả giá không
     * đổi lấy gì.
     */
    /**
     * Khai kiểu JSON cho tầng ánh xạ.
     *
     * <p>Thiếu khai báo này thì Hibernate gửi giá trị đi dưới dạng chuỗi ký tự, và PostgreSQL
     * từ chối vì nó KHÔNG tự ép chuỗi sang jsonb. Lỗi chỉ xuất hiện ở lần ghi thật đầu tiên —
     * biên dịch sạch, kiểm thử đơn vị sạch, rồi đổ ở môi trường chạy.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata_suggestion", columnDefinition = "jsonb")
    private String metadataSuggestion;

    // ── Câu hỏi nghiệp vụ, trả lời ngay trên thực thể ──────────────────────────────────────

    /** Đã quá hạn hiệu lực chưa. Tài liệu khai "không có thời hạn" thì không bao giờ hết hạn. */
    public boolean isExpired(LocalDate today) {
        return !hasNoExpiry && effectiveTo != null && effectiveTo.isBefore(today);
    }

    /**
     * Sắp hết hiệu lực trong {@code days} ngày tới.
     *
     * <p>Phân biệt với {@link #isExpired}: "chưa xác định thời hạn" ({@code effectiveTo} rỗng mà
     * {@code hasNoExpiry} tắt) KHÔNG phải sắp hết hạn — nó là thiếu thông tin, và gộp hai thứ
     * đó lại là cách sinh ra cảnh báo giả hàng loạt.
     */
    public boolean isExpiringSoon(LocalDate today, int days) {
        if (hasNoExpiry || effectiveTo == null) return false;
        return !effectiveTo.isBefore(today) && !effectiveTo.isAfter(today.plusDays(days));
    }

    /** Thời hạn chưa ai khai — khác hẳn "không có thời hạn". */
    public boolean hasUnknownExpiry() {
        return !hasNoExpiry && effectiveTo == null;
    }
}
