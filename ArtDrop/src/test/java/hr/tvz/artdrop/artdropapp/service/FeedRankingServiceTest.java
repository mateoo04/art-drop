package hr.tvz.artdrop.artdropapp.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import hr.tvz.artdrop.artdropapp.dto.FeedSnapshotEntry;
import hr.tvz.artdrop.artdropapp.dto.FeedSnapshotRowKind;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeedRankingServiceTest {

    private FeedRankingService service;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        service = new FeedRankingService(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Caffeine.newBuilder().<String, List<FeedSnapshotEntry>>build(),
                1.5,  // wCircle
                1.0,  // wEngage
                2.0,  // wRecency
                4.0,  // wSeen
                36.0, // recency half-life hours
                96.0, // engagement half-life hours
                24.0, // seen-recency half-life hours
                30, 90, 300, 7,
                6, 12
        );
        now = LocalDateTime.of(2026, 5, 3, 12, 0);
    }

    @Test
    void mergeInsertsFirstChallengeAfterFirstArtworkBlock() {
        List<Long> art = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        List<Long> chall = List.of(100L, 200L);
        List<FeedSnapshotEntry> merged = FeedRankingService.mergeArtworkIdsWithChallengePromos(art, chall, 6, 12);
        assertThat(merged).hasSize(8);
        assertThat(merged.get(5).kind()).isEqualTo(FeedSnapshotRowKind.ARTWORK);
        assertThat(merged.get(5).id()).isEqualTo(6L);
        assertThat(merged.get(6).kind()).isEqualTo(FeedSnapshotRowKind.CHALLENGE_PROMO);
        assertThat(merged.get(6).id()).isEqualTo(100L);
        assertThat(merged.get(7).kind()).isEqualTo(FeedSnapshotRowKind.ARTWORK);
        assertThat(merged.get(7).id()).isEqualTo(7L);
    }

    @Test
    void mergeSecondChallengeRespectsInterval() {
        List<Long> art = new java.util.ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            art.add(i);
        }
        List<Long> chall = List.of(10L, 20L, 30L);
        List<FeedSnapshotEntry> merged = FeedRankingService.mergeArtworkIdsWithChallengePromos(art, chall, 6, 12);
        long promoCount = merged.stream().filter(e -> e.kind() == FeedSnapshotRowKind.CHALLENGE_PROMO).count();
        assertThat(promoCount).isEqualTo(3);
    }

    @Test
    void mergeWithNoChallengesReturnsArtworksOnly() {
        List<Long> art = List.of(1L, 2L, 3L);
        List<FeedSnapshotEntry> merged = FeedRankingService.mergeArtworkIdsWithChallengePromos(art, List.of(), 6, 12);
        assertThat(merged).hasSize(3);
        assertThat(merged).allMatch(e -> e.kind() == FeedSnapshotRowKind.ARTWORK);
    }

    @Test
    void mergeEmptyArtworksReturnsEmpty() {
        assertThat(FeedRankingService.mergeArtworkIdsWithChallengePromos(List.of(), List.of(1L), 6, 12)).isEmpty();
    }

    @Test
    void anonymousViewerIgnoresCircleAndSeen() {
        Artwork a = artwork(1L, 100L, 5, 0, now.minusHours(1));
        double whenAnon = service.scoreArtwork(a, null, true, now.minusMinutes(1), now);
        double whenAnonNoCircle = service.scoreArtwork(a, null, false, null, now);
        assertThat(whenAnon).isEqualTo(whenAnonNoCircle);
    }

    @Test
    void followedArtworkOutranksUnfollowedEquivalent() {
        Artwork followed = artwork(1L, 100L, 5, 1, now.minusHours(2));
        Artwork unfollowed = artwork(2L, 200L, 5, 1, now.minusHours(2));
        double sFollowed = service.scoreArtwork(followed, 42L, true, null, now);
        double sUnfollowed = service.scoreArtwork(unfollowed, 42L, false, null, now);
        assertThat(sFollowed).isGreaterThan(sUnfollowed);
    }

    @Test
    void seenArtworkRanksLowerThanUnseen() {
        Artwork seen = artwork(1L, 100L, 5, 1, now.minusHours(2));
        Artwork unseen = artwork(2L, 100L, 5, 1, now.minusHours(2));
        double sSeen = service.scoreArtwork(seen, 42L, false, now.minusMinutes(2), now);
        double sUnseen = service.scoreArtwork(unseen, 42L, false, null, now);
        assertThat(sUnseen).isGreaterThan(sSeen);
    }

    @Test
    void recentlySeenPenalizedHarderThanLongAgoSeen() {
        Artwork recent = artwork(1L, 100L, 5, 1, now.minusHours(2));
        Artwork ancient = artwork(2L, 100L, 5, 1, now.minusHours(2));
        double sRecent = service.scoreArtwork(recent, 42L, false, now.minusMinutes(5), now);
        double sAncient = service.scoreArtwork(ancient, 42L, false, now.minusDays(5), now);
        assertThat(sAncient).isGreaterThan(sRecent);
    }

    @Test
    void justSeenAppliesNearMaxPenalty() {
        Artwork a = artwork(1L, 100L, 5, 1, now.minusHours(2));
        double sUnseen = service.scoreArtwork(a, 42L, false, null, now);
        double sJustSeen = service.scoreArtwork(a, 42L, false, now.minusMinutes(1), now);
        assertThat(sUnseen - sJustSeen).isGreaterThan(3.5);
    }

    @Test
    void seenAtTimeConstantAppliesOneOverEPenalty() {
        Artwork a = artwork(1L, 100L, 5, 1, now.minusHours(2));
        double sUnseen = service.scoreArtwork(a, 42L, false, null, now);
        double sSeenAtTau = service.scoreArtwork(a, 42L, false, now.minusHours(24), now);
        double penalty = sUnseen - sSeenAtTau;
        assertThat(penalty).isBetween(1.3, 1.6);
    }

    @Test
    void recencyDecaysOverTime() {
        Artwork fresh = artwork(1L, 100L, 0, 0, now.minusHours(1));
        Artwork stale = artwork(2L, 100L, 0, 0, now.minusDays(7));
        double sFresh = service.scoreArtwork(fresh, 42L, false, null, now);
        double sStale = service.scoreArtwork(stale, 42L, false, null, now);
        assertThat(sFresh).isGreaterThan(sStale);
    }

    @Test
    void freshLowEngagementBeatsStaleHighEngagement() {
        Artwork stalePopular = artwork(1L, 100L, 50, 20, now.minusDays(21));
        Artwork freshQuiet = artwork(2L, 100L, 5, 0, now.minusDays(1));
        double sStale = service.scoreArtwork(stalePopular, 42L, false, null, now);
        double sFresh = service.scoreArtwork(freshQuiet, 42L, false, null, now);
        assertThat(sFresh).isGreaterThan(sStale);
    }

    @Test
    void tieJitterIsDeterministicForSameInputs() {
        double a = FeedRankingService.tieJitter(42L, 100L);
        double b = FeedRankingService.tieJitter(42L, 100L);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void tieJitterDiffersAcrossArtworks() {
        double a = FeedRankingService.tieJitter(42L, 100L);
        double b = FeedRankingService.tieJitter(42L, 101L);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void tieJitterStaysSmall() {
        for (long i = 0; i < 200; i++) {
            double j = FeedRankingService.tieJitter(7L, i);
            assertThat(j).isBetween(0.0, 0.01);
        }
    }

    private static Artwork artwork(long id, long authorId, int likes, int comments, LocalDateTime publishedAt) {
        Artwork a = new Artwork();
        a.setId(id);
        User author = new User();
        author.setId(authorId);
        a.setAuthor(author);
        a.setLikeCount(likes);
        a.setComments(new java.util.ArrayList<>());
        for (int i = 0; i < comments; i++) {
            a.getComments().add(new hr.tvz.artdrop.artdropapp.model.Comment());
        }
        a.setPublishedAt(publishedAt);
        return a;
    }
}
