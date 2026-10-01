package vn.utc.tracuu.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TurnWarningRepository extends JpaRepository<TurnWarning, Long> {
    List<TurnWarning> findByTurnId(Long turnId);
}
