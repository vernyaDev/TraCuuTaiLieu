package vn.utc.tracuu.gap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GapClusterRepository extends JpaRepository<GapCluster, Long> {

    @Query("""
           select g from GapCluster g
           where (cast(:status as string) is null or g.status = :status)
             and (cast(:reason as string) is null or g.reason = :reason)
           order by g.memberCount desc, g.lastSeen desc
           """)
    Page<GapCluster> search(@Param("status") String status, @Param("reason") String reason,
                            Pageable page);
}
