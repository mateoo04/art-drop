package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtworkJpaRepository extends JpaRepository<Artwork, Long> {
    List<Artwork> findByMediumContainingIgnoreCase(String medium);

    Optional<Artwork> findByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);

    long deleteByTitleIgnoreCase(String title);

    List<Artwork> findByAuthor_IdOrderByPublishedAtDesc(Long authorId);

    long countByAuthor_Id(Long authorId);
}
