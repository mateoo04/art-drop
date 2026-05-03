package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.FeedSeen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface FeedSeenJpaRepository extends JpaRepository<FeedSeen, Long> {

    @Query("SELECT s.artworkId, MAX(s.seenAt) FROM FeedSeen s " +
            "WHERE s.viewerId = :viewerId AND s.seenAt > :since AND s.artworkId IN :artworkIds " +
            "GROUP BY s.artworkId")
    List<Object[]> findLastSeenByArtwork(
            @Param("viewerId") Long viewerId,
            @Param("since") LocalDateTime since,
            @Param("artworkIds") Collection<Long> artworkIds);

    boolean existsByViewerIdAndArtworkIdAndSeenAtAfter(Long viewerId, Long artworkId, LocalDateTime seenAfter);

    @Modifying
    @Transactional
    @Query("DELETE FROM FeedSeen s WHERE s.seenAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
