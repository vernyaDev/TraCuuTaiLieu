package vn.utc.tracuu.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChunkSetRepository extends JpaRepository<ChunkSet, Long> {

    Optional<ChunkSet> findByActiveTrue();

    Optional<ChunkSet> findByStrategyName(String strategyName);
}
