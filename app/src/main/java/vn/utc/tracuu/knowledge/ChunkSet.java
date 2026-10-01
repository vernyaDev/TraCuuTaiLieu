package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một TẬP ĐOẠN, ứng với một chiến lược tách đoạn.
 *
 * <p>Cách hiển nhiên là mỗi lần đổi chiến lược thì xoá hết đoạn cũ và tách lại. Nhưng ma trận
 * thí nghiệm ở US-030 chạy nhiều lần trên nhiều chiến lược, mà mỗi lần nạp lại kho 1.000 tài
 * liệu tốn tới 8 giờ. Ba chiến lược nhân vài lần chạy là vài ngày chỉ để chờ nạp.
 *
 * <p>Giữ nhiều tập song song thì nạp mỗi chiến lược <strong>đúng một lần</strong>, sau đó chạy
 * ma trận bao nhiêu lần cũng được. Giá phải trả là dung lượng, và nó nằm gọn trong ngưỡng.
 *
 * <p><strong>Bản triển khai cho doanh nghiệp chỉ có MỘT tập đang hoạt động.</strong> Nhiều tập
 * là công cụ của tác giả, không phải tính năng của người dùng.
 */
@Entity
@Table(name = "chunk_sets")
@Getter @Setter @NoArgsConstructor
public class ChunkSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strategy_name", nullable = false, length = 60)
    private String strategyName;

    /**
     * Khai kiểu JSON cho tầng ánh xạ.
     *
     * <p>Thiếu khai báo này thì Hibernate gửi giá trị đi dưới dạng chuỗi ký tự, và PostgreSQL
     * từ chối vì nó KHÔNG tự ép chuỗi sang jsonb. Lỗi chỉ xuất hiện ở lần ghi thật đầu tiên —
     * biên dịch sạch, kiểm thử đơn vị sạch, rồi đổ ở môi trường chạy.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String params = "{}";

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    /**
     * Chữ ký của mô hình sinh biểu diễn đã dùng để nạp tập đoạn này.
     *
     * <p>Rỗng nghĩa là <em>chưa biết</em> — tập tạo trước khi có cột này. Lần ghi đoạn kế tiếp
     * sẽ đóng dấu chữ ký đang chạy vào đó.
     *
     * <p>Không có trường này thì không cách nào phát hiện việc trộn hai không gian vector: nửa
     * kho nạp bằng một mô hình, nửa kia bằng mô hình khác, khoảng cách giữa chúng vẫn tính ra
     * số đẹp nhưng vô nghĩa, và <strong>không lỗi nào báo ra</strong>.
     */
    @Column(name = "embedding_signature", length = 200)
    private String embeddingSignature;

    /** Số chiều đi kèm: hai mô hình cùng tên khác số chiều cũng là hai không gian khác nhau. */
    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public ChunkSet(String strategyName, String params, boolean active) {
        this.strategyName = strategyName;
        this.params = params;
        this.active = active;
    }
}
