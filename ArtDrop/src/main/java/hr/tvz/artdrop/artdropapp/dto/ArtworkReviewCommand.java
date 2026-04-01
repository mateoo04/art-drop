package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ArtworkReviewCommand(
        @NotNull
        @NotBlank
        String title,
        @NotBlank
        @Size(max = 80)
        String reviewer,
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "referenceUrl must be an http(s) URL")
        String referenceUrl,
        @PositiveOrZero
        Integer score
) {
}
