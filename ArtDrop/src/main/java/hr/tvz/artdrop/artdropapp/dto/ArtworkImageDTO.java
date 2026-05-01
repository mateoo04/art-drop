package hr.tvz.artdrop.artdropapp.dto;

public record ArtworkImageDTO(
        Long id,
        String publicId,
        Integer sortOrder,
        boolean isCover,
        String caption
) {}
