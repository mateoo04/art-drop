package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeKind;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
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
        Comparator<Challenge> byStatus = Comparator.comparingInt(c -> statusRank(c.getStatus()));
        Comparator<Challenge> featuredFirst = Comparator.comparingInt(c ->
                c.getKind() == ChallengeKind.FEATURED ? 0 : 1);
        Comparator<Challenge> byStartsAtDesc = Comparator
                .comparing((Challenge c) -> c.getStartsAt() == null ? LocalDateTime.MIN : c.getStartsAt())
                .reversed();
        return challengeRepository.findAll()
                .stream()
                .sorted(byStatus.thenComparing(featuredFirst).thenComparing(byStartsAtDesc))
                .map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS))
                .toList();
    }

    private static int statusRank(ChallengeStatus status) {
        if (status == ChallengeStatus.ACTIVE) return 0;
        if (status == ChallengeStatus.UPCOMING) return 1;
        if (status == ChallengeStatus.ENDED) return 2;
        return 3;
    }

    @Override
    public Optional<ChallengeDTO> findById(Long id) {
        return challengeRepository.findById(id).map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS));
    }

    @Override
    public List<SubmissionThumbnailDTO> findSubmissions(Long challengeId, int limit, int offset, String sort) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        int page = Math.max(0, offset) / safeLimit;
        PageRequest pageRequest = PageRequest.of(page, safeLimit);
        List<ChallengeSubmission> submissions = "top".equalsIgnoreCase(sort)
                ? submissionRepository.findByChallengeIdOrderByLikeCountDesc(challengeId, pageRequest)
                : submissionRepository.findByChallengeIdOrderBySubmittedAtDesc(challengeId, pageRequest);
        return submissions.stream().map(this::mapToThumbnail).toList();
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
                artwork == null ? null : artwork.getCoverPublicId(),
                artwork == null ? null : artwork.getTitle() + " - " + artwork.getMedium(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug()
        );
    }
}
