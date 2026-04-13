package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class JpaArtworkRepositoryAdapter implements ArtworkRepository {

    private final ArtworkJpaRepository artworkJpaRepository;

    public JpaArtworkRepositoryAdapter(ArtworkJpaRepository artworkJpaRepository) {
        this.artworkJpaRepository = artworkJpaRepository;
    }

    @Override
    public List<Artwork> findAll() {
        return artworkJpaRepository.findAll();
    }

    @Override
    public Optional<Artwork> findById(Long id) {
        return artworkJpaRepository.findById(id);
    }

    @Override
    public List<Artwork> findByMedium(String val) {
        return artworkJpaRepository.findByMediumContainingIgnoreCase(val);
    }

    @Override
    public Optional<Artwork> findOneByTitleIgnoreCase(String title) {
        return artworkJpaRepository.findByTitleIgnoreCase(title);
    }

    @Override
    public boolean existsByTitleIgnoreCase(String title) {
        return artworkJpaRepository.existsByTitleIgnoreCase(title);
    }

    @Override
    @Transactional
    public boolean deleteByTitleIgnoreCase(String title) {
        return artworkJpaRepository.deleteByTitleIgnoreCase(title) > 0;
    }

    @Override
    public void addArtwork(Artwork artwork) {
        artworkJpaRepository.save(artwork);
    }
}
