package hr.tvz.artdrop.artdropapp.service;

import com.github.benmanes.caffeine.cache.Cache;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.FeedCursor;
import hr.tvz.artdrop.artdropapp.dto.FeedSnapshotEntry;
import hr.tvz.artdrop.artdropapp.dto.FeedSnapshotRowKind;
import hr.tvz.artdrop.artdropapp.dto.HomeFeedItemDTO;
import hr.tvz.artdrop.artdropapp.dto.HomeFeedResponse;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.FeedSeenJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserFollowJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FeedRankingService {

    private final ArtworkJpaRepository artworkRepository;
    private final UserFollowJpaRepository userFollowRepository;
    private final FeedSeenJpaRepository feedSeenRepository;
    private final ArtworkService artworkService;
    private final ChallengeJpaRepository challengeJpaRepository;
    private final ChallengeSubmissionJpaRepository challengeSubmissionJpaRepository;
    private final ChallengeService challengeService;
    private final Cache<String, List<FeedSnapshotEntry>> feedSnapshotCache;

    private final double wCircle;
    private final double wEngage;
    private final double wRecency;
    private final double wSeen;
    private final double recencyHalfLifeHours;
    private final double engagementHalfLifeHours;
    private final double seenRecencyHalfLifeHours;
    private final int candidateWindowDays;
    private final int circleWindowDays;
    private final int candidateCap;
    private final int seenWindowDays;
    private final int challengeFirstAfterArtworks;
    private final int challengeEveryNArtworks;

    public FeedRankingService(
            ArtworkJpaRepository artworkRepository,
            UserFollowJpaRepository userFollowRepository,
            FeedSeenJpaRepository feedSeenRepository,
            ArtworkService artworkService,
            ChallengeJpaRepository challengeJpaRepository,
            ChallengeSubmissionJpaRepository challengeSubmissionJpaRepository,
            ChallengeService challengeService,
            Cache<String, List<FeedSnapshotEntry>> feedSnapshotCache,
            @Value("${feed.ranking.w-circle:1.5}") double wCircle,
            @Value("${feed.ranking.w-engage:1.0}") double wEngage,
            @Value("${feed.ranking.w-recency:2.0}") double wRecency,
            @Value("${feed.ranking.w-seen:1.5}") double wSeen,
            @Value("${feed.ranking.recency-half-life-hours:36}") double recencyHalfLifeHours,
            @Value("${feed.ranking.engagement-half-life-hours:96}") double engagementHalfLifeHours,
            @Value("${feed.ranking.seen-recency-half-life-hours:24}") double seenRecencyHalfLifeHours,
            @Value("${feed.ranking.candidate-window-days:30}") int candidateWindowDays,
            @Value("${feed.ranking.circle-window-days:90}") int circleWindowDays,
            @Value("${feed.ranking.candidate-cap:300}") int candidateCap,
            @Value("${feed.ranking.seen-window-days:7}") int seenWindowDays,
            @Value("${feed.home.challenge.first-after-artworks:6}") int challengeFirstAfterArtworks,
            @Value("${feed.home.challenge.every-n-artworks:12}") int challengeEveryNArtworks
    ) {
        this.artworkRepository = artworkRepository;
        this.userFollowRepository = userFollowRepository;
        this.feedSeenRepository = feedSeenRepository;
        this.artworkService = artworkService;
        this.challengeJpaRepository = challengeJpaRepository;
        this.challengeSubmissionJpaRepository = challengeSubmissionJpaRepository;
        this.challengeService = challengeService;
        this.feedSnapshotCache = feedSnapshotCache;
        this.wCircle = wCircle;
        this.wEngage = wEngage;
        this.wRecency = wRecency;
        this.wSeen = wSeen;
        this.recencyHalfLifeHours = recencyHalfLifeHours;
        this.engagementHalfLifeHours = engagementHalfLifeHours;
        this.seenRecencyHalfLifeHours = seenRecencyHalfLifeHours;
        this.candidateWindowDays = candidateWindowDays;
        this.circleWindowDays = circleWindowDays;
        this.candidateCap = candidateCap;
        this.seenWindowDays = seenWindowDays;
        this.challengeFirstAfterArtworks = challengeFirstAfterArtworks;
        this.challengeEveryNArtworks = challengeEveryNArtworks;
    }

    public HomeFeedResponse getHomeFeed(Long viewerId, String viewerUsername, String medium, String cursor, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String normalizedMedium = (medium == null || medium.isBlank() || "All".equalsIgnoreCase(medium)) ? null : medium;

        Snapshot active;
        int offset;
        FeedCursor decoded = FeedCursor.decode(cursor).orElse(null);
        if (decoded != null) {
            List<FeedSnapshotEntry> cached = feedSnapshotCache.getIfPresent(decoded.snapshotId());
            if (cached != null) {
                active = new Snapshot(decoded.snapshotId(), cached);
                offset = decoded.offset();
            } else {
                active = computeAndCacheSnapshot(viewerId, normalizedMedium);
                offset = 0;
            }
        } else {
            active = computeAndCacheSnapshot(viewerId, normalizedMedium);
            offset = 0;
        }

        if (active.rows().isEmpty() || offset >= active.rows().size()) {
            return new HomeFeedResponse(List.of(), null, false);
        }

        int end = Math.min(offset + safeLimit, active.rows().size());
        List<FeedSnapshotEntry> pageSlice = active.rows().subList(offset, end);
        List<HomeFeedItemDTO> items = hydrateSnapshotSlice(pageSlice, viewerUsername);

        boolean hasMore = end < active.rows().size();
        String nextCursor = hasMore ? new FeedCursor(active.id(), end).encode() : null;
        return new HomeFeedResponse(items, nextCursor, hasMore);
    }

    private List<HomeFeedItemDTO> hydrateSnapshotSlice(List<FeedSnapshotEntry> slice, String viewerUsername) {
        if (slice.isEmpty()) {
            return List.of();
        }
        List<Long> artIdsInOrder = new ArrayList<>();
        for (FeedSnapshotEntry e : slice) {
            if (e.kind() == FeedSnapshotRowKind.ARTWORK) {
                artIdsInOrder.add(e.id());
            }
        }
        Map<Long, ArtworkDTO> artById = new LinkedHashMap<>();
        if (!artIdsInOrder.isEmpty()) {
            for (ArtworkDTO dto : artworkService.findByIdsOrdered(artIdsInOrder, viewerUsername)) {
                artById.put(dto.id(), dto);
            }
        }
        Map<Long, ChallengeDTO> chById = new HashMap<>();
        for (FeedSnapshotEntry e : slice) {
            if (e.kind() == FeedSnapshotRowKind.CHALLENGE_PROMO) {
                long cid = e.id();
                if (!chById.containsKey(cid)) {
                    challengeService.findById(cid).ifPresent(d -> chById.put(cid, d));
                }
            }
        }
        List<HomeFeedItemDTO> out = new ArrayList<>(slice.size());
        for (FeedSnapshotEntry e : slice) {
            if (e.kind() == FeedSnapshotRowKind.ARTWORK) {
                ArtworkDTO dto = artById.get(e.id());
                if (dto != null) {
                    out.add(new HomeFeedItemDTO("ARTWORK", dto, null));
                }
            } else {
                ChallengeDTO dto = chById.get(e.id());
                if (dto != null) {
                    out.add(new HomeFeedItemDTO("CHALLENGE_PROMO", null, dto));
                }
            }
        }
        return out;
    }

    private record Snapshot(String id, List<FeedSnapshotEntry> rows) {}

    private Snapshot computeAndCacheSnapshot(Long viewerId, String medium) {
        List<Long> rankedArt = computeRankedIds(viewerId, medium, LocalDateTime.now());
        List<Long> promoChallenges = buildShuffledPromoChallengeIds(viewerId);
        List<FeedSnapshotEntry> rows = mergeArtworkIdsWithChallengePromos(
                rankedArt,
                promoChallenges,
                challengeFirstAfterArtworks,
                challengeEveryNArtworks
        );
        String snapshotId = UUID.randomUUID().toString();
        feedSnapshotCache.put(snapshotId, rows);
        return new Snapshot(snapshotId, rows);
    }

    /**
     * ACTIVE challenges only, excluding those the viewer already submitted to; shuffled per snapshot.
     */
    private List<Long> buildShuffledPromoChallengeIds(Long viewerId) {
        List<Long> activeIds = challengeJpaRepository.findByStatus(ChallengeStatus.ACTIVE).stream()
                .map(c -> c.getId())
                .toList();
        if (activeIds.isEmpty()) {
            return List.of();
        }
        Set<Long> exclude = new HashSet<>();
        if (viewerId != null) {
            exclude.addAll(challengeSubmissionJpaRepository.findDistinctChallengeIdsBySubmittedBy(viewerId));
        }
        List<Long> eligible = new ArrayList<>();
        for (Long id : activeIds) {
            if (!exclude.contains(id)) {
                eligible.add(id);
            }
        }
        Collections.shuffle(eligible, ThreadLocalRandom.current());
        return eligible;
    }

    /**
     * After {@code firstPromoAfterArtworkCount} artworks, inserts a challenge promo, then every {@code artworkIntervalBetweenPromos} artworks.
     */
    static List<FeedSnapshotEntry> mergeArtworkIdsWithChallengePromos(
            List<Long> rankedArtIds,
            List<Long> promoChallengeIds,
            int firstPromoAfterArtworkCount,
            int artworkIntervalBetweenPromos
    ) {
        if (rankedArtIds.isEmpty()) {
            return List.of();
        }
        int interval = Math.max(1, artworkIntervalBetweenPromos);
        int first = Math.max(1, firstPromoAfterArtworkCount);
        List<FeedSnapshotEntry> out = new ArrayList<>(rankedArtIds.size() + promoChallengeIds.size());
        int challIdx = 0;
        int artCount = 0;
        for (Long aid : rankedArtIds) {
            out.add(new FeedSnapshotEntry(FeedSnapshotRowKind.ARTWORK, aid));
            artCount++;
            if (challIdx < promoChallengeIds.size()
                    && artCount >= first
                    && (artCount - first) % interval == 0) {
                out.add(new FeedSnapshotEntry(FeedSnapshotRowKind.CHALLENGE_PROMO, promoChallengeIds.get(challIdx++)));
            }
        }
        return out;
    }

    List<Long> computeRankedIds(Long viewerId, String medium, LocalDateTime now) {
        long viewerIdForQuery = viewerId == null ? -1L : viewerId;
        LocalDateTime recentSince = now.minusDays(candidateWindowDays);
        LocalDateTime circleSince = now.minusDays(circleWindowDays);
        List<Artwork> candidates = artworkRepository.findRankingCandidates(
                viewerIdForQuery, recentSince, circleSince, medium,
                PageRequest.of(0, candidateCap));
        if (candidates.isEmpty()) return List.of();

        Set<Long> circleSet = viewerId == null
                ? Set.of()
                : new HashSet<>(userFollowRepository.findFolloweeIdsByFollowerId(viewerId));

        Map<Long, LocalDateTime> lastSeen = new HashMap<>();
        if (viewerId != null) {
            LocalDateTime seenSince = now.minusDays(seenWindowDays);
            List<Long> ids = candidates.stream().map(Artwork::getId).toList();
            for (Object[] row : feedSeenRepository.findLastSeenByArtwork(viewerId, seenSince, ids)) {
                lastSeen.put(((Number) row[0]).longValue(), (LocalDateTime) row[1]);
            }
        }

        record Scored(Long id, double score) {}
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (Artwork a : candidates) {
            double s = scoreArtwork(
                    a,
                    viewerId,
                    circleSet.contains(a.getAuthor() == null ? null : a.getAuthor().getId()),
                    lastSeen.get(a.getId()),
                    now);
            scored.add(new Scored(a.getId(), s));
        }
        scored.sort((x, y) -> Double.compare(y.score(), x.score()));
        List<Long> out = new ArrayList<>(scored.size());
        for (Scored s : scored) out.add(s.id());
        return out;
    }

    public double scoreArtwork(Artwork artwork, Long viewerId, boolean isFollowed, LocalDateTime lastSeenAt, LocalDateTime now) {
        boolean anonymous = viewerId == null;
        double circleTerm = anonymous ? 0.0 : (isFollowed ? wCircle : 0.0);

        int likes = artwork.getLikeCount() == null ? 0 : artwork.getLikeCount();
        int comments = artwork.getComments() == null ? 0 : artwork.getComments().size();

        double ageHours = artwork.getPublishedAt() == null
                ? Double.POSITIVE_INFINITY
                : Math.max(0.0, java.time.Duration.between(artwork.getPublishedAt(), now).toMinutes() / 60.0);

        double recencyTerm = Double.isInfinite(ageHours)
                ? 0.0
                : wRecency * Math.exp(-ageHours / recencyHalfLifeHours);

        double engagementDecay = Double.isInfinite(ageHours)
                ? 0.0
                : Math.exp(-ageHours / engagementHalfLifeHours);
        double engageTerm = wEngage * Math.log1p(likes + 2.0 * comments) * engagementDecay;

        double seenTerm;
        if (anonymous || lastSeenAt == null) {
            seenTerm = 0.0;
        } else {
            double hoursSinceSeen = Math.max(0.0, java.time.Duration.between(lastSeenAt, now).toMinutes() / 60.0);
            seenTerm = wSeen * Math.exp(-hoursSinceSeen / seenRecencyHalfLifeHours);
        }

        double jitter = tieJitter(viewerId, artwork.getId());

        return circleTerm + engageTerm + recencyTerm - seenTerm + jitter;
    }

    static double tieJitter(Long viewerId, Long artworkId) {
        long v = viewerId == null ? 0L : viewerId;
        long a = artworkId == null ? 0L : artworkId;
        long mixed = v * 0x9E3779B97F4A7C15L + a;
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        long unsigned = mixed & 0x7fffffffffffffffL;
        return (unsigned % 1000L) / 100_000.0;
    }
}
