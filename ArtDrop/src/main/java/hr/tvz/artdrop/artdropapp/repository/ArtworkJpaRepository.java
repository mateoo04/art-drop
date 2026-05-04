package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
            "AND (:medium IS NULL OR LOWER(a.medium) LIKE LOWER(CONCAT('%', :medium, '%'))) " +
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

    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.author.id = :authorId AND (a.saleStatus IS NOT NULL OR a.price IS NOT NULL)")
    long countListedByAuthorId(Long authorId);

    @Query("SELECT DISTINCT a FROM Artwork a LEFT JOIN a.tags t WHERE " +
            "(LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
            "(a.description IS NOT NULL AND LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
            "(LOWER(a.medium) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
            "(LOWER(a.author.displayName) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
            "(t IS NOT NULL AND LOWER(t) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY a.publishedAt DESC")
    List<Artwork> searchArtworks(@Param("q") String q, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Artwork a SET a.saleStatus = NULL, a.price = NULL, a.updatedAt = CURRENT_TIMESTAMP WHERE a.author.id = :authorId AND (a.saleStatus IS NOT NULL OR a.price IS NOT NULL)")
    int unlistAllForAuthor(Long authorId);
}
