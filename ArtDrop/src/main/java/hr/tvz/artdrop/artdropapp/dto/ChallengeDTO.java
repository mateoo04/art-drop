package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record ChallengeDTO(
        Long id,
        Long artworkId,
        String title,
        String description,
        String theme,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status
) {
}
