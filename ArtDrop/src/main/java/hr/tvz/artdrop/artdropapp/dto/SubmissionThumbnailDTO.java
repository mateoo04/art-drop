package hr.tvz.artdrop.artdropapp.dto;

public record SubmissionThumbnailDTO(
        Long submissionId,
        Long artworkId,
        String title,
        String imageUrl,
        String imageAlt,
        String artistDisplayName,
        String artistSlug
) {}
