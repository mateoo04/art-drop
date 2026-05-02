package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeSubmissionJpaRepository extends JpaRepository<ChallengeSubmission, Long> {
    List<ChallengeSubmission> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId, Pageable pageable);

    @Query("""
            SELECT s FROM ChallengeSubmission s
            WHERE s.challenge.id = :challengeId
            ORDER BY s.artwork.likeCount DESC, s.submittedAt DESC
            """)
    List<ChallengeSubmission> findByChallengeIdOrderByLikeCountDesc(
            @Param("challengeId") Long challengeId, Pageable pageable);

    long countByChallengeId(Long challengeId);
}
