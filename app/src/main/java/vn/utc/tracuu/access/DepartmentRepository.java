package vn.utc.tracuu.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query(value = "select count(*) from user_departments where department_id = :id", nativeQuery = true)
    long countMembers(@Param("id") Long id);

    @Query(value = "select count(*) from document_acl where subject_type = 'PHONG_BAN' and subject_id = :id",
           nativeQuery = true)
    long countAclEntries(@Param("id") Long id);
}
