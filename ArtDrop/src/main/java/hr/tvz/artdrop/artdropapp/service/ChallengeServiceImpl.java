package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeJpaRepository challengeJpaRepository;

    public ChallengeServiceImpl(ChallengeJpaRepository challengeJpaRepository) {
        this.challengeJpaRepository = challengeJpaRepository;
    }

    @Override
    public List<ChallengeDTO> findByArtworkId(Long artworkId) {
        return challengeJpaRepository.findByArtworkId(artworkId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private ChallengeDTO mapToDto(Challenge challenge) {
        return new ChallengeDTO(
                challenge.getId(),
                challenge.getArtwork().getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getTheme(),
                challenge.getStartsAt(),
                challenge.getEndsAt(),
                challenge.getStatus()
        );
    }
}
