package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
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
                1L,
                "Golden Hour",
                "Digital Painting",
                "Landscape inspired by sunset colors.",
                "https://example.com/images/golden-hour.jpg",
                List.of(
                        new ArtworkImage(1L, null, "https://example.com/images/golden-hour.jpg", 0, true, "Cover image", LocalDateTime.of(2025, 10, 15, 18, 30)),
                        new ArtworkImage(2L, null, "https://example.com/images/golden-hour-closeup.jpg", 1, false, "Brush texture close-up", LocalDateTime.of(2025, 10, 15, 18, 31))
                ),
                List.of("sunset", "landscape", "digital"),
                LocalDateTime.of(2025, 10, 15, 18, 30),
                125,
                LocalDateTime.of(2025, 10, 15, 18, 20),
                LocalDateTime.of(2025, 10, 15, 18, 30),
                List.of(),
                List.of()
        ));

        artworks.add(new Artwork(
                2L,
                2L,
                "Urban Sketch",
                "Traditional Ink",
                "Quick ink sketch of a city street.",
                "https://example.com/images/urban-sketch.jpg",
                List.of(
                        new ArtworkImage(3L, null, "https://example.com/images/urban-sketch.jpg", 0, true, "Main scan", LocalDateTime.of(2025, 11, 2, 14, 0))
                ),
                List.of("city", "ink", "traditional"),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                87,
                LocalDateTime.of(2025, 11, 2, 13, 45),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                List.of(),
                List.of()
        ));

        artworks.add(new Artwork(
                3L,
                2L,
                "Rural Sketch",
                "Digital Photo",
                "Quick ink sketch of a city street.",
                "https://example.com/images/urban-sketch.jpg",
                List.of(
                        new ArtworkImage(4L, null, "https://example.com/images/urban-sketch.jpg", 0, true, "Reference shot", LocalDateTime.of(2025, 11, 2, 14, 0))
                ),
                List.of("city", "ink", "traditional"),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                87,
                LocalDateTime.of(2025, 11, 2, 13, 50),
                LocalDateTime.of(2025, 11, 2, 14, 0),
                List.of(),
                List.of()
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
