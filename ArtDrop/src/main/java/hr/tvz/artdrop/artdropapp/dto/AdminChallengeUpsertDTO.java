package hr.tvz.artdrop.artdropapp.dto;

import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminChallengeUpsertDTO(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        @Size(max = 500) String quote,
        @NotNull ChallengeStatus status,
        @Size(max = 100) String theme,
        @Size(max = 500) String coverImageUrl,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
