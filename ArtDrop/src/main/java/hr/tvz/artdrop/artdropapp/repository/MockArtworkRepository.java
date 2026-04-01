package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class MockArtworkRepository implements ArtworkRepository {

    private final List<Artwork> artworks = new ArrayList<>();

    public MockArtworkRepository() {
        artworks.add(new Artwork(
                1L,
                "Golden Hour",
                "Digital Painting",
                "Landscape inspired by sunset colors.",
                "https://example.com/images/golden-hour.jpg",
                List.of("sunset", "landscape", "digital"),
                LocalDateTime.of(2025, 10, 15, 18, 30),
                125
        ));

        artworks.add(new Artwork(
                2L,
                "Urban Sketch",
                "Traditional Ink",
                "Quick ink sketch of a city street.",
                "https://example.com/images/urban-sketch.jpg",
                List.of("city", "ink", "traditional"),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                87
        ));

        artworks.add(new Artwork(
                3L,
                "Rural Sketch",
                "Digital Photo",
                "Quick ink sketch of a city street.",
                "https://example.com/images/urban-sketch.jpg",
                List.of("city", "ink", "traditional"),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                87
        ));
    }

    @Override
    public List<Artwork> findAll() {
        return artworks;
    }

    @Override
    public Optional<Artwork> findById(Long id) {
        return artworks.stream()
                .filter(artwork -> artwork.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Artwork> findByMedium(String val) {
        return artworks.stream()
                .filter(artwork -> artwork.getMedium().toLowerCase().contains(val.toLowerCase()))
                .toList();
    }

    @Override
    public Optional<Artwork> findOneByTitleIgnoreCase(String title) {
        if (title == null) {
            return Optional.empty();
        }
        String needle = title.toLowerCase();
        return artworks.stream()
                .filter(a -> a.getTitle().toLowerCase().equals(needle))
                .findFirst();
    }

    @Override
    public boolean existsByTitleIgnoreCase(String title) {
        return findOneByTitleIgnoreCase(title).isPresent();
    }

    @Override
    public boolean deleteByTitleIgnoreCase(String title) {
        return artworks.removeIf(a -> a.getTitle().equalsIgnoreCase(title));
    }

    @Override
    public void addArtwork(Artwork artwork) {
        Long newId = artworks.stream()
                .map(Artwork::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(0L) + 1;

        artwork.setId(newId);

        artworks.add(artwork);
    }
}
