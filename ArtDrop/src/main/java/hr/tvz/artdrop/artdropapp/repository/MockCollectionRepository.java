package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Collection;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MockCollectionRepository implements CollectionRepository {

    private final List<Collection> collections = List.of(
            new Collection(
                    1L,
                    1L,
                    "Urban Scenes",
                    "Street-focused sketches and photo references.",
                    List.of(2L, 3L),
                    LocalDateTime.of(2026, 1, 15, 10, 30),
                    LocalDateTime.of(2026, 1, 15, 10, 30),
                    true
            ),
            new Collection(
                    2L,
                    1L,
                    "Sunset Studies",
                    "Warm color palette experiments for landscape compositions.",
                    List.of(1L),
                    LocalDateTime.of(2026, 2, 3, 18, 0),
                    LocalDateTime.of(2026, 2, 3, 18, 0),
                    false
            ),
            new Collection(
                    3L,
                    2L,
                    "Print Drafts",
                    "Mixed-media drafts prepared for print review.",
                    List.of(3L, 1L),
                    LocalDateTime.of(2026, 3, 10, 12, 45),
                    LocalDateTime.of(2026, 3, 10, 12, 45),
                    true
            )
    );

    @Override
    public List<Collection> findAll() {
        return collections;
    }

    @Override
    public Optional<Collection> findById(Long id) {
        return collections.stream()
                .filter(collection -> collection.getId().equals(id))
                .findFirst();
    }
}
