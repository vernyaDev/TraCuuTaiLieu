package vn.utc.tracuu.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một trích dẫn của lượt, kèm CẢ BA điểm số.
 *
 * <p>Ba điểm lưu riêng chứ không lưu mỗi điểm cuối: không tách ba thì không phân tích được vì
 * sao nhánh lai thắng hay thua từng nhánh riêng, mà đó lại là câu hỏi trung tâm của chương
 * thực nghiệm.
 */
@Entity
@Table(name = "turn_citations")
@Getter @Setter @NoArgsConstructor
public class TurnCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(nullable = false)
    private int rank;

    @Column(name = "score_bm25")
    private Float scoreBm25;

    @Column(name = "score_vector")
    private Float scoreVector;

    @Column(name = "score_rrf")
    private Float scoreRrf;

    @Column(name = "used_in_answer", nullable = false)
    private boolean usedInAnswer = true;
}
