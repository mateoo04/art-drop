package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;

import java.util.List;

public interface ChallengeService {
    List<ChallengeDTO> findByArtworkId(Long artworkId);
}
