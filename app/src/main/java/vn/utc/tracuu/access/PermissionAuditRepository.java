package vn.utc.tracuu.access;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PermissionAuditRepository extends JpaRepository<PermissionAudit, Long> {

    Page<PermissionAudit> findByTargetTypeAndTargetIdOrderByAtDesc(
            String targetType, Long targetId, Pageable pageable);

    @Modifying
    @Query("delete from PermissionAudit a where a.at < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
