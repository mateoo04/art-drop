package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.FeaturedChallengeStateDTO;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeaturedChallengeServiceTest extends AbstractPostgresIntegrationTest {

    @Autowired private FeaturedChallengeService service;
    @Autowired private ChallengeJpaRepository challengeRepo;

    private Challenge createChallenge(String title) {
        var c = new Challenge();
        c.setTitle(title);
        c.setStatus(ChallengeStatus.ACTIVE);
        c.setStartsAt(LocalDateTime.now().minusDays(1));
        c.setEndsAt(LocalDateTime.now().plusDays(7));
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return challengeRepo.save(c);
    }

    // --- Task 15: initial state ---

    @Test
    void getState_returnsEmptyWhenNoFeaturedSet() {
        // The test profile includes the dev seed which sets current_challenge_id = 1.
        // Clear it first so we can assert the truly-empty state.
        service.clearCurrent();
        FeaturedChallengeStateDTO state = service.getState(null);
        assertThat(state.current()).isNull();
        assertThat(state.next()).isNull();
        assertThat(state.triggerType()).isNull();
    }

    // --- Task 16: setCurrent + clearCurrent ---

    @Test
    void setCurrent_setsTheFeaturedChallenge() {
        var c = createChallenge("First");
        service.setCurrent(c.getId());
        assertThat(service.getState(null).current().id()).isEqualTo(c.getId());
    }

    @Test
    void setCurrent_clearsAnyPendingSchedule() {
        var current = createChallenge("Current");
        var pending = createChallenge("Pending");
        service.setCurrent(current.getId());
        service.scheduleReplacement(pending.getId(),
                FeaturedTriggerType.AT_TIME,
                LocalDateTime.now().plusHours(1));

        var second = createChallenge("Second");
        service.setCurrent(second.getId());

        var state = service.getState(null);
        assertThat(state.current().id()).isEqualTo(second.getId());
        assertThat(state.next()).isNull();
        assertThat(state.triggerType()).isNull();
    }

    @Test
    void clearCurrent_unsetsTheFeaturedChallenge() {
        var c = createChallenge("First");
        service.setCurrent(c.getId());
        service.clearCurrent();
        assertThat(service.getState(null).current()).isNull();
    }

    // --- Task 17: scheduleReplacement ---

    @Test
    void scheduleReplacement_atTime_storesTrigger() {
        var current = createChallenge("Current");
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        var when = LocalDateTime.now().plusHours(2);
        service.scheduleReplacement(next.getId(), FeaturedTriggerType.AT_TIME, when);

        var state = service.getState(null);
        assertThat(state.next().id()).isEqualTo(next.getId());
        assertThat(state.triggerType()).isEqualTo("AT_TIME");
        assertThat(state.triggerAt()).isEqualToIgnoringNanos(when);
    }

    @Test
    void scheduleReplacement_whenCurrentEnds_storesTrigger() {
        var current = createChallenge("Current");
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.WHEN_CURRENT_ENDS, null);

        var state = service.getState(null);
        assertThat(state.triggerType()).isEqualTo("WHEN_CURRENT_ENDS");
        assertThat(state.triggerAt()).isNull();
    }

    @Test
    void scheduleReplacement_atTime_requiresTriggerAt() {
        var current = createChallenge("Current");
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleReplacement(next.getId(), FeaturedTriggerType.AT_TIME, null));
    }

    @Test
    void scheduleReplacement_whenCurrentEnds_requiresCurrent() {
        // Seed sets a current; clear it so no current challenge is set.
        service.clearCurrent();
        var next = createChallenge("Next");
        assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleReplacement(next.getId(), FeaturedTriggerType.WHEN_CURRENT_ENDS, null));
    }

    @Test
    void scheduleReplacement_rejectsNextEqualToCurrent() {
        var c = createChallenge("Same");
        service.setCurrent(c.getId());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleReplacement(c.getId(), FeaturedTriggerType.AT_TIME, LocalDateTime.now().plusDays(1)));
    }

    // --- Task 18: processScheduleIfReady ---

    @Test
    void processScheduleIfReady_returnsFalseWhenNoTrigger() {
        assertThat(service.processScheduleIfReady()).isFalse();
    }

    @Test
    void processScheduleIfReady_atTime_firesWhenDue() {
        var current = createChallenge("Current");
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.AT_TIME,
                LocalDateTime.now().minusSeconds(1));

        boolean fired = service.processScheduleIfReady();

        assertThat(fired).isTrue();
        var state = service.getState(null);
        assertThat(state.current().id()).isEqualTo(next.getId());
        assertThat(state.next()).isNull();
        assertThat(state.triggerType()).isNull();
    }

    @Test
    void processScheduleIfReady_atTime_waitsWhenNotDue() {
        var current = createChallenge("Current");
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.AT_TIME,
                LocalDateTime.now().plusHours(1));

        assertThat(service.processScheduleIfReady()).isFalse();
        assertThat(service.getState(null).current().id()).isEqualTo(current.getId());
    }

    @Test
    void processScheduleIfReady_whenCurrentEnds_firesAfterEndsAt() {
        var current = createChallenge("Current");
        // Force endsAt to past
        current.setEndsAt(LocalDateTime.now().minusMinutes(1));
        challengeRepo.save(current);
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.WHEN_CURRENT_ENDS, null);

        boolean fired = service.processScheduleIfReady();

        assertThat(fired).isTrue();
        assertThat(service.getState(null).current().id()).isEqualTo(next.getId());
    }

    @Test
    void processScheduleIfReady_whenCurrentEnds_waitsWhileActive() {
        var current = createChallenge("Current"); // endsAt is +7 days
        var next = createChallenge("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.WHEN_CURRENT_ENDS, null);

        assertThat(service.processScheduleIfReady()).isFalse();
        assertThat(service.getState(null).current().id()).isEqualTo(current.getId());
    }
}
