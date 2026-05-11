package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.FeaturedChallengeStateDTO;
import hr.tvz.artdrop.artdropapp.dto.FeaturedStatusSnapshot;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.FeaturedChallenge;
import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.FeaturedChallengeJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class FeaturedChallengeServiceImpl implements FeaturedChallengeService {

    private static final Logger log = LoggerFactory.getLogger(FeaturedChallengeServiceImpl.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FeaturedChallengeJpaRepository featuredRepository;
    private final ChallengeJpaRepository challengeRepository;
    private final ChallengeService challengeService;

    public FeaturedChallengeServiceImpl(
            FeaturedChallengeJpaRepository featuredRepository,
            ChallengeJpaRepository challengeRepository,
            @Lazy ChallengeService challengeService
    ) {
        this.featuredRepository = featuredRepository;
        this.challengeRepository = challengeRepository;
        this.challengeService = challengeService;
    }

    @Override
    @Transactional(readOnly = true)
    public FeaturedChallengeStateDTO getState(String viewerUsername) {
        FeaturedChallenge row = loadOrInit();
        ChallengeDTO current = row.getCurrentChallengeId() == null ? null
                : challengeService.findById(row.getCurrentChallengeId(), viewerUsername).orElse(null);
        ChallengeDTO next = row.getNextChallengeId() == null ? null
                : challengeService.findById(row.getNextChallengeId(), viewerUsername).orElse(null);
        return new FeaturedChallengeStateDTO(
                current,
                next,
                row.getTriggerType() == null ? null : row.getTriggerType().name(),
                row.getTriggerAt()
        );
    }

    @Override
    @Transactional
    public void setCurrent(Long challengeId) {
        Challenge c = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("challenge not found: " + challengeId));
        if (c.getStatus() == ChallengeStatus.ENDED) {
            throw new IllegalArgumentException("cannot feature an ENDED challenge");
        }
        FeaturedChallenge row = loadOrInit();
        row.setCurrentChallengeId(c.getId());
        row.setNextChallengeId(null);
        row.setTriggerType(null);
        row.setTriggerAt(null);
        row.setUpdatedAt(LocalDateTime.now());
        featuredRepository.save(row);
    }

    @Override
    @Transactional
    public void clearCurrent() {
        FeaturedChallenge row = loadOrInit();
        row.setCurrentChallengeId(null);
        row.setNextChallengeId(null);
        row.setTriggerType(null);
        row.setTriggerAt(null);
        row.setUpdatedAt(LocalDateTime.now());
        featuredRepository.save(row);
    }

    @Override
    @Transactional
    public void scheduleReplacement(Long nextChallengeId, FeaturedTriggerType triggerType, LocalDateTime triggerAt) {
        if (triggerType == null) throw new IllegalArgumentException("triggerType required");
        if (nextChallengeId == null) throw new IllegalArgumentException("nextChallengeId required");
        Challenge nextChallenge = challengeRepository.findById(nextChallengeId)
                .orElseThrow(() -> new IllegalArgumentException("next challenge not found: " + nextChallengeId));
        if (nextChallenge.getStatus() == ChallengeStatus.ENDED) {
            throw new IllegalArgumentException("cannot schedule an ENDED challenge as next featured");
        }
        FeaturedChallenge row = loadOrInit();
        if (nextChallengeId.equals(row.getCurrentChallengeId())) {
            throw new IllegalArgumentException("next must differ from current");
        }
        if (triggerType == FeaturedTriggerType.AT_TIME) {
            if (triggerAt == null) throw new IllegalArgumentException("triggerAt required for AT_TIME");
        } else {
            if (row.getCurrentChallengeId() == null) {
                throw new IllegalArgumentException("WHEN_CURRENT_ENDS requires a current featured challenge");
            }
            Challenge current = challengeRepository.findById(row.getCurrentChallengeId()).orElse(null);
            if (current == null || current.getEndsAt() == null) {
                throw new IllegalArgumentException("WHEN_CURRENT_ENDS requires current.endsAt to be set");
            }
            triggerAt = null;
        }
        row.setNextChallengeId(nextChallenge.getId());
        row.setTriggerType(triggerType);
        row.setTriggerAt(triggerAt);
        row.setUpdatedAt(LocalDateTime.now());
        featuredRepository.save(row);
    }

    @Override
    @Transactional
    public void clearSchedule() {
        FeaturedChallenge row = loadOrInit();
        row.setNextChallengeId(null);
        row.setTriggerType(null);
        row.setTriggerAt(null);
        row.setUpdatedAt(LocalDateTime.now());
        featuredRepository.save(row);
    }

    @Override
    @Transactional
    public boolean processScheduleIfReady() {
        FeaturedChallenge row = featuredRepository.findSingletonForUpdate().orElse(null);
        if (row == null || row.getTriggerType() == null) return false;

        boolean shouldFire = false;
        LocalDateTime now = LocalDateTime.now();
        if (row.getTriggerType() == FeaturedTriggerType.AT_TIME) {
            shouldFire = row.getTriggerAt() != null && !now.isBefore(row.getTriggerAt());
        } else if (row.getTriggerType() == FeaturedTriggerType.WHEN_CURRENT_ENDS) {
            Challenge current = row.getCurrentChallengeId() == null ? null
                    : challengeRepository.findById(row.getCurrentChallengeId()).orElse(null);
            shouldFire = current != null && current.getEndsAt() != null && !now.isBefore(current.getEndsAt());
        }
        if (!shouldFire) return false;

        Long nextId = row.getNextChallengeId();
        if (nextId == null) {
            log.warn("Featured rotation: trigger fired but next_challenge_id is null; clearing schedule");
            row.setTriggerType(null);
            row.setTriggerAt(null);
            featuredRepository.save(row);
            return false;
        }
        Optional<Challenge> nextOpt = challengeRepository.findById(nextId);
        if (nextOpt.isEmpty()) {
            log.warn("Featured rotation: next challenge {} not found; clearing schedule", nextId);
            row.setNextChallengeId(null);
            row.setTriggerType(null);
            row.setTriggerAt(null);
            featuredRepository.save(row);
            return false;
        }

        Long oldId = row.getCurrentChallengeId();
        row.setCurrentChallengeId(nextId);
        row.setNextChallengeId(null);
        row.setTriggerType(null);
        row.setTriggerAt(null);
        row.setUpdatedAt(LocalDateTime.now());
        featuredRepository.save(row);
        log.info("Featured rotated: {} -> {}", oldId, nextId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public FeaturedStatusSnapshot getStatusForLogging() {
        FeaturedChallenge row = loadOrInit();
        Challenge current = row.getCurrentChallengeId() == null ? null
                : challengeRepository.findById(row.getCurrentChallengeId()).orElse(null);
        String title = current == null ? "(none)" : current.getTitle();
        Duration timeUntilEnd = (current == null || current.getEndsAt() == null) ? Duration.ZERO
                : Duration.between(LocalDateTime.now(), current.getEndsAt());
        String nextSwap;
        if (row.getTriggerType() == null) {
            nextSwap = "none scheduled";
        } else if (row.getTriggerType() == FeaturedTriggerType.AT_TIME) {
            nextSwap = "at " + (row.getTriggerAt() == null ? "?" : row.getTriggerAt().format(TS_FMT));
        } else {
            nextSwap = "when current ends";
        }
        return new FeaturedStatusSnapshot(title, timeUntilEnd, nextSwap);
    }

    private FeaturedChallenge loadOrInit() {
        return featuredRepository.findById(FeaturedChallenge.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("featured_challenge singleton row missing"));
    }
}
