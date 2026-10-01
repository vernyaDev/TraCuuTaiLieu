package vn.utc.tracuu.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentAclRepository extends JpaRepository<DocumentAcl, Long> {

    List<DocumentAcl> findByDocumentId(Long documentId);

    List<DocumentAcl> findBySubjectTypeAndSubjectId(SubjectType subjectType, Long subjectId);

    @Modifying
    @Query("delete from DocumentAcl a where a.documentId = :docId and a.subjectType = :type "
         + "and a.subjectId = :subjectId and a.effect = :effect")
    int deleteEntry(@Param("docId") Long docId,
                    @Param("type") SubjectType type,
                    @Param("subjectId") Long subjectId,
                    @Param("effect") AclEffect effect);

    @Modifying
    @Query("delete from DocumentAcl a where a.subjectType = 'PHONG_BAN' and a.subjectId = :deptId")
    int deleteByDepartment(@Param("deptId") Long deptId);
}
