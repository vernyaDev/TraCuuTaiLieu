package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một lượt chạy đo.
 *
 * <p>Cột cấu hình giữ NGUYÊN VẸN tệp cấu hình lúc chạy. Đây là hiện thực trực tiếp của yêu
 * cầu "kết quả tự mang theo điều kiện sinh ra nó": người phản biện đọc bảng phải biết nó sinh
 * ra trong điều kiện gì, và hai lượt chạy không kèm cấu hình thì không so được với nhau mà
 * trông vẫn như so được.
 */
@Entity
@Table(name = "eval_runs")
@Getter @Setter @NoArgsConstructor
public class EvalRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String label;

    /**
     * Khai kiểu JSON cho tầng ánh xạ.
     *
     * <p>Thiếu khai báo này thì Hibernate gửi giá trị đi dưới dạng chuỗi ký tự, và PostgreSQL
     * từ chối vì nó KHÔNG tự ép chuỗi sang jsonb. Lỗi chỉ xuất hiện ở lần ghi thật đầu tiên —
     * biên dịch sạch, kiểm thử đơn vị sạch, rồi đổ ở môi trường chạy.
     */
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String config;

    @Column(name = "chunk_set_id")
    private Long chunkSetId;

    @Column(name = "question_set_version", nullable = false, length = 40)
    private String questionSetVersion;

    @Column(name = "retrieval_mode", nullable = false, length = 20)
    private String retrievalMode;

    @Column(name = "model_name", length = 120)
    private String modelName;

    private Float temperature;

    private Long seed;

    @Column(name = "top_k")
    private Integer topK;

    @Column(name = "context_max_tokens")
    private Integer contextMaxTokens;

    @Column(nullable = false, length = 20)
    private String status = "DANG_CHAY";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;
}
