package vn.utc.tracuu.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IngestionBatchRepository extends JpaRepository<IngestionBatch, Long> {
    Optional<IngestionBatch> findByTrackingId(UUID trackingId);
}
