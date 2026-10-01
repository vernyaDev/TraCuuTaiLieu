package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một câu hỏi trong bộ chuẩn. */
@Entity
@Table(name = "ground_truth_questions")
@Getter @Setter @NoArgsConstructor
public class GroundTruthQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_set_version", nullable = false, length = 40)
    private String questionSetVersion;

    @Column(nullable = false)
    private int ordinal;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(nullable = false, length = 20)
    private String complexity = "DON_GIAN";

    @Column(name = "expected_answer", columnDefinition = "text")
    private String expectedAnswer;

    @Column(length = 1000)
    private String note;
}
