package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentJpaRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.artwork.id = :artworkId " +
            "AND c.parentCommentId IS NULL " +
            "AND (c.isDeleted = false OR c.isDeleted IS NULL) " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findTopLevelByArtwork(@Param("artworkId") Long artworkId, Pageable pageable);
}
