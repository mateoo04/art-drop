package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;

public record ArtworkImageCommand(
        @NotBlank
        String publicId,
        Integer sortOrder,
        Boolean isCover,
        String caption
) {}
