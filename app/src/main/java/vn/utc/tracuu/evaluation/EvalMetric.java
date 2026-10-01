package vn.utc.tracuu.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một chỉ số tổng hợp của một lượt chạy. */
@Entity
@Table(name = "eval_metrics")
@Getter @Setter @NoArgsConstructor
public class EvalMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "metric_name", nullable = false, length = 60)
    private String metricName;

    private Integer k;

    @Column(nullable = false)
    private float value;

    public EvalMetric(Long runId, String metricName, Integer k, float value) {
        this.runId = runId;
        this.metricName = metricName;
        this.k = k;
        this.value = value;
    }
}
