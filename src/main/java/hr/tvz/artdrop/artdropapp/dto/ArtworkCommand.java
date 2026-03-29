package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ArtworkCommand(
        @NotNull
        @NotBlank
        String title,
        @NotBlank
        String medium,
        @Size(max = 2000)
        String description,
        @NotBlank
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "imageUrl must be an http(s) URL")
        String imageUrl,
        @PositiveOrZero
        Integer catalogSequence
) {
}
