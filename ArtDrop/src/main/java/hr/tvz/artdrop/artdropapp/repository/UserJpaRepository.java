package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findBySlug(String slug);
}
