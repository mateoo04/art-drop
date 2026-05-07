package hr.tvz.artdrop.artdropapp.dto;

public record ArtistSummaryDTO(
        Long id,
        String displayName,
        String slug,
        String avatarUrl
) {}
