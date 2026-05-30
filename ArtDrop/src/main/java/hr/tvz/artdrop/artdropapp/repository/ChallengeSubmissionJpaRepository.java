package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    long deleteByArtworkId(Long artworkId);

    Optional<ChallengeSubmission> findFirstByArtworkIdAndChallenge_StatusNot(
            Long artworkId, ChallengeStatus status);

    boolean existsByChallenge_IdAndSubmittedBy(Long challengeId, Long submittedBy);

    @Query("""
            SELECT s.artwork.id FROM ChallengeSubmission s
            WHERE s.challenge.id = :challengeId AND s.submittedBy = :userId
            """)
    Optional<Long> findArtworkIdByChallengeIdAndSubmittedBy(
            @Param("challengeId") Long challengeId, @Param("userId") Long userId);

    @Query("""
            SELECT s.challenge.id, s.artwork.id FROM ChallengeSubmission s
            WHERE s.submittedBy = :userId AND s.challenge.status <> 'ENDED'
            """)
    List<Object[]> findActiveEntriesBySubmittedBy(@Param("userId") Long userId);

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
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT c.id FROM Challenge c
            WHERE c.status = 'ACTIVE'
              AND c.startsAt <= :artworkPublishedAt
              AND NOT EXISTS (
                  SELECT 1 FROM ChallengeSubmission s
                  WHERE s.challenge.id = c.id
                    AND (s.artwork.id = :artworkId OR s.submittedBy = :userId)
              )
            ORDER BY c.endsAt ASC
            """)
    List<Long> findEligibleChallengeIdsForArtwork(
            @Param("artworkId") Long artworkId,
            @Param("userId") Long userId,
            @Param("artworkPublishedAt") LocalDateTime artworkPublishedAt);
}
