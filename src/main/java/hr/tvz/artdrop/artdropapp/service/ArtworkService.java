package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;

import java.util.List;
import java.util.Optional;

public interface ArtworkService {

    List<ArtworkDTO> findAll();

    Optional<ArtworkDTO> findById(Long id);

    List<ArtworkDTO> findByMedium(String val);

    Optional<ArtworkDTO> findOneByTitle(String title);

    boolean createArtwork(ArtworkCommand command);

    boolean deleteByTitle(String title);
}
