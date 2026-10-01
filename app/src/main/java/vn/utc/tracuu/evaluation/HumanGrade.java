package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Điểm do NGƯỜI chấm cho một kết quả.
 *
 * <p>Bảng này tách khỏi bảng kết quả chứ không ghi đè lên nó, và đó là điểm quan trọng: thứ có
 * giá trị không phải điểm cuối cùng mà là <strong>độ lệch giữa máy chấm và người chấm</strong>.
 * Một bộ chấm tự động lệch xa người chấm thì mọi con số nó sinh ra đều phải đọc kèm cảnh báo;
 * ghi đè điểm máy bằng điểm người là xoá mất chính phép so ấy.
 */
@Entity
@Table(name = "human_grades")
@Getter @Setter @NoArgsConstructor
public class HumanGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(nullable = false, length = 120)
    private String grader;

    @Column(nullable = false)
    private float score;

    @Column(length = 1000)
    private String note;

    @Column(name = "graded_at", nullable = false)
    private Instant gradedAt = Instant.now();
}
