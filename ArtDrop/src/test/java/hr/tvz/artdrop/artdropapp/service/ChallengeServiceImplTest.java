package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.FeaturedChallenge;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.FeaturedChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeServiceImplTest {

    private final ChallengeJpaRepository challengeRepo = mock(ChallengeJpaRepository.class);
    private final ChallengeSubmissionJpaRepository submissionRepo = mock(ChallengeSubmissionJpaRepository.class);
    private final ArtworkJpaRepository artworkRepo = mock(ArtworkJpaRepository.class);
    private final UserJpaRepository userRepo = mock(UserJpaRepository.class);
    private final FeaturedChallengeJpaRepository featuredRepo = mock(FeaturedChallengeJpaRepository.class);
    private final ArtworkService artworkService = mock(ArtworkService.class);

    private final ChallengeServiceImpl svc = new ChallengeServiceImpl(
            challengeRepo, submissionRepo, artworkRepo, userRepo, featuredRepo, artworkService
    );

    @BeforeEach
    void defaults() {
        when(featuredRepo.findById(FeaturedChallenge.SINGLETON_ID)).thenReturn(Optional.empty());
        when(submissionRepo.countByChallengeId(anyLong())).thenReturn(0L);
        when(submissionRepo.findByChallengeIdOrderBySubmittedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(List.of());
    }

    private static Challenge challenge(Long id, String title, ChallengeStatus status, LocalDateTime startsAt) {
        Challenge c = new Challenge();
        c.setId(id);
        c.setTitle(title);
        c.setStatus(status);
        c.setStartsAt(startsAt);
        return c;
    }

    private static User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private static Artwork artwork(Long id, User author, LocalDateTime publishedAt) {
        Artwork a = new Artwork();
        a.setId(id);
        a.setAuthor(author);
        a.setPublishedAt(publishedAt);
        return a;
    }

    // ----- findAll -----

    @Test
    void findAll_returnsAllSortedByStatus() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Ended", ChallengeStatus.ENDED, LocalDateTime.of(2025, 1, 1, 0, 0)),
                challenge(2L, "Active", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(3L, "Upcoming", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 6, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.findAll(null);

        assertThat(result).extracting(ChallengeDTO::title)
                .containsExactly("Active", "Upcoming", "Ended");
    }

    @Test
    void findAll_marksAndPrioritizesFeaturedChallenge() {
        FeaturedChallenge fc = new FeaturedChallenge();
        fc.setId(FeaturedChallenge.SINGLETON_ID);
        fc.setCurrentChallengeId(3L);
        when(featuredRepo.findById(FeaturedChallenge.SINGLETON_ID)).thenReturn(Optional.of(fc));
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Active1", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 5, 1, 0, 0)),
                challenge(3L, "Featured", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.findAll(null);

        assertThat(result.get(0).title()).isEqualTo("Featured");
        assertThat(result.get(0).isFeatured()).isTrue();
        assertThat(result.get(1).isFeatured()).isFalse();
    }

    @Test
    void findAll_handlesNullStartsAt() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "WithDate", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(2L, "NoDate", ChallengeStatus.ACTIVE, null)
        ));

        List<ChallengeDTO> result = svc.findAll(null);

        assertThat(result.get(0).title()).isEqualTo("WithDate");
    }

    @Test
    void findAll_includesViewerEntryWhenViewerHasSubmission() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        List<Object[]> activeEntries = List.<Object[]>of(new Object[]{5L, 100L});
        when(submissionRepo.findActiveEntriesBySubmittedBy(1L)).thenReturn(activeEntries);
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(5L, "Mine", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.findAll("joe");

        assertThat(result.get(0).viewerHasEntry()).isTrue();
        assertThat(result.get(0).viewerEntryArtworkId()).isEqualTo(100L);
    }

    @Test
    void findAll_treatsMissingViewerAsAnonymous() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(5L, "X", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.findAll("ghost");

        assertThat(result.get(0).viewerHasEntry()).isFalse();
    }

    // ----- findById -----

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());
        assertThat(svc.findById(99L, null)).isEmpty();
    }

    @Test
    void findById_returnsDtoWhenFound() {
        when(challengeRepo.findById(5L)).thenReturn(Optional.of(
                challenge(5L, "Hello", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Optional<ChallengeDTO> result = svc.findById(5L, null);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Hello");
    }

    @Test
    void findById_includesViewerEntryWhenViewerSubmitted() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(submissionRepo.findArtworkIdByChallengeIdAndSubmittedBy(5L, 1L)).thenReturn(Optional.of(200L));
        when(challengeRepo.findById(5L)).thenReturn(Optional.of(
                challenge(5L, "X", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Optional<ChallengeDTO> result = svc.findById(5L, "joe");

        assertThat(result.get().viewerHasEntry()).isTrue();
        assertThat(result.get().viewerEntryArtworkId()).isEqualTo(200L);
    }

    // ----- searchChallenges -----

    @Test
    void searchChallenges_returnsEmptyWhenQueryBlank() {
        assertThat(svc.searchChallenges("   ", 10, 0, null)).isEmpty();
        assertThat(svc.searchChallenges(null, 10, 0, null)).isEmpty();
    }

    @Test
    void searchChallenges_clampsLimits() {
        when(challengeRepo.searchChallenges(eq("art"), any(PageRequest.class))).thenReturn(List.of(
                challenge(1L, "Art", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.searchChallenges("art", 999, -1, null);

        assertThat(result).hasSize(1);
    }

    // ----- findSubmissions -----

    @Test
    void findSubmissions_topSortUsesLikeOrder() {
        when(submissionRepo.findByChallengeIdOrderByLikeCountDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(submission(1L, 5L, "Title", user(1L, "joe"))));

        List<SubmissionThumbnailDTO> result = svc.findSubmissions(1L, 10, 0, "top");

        assertThat(result).hasSize(1);
        verify(submissionRepo).findByChallengeIdOrderByLikeCountDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void findSubmissions_defaultSortUsesSubmittedAtDesc() {
        when(submissionRepo.findByChallengeIdOrderBySubmittedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(submission(1L, 5L, "Title", user(1L, "joe"))));

        svc.findSubmissions(1L, 10, 0, "newest");

        verify(submissionRepo).findByChallengeIdOrderBySubmittedAtDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void findSubmissions_handlesNullArtwork() {
        ChallengeSubmission s = new ChallengeSubmission();
        s.setId(1L);
        s.setArtwork(null);
        when(submissionRepo.findByChallengeIdOrderBySubmittedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(s));

        List<SubmissionThumbnailDTO> result = svc.findSubmissions(1L, 10, 0, null);

        assertThat(result.get(0).artworkId()).isNull();
    }

    private ChallengeSubmission submission(Long id, Long artworkId, String title, User author) {
        Artwork a = new Artwork();
        a.setId(artworkId);
        a.setTitle(title);
        a.setMedium("oil");
        a.setAuthor(author);
        ChallengeSubmission s = new ChallengeSubmission();
        s.setId(id);
        s.setArtwork(a);
        s.setSubmittedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return s;
    }

    // ----- submitArtwork -----

    @Test
    void submitArtwork_unauthenticatedWhenNullUsername() {
        var r = svc.submitArtwork(1L, 1L, null);
        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.UNAUTHENTICATED);
    }

    @Test
    void submitArtwork_unauthenticatedWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        var r = svc.submitArtwork(1L, 1L, "ghost");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.UNAUTHENTICATED);
    }

    @Test
    void submitArtwork_notFoundWhenChallengeMissing() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        var r = svc.submitArtwork(99L, 1L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.NOT_FOUND);
    }

    @Test
    void submitArtwork_notFoundWhenArtworkMissing() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(99L)).thenReturn(Optional.empty());

        var r = svc.submitArtwork(1L, 99L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.NOT_FOUND);
    }

    @Test
    void submitArtwork_forbiddenWhenNotOwner() {
        User joe = user(1L, "joe");
        User other = user(2L, "other");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, other, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.FORBIDDEN_NOT_OWNER);
    }

    @Test
    void submitArtwork_forbiddenWhenChallengeNotActive() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ENDED, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.FORBIDDEN_CHALLENGE_NOT_ACTIVE);
    }

    @Test
    void submitArtwork_forbiddenWhenArtworkTooOld() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.FORBIDDEN_ARTWORK_TOO_OLD);
    }

    @Test
    void submitArtwork_forbiddenWhenArtworkNeverPublished() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(artwork(2L, joe, null)));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.FORBIDDEN_ARTWORK_TOO_OLD);
    }

    @Test
    void submitArtwork_conflictWhenAlreadySubmitted() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));
        when(submissionRepo.findByChallengeIdAndArtworkId(1L, 2L))
                .thenReturn(Optional.of(new ChallengeSubmission()));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.CONFLICT_ALREADY_SUBMITTED);
    }

    @Test
    void submitArtwork_conflictWhenUserAlreadyHasEntry() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));
        when(submissionRepo.existsByChallenge_IdAndSubmittedBy(1L, 1L)).thenReturn(true);

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.CONFLICT_USER_ALREADY_HAS_ENTRY);
    }

    @Test
    void submitArtwork_conflictWhenInOtherChallenge() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));
        when(submissionRepo.findFirstByArtworkIdAndChallenge_StatusNot(eq(2L), eq(ChallengeStatus.ENDED)))
                .thenReturn(Optional.of(new ChallengeSubmission()));

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.CONFLICT_IN_OTHER_CHALLENGE);
    }

    @Test
    void submitArtwork_createsSubmission() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(artworkRepo.findById(2L)).thenReturn(Optional.of(
                artwork(2L, joe, LocalDateTime.of(2026, 2, 1, 0, 0))
        ));
        when(submissionRepo.save(any())).thenAnswer(inv -> {
            ChallengeSubmission s = inv.getArgument(0);
            s.setId(500L);
            return s;
        });

        var r = svc.submitArtwork(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.SubmitOutcome.CREATED);
        assertThat(r.submissionId()).isEqualTo(500L);
    }

    // ----- withdrawSubmission -----

    @Test
    void withdraw_unauthenticatedWhenNullUsername() {
        var r = svc.withdrawSubmission(1L, 1L, null);
        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.UNAUTHENTICATED);
    }

    @Test
    void withdraw_unauthenticatedWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        var r = svc.withdrawSubmission(1L, 1L, "ghost");
        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.UNAUTHENTICATED);
    }

    @Test
    void withdraw_notFoundWhenSubmissionMissing() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(submissionRepo.findByChallengeIdAndArtworkId(1L, 2L)).thenReturn(Optional.empty());

        var r = svc.withdrawSubmission(1L, 2L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.NOT_FOUND);
    }

    @Test
    void withdraw_forbiddenWhenNotOwner() {
        User joe = user(1L, "joe");
        User other = user(2L, "other");
        ChallengeSubmission s = new ChallengeSubmission();
        s.setArtwork(artwork(10L, other, LocalDateTime.now()));
        s.setChallenge(challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(submissionRepo.findByChallengeIdAndArtworkId(1L, 10L)).thenReturn(Optional.of(s));

        var r = svc.withdrawSubmission(1L, 10L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.FORBIDDEN_NOT_OWNER);
    }

    @Test
    void withdraw_forbiddenWhenChallengeEnded() {
        User joe = user(1L, "joe");
        ChallengeSubmission s = new ChallengeSubmission();
        s.setArtwork(artwork(10L, joe, LocalDateTime.now()));
        s.setChallenge(challenge(1L, "C", ChallengeStatus.ENDED, LocalDateTime.of(2026, 1, 1, 0, 0)));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(submissionRepo.findByChallengeIdAndArtworkId(1L, 10L)).thenReturn(Optional.of(s));

        var r = svc.withdrawSubmission(1L, 10L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.FORBIDDEN_CHALLENGE_ENDED);
    }

    @Test
    void withdraw_okDeletesSubmission() {
        User joe = user(1L, "joe");
        ChallengeSubmission s = new ChallengeSubmission();
        s.setArtwork(artwork(10L, joe, LocalDateTime.now()));
        s.setChallenge(challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(submissionRepo.findByChallengeIdAndArtworkId(1L, 10L)).thenReturn(Optional.of(s));

        var r = svc.withdrawSubmission(1L, 10L, "joe");

        assertThat(r.outcome()).isEqualTo(ChallengeService.WithdrawOutcome.OK);
        verify(submissionRepo).delete(s);
    }

    // ----- findEligibleArtworksForChallenge -----

    @Test
    void findEligibleArtworks_emptyWhenAnonymous() {
        assertThat(svc.findEligibleArtworksForChallenge(1L, null)).isEmpty();
    }

    @Test
    void findEligibleArtworks_emptyWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(svc.findEligibleArtworksForChallenge(1L, "ghost")).isEmpty();
    }

    @Test
    void findEligibleArtworks_emptyWhenChallengeNotFound() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.findEligibleArtworksForChallenge(99L, "joe")).isEmpty();
    }

    @Test
    void findEligibleArtworks_emptyWhenChallengeNotActive() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        assertThat(svc.findEligibleArtworksForChallenge(1L, "joe")).isEmpty();
    }

    @Test
    void findEligibleArtworks_emptyWhenUserAlreadySubmitted() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(submissionRepo.existsByChallenge_IdAndSubmittedBy(1L, 1L)).thenReturn(true);

        assertThat(svc.findEligibleArtworksForChallenge(1L, "joe")).isEmpty();
        verify(submissionRepo, never()).findEligibleArtworkIds(anyLong(), any());
    }

    @Test
    void findEligibleArtworks_delegatesToArtworkService() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(submissionRepo.findEligibleArtworkIds(eq(1L), any())).thenReturn(List.of(10L, 11L));
        when(artworkService.findByIdsOrdered(eq(List.of(10L, 11L)), eq("joe"))).thenReturn(List.of());

        svc.findEligibleArtworksForChallenge(1L, "joe");

        verify(artworkService).findByIdsOrdered(eq(List.of(10L, 11L)), eq("joe"));
    }

    @Test
    void findEligibleArtworks_treatsNullStartsAtAsMin() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "C", ChallengeStatus.ACTIVE, null)
        ));
        when(submissionRepo.findEligibleArtworkIds(eq(1L), eq(LocalDateTime.MIN))).thenReturn(List.of());
        when(artworkService.findByIdsOrdered(any(), any())).thenReturn(List.of());

        svc.findEligibleArtworksForChallenge(1L, "joe");

        verify(submissionRepo).findEligibleArtworkIds(eq(1L), eq(LocalDateTime.MIN));
    }

    // ----- findEligibleChallengesForArtwork -----

    @Test
    void findEligibleChallenges_emptyWhenAnonymous() {
        assertThat(svc.findEligibleChallengesForArtwork(1L, null)).isEmpty();
    }

    @Test
    void findEligibleChallenges_emptyWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(svc.findEligibleChallengesForArtwork(1L, "ghost")).isEmpty();
    }

    @Test
    void findEligibleChallenges_emptyWhenArtworkNotFound() {
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(1L, "joe")));
        when(artworkRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.findEligibleChallengesForArtwork(99L, "joe")).isEmpty();
    }

    @Test
    void findEligibleChallenges_emptyWhenNotOwner() {
        User joe = user(1L, "joe");
        User other = user(2L, "other");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(artworkRepo.findById(10L)).thenReturn(Optional.of(
                artwork(10L, other, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        assertThat(svc.findEligibleChallengesForArtwork(10L, "joe")).isEmpty();
    }

    @Test
    void findEligibleChallenges_emptyWhenArtworkUnpublished() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(artworkRepo.findById(10L)).thenReturn(Optional.of(artwork(10L, joe, null)));

        assertThat(svc.findEligibleChallengesForArtwork(10L, "joe")).isEmpty();
    }

    @Test
    void findEligibleChallenges_emptyWhenNoEligibleIds() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(artworkRepo.findById(10L)).thenReturn(Optional.of(
                artwork(10L, joe, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(submissionRepo.findEligibleChallengeIdsForArtwork(eq(10L), eq(1L), any())).thenReturn(List.of());

        assertThat(svc.findEligibleChallengesForArtwork(10L, "joe")).isEmpty();
    }

    @Test
    void findEligibleChallenges_returnsMappedDtos() {
        User joe = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(joe));
        when(artworkRepo.findById(10L)).thenReturn(Optional.of(
                artwork(10L, joe, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));
        when(submissionRepo.findEligibleChallengeIdsForArtwork(eq(10L), eq(1L), any()))
                .thenReturn(List.of(5L));
        when(challengeRepo.findAllById(List.of(5L))).thenReturn(List.of(
                challenge(5L, "Eligible", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        List<ChallengeDTO> result = svc.findEligibleChallengesForArtwork(10L, "joe");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Eligible");
    }
}
