package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findBySlug(String slug);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsBySlug(String slug);

    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "ORDER BY u.username ASC"
    )
    org.springframework.data.domain.Page<User> searchByUsernameDisplayNameOrEmail(
            @org.springframework.data.repository.query.Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
}
