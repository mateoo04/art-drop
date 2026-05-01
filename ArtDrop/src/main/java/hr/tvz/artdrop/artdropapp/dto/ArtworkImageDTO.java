package hr.tvz.artdrop.artdropapp.dto;

public record ArtworkImageDTO(
        Long id,
        String imageUrl,
        Integer sortOrder,
        boolean isCover,
        String caption
) {}
