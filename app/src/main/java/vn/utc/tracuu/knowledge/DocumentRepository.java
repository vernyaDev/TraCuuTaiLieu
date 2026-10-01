package vn.utc.tracuu.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByContentHash(String contentHash);

    List<Document> findBySupersedesDocumentId(Long supersededId);

    /**
     * Danh sách tài liệu cho màn xem xét, có lọc và phân trang.
     *
     * <p>{@code expiringBefore} phục vụ nhóm <em>sắp hết hiệu lực</em>: cố ý loại cả tài liệu
     * khai "không có thời hạn" lẫn tài liệu chưa khai thời hạn — cái sau là <strong>thiếu thông
     * tin</strong>, không phải sắp hết hạn, và trộn hai thứ là cách sinh cảnh báo giả hàng loạt.
     */
    /*
     * cast(:q as string) là BẮT BUỘC, không phải trang trí.
     *
     * Thiếu nó, PostgreSQL không suy được kiểu của tham số trong '%'||?||'%' và chọn toán tử
     * nối của bytea, rồi đổ với "function lower(bytea) does not exist". Lỗi xảy ra ở lúc
     * CHUẨN BỊ câu lệnh nên nó đổ kể cả khi tham số có giá trị — và vì đây là bộ lọc tuỳ chọn,
     * nó đổ ở đúng trường hợp mặc định: mở màn danh sách mà không lọc gì.
     * Phát hiện khi chạy thật 29/09.
     */
    @Query("""
           select d from Document d
           where (cast(:q as string) is null
                  or lower(d.title) like lower(concat('%', cast(:q as string), '%'))
                  or lower(d.originalFilename) like lower(concat('%', cast(:q as string), '%')))
             and (:status is null or d.status = :status)
             and (:categoryId is null or d.category.id = :categoryId)
             and (:expiringBefore is null
                  or (d.hasNoExpiry = false and d.effectiveTo is not null
                      and d.effectiveTo <= :expiringBefore))
           """)
    Page<Document> search(@Param("q") String q,
                          @Param("status") DocumentStatus status,
                          @Param("categoryId") Long categoryId,
                          @Param("expiringBefore") LocalDate expiringBefore,
                          Pageable pageable);

    @Query("select d from Document d where d.status = :status")
    List<Document> findByStatus(@Param("status") DocumentStatus status);

    /**
     * Tài liệu cùng cụm chủ đề, dùng làm BỘ LỌC THU HẸP khi dò chồng lấn.
     *
     * <p>Hai tài liệu khác cụm thì không cần so nữa — đây là toàn bộ vai trò của cụm chủ đề.
     * Quyết định về chồng lấn nằm ở {@link DocumentOverlapPair}, không nằm ở đây.
     */
    @Query("""
           select d from Document d
           where d.topicClusterId = :clusterId and d.id <> :excludeId
             and d.status = vn.utc.tracuu.knowledge.DocumentStatus.DA_LAP_CHI_MUC
           """)
    List<Document> findSameCluster(@Param("clusterId") Long clusterId,
                                   @Param("excludeId") Long excludeId);

    /** Số người mất quyền đọc nếu nâng nhãn mật lên {@code newSensitivity}. */
    @Query(value = """
           SELECT count(DISTINCT u.id) FROM users u
           WHERE u.clearance_level < :newSensitivity
             AND EXISTS (
               SELECT 1 FROM document_acl a
               WHERE a.document_id = :docId AND a.effect = 'CHO_PHEP'
                 AND ( (a.subject_type = 'NGUOI_DUNG' AND a.subject_id = u.id)
                    OR (a.subject_type = 'PHONG_BAN' AND a.subject_id IN
                        (SELECT department_id FROM user_departments WHERE user_id = u.id)) ))
           """, nativeQuery = true)
    long countUsersLosingAccess(@Param("docId") Long docId,
                                @Param("newSensitivity") int newSensitivity);

    /** Số trích dẫn đang trỏ tới tài liệu — con số hậu quả cho hộp thoại xác nhận xoá vĩnh viễn. */
    @Query(value = """
           SELECT count(*) FROM turn_citations tc
           JOIN chunks c ON c.id = tc.chunk_id
           WHERE c.document_id = :docId
           """, nativeQuery = true)
    long countCitations(@Param("docId") Long docId);

    long countByStatus(DocumentStatus status);
}
