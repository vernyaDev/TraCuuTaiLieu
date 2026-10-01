package vn.utc.tracuu.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvalResultRepository extends JpaRepository<EvalResult, Long> {

    Optional<EvalResult> findByRunIdAndQuestionId(Long runId, Long questionId);

    List<EvalResult> findByRunId(Long runId);

    /**
     * Câu chưa có kết quả trong lượt chạy.
     *
     * <p>Đây là toàn bộ cơ chế "chạy tiếp phần dở dang": một dòng cho mỗi câu trong mỗi lượt
     * nên việc chạy tiếp chỉ là truy vấn tìm câu còn thiếu, không cần cơ chế điểm dừng riêng.
     */
    @Query("""
           select q from GroundTruthQuestion q
           where q.questionSetVersion = :version
             and not exists (
                 select 1 from EvalResult r
                 where r.runId = :runId and r.questionId = q.id)
           order by q.ordinal
           """)
    List<GroundTruthQuestion> findPending(@Param("runId") Long runId,
                                          @Param("version") String version);
}
