package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminChallengeRowDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminChallengeUpsertDTO;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.FeaturedChallenge;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.FeaturedChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminChallengeServiceImplTest {

    private final ChallengeJpaRepository challengeRepo = mock(ChallengeJpaRepository.class);
    private final ChallengeSubmissionJpaRepository submissionRepo = mock(ChallengeSubmissionJpaRepository.class);
    private final UserJpaRepository userRepo = mock(UserJpaRepository.class);
    private final FeaturedChallengeJpaRepository featuredRepo = mock(FeaturedChallengeJpaRepository.class);

    private final AdminChallengeServiceImpl svc =
            new AdminChallengeServiceImpl(challengeRepo, submissionRepo, userRepo, featuredRepo);

    @BeforeEach
    void resetFeaturedToEmpty() {
        when(featuredRepo.findById(FeaturedChallenge.SINGLETON_ID)).thenReturn(Optional.empty());
        when(submissionRepo.countByChallengeId(anyLong())).thenReturn(0L);
    }

    private static Challenge challenge(Long id, String title, ChallengeStatus status, LocalDateTime startsAt) {
        Challenge c = new Challenge();
        c.setId(id);
        c.setTitle(title);
        c.setStatus(status);
        c.setStartsAt(startsAt);
        return c;
    }

    private static AdminChallengeUpsertDTO upsert(String title, ChallengeStatus status) {
        return new AdminChallengeUpsertDTO(
                title, "desc", "quote", status, "theme", "https://cdn/cover.jpg",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0)
        );
    }

    // ---------- list ----------

    @Test
    void list_returnsAllWhenNoFilters() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Alpha", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0)),
                challenge(2L, "Beta", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 4, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 0, 10, null);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(AdminChallengeRowDTO::title).containsExactly("Beta", "Alpha");
    }

    @Test
    void list_filtersByStatus() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Alpha", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0)),
                challenge(2L, "Beta", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 4, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, ChallengeStatus.ACTIVE, 0, 10, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Alpha");
    }

    @Test
    void list_filtersByQueryMatchingTitle() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Spring Bloom", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0)),
                challenge(2L, "Winter", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 4, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list("spring", null, 0, 10, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).title()).isEqualTo("Spring Bloom");
    }

    @Test
    void list_filtersByQueryMatchingDescription() {
        Challenge a = challenge(1L, "Alpha", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0));
        a.setDescription("Special description text");
        Challenge b = challenge(2L, "Beta", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 4, 1, 0, 0));
        when(challengeRepo.findAll()).thenReturn(List.of(a, b));

        Page<AdminChallengeRowDTO> page = svc.list("SPECIAL", null, 0, 10, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    void list_handlesNullTitleAndDescriptionWithQuery() {
        Challenge c = challenge(1L, null, ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0));
        c.setDescription(null);
        when(challengeRepo.findAll()).thenReturn(List.of(c));

        Page<AdminChallengeRowDTO> page = svc.list("anything", null, 0, 10, null);

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void list_sortsByTitleWhenSortIsTitle() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Zeta", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0)),
                challenge(2L, "Alpha", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 4, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 0, 10, "title");

        assertThat(page.getContent()).extracting(AdminChallengeRowDTO::title)
                .containsExactly("Alpha", "Zeta");
    }

    @Test
    void list_sortsByStartsDescByDefault() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Old", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(2L, "New", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 6, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 0, 10, "starts_desc");

        assertThat(page.getContent()).extracting(AdminChallengeRowDTO::title)
                .containsExactly("New", "Old");
    }

    @Test
    void list_treatsBlankSortAsDefault() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Old", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(2L, "New", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 6, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 0, 10, "   ");

        assertThat(page.getContent().get(0).title()).isEqualTo("New");
    }

    @Test
    void list_treatsNullStartsAtAsMin() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "WithDate", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(2L, "NoDate", ChallengeStatus.ACTIVE, null)
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 0, 10, null);

        assertThat(page.getContent().get(0).title()).isEqualTo("WithDate");
        assertThat(page.getContent().get(1).title()).isEqualTo("NoDate");
    }

    @Test
    void list_paginatesCorrectly() {
        List<Challenge> all = List.of(
                challenge(1L, "A", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0)),
                challenge(2L, "B", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 2, 1, 0, 0)),
                challenge(3L, "C", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 3, 1, 0, 0))
        );
        when(challengeRepo.findAll()).thenReturn(all);

        Page<AdminChallengeRowDTO> page1 = svc.list(null, null, 0, 2, "title");
        Page<AdminChallengeRowDTO> page2 = svc.list(null, null, 1, 2, "title");

        assertThat(page1.getContent()).hasSize(2);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page1.getTotalElements()).isEqualTo(3);
    }

    @Test
    void list_returnsEmptyPageWhenPageBeyondTotal() {
        when(challengeRepo.findAll()).thenReturn(List.of(
                challenge(1L, "Only", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Page<AdminChallengeRowDTO> page = svc.list(null, null, 5, 10, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // ---------- get ----------

    @Test
    void get_returnsRowWhenFound() {
        when(challengeRepo.findById(1L)).thenReturn(Optional.of(
                challenge(1L, "Found", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Optional<AdminChallengeRowDTO> result = svc.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Found");
        assertThat(result.get().isFeatured()).isFalse();
    }

    @Test
    void get_returnsEmptyWhenNotFound() {
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.get(99L)).isEmpty();
    }

    @Test
    void get_marksFeaturedWhenMatchingCurrentChallengeId() {
        FeaturedChallenge featured = new FeaturedChallenge();
        featured.setId(FeaturedChallenge.SINGLETON_ID);
        featured.setCurrentChallengeId(7L);
        when(featuredRepo.findById(FeaturedChallenge.SINGLETON_ID)).thenReturn(Optional.of(featured));
        when(challengeRepo.findById(7L)).thenReturn(Optional.of(
                challenge(7L, "Featured", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        Optional<AdminChallengeRowDTO> result = svc.get(7L);

        assertThat(result).isPresent();
        assertThat(result.get().isFeatured()).isTrue();
    }

    @Test
    void get_isNotFeaturedWhenFeaturedRecordHasNullCurrent() {
        FeaturedChallenge featured = new FeaturedChallenge();
        featured.setId(FeaturedChallenge.SINGLETON_ID);
        featured.setCurrentChallengeId(null);
        when(featuredRepo.findById(FeaturedChallenge.SINGLETON_ID)).thenReturn(Optional.of(featured));
        when(challengeRepo.findById(7L)).thenReturn(Optional.of(
                challenge(7L, "Any", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        assertThat(svc.get(7L).get().isFeatured()).isFalse();
    }

    // ---------- create ----------

    @Test
    void create_savesChallengeWithProvidedFields() {
        User admin = new User();
        admin.setId(42L);
        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(admin));
        ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
        when(challengeRepo.save(captor.capture())).thenAnswer(inv -> {
            Challenge c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        AdminChallengeRowDTO row = svc.create(upsert("Title", ChallengeStatus.ACTIVE), "admin");

        Challenge saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Title");
        assertThat(saved.getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
        assertThat(saved.getCreatedBy()).isEqualTo(42L);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(row.id()).isEqualTo(100L);
    }

    @Test
    void create_defaultsStatusToUpcomingWhenNull() {
        when(userRepo.findByUsername(any())).thenReturn(Optional.empty());
        ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
        when(challengeRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        AdminChallengeUpsertDTO dto = new AdminChallengeUpsertDTO(
                " Title ", "d", "q", null, "t", "u",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0)
        );
        svc.create(dto, "admin");

        assertThat(captor.getValue().getStatus()).isEqualTo(ChallengeStatus.UPCOMING);
        assertThat(captor.getValue().getTitle()).isEqualTo("Title");
    }

    @Test
    void create_handlesNullAdminUsername() {
        when(challengeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.create(upsert("Title", ChallengeStatus.ACTIVE), null);

        verify(userRepo, never()).findByUsername(any());
    }

    @Test
    void create_handlesBlankAdminUsername() {
        when(challengeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.create(upsert("Title", ChallengeStatus.ACTIVE), "   ");

        verify(userRepo, never()).findByUsername(any());
    }

    @Test
    void create_handlesUnknownAdminUsername() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());
        ArgumentCaptor<Challenge> captor = ArgumentCaptor.forClass(Challenge.class);
        when(challengeRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        svc.create(upsert("Title", ChallengeStatus.ACTIVE), "ghost");

        assertThat(captor.getValue().getCreatedBy()).isNull();
    }

    // ---------- update ----------

    @Test
    void update_returnsUpdatedRowWhenFound() {
        Challenge existing = challenge(5L, "Old", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(challengeRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(challengeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<AdminChallengeRowDTO> result = svc.update(5L, upsert("New", ChallengeStatus.ACTIVE));

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("New");
        assertThat(result.get().status()).isEqualTo("ACTIVE");
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_returnsEmptyWhenNotFound() {
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.update(99L, upsert("X", ChallengeStatus.ACTIVE))).isEmpty();
        verify(challengeRepo, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_returnsTrueWhenExists() {
        when(challengeRepo.existsById(3L)).thenReturn(true);

        assertThat(svc.delete(3L)).isTrue();
        verify(challengeRepo, times(1)).deleteById(3L);
    }

    @Test
    void delete_returnsFalseWhenNotExists() {
        when(challengeRepo.existsById(99L)).thenReturn(false);

        assertThat(svc.delete(99L)).isFalse();
        verify(challengeRepo, never()).deleteById(anyLong());
    }

    // ---------- setStatus ----------

    @Test
    void setStatus_returnsUpdatedRowWhenFound() {
        Challenge existing = challenge(4L, "Title", ChallengeStatus.UPCOMING, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(challengeRepo.findById(4L)).thenReturn(Optional.of(existing));
        when(challengeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<AdminChallengeRowDTO> result = svc.setStatus(4L, ChallengeStatus.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo("ACTIVE");
        assertThat(existing.getStatus()).isEqualTo(ChallengeStatus.ACTIVE);
        assertThat(existing.getUpdatedAt()).isNotNull();
    }

    @Test
    void setStatus_returnsEmptyWhenNotFound() {
        when(challengeRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.setStatus(99L, ChallengeStatus.ACTIVE)).isEmpty();
        verify(challengeRepo, never()).save(any());
    }

    @Test
    void toRow_includesSubmissionCount() {
        when(submissionRepo.countByChallengeId(11L)).thenReturn(7L);
        when(challengeRepo.findById(11L)).thenReturn(Optional.of(
                challenge(11L, "Counted", ChallengeStatus.ACTIVE, LocalDateTime.of(2026, 1, 1, 0, 0))
        ));

        assertThat(svc.get(11L).get().submissionCount()).isEqualTo(7L);
    }

    @Test
    void toRow_handlesNullStatus() {
        Challenge c = challenge(12L, "NoStatus", null, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(challengeRepo.findById(12L)).thenReturn(Optional.of(c));

        assertThat(svc.get(12L).get().status()).isNull();
    }
}
