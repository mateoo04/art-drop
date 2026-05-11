package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.model.Authority;
import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.AuthorityJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.SellerApplicationJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SellerApplicationServiceImplTest {

    private final SellerApplicationJpaRepository appRepo = mock(SellerApplicationJpaRepository.class);
    private final UserJpaRepository userRepo = mock(UserJpaRepository.class);
    private final AuthorityJpaRepository authorityRepo = mock(AuthorityJpaRepository.class);
    private final ArtworkJpaRepository artworkRepo = mock(ArtworkJpaRepository.class);

    private final SellerApplicationServiceImpl svc =
            new SellerApplicationServiceImpl(appRepo, userRepo, authorityRepo, artworkRepo, 14L);

    // ----- helpers -----

    private static User user(Long id, String username, String... roles) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEnabled(true);
        u.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        Set<Authority> auths = new HashSet<>();
        for (String r : roles) {
            Authority a = new Authority();
            a.setId((long) r.hashCode());
            a.setName(r);
            auths.add(a);
        }
        u.setAuthorities(auths);
        return u;
    }

    private static SellerApplication application(Long id, Long userId, SellerApplicationStatus status) {
        SellerApplication a = new SellerApplication();
        a.setId(id);
        a.setUserId(userId);
        a.setMessage("a sufficiently long message about why I want to sell art on this platform");
        a.setStatus(status);
        a.setSubmittedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return a;
    }

    // ----- findMyLatest -----

    @Test
    void findMyLatest_emptyWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThat(svc.findMyLatest("ghost")).isEmpty();
    }

    @Test
    void findMyLatest_emptyWhenUserHasNoApplication() {
        User u = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());

        assertThat(svc.findMyLatest("joe")).isEmpty();
    }

    @Test
    void findMyLatest_returnsDtoWhenApplicationExists() {
        User u = user(1L, "joe");
        SellerApplication app = application(10L, 1L, SellerApplicationStatus.PENDING);
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(app));

        Optional<SellerApplicationDTO> result = svc.findMyLatest("joe");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(10L);
        assertThat(result.get().derivedSellerStatus()).isEqualTo("PENDING");
    }

    // ----- submit -----

    @Test
    void submit_failsWhenUserNotFound() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        var result = svc.submit("ghost", new SubmitSellerApplicationCommand("msg"));

        assertThat(result).isInstanceOf(SellerApplicationService.SubmitFailure.class);
        var f = (SellerApplicationService.SubmitFailure) result;
        assertThat(f.error()).isEqualTo(SellerApplicationService.SubmitError.USER_NOT_FOUND);
    }

    @Test
    void submit_failsWhenAlreadySeller() {
        User u = user(1L, "alice", "ROLE_SELLER");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(u));

        var result = svc.submit("alice", new SubmitSellerApplicationCommand("msg"));

        var f = (SellerApplicationService.SubmitFailure) result;
        assertThat(f.error()).isEqualTo(SellerApplicationService.SubmitError.ALREADY_SELLER);
    }

    @Test
    void submit_failsWhenPendingApplicationExists() {
        User u = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.existsByUserIdAndStatus(1L, SellerApplicationStatus.PENDING)).thenReturn(true);

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("msg"));

        var f = (SellerApplicationService.SubmitFailure) result;
        assertThat(f.error()).isEqualTo(SellerApplicationService.SubmitError.ALREADY_PENDING);
    }

    @Test
    void submit_failsWhenInCooldownAfterRejection() {
        User u = user(1L, "joe");
        SellerApplication rejected = application(5L, 1L, SellerApplicationStatus.REJECTED);
        rejected.setDecidedAt(LocalDateTime.now().minusDays(1));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.existsByUserIdAndStatus(1L, SellerApplicationStatus.PENDING)).thenReturn(false);
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(rejected));

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("msg"));

        var f = (SellerApplicationService.SubmitFailure) result;
        assertThat(f.error()).isEqualTo(SellerApplicationService.SubmitError.COOLDOWN_ACTIVE);
        assertThat(f.canReapplyAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void submit_failsWhenInCooldownAfterRevocation() {
        User u = user(1L, "joe");
        SellerApplication revoked = application(5L, 1L, SellerApplicationStatus.APPROVED);
        revoked.setRevokedAt(LocalDateTime.now().minusDays(2));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(revoked));

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("msg"));

        assertThat(((SellerApplicationService.SubmitFailure) result).error())
                .isEqualTo(SellerApplicationService.SubmitError.COOLDOWN_ACTIVE);
    }

    @Test
    void submit_succeedsWhenNoPreviousApplication() {
        User u = user(1L, "joe");
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("  message here  "));

        assertThat(result).isInstanceOf(SellerApplicationService.SubmitOk.class);
        ArgumentCaptor<SellerApplication> captor = ArgumentCaptor.forClass(SellerApplication.class);
        verify(appRepo).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("message here");
        assertThat(captor.getValue().getStatus()).isEqualTo(SellerApplicationStatus.PENDING);
    }

    @Test
    void submit_succeedsAfterCooldownExpires() {
        User u = user(1L, "joe");
        SellerApplication rejected = application(5L, 1L, SellerApplicationStatus.REJECTED);
        rejected.setDecidedAt(LocalDateTime.now().minusDays(30));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(rejected));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("msg"));

        assertThat(result).isInstanceOf(SellerApplicationService.SubmitOk.class);
    }

    @Test
    void submit_succeedsWhenLatestHasNoCooldownAnchor() {
        User u = user(1L, "joe");
        // A pending past application without decided/revoked dates would not happen via cooldown,
        // but cooldownAnchorFor returns null for non-rejected non-approved-revoked states.
        SellerApplication pending = application(5L, 1L, SellerApplicationStatus.PENDING);
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(u));
        when(appRepo.existsByUserIdAndStatus(1L, SellerApplicationStatus.PENDING)).thenReturn(false);
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(pending));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.submit("joe", new SubmitSellerApplicationCommand("msg"));

        assertThat(result).isInstanceOf(SellerApplicationService.SubmitOk.class);
    }

    // ----- listApplications -----

    @Test
    void listApplications_nullFilterReturnsAll() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findAllByOrderBySubmittedAtDesc(any())).thenReturn(new PageImpl<>(List.of(app)));
        when(userRepo.findById(5L)).thenReturn(Optional.of(user(5L, "joe")));

        Page<SellerApplicationDTO> page = svc.listApplications(null, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    void listApplications_blankFilterReturnsAll() {
        when(appRepo.findAllByOrderBySubmittedAtDesc(any())).thenReturn(new PageImpl<>(List.of()));

        Page<SellerApplicationDTO> page = svc.listApplications("   ", 0, 10);

        assertThat(page.getContent()).isEmpty();
        verify(appRepo).findAllByOrderBySubmittedAtDesc(any());
    }

    @Test
    void listApplications_allFilterReturnsAll() {
        when(appRepo.findAllByOrderBySubmittedAtDesc(any())).thenReturn(new PageImpl<>(List.of()));

        svc.listApplications("all", 0, 10);

        verify(appRepo).findAllByOrderBySubmittedAtDesc(any());
    }

    @Test
    void listApplications_filtersByStatus() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findByStatusOrderBySubmittedAtAsc(eq(SellerApplicationStatus.PENDING), any()))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(userRepo.findById(5L)).thenReturn(Optional.empty());

        Page<SellerApplicationDTO> page = svc.listApplications("PENDING", 0, 10);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void listApplications_returnsEmptyForInvalidStatus() {
        Page<SellerApplicationDTO> page = svc.listApplications("NOT_A_STATUS", 0, 10);

        assertThat(page.getContent()).isEmpty();
        verify(appRepo, never()).findAllByOrderBySubmittedAtDesc(any());
        verify(appRepo, never()).findByStatusOrderBySubmittedAtAsc(any(), any());
    }

    // ----- searchUsers -----

    @Test
    void searchUsers_emptyQueryUsesFindAll() {
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(user(1L, "alice")));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, null, null, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).username()).isEqualTo("alice");
    }

    @Test
    void searchUsers_withQueryUsesSearch() {
        when(userRepo.searchByUsernameDisplayNameOrEmail(eq("ali"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, "alice"))));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers("ali", null, null, null, 0, 10);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void searchUsers_filtersByDerivedStatus() {
        User noneUser = user(1L, "alice");
        User pendingUser = user(2L, "bob");
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(noneUser, pendingUser));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(2L))
                .thenReturn(Optional.of(application(10L, 2L, SellerApplicationStatus.PENDING)));

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, List.of("PENDING"), null, null, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).username()).isEqualTo("bob");
    }

    @Test
    void searchUsers_filtersByRole() {
        User regular = user(1L, "alice");
        User seller = user(2L, "bob", "ROLE_SELLER");
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(regular, seller));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, "ROLE_SELLER", null, 0, 10);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).username()).isEqualTo("bob");
    }

    @Test
    void searchUsers_sortsByUsername() {
        User a = user(1L, "zeta");
        User b = user(2L, "alpha");
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(a, b));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, null, "username", 0, 10);

        assertThat(page.getContent().get(0).username()).isEqualTo("alpha");
    }

    @Test
    void searchUsers_sortsByOldestPending() {
        User a = user(1L, "alice");
        User b = user(2L, "bob");
        User c = user(3L, "carol");

        SellerApplication oldPending = application(10L, 1L, SellerApplicationStatus.PENDING);
        oldPending.setSubmittedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        SellerApplication newPending = application(11L, 2L, SellerApplicationStatus.PENDING);
        newPending.setSubmittedAt(LocalDateTime.of(2026, 1, 1, 0, 0));

        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(b, a, c));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(oldPending));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(2L)).thenReturn(Optional.of(newPending));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(3L)).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, null, "oldest_pending", 0, 10);

        assertThat(page.getContent()).extracting(AdminUserSummaryDTO::username)
                .containsExactly("alice", "bob", "carol");
    }

    @Test
    void searchUsers_sortsByMostArtworks() {
        User a = user(1L, "alice");
        User b = user(2L, "bob");
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(a, b));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());
        when(artworkRepo.countByAuthor_Id(1L)).thenReturn(2L);
        when(artworkRepo.countByAuthor_Id(2L)).thenReturn(5L);

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, null, "most_artworks", 0, 10);

        assertThat(page.getContent().get(0).username()).isEqualTo("bob");
    }

    @Test
    void searchUsers_paginates() {
        when(userRepo.findAll(any(Sort.class))).thenReturn(List.of(
                user(1L, "a"), user(2L, "b"), user(3L, "c"), user(4L, "d")
        ));
        when(appRepo.findTopByUserIdOrderBySubmittedAtDesc(anyLong())).thenReturn(Optional.empty());

        Page<AdminUserSummaryDTO> page = svc.searchUsers(null, null, null, "username", 1, 2);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    // ----- getUserDetail -----

    @Test
    void getUserDetail_emptyWhenUserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.getUserDetail(99L)).isEmpty();
    }

    @Test
    void getUserDetail_returnsDetailWithHistory() {
        User u = user(1L, "joe");
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(appRepo.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of(
                application(10L, 1L, SellerApplicationStatus.REJECTED),
                application(11L, 1L, SellerApplicationStatus.APPROVED)
        ));

        Optional<AdminUserDetailDTO> result = svc.getUserDetail(1L);

        assertThat(result).isPresent();
        assertThat(result.get().applicationHistory()).hasSize(2);
    }

    // ----- approve -----

    @Test
    void approve_failsWhenApplicationNotFound() {
        when(appRepo.findById(99L)).thenReturn(Optional.empty());

        var result = svc.approve(99L, "admin", new SellerApplicationDecisionCommand("reason"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_FOUND);
    }

    @Test
    void approve_failsWhenNotPending() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.APPROVED);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));

        var result = svc.approve(1L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_PENDING);
    }

    @Test
    void approve_failsWhenAdminNotFound() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepo.findById(5L)).thenReturn(Optional.of(user(5L, "joe")));

        var result = svc.approve(1L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_FOUND);
    }

    @Test
    void approve_failsWhenApplicantNotFound() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(userRepo.findById(5L)).thenReturn(Optional.empty());

        var result = svc.approve(1L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_FOUND);
    }

    @Test
    void approve_grantsSellerRoleAndSaves() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        User applicant = user(5L, "joe");
        User admin = user(99L, "admin", "ROLE_ADMIN");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepo.findById(5L)).thenReturn(Optional.of(applicant));
        when(authorityRepo.findByName("ROLE_SELLER")).thenReturn(Optional.of(new Authority(1L, "ROLE_SELLER")));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.approve(1L, "admin", new SellerApplicationDecisionCommand("looks good"));

        assertThat(result).isInstanceOf(SellerApplicationService.DecideOk.class);
        assertThat(app.getStatus()).isEqualTo(SellerApplicationStatus.APPROVED);
        assertThat(app.getDecisionReason()).isEqualTo("looks good");
        assertThat(applicant.getAuthorities()).anyMatch(a -> "ROLE_SELLER".equals(a.getName()));
    }

    @Test
    void approve_createsSellerAuthorityIfMissing() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        User applicant = user(5L, "joe");
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(userRepo.findById(5L)).thenReturn(Optional.of(applicant));
        when(authorityRepo.findByName("ROLE_SELLER")).thenReturn(Optional.empty());
        when(authorityRepo.save(any())).thenAnswer(inv -> {
            Authority a = inv.getArgument(0);
            a.setId(50L);
            return a;
        });
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.approve(1L, "admin", null);

        assertThat(result).isInstanceOf(SellerApplicationService.DecideOk.class);
        verify(authorityRepo, times(1)).save(any());
        assertThat(app.getDecisionReason()).isNull();
    }

    // ----- reject -----

    @Test
    void reject_failsWhenApplicationNotFound() {
        when(appRepo.findById(99L)).thenReturn(Optional.empty());

        var result = svc.reject(99L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_FOUND);
    }

    @Test
    void reject_failsWhenNotPending() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.REJECTED);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));

        var result = svc.reject(1L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_PENDING);
    }

    @Test
    void reject_failsWhenAdminOrApplicantMissing() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.empty());

        var result = svc.reject(1L, "admin", new SellerApplicationDecisionCommand("r"));

        assertThat(((SellerApplicationService.DecideFailure) result).error())
                .isEqualTo(SellerApplicationService.DecideError.NOT_FOUND);
    }

    @Test
    void reject_savesRejectedStatus() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(userRepo.findById(5L)).thenReturn(Optional.of(user(5L, "joe")));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.reject(1L, "admin", new SellerApplicationDecisionCommand("not yet"));

        assertThat(result).isInstanceOf(SellerApplicationService.DecideOk.class);
        assertThat(app.getStatus()).isEqualTo(SellerApplicationStatus.REJECTED);
        assertThat(app.getDecisionReason()).isEqualTo("not yet");
    }

    @Test
    void reject_handlesNullCommand() {
        SellerApplication app = application(1L, 5L, SellerApplicationStatus.PENDING);
        when(appRepo.findById(1L)).thenReturn(Optional.of(app));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(userRepo.findById(5L)).thenReturn(Optional.of(user(5L, "joe")));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = svc.reject(1L, "admin", null);

        assertThat(result).isInstanceOf(SellerApplicationService.DecideOk.class);
        assertThat(app.getDecisionReason()).isNull();
    }

    // ----- revoke -----

    @Test
    void revoke_failsWhenUserNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        var result = svc.revoke(99L, "admin", new RevokeSellerCommand("reason"));

        assertThat(((SellerApplicationService.RevokeFailure) result).error())
                .isEqualTo(SellerApplicationService.RevokeError.USER_NOT_FOUND);
    }

    @Test
    void revoke_failsWhenUserNotSeller() {
        User u = user(5L, "joe");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));

        var result = svc.revoke(5L, "admin", new RevokeSellerCommand("reason"));

        assertThat(((SellerApplicationService.RevokeFailure) result).error())
                .isEqualTo(SellerApplicationService.RevokeError.NOT_A_SELLER);
    }

    @Test
    void revoke_failsWhenAdminNotFound() {
        User u = user(5L, "joe", "ROLE_SELLER");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.empty());

        var result = svc.revoke(5L, "admin", new RevokeSellerCommand("reason"));

        assertThat(((SellerApplicationService.RevokeFailure) result).error())
                .isEqualTo(SellerApplicationService.RevokeError.USER_NOT_FOUND);
    }

    @Test
    void revoke_removesSellerRoleAndUnlistsArtworks() {
        User u = user(5L, "joe", "ROLE_SELLER");
        SellerApplication approval = application(10L, 5L, SellerApplicationStatus.APPROVED);
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(appRepo.findCurrentApprovalForUser(5L)).thenReturn(Optional.of(approval));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(artworkRepo.unlistAllForAuthor(5L)).thenReturn(3);

        var result = svc.revoke(5L, "admin", new RevokeSellerCommand("policy violation"));

        assertThat(result).isInstanceOf(SellerApplicationService.RevokeOk.class);
        assertThat(((SellerApplicationService.RevokeOk) result).unlistedCount()).isEqualTo(3);
        assertThat(u.getAuthorities()).noneMatch(a -> "ROLE_SELLER".equals(a.getName()));
        assertThat(approval.getRevokedAt()).isNotNull();
        assertThat(approval.getRevokeReason()).isEqualTo("policy violation");
    }

    @Test
    void revoke_succeedsWhenNoCurrentApproval() {
        User u = user(5L, "joe", "ROLE_SELLER");
        when(userRepo.findById(5L)).thenReturn(Optional.of(u));
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user(99L, "admin")));
        when(appRepo.findCurrentApprovalForUser(5L)).thenReturn(Optional.empty());
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(artworkRepo.unlistAllForAuthor(5L)).thenReturn(0);

        var result = svc.revoke(5L, "admin", new RevokeSellerCommand("reason"));

        assertThat(result).isInstanceOf(SellerApplicationService.RevokeOk.class);
        verify(appRepo, never()).save(any());
    }

    // ----- countListedArtworks -----

    @Test
    void countListedArtworks_returnsCountFromRepo() {
        when(artworkRepo.countListedByAuthorId(5L)).thenReturn(7L);

        ListedArtworkCountDTO dto = svc.countListedArtworks(5L);

        assertThat(dto.count()).isEqualTo(7L);
    }

    // ----- promoteToAdmin -----

    @Test
    void promoteToAdmin_notFoundReturnsNotFound() {
        when(userRepo.findByIdWithAuthorities(99L)).thenReturn(Optional.empty());
        assertThat(svc.promoteToAdmin(99L, "actor")).isEqualTo("NOT_FOUND");
    }

    @Test
    void promoteToAdmin_alreadyAdminReturnsOk() {
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(user(1L, "joe", "ROLE_ADMIN")));
        assertThat(svc.promoteToAdmin(1L, "actor")).isEqualTo("OK");
        verify(userRepo, never()).save(any());
    }

    @Test
    void promoteToAdmin_grantsAdminWhenAbsent() {
        User u = user(1L, "joe");
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(u));
        when(authorityRepo.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new Authority(1L, "ROLE_ADMIN")));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.promoteToAdmin(1L, "actor")).isEqualTo("OK");
        assertThat(u.getAuthorities()).anyMatch(a -> "ROLE_ADMIN".equals(a.getName()));
    }

    @Test
    void promoteToAdmin_createsAuthorityWhenMissing() {
        User u = user(1L, "joe");
        u.setAuthorities(null);
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(u));
        when(authorityRepo.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());
        when(authorityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.promoteToAdmin(1L, "actor")).isEqualTo("OK");
        verify(authorityRepo).save(any());
    }

    // ----- grantSellerRole -----

    @Test
    void grantSellerRole_notFoundReturnsNotFound() {
        when(userRepo.findByIdWithAuthorities(99L)).thenReturn(Optional.empty());
        assertThat(svc.grantSellerRole(99L, "actor")).isEqualTo("NOT_FOUND");
    }

    @Test
    void grantSellerRole_alreadySellerReturnsOk() {
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(user(1L, "joe", "ROLE_SELLER")));
        assertThat(svc.grantSellerRole(1L, "actor")).isEqualTo("OK");
        verify(userRepo, never()).save(any());
    }

    @Test
    void grantSellerRole_grantsWhenAbsent() {
        User u = user(1L, "joe");
        u.setAuthorities(null);
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(u));
        when(authorityRepo.findByName("ROLE_SELLER")).thenReturn(Optional.of(new Authority(1L, "ROLE_SELLER")));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.grantSellerRole(1L, "actor")).isEqualTo("OK");
    }

    @Test
    void grantSellerRole_createsAuthorityWhenMissing() {
        User u = user(1L, "joe");
        when(userRepo.findByIdWithAuthorities(1L)).thenReturn(Optional.of(u));
        when(authorityRepo.findByName("ROLE_SELLER")).thenReturn(Optional.empty());
        when(authorityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.grantSellerRole(1L, "actor")).isEqualTo("OK");
        verify(authorityRepo).save(any());
    }

    // ----- deactivateUser -----

    @Test
    void deactivateUser_notFoundReturnsNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        assertThat(svc.deactivateUser(99L, "actor")).isEqualTo("NOT_FOUND");
    }

    @Test
    void deactivateUser_selfDeactivateReturnsSelfDeactivate() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(user(1L, "joe")));
        assertThat(svc.deactivateUser(1L, "joe")).isEqualTo("SELF_DEACTIVATE");
        verify(userRepo, never()).save(any());
    }

    @Test
    void deactivateUser_disablesAndReturnsOk() {
        User u = user(1L, "joe");
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.deactivateUser(1L, "admin")).isEqualTo("OK");
        assertThat(u.isEnabled()).isFalse();
    }

    @Test
    void deactivateUser_nullActingUsernameDoesNotSelfDeactivate() {
        User u = user(1L, "joe");
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.deactivateUser(1L, null)).isEqualTo("OK");
    }

    // ----- reactivateUser -----

    @Test
    void reactivateUser_notFoundReturnsNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());
        assertThat(svc.reactivateUser(99L, "actor")).isEqualTo("NOT_FOUND");
    }

    @Test
    void reactivateUser_enablesAndReturnsOk() {
        User u = user(1L, "joe");
        u.setEnabled(false);
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.reactivateUser(1L, "admin")).isEqualTo("OK");
        assertThat(u.isEnabled()).isTrue();
    }
}
