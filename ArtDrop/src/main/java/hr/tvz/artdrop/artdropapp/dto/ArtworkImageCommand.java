package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ArtworkImageCommand(
        @NotBlank
        @Pattern(regexp = "(?i)^https?://\\S+$", message = "imageUrl must be an http(s) URL")
        String imageUrl,
        Integer sortOrder,
        Boolean isCover,
        String caption
) {}
