package hr.tvz.artdrop.artdropapp.dto;

import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleReplacementRequest(
        @NotNull Long nextChallengeId,
        @NotNull FeaturedTriggerType triggerType,
        LocalDateTime triggerAt
) {}
