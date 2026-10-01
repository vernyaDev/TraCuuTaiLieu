package vn.utc.tracuu.qa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TurnRepository extends JpaRepository<Turn, Long> {

    List<Turn> findByConversationIdOrderByOrdinal(Long conversationId);

    @Query("select coalesce(max(t.ordinal), 0) from Turn t where t.conversationId = :cid")
    int maxOrdinal(@Param("cid") Long conversationId);

    /** Các lượt gần nhất, để dựng ngữ cảnh viết lại câu tiếp nối. */
    @Query("""
           select t from Turn t
           where t.conversationId = :cid and t.status = 'XONG'
           order by t.ordinal desc
           """)
    List<Turn> findRecent(@Param("cid") Long conversationId, org.springframework.data.domain.Pageable page);
}
