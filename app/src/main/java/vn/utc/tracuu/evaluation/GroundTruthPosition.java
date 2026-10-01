package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một vị trí đáp án chấp nhận được.
 *
 * <p>Tách khỏi câu hỏi vì <strong>một câu hỏi có nhiều vị trí đều đúng</strong>. Vị trí khai
 * bằng mã tài liệu cộng khoảng ký tự, KHÔNG khai bằng mã đoạn — nhờ vậy cùng một bộ câu hỏi
 * ánh xạ được sang mọi tập đoạn mà không phải soạn lại đáp án cho từng chiến lược tách.
 */
@Entity
@Table(name = "ground_truth_positions")
@Getter @Setter @NoArgsConstructor
public class GroundTruthPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "char_start", nullable = false)
    private int charStart;

    @Column(name = "char_end", nullable = false)
    private int charEnd;
}
