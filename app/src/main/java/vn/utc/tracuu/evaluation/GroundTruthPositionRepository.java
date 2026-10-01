package vn.utc.tracuu.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroundTruthPositionRepository extends JpaRepository<GroundTruthPosition, Long> {
    List<GroundTruthPosition> findByQuestionId(Long questionId);
}
