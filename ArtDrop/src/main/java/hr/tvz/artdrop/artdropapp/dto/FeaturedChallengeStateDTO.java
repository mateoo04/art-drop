package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record FeaturedChallengeStateDTO(
        ChallengeDTO current,
        ChallengeDTO next,
        String triggerType,
        LocalDateTime triggerAt
) {}
