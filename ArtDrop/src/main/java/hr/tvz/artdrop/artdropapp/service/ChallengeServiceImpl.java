package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChallengeServiceImpl implements ChallengeService {

    private static final int DEFAULT_PREVIEW_SUBMISSIONS = 6;

    private final ChallengeJpaRepository challengeRepository;
    private final ChallengeSubmissionJpaRepository submissionRepository;

    public ChallengeServiceImpl(
            ChallengeJpaRepository challengeRepository,
            ChallengeSubmissionJpaRepository submissionRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public List<ChallengeDTO> findAll() {
        return challengeRepository.findAllByOrderByStatusAscStartsAtDesc()
                .stream()
                .map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS))
                .toList();
    }

    @Override
    public Optional<ChallengeDTO> findById(Long id) {
        return challengeRepository.findById(id).map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS));
    }

    @Override
    public List<SubmissionThumbnailDTO> findSubmissions(Long challengeId, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        int page = Math.max(0, offset) / safeLimit;
        return submissionRepository
                .findByChallengeIdOrderBySubmittedAtDesc(challengeId, PageRequest.of(page, safeLimit))
                .stream()
                .map(this::mapToThumbnail)
                .toList();
    }

    private ChallengeDTO mapToDto(Challenge challenge, int previewLimit) {
        long total = submissionRepository.countByChallengeId(challenge.getId());
        List<SubmissionThumbnailDTO> preview = submissionRepository
                .findByChallengeIdOrderBySubmittedAtDesc(challenge.getId(), PageRequest.of(0, previewLimit))
                .stream()
                .map(this::mapToThumbnail)
                .toList();
        return new ChallengeDTO(
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                challenge.getQuote(),
                challenge.getKind() == null ? null : challenge.getKind().name(),
                challenge.getStatus() == null ? null : challenge.getStatus().name(),
                challenge.getTheme(),
                challenge.getCoverImageUrl(),
                challenge.getStartsAt(),
                challenge.getEndsAt(),
                total,
                preview
        );
    }

    private SubmissionThumbnailDTO mapToThumbnail(ChallengeSubmission submission) {
        Artwork artwork = submission.getArtwork();
        User author = artwork == null ? null : artwork.getAuthor();
        return new SubmissionThumbnailDTO(
                submission.getId(),
                artwork == null ? null : artwork.getId(),
                artwork == null ? null : artwork.getTitle(),
                artwork == null ? null : artwork.getImageUrl(),
                artwork == null ? null : artwork.getTitle() + " - " + artwork.getMedium(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug()
        );
    }
}
