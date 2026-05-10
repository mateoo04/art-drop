package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeKind;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChallengeServiceImpl implements ChallengeService {

    private static final int DEFAULT_PREVIEW_SUBMISSIONS = 6;

    private final ChallengeJpaRepository challengeRepository;
    private final ChallengeSubmissionJpaRepository submissionRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final UserJpaRepository userRepository;
    private final ArtworkService artworkService;

    public ChallengeServiceImpl(
            ChallengeJpaRepository challengeRepository,
            ChallengeSubmissionJpaRepository submissionRepository,
            ArtworkJpaRepository artworkRepository,
            UserJpaRepository userRepository,
            @Lazy ArtworkService artworkService
    ) {
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
        this.artworkRepository = artworkRepository;
        this.userRepository = userRepository;
        this.artworkService = artworkService;
    }

    @Override
    public List<ChallengeDTO> findAll(String viewerUsername) {
        Comparator<Challenge> byStatus = Comparator.comparingInt(c -> statusRank(c.getStatus()));
        Comparator<Challenge> featuredFirst = Comparator.comparingInt(c ->
                c.getKind() == ChallengeKind.FEATURED ? 0 : 1);
        Comparator<Challenge> byStartsAtDesc = Comparator
                .comparing((Challenge c) -> c.getStartsAt() == null ? LocalDateTime.MIN : c.getStartsAt())
                .reversed();
        Map<Long, Long> viewerEntries = loadViewerEntries(viewerUsername);
        return challengeRepository.findAll()
                .stream()
                .sorted(byStatus.thenComparing(featuredFirst).thenComparing(byStartsAtDesc))
                .map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS, viewerEntries))
                .toList();
    }

    private static int statusRank(ChallengeStatus status) {
        if (status == ChallengeStatus.ACTIVE) return 0;
        if (status == ChallengeStatus.UPCOMING) return 1;
        if (status == ChallengeStatus.ENDED) return 2;
        return 3;
    }

    @Override
    public Optional<ChallengeDTO> findById(Long id, String viewerUsername) {
        Map<Long, Long> viewerEntries = loadViewerEntryFor(id, viewerUsername);
        return challengeRepository.findById(id).map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS, viewerEntries));
    }

    @Override
    public List<ChallengeDTO> searchChallenges(String query, int limit, int offset, String viewerUsername) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        int safeOffset = Math.max(0, offset);
        PageRequest pageRequest = PageRequest.of(safeOffset / safeLimit, safeLimit);
        Map<Long, Long> viewerEntries = loadViewerEntries(viewerUsername);
        return challengeRepository.searchChallenges(trimmed, pageRequest).stream()
                .map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS, viewerEntries))
                .toList();
    }

    private Map<Long, Long> loadViewerEntries(String viewerUsername) {
        if (viewerUsername == null) return Map.of();
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : submissionRepository.findActiveEntriesBySubmittedBy(viewer.get().getId())) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<Long, Long> loadViewerEntryFor(Long challengeId, String viewerUsername) {
        if (viewerUsername == null) return Map.of();
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return Map.of();
        return submissionRepository
                .findArtworkIdByChallengeIdAndSubmittedBy(challengeId, viewer.get().getId())
                .map(artworkId -> Map.of(challengeId, artworkId))
                .orElse(Map.of());
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

    private ChallengeDTO mapToDto(Challenge challenge, int previewLimit, Map<Long, Long> viewerEntries) {
        long total = submissionRepository.countByChallengeId(challenge.getId());
        List<SubmissionThumbnailDTO> preview = submissionRepository
                .findByChallengeIdOrderBySubmittedAtDesc(challenge.getId(), PageRequest.of(0, previewLimit))
                .stream()
                .map(this::mapToThumbnail)
                .toList();
        Long viewerEntryArtworkId = viewerEntries.get(challenge.getId());
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
                preview,
                viewerEntryArtworkId != null,
                viewerEntryArtworkId
        );
    }

    @Override
    @Transactional
    public SubmitResult submitArtwork(Long challengeId, Long artworkId, String username) {
        if (username == null) {
            return new SubmitResult(SubmitOutcome.UNAUTHENTICATED, null);
        }
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return new SubmitResult(SubmitOutcome.UNAUTHENTICATED, null);
        }
        Optional<Challenge> challengeOpt = challengeRepository.findById(challengeId);
        if (challengeOpt.isEmpty()) {
            return new SubmitResult(SubmitOutcome.NOT_FOUND, null);
        }
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) {
            return new SubmitResult(SubmitOutcome.NOT_FOUND, null);
        }
        Challenge challenge = challengeOpt.get();
        Artwork artwork = artworkOpt.get();
        if (artwork.getAuthor() == null || !user.get().getId().equals(artwork.getAuthor().getId())) {
            return new SubmitResult(SubmitOutcome.FORBIDDEN_NOT_OWNER, null);
        }
        if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
            return new SubmitResult(SubmitOutcome.FORBIDDEN_CHALLENGE_NOT_ACTIVE, null);
        }
        if (artwork.getPublishedAt() == null
                || (challenge.getStartsAt() != null && artwork.getPublishedAt().isBefore(challenge.getStartsAt()))) {
            return new SubmitResult(SubmitOutcome.FORBIDDEN_ARTWORK_TOO_OLD, null);
        }
        if (submissionRepository.findByChallengeIdAndArtworkId(challengeId, artworkId).isPresent()) {
            return new SubmitResult(SubmitOutcome.CONFLICT_ALREADY_SUBMITTED, null);
        }
        if (submissionRepository.existsByChallenge_IdAndSubmittedBy(challengeId, user.get().getId())) {
            return new SubmitResult(SubmitOutcome.CONFLICT_USER_ALREADY_HAS_ENTRY, null);
        }
        Optional<ChallengeSubmission> existing = submissionRepository
                .findFirstByArtworkIdAndChallenge_StatusNot(artworkId, ChallengeStatus.ENDED);
        if (existing.isPresent()) {
            return new SubmitResult(SubmitOutcome.CONFLICT_IN_OTHER_CHALLENGE, null);
        }
        ChallengeSubmission submission = new ChallengeSubmission(
                null, challenge, artwork, user.get().getId(), LocalDateTime.now()
        );
        ChallengeSubmission saved = submissionRepository.save(submission);
        return new SubmitResult(SubmitOutcome.CREATED, saved.getId());
    }

    @Override
    @Transactional
    public WithdrawResult withdrawSubmission(Long challengeId, Long artworkId, String username) {
        if (username == null) {
            return new WithdrawResult(WithdrawOutcome.UNAUTHENTICATED);
        }
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return new WithdrawResult(WithdrawOutcome.UNAUTHENTICATED);
        }
        Optional<ChallengeSubmission> existing = submissionRepository
                .findByChallengeIdAndArtworkId(challengeId, artworkId);
        if (existing.isEmpty()) {
            return new WithdrawResult(WithdrawOutcome.NOT_FOUND);
        }
        ChallengeSubmission submission = existing.get();
        Artwork artwork = submission.getArtwork();
        if (artwork == null
                || artwork.getAuthor() == null
                || !user.get().getId().equals(artwork.getAuthor().getId())) {
            return new WithdrawResult(WithdrawOutcome.FORBIDDEN_NOT_OWNER);
        }
        Challenge challenge = submission.getChallenge();
        if (challenge != null && challenge.getStatus() == ChallengeStatus.ENDED) {
            return new WithdrawResult(WithdrawOutcome.FORBIDDEN_CHALLENGE_ENDED);
        }
        submissionRepository.delete(submission);
        return new WithdrawResult(WithdrawOutcome.OK);
    }

    @Override
    public List<ArtworkDTO> findEligibleArtworksForChallenge(Long challengeId, String username) {
        if (username == null) return List.of();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return List.of();
        Optional<Challenge> challengeOpt = challengeRepository.findById(challengeId);
        if (challengeOpt.isEmpty()) return List.of();
        Challenge challenge = challengeOpt.get();
        if (challenge.getStatus() != ChallengeStatus.ACTIVE) return List.of();
        if (submissionRepository.existsByChallenge_IdAndSubmittedBy(challengeId, user.get().getId())) {
            return List.of();
        }
        LocalDateTime startsAt = challenge.getStartsAt();
        if (startsAt == null) startsAt = LocalDateTime.MIN;
        List<Long> ids = submissionRepository.findEligibleArtworkIds(user.get().getId(), startsAt);
        return artworkService.findByIdsOrdered(ids, username);
    }

    @Override
    public List<ChallengeDTO> findEligibleChallengesForArtwork(Long artworkId, String username) {
        if (username == null) return List.of();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return List.of();
        Optional<Artwork> artworkOpt = artworkRepository.findById(artworkId);
        if (artworkOpt.isEmpty()) return List.of();
        Artwork artwork = artworkOpt.get();
        if (artwork.getAuthor() == null || !user.get().getId().equals(artwork.getAuthor().getId())) {
            return List.of();
        }
        if (artwork.getPublishedAt() == null) return List.of();
        List<Long> ids = submissionRepository
                .findEligibleChallengeIdsForArtwork(artworkId, user.get().getId(), artwork.getPublishedAt());
        if (ids.isEmpty()) return List.of();
        return challengeRepository.findAllById(ids).stream()
                .map(c -> mapToDto(c, DEFAULT_PREVIEW_SUBMISSIONS, Map.of()))
                .toList();
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
