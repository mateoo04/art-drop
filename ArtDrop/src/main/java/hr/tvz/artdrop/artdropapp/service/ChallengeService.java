package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;

import java.util.List;
import java.util.Optional;

public interface ChallengeService {
    List<ChallengeDTO> findAll(String viewerUsername);

    Optional<ChallengeDTO> findById(Long id, String viewerUsername);

    List<SubmissionThumbnailDTO> findSubmissions(Long challengeId, int limit, int offset, String sort);

    List<ChallengeDTO> searchChallenges(String query, int limit, int offset, String viewerUsername);

    SubmitResult submitArtwork(Long challengeId, Long artworkId, String username);

    WithdrawResult withdrawSubmission(Long challengeId, Long artworkId, String username);

    List<ArtworkDTO> findEligibleArtworksForChallenge(Long challengeId, String username);

    List<ChallengeDTO> findEligibleChallengesForArtwork(Long artworkId, String username);

    enum SubmitOutcome {
        CREATED,
        NOT_FOUND,
        UNAUTHENTICATED,
        FORBIDDEN_NOT_OWNER,
        FORBIDDEN_CHALLENGE_NOT_ACTIVE,
        FORBIDDEN_ARTWORK_TOO_OLD,
        CONFLICT_ALREADY_SUBMITTED,
        CONFLICT_IN_OTHER_CHALLENGE,
        CONFLICT_USER_ALREADY_HAS_ENTRY
    }

    record SubmitResult(SubmitOutcome outcome, Long submissionId) {}

    enum WithdrawOutcome {
        OK,
        NOT_FOUND,
        UNAUTHENTICATED,
        FORBIDDEN_NOT_OWNER,
        FORBIDDEN_CHALLENGE_ENDED
    }

    record WithdrawResult(WithdrawOutcome outcome) {}
}
