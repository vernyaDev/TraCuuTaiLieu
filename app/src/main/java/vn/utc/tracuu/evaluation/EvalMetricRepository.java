package vn.utc.tracuu.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvalMetricRepository extends JpaRepository<EvalMetric, Long> {
    List<EvalMetric> findByRunId(Long runId);
}
