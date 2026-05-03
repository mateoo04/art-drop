package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record AdminChallengeRowDTO(
        Long id,
        String title,
        String description,
        String quote,
        String kind,
        String status,
        String theme,
        String coverImageUrl,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        long submissionCount
) {}
