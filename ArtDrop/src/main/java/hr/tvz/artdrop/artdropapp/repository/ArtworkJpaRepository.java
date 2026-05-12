package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtworkJpaRepository extends JpaRepository<Artwork, Long> {
    List<Artwork> findByMediumContainingIgnoreCase(String medium);

    List<Artwork> findByMediumContainingIgnoreCase(String medium, Pageable pageable);

    Optional<Artwork> findByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);

    long deleteByTitleIgnoreCase(String title);

    List<Artwork> findByAuthor_IdOrderByPublishedAtDesc(Long authorId);

    List<Artwork> findByAuthor_IdOrderByPublishedAtDesc(Long authorId, Pageable pageable);

    long countByAuthor_Id(Long authorId);

    @Query("SELECT a FROM Artwork a WHERE a.author.id IN " +
            "(SELECT f.followeeId FROM UserFollow f WHERE f.followerId = :viewerId) " +
            "ORDER BY a.publishedAt DESC")
    List<Artwork> findCircleFeed(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("SELECT a FROM Artwork a " +
            "WHERE a.author.id <> :viewerId " +
            "AND (a.publishedAt >= :recentSince " +
            "     OR (a.publishedAt >= :circleSince " +
            "         AND a.author.id IN (SELECT f.followeeId FROM UserFollow f WHERE f.followerId = :viewerId))) " +
            "AND (CAST(:medium AS string) IS NULL OR LOWER(a.medium) LIKE LOWER(CONCAT('%', CAST(:medium AS string), '%'))) " +
            "ORDER BY a.publishedAt DESC")
    List<Artwork> findRankingCandidates(
            @Param("viewerId") Long viewerId,
            @Param("recentSince") java.time.LocalDateTime recentSince,
            @Param("circleSince") java.time.LocalDateTime circleSince,
            @Param("medium") String medium,
            Pageable pageable);

    List<Artwork> findByIdIn(Collection<Long> ids);

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediums();

    @Query("SELECT a FROM Artwork a WHERE a.author.id <> :viewerId ORDER BY a.publishedAt DESC")
    List<Artwork> findAllExcludingAuthor(@Param("viewerId") Long viewerId, Pageable pageable);

    @Query("SELECT a FROM Artwork a WHERE a.author.id <> :viewerId AND LOWER(a.medium) LIKE LOWER(CONCAT('%', :medium, '%')) ORDER BY a.publishedAt DESC")
    List<Artwork> findByMediumExcludingAuthor(
            @Param("medium") String medium,
            @Param("viewerId") Long viewerId,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.author.id = :authorId " +
            "AND a.saleState IN (hr.tvz.artdrop.artdropapp.model.SaleState.AVAILABLE, hr.tvz.artdrop.artdropapp.model.SaleState.RESERVED)")
    long countListedByAuthorId(Long authorId);

    @Query(value = """
            SELECT a.*,
                   (SELECT COUNT(*) FROM artwork_like al WHERE al.artwork_id = a.id) AS likeCount
            FROM artwork a
            WHERE a.search_tsv @@ to_tsquery('english', :tsq)
               OR EXISTS (
                   SELECT 1 FROM artwork_tags t
                   WHERE t.artwork_id = a.id AND t.tag ILIKE '%' || :q || '%'
               )
               OR EXISTS (
                   SELECT 1 FROM app_user u
                   WHERE u.id = a.author_id AND u.display_name ILIKE '%' || :q || '%'
               )
            ORDER BY ts_rank_cd(a.search_tsv, to_tsquery('english', :tsq)) DESC,
                     a.published_at DESC
            """, nativeQuery = true)
    List<Artwork> searchArtworks(@Param("tsq") String tsq, @Param("q") String q, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Artwork a SET a.saleState = hr.tvz.artdrop.artdropapp.model.SaleState.DRAFT, a.price = NULL, a.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE a.author.id = :authorId " +
            "AND a.saleState IN (hr.tvz.artdrop.artdropapp.model.SaleState.AVAILABLE, hr.tvz.artdrop.artdropapp.model.SaleState.RESERVED)")
    int unlistAllForAuthor(Long authorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Artwork a WHERE a.id = :id")
    Optional<Artwork> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT a FROM Artwork a WHERE a.saleState = hr.tvz.artdrop.artdropapp.model.SaleState.RESERVED " +
            "AND a.reservedUntil < :now")
    List<Artwork> findExpiredReservations(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM Artwork a " +
            "WHERE a.reservedByUserId = :userId " +
            "AND a.saleState = hr.tvz.artdrop.artdropapp.model.SaleState.RESERVED " +
            "AND a.reservedUntil > :now")
    Optional<Artwork> findActiveReservationByUser(@Param("userId") Long userId,
                                                  @Param("now") LocalDateTime now);
}
