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
    public List<Artwork> findByTitle(String val) {
        return artworks.stream()
                .filter(artwork -> artwork.getTitle().toLowerCase().contains(val.toLowerCase()))
                .toList();
    }
}
