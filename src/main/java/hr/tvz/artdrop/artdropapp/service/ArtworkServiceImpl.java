package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.repository.ArtworkRepository;
import org.springframework.stereotype.Service;

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