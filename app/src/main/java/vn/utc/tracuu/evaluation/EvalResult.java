package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Kết quả của một câu hỏi trong một lượt chạy.
 *
 * <p>Một dòng cho mỗi câu hỏi trong mỗi lượt, nên <em>chạy tiếp phần dở dang</em> chỉ là truy
 * vấn tìm câu chưa có kết quả — không cần cơ chế điểm dừng riêng. Với một lượt chạy kéo dài
 * hàng giờ trên một máy cũng đang dùng để lập trình, khả năng chạy tiếp không phải tiện nghi
 * mà là điều kiện để bộ đo dùng được.
 */
@Entity
@Table(name = "eval_results")
@Getter @Setter @NoArgsConstructor
public class EvalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(nullable = false)
    private boolean refused = false;

    @Column(name = "refusal_reason", length = 40)
    private String refusalReason;

    @Column(name = "precision_at_k")
    private Float precisionAtK;

    @Column(name = "recall_at_k")
    private Float recallAtK;

    @Column(name = "reciprocal_rank")
    private Float reciprocalRank;

    @Column(name = "auto_score")
    private Float autoScore;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_reason", length = 500)
    private String errorReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
