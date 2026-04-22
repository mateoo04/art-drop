package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionJpaRepository extends JpaRepository<Collection, Long> {
}
