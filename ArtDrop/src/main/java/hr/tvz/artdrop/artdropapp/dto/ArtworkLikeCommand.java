package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ArtworkLikeCommand(
        @NotNull
        @NotBlank
        String title,
        @NotBlank
        @Pattern(regexp = "^(?i)(LIKE|UNLIKE)$", message = "action must be LIKE or UNLIKE")
        String action,
        @NotBlank
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "sourceUrl must be an http(s) URL")
        String sourceUrl,
        @PositiveOrZero
        Integer delta
) {
}
