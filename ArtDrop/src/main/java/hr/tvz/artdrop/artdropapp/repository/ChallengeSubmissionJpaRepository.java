package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChallengeSubmissionJpaRepository extends JpaRepository<ChallengeSubmission, Long> {

    @Query("""
            SELECT DISTINCT s.challenge.id FROM ChallengeSubmission s
            WHERE s.submittedBy = :userId
            """)
    List<Long> findDistinctChallengeIdsBySubmittedBy(@Param("userId") Long userId);
    List<ChallengeSubmission> findByChallengeIdOrderBySubmittedAtDesc(Long challengeId, Pageable pageable);

    @Query("""
            SELECT s FROM ChallengeSubmission s
            WHERE s.challenge.id = :challengeId
            ORDER BY s.artwork.likeCount DESC, s.submittedAt DESC
            """)
    List<ChallengeSubmission> findByChallengeIdOrderByLikeCountDesc(
            @Param("challengeId") Long challengeId, Pageable pageable);

    long countByChallengeId(Long challengeId);

    Optional<ChallengeSubmission> findByChallengeIdAndArtworkId(Long challengeId, Long artworkId);

    Optional<ChallengeSubmission> findFirstByArtworkIdAndChallenge_StatusNot(
            Long artworkId, ChallengeStatus status);

    @Query("""
            SELECT a.id FROM Artwork a
            WHERE a.author.id = :authorId
              AND a.publishedAt >= :since
              AND NOT EXISTS (
                  SELECT 1 FROM ChallengeSubmission s
                  WHERE s.artwork.id = a.id AND s.challenge.status <> 'ENDED'
              )
            ORDER BY a.publishedAt DESC
            """)
    List<Long> findEligibleArtworkIds(
            @Param("authorId") Long authorId,
            @Param("since") java.time.LocalDateTime since);

    @Query("""
            SELECT c.id FROM Challenge c
            WHERE c.status = 'ACTIVE'
              AND c.startsAt <= :artworkPublishedAt
              AND NOT EXISTS (
                  SELECT 1 FROM ChallengeSubmission s
                  WHERE s.challenge.id = c.id AND s.artwork.id = :artworkId
              )
            ORDER BY c.endsAt ASC
            """)
    List<Long> findEligibleChallengeIdsForArtwork(
            @Param("artworkId") Long artworkId,
            @Param("artworkPublishedAt") java.time.LocalDateTime artworkPublishedAt);
}
