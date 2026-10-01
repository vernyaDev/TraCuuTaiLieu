package vn.utc.tracuu.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnCitationRepository extends JpaRepository<TurnCitation, Long> {
    List<TurnCitation> findByTurnIdOrderByRank(Long turnId);
}
