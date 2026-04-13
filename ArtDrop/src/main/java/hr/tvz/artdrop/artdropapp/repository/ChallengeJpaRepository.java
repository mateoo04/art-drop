package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeJpaRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByArtworkId(Long artworkId);
}
