package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeSubmissionJpaRepository extends JpaRepository<ChallengeSubmission, Long> {
    List<ChallengeSubmission> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId, Pageable pageable);

    long countByChallengeId(Long challengeId);
}
