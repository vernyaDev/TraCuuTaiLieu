package vn.utc.tracuu.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một lượt hỏi đáp.
 *
 * <p>{@code rawQuestion} và {@code rewrittenQuestion} lưu RIÊNG. Hai lý do, cả hai đều là yêu
 * cầu đã chốt: người dùng phải xem được câu đã viết lại, và khi một câu bị từ chối thì phải
 * biết việc viết lại có phải nguyên nhân thật hay không. Ghi đè bản gốc là xoá mất bằng chứng
 * đúng lúc cần đến nó nhất.
 *
 * <p>Hai cột độ trễ là nguồn dữ liệu THẬT cho ngưỡng thời gian phản hồi — đo trên chính lưu
 * lượng dùng thật, không chỉ đo trong phòng thí nghiệm.
 */
@Entity
@Table(name = "turns")
@Getter @Setter @NoArgsConstructor
public class Turn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "raw_question", nullable = false, columnDefinition = "text")
    private String rawQuestion;

    @Column(name = "rewritten_question", columnDefinition = "text")
    private String rewrittenQuestion;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(length = 20)
    private String complexity;

    @Column(nullable = false, length = 20)
    private String status = "DANG_CHAY";

    @Column(name = "refusal_reason", length = 40)
    private String refusalReason;

    @Column(name = "refusal_stage", length = 20)
    private String refusalStage;

    @Column(name = "chunk_set_id")
    private Long chunkSetId;

    @Column(name = "latency_first_token_ms")
    private Integer latencyFirstTokenMs;

    @Column(name = "latency_total_ms")
    private Integer latencyTotalMs;

    @Column(name = "latency_retrieval_ms")
    private Integer latencyRetrievalMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
