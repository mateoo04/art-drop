package hr.tvz.artdrop.artdropapp.job;

import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.service.FeaturedChallengeService;
import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "artdrop.featured-rotation-cron=0/2 * * * * ?",
        "spring.quartz.auto-startup=true"
})
class FeaturedChallengeRotationJobIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private FeaturedChallengeService service;
    @Autowired private ChallengeJpaRepository challengeRepo;
    @Autowired private Scheduler scheduler;

    @Test
    void atTimeTrigger_firesWhenJobRuns() throws Exception {
        Challenge current = save("Current");
        Challenge next = save("Next");
        service.setCurrent(current.getId());
        service.scheduleReplacement(next.getId(),
                FeaturedTriggerType.AT_TIME, LocalDateTime.now().minusSeconds(1));

        if (!scheduler.isStarted()) scheduler.start();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() ->
                        assertThat(service.getState(null).current().id()).isEqualTo(next.getId()));
    }

    private Challenge save(String title) {
        Challenge c = new Challenge();
        c.setTitle(title);
        c.setStatus(ChallengeStatus.ACTIVE);
        c.setStartsAt(LocalDateTime.now().minusDays(1));
        c.setEndsAt(LocalDateTime.now().plusDays(7));
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return challengeRepo.save(c);
    }
}
