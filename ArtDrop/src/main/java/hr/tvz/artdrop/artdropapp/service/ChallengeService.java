package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;

import java.util.List;
import java.util.Optional;

public interface ChallengeService {
    List<ChallengeDTO> findAll();

    Optional<ChallengeDTO> findById(Long id);

    List<SubmissionThumbnailDTO> findSubmissions(Long challengeId, int limit, int offset);
}
