package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;

import java.util.List;
import java.util.Optional;

public interface ArtworkRepository {

    List<Artwork> findAll();

    Optional<Artwork> findById(Long id);

    List<Artwork> findByTitle(String val);
}
