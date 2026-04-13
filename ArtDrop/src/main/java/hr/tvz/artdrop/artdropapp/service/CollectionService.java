package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CollectionDTO;

import java.util.List;
import java.util.Optional;

public interface CollectionService {

    List<CollectionDTO> findAll();

    Optional<CollectionDTO> findById(Long id);
}
