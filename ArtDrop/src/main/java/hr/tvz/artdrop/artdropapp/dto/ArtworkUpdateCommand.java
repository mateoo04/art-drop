package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ArtworkUpdateCommand(
        String title,
        String medium,
        @Size(max = 2000)
        String description,
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "imageUrl must be an http(s) URL")
        String imageUrl
) {
}
