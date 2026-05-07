package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeJpaRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByStatus(ChallengeStatus status);

    @Query("SELECT c FROM Challenge c WHERE " +
            "LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "(c.description IS NOT NULL AND LOWER(c.description) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
            "(c.theme IS NOT NULL AND LOWER(c.theme) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY CASE c.status " +
            "WHEN hr.tvz.artdrop.artdropapp.model.ChallengeStatus.ACTIVE THEN 0 " +
            "WHEN hr.tvz.artdrop.artdropapp.model.ChallengeStatus.UPCOMING THEN 1 " +
            "ELSE 2 END, c.endsAt ASC")
    List<Challenge> searchChallenges(@Param("q") String q, Pageable pageable);
}
