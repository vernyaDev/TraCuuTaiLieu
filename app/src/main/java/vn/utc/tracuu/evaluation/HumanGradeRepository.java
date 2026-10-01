package vn.utc.tracuu.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HumanGradeRepository extends JpaRepository<HumanGrade, Long> {

    Optional<HumanGrade> findByResultIdAndGrader(Long resultId, String grader);

    /**
     * Cặp (điểm máy, điểm người) của một lượt chạy — nguồn duy nhất cho phép so hai bên.
     *
     * <p>Chỉ lấy những kết quả CÓ CẢ HAI điểm. Kết quả chỉ có một bên không nói được gì về độ
     * lệch, và đưa nó vào phép tính trung bình là làm loãng đúng con số cần đọc.
     */
    @Query("""
           select r.autoScore, h.score
           from EvalResult r join HumanGrade h on h.resultId = r.id
           where r.runId = :runId and r.autoScore is not null
           """)
    List<Object[]> autoVersusHuman(@Param("runId") Long runId);
}
