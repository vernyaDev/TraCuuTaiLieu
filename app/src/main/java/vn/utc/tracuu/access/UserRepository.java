package vn.utc.tracuu.access;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
           select u from User u left join fetch u.departments
           where lower(u.username) = lower(:username)
           """)
    Optional<User> findByUsernameWithDepartments(@Param("username") String username);

    @Query("""
           select u from User u left join fetch u.departments where u.id = :id
           """)
    Optional<User> findByIdWithDepartments(@Param("id") Long id);

    @Query("""
           select u from User u
           where (cast(:q as string) is null
                  or lower(u.fullName) like lower(concat('%', cast(:q as string), '%'))
                  or lower(u.username) like lower(concat('%', cast(:q as string), '%')))
             and (:active is null or u.active = :active)
           """)
    Page<User> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @Query("select d.id from User u join u.departments d where u.id = :userId")
    List<Long> findDepartmentIds(@Param("userId") Long userId);

    /**
     * Tên phòng ban của NHIỀU người trong MỘT truy vấn.
     *
     * <p>Màn danh sách tài khoản cần tên phòng ban của cả trang. Đọc qua {@code u.getDepartments()}
     * cho từng người thì hoặc nổ ra ngoài phiên Hibernate, hoặc — nếu bọc trong giao dịch — sinh
     * thêm một truy vấn cho mỗi dòng. Truy vấn này lấy đủ cho cả trang một lần, nên sửa được cả
     * hai vấn đề cùng lúc thay vì đổi cái này lấy cái kia.
     */
    @Query("""
           select u.id, d.name from User u join u.departments d
           where u.id in :userIds
           order by d.name
           """)
    List<Object[]> findDepartmentNamesFor(@Param("userIds") List<Long> userIds);

    long countByRole(Role role);
}
