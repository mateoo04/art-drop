package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Collection;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository {

    List<Collection> findAll();

    Optional<Collection> findById(Long id);
}
