package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.repository.ArtworkRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkRepository artworkRepository;

    public ArtworkServiceImpl(ArtworkRepository artworkRepository) {
        this.artworkRepository = artworkRepository;
    }

    @Override
    public List<ArtworkDTO> findAll() {
        return artworkRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<ArtworkDTO> findById(Long id) {
        return artworkRepository.findById(id)
                .map(this::mapToDTO);
    }

    @Override
    public List<ArtworkDTO> findByMedium(String medium) {
        return artworkRepository.findByMedium(medium)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<ArtworkDTO> findOneByTitle(String title) {
        return artworkRepository.findOneByTitleIgnoreCase(title)
                .map(this::mapToDTO);
    }

    @Override
    public boolean createArtwork(ArtworkCommand command) {
        if (artworkRepository.existsByTitleIgnoreCase(command.title())) {
            return false;
        }
        Artwork artwork = new Artwork(
                null,
                command.title(),
                command.medium(),
                command.description(),
                command.imageUrl(),
                List.of(),
                LocalDateTime.now(),
                0
        );
        artworkRepository.addArtwork(artwork);
        return true;
    }

    @Override
    public boolean deleteByTitle(String title) {
        return artworkRepository.deleteByTitleIgnoreCase(title);
    }

    private ArtworkDTO mapToDTO(Artwork artwork) {
        return new ArtworkDTO(
                artwork.getTitle(),
                artwork.getMedium(),
                artwork.getTags(),
                artwork.getPublishedAt(),
                artwork.getLikeCount(),
                0
        );
    }
}
