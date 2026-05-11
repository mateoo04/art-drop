package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.FeaturedChallengeStateDTO;
import hr.tvz.artdrop.artdropapp.dto.FeaturedStatusSnapshot;
import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;

import java.time.LocalDateTime;

public interface FeaturedChallengeService {
    FeaturedChallengeStateDTO getState(String viewerUsername);
    void setCurrent(Long challengeId);
    void clearCurrent();
    void scheduleReplacement(Long nextChallengeId, FeaturedTriggerType triggerType, LocalDateTime triggerAt);
    void clearSchedule();
    boolean processScheduleIfReady();
    FeaturedStatusSnapshot getStatusForLogging();
}
