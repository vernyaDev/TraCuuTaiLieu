package vn.utc.tracuu.gap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface RefusalLogRepository extends JpaRepository<RefusalLog, Long> {

    @Query("""
           select r from RefusalLog r
           where (:from is null or r.createdAt >= :from)
             and (:to is null or r.createdAt <= :to)
             and (cast(:reason as string) is null or r.reason = :reason)
           order by r.createdAt desc
           """)
    Page<RefusalLog> search(@Param("from") Instant from, @Param("to") Instant to,
                            @Param("reason") String reason, Pageable page);

    @Query(value = """
           SELECT r.* FROM refusal_log r
           JOIN gap_members m ON m.refusal_id = r.id
           WHERE m.cluster_id = :clusterId
           ORDER BY r.created_at DESC
           """, nativeQuery = true)
    List<RefusalLog> findByCluster(@Param("clusterId") Long clusterId);

    /** Dọn theo vòng đời dữ liệu: câu hỏi của người dùng là dữ liệu nhạy cảm. */
    @Modifying
    @Query("delete from RefusalLog r where r.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
