package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT DISTINCT a.medium FROM Artwork a WHERE a.medium IS NOT NULL ORDER BY a.medium")
    List<String> findDistinctMediums();
}
