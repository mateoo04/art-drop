package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CollectionDTO;
import hr.tvz.artdrop.artdropapp.model.Collection;
import hr.tvz.artdrop.artdropapp.repository.CollectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionServiceImpl(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    @Override
    public List<CollectionDTO> findAll() {
        return collectionRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<CollectionDTO> findById(Long id) {
        return collectionRepository.findById(id)
                .map(this::mapToDTO);
    }

    private CollectionDTO mapToDTO(Collection collection) {
        return new CollectionDTO(
                collection.getId(),
                collection.getName(),
                collection.getDescription(),
                collection.getArtworkIds(),
                collection.getCreatedAt(),
                collection.getIsPublic()
        );
    }
}
