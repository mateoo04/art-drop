package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.FeaturedChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FeaturedChallengeJpaRepository extends JpaRepository<FeaturedChallenge, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FeaturedChallenge f where f.id = 1")
    Optional<FeaturedChallenge> findSingletonForUpdate();
}
