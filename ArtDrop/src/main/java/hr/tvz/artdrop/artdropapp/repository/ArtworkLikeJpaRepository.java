package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.ArtworkLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ArtworkLikeJpaRepository extends JpaRepository<ArtworkLike, Long> {
    boolean existsByArtworkIdAndUserId(Long artworkId, Long userId);

    long deleteByArtworkIdAndUserId(Long artworkId, Long userId);

    @Query("SELECT l.artworkId FROM ArtworkLike l WHERE l.userId = :userId AND l.artworkId IN :artworkIds")
    List<Long> findArtworkIdsLikedByUser(@Param("userId") Long userId, @Param("artworkIds") Collection<Long> artworkIds);
}
