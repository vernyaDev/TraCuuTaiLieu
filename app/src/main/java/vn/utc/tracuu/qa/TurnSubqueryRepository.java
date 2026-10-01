package vn.utc.tracuu.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnSubqueryRepository extends JpaRepository<TurnSubquery, Long> {
    List<TurnSubquery> findByTurnIdOrderByOrdinal(Long turnId);
}
