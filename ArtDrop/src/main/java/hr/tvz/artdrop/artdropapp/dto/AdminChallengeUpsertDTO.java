package hr.tvz.artdrop.artdropapp.dto;

import hr.tvz.artdrop.artdropapp.model.ChallengeKind;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AdminChallengeUpsertDTO(
        @NotBlank String title,
        String description,
        String quote,
        @NotNull ChallengeKind kind,
        @NotNull ChallengeStatus status,
        String theme,
        String coverImageUrl,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
