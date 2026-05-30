package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "authorities")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithAuthorities(@Param("id") Long id);

    Optional<User> findBySlug(String slug);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsBySlug(String slug);

    @Query(
        "SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "ORDER BY u.username ASC"
    )
    Page<User> searchByUsernameDisplayNameOrEmail(@Param("q") String q, Pageable pageable);

    @Query(value = """
            SELECT * FROM app_user u
            WHERE u.enabled = TRUE
              AND (u.display_name ILIKE '%' || :q || '%'
                OR u.slug         ILIKE '%' || :q || '%'
                OR u.username     ILIKE '%' || :q || '%'
                OR u.display_name % :q
                OR u.slug         % :q
                OR u.username     % :q)
            ORDER BY GREATEST(
                       similarity(u.display_name, :q),
                       similarity(u.slug,         :q),
                       similarity(u.username,     :q)
                     ) DESC,
                     u.display_name ASC
            """, nativeQuery = true)
    List<User> searchPublic(@Param("q") String q, Pageable pageable);
}
