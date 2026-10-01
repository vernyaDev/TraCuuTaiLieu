package vn.utc.tracuu.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OverlapPairRepository
        extends JpaRepository<DocumentOverlapPair, DocumentOverlapPair.Key> {

    Page<DocumentOverlapPair> findByDecisionOrderBySimilarityDesc(
            OverlapDecision decision, Pageable pageable);

    @Query("select p from DocumentOverlapPair p where p.docAId = :a and p.docBId = :b")
    Optional<DocumentOverlapPair> findPair(@Param("a") Long a, @Param("b") Long b);

    /**
     * Các cặp đã được xác nhận CÙNG TỒN TẠI hợp lệ, cho một nhóm tài liệu.
     *
     * <p>{@code AC-003} của US-025 đòi lúc TRẢ LỜI phải biết cặp nào đã xác nhận cùng tồn tại để
     * kèm cảnh báo — tức quyết định theo cặp phải đọc được ngay trong đường sinh câu trả lời,
     * không chỉ ở màn quản trị.
     */
    @Query("""
           select p from DocumentOverlapPair p
           where p.decision = vn.utc.tracuu.knowledge.OverlapDecision.CUNG_TON_TAI
             and p.docAId in :ids and p.docBId in :ids
           """)
    List<DocumentOverlapPair> findCoexistingAmong(@Param("ids") List<Long> ids);
}
