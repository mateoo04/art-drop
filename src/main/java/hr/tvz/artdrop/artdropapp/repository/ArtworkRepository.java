package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository {

    List<Artwork> findAll();

    Optional<Artwork> findById(Long id);

    List<Artwork> findByMedium(String val);

    Optional<Artwork> findOneByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);

    boolean deleteByTitleIgnoreCase(String title);

    void addArtwork(Artwork artwork);
}
