package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChallengeDTO(
        Long id,
        String title,
        String description,
        String quote,
        boolean isFeatured,
        String status,
        String theme,
        String coverImageUrl,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        long submissionCount,
        List<SubmissionThumbnailDTO> submissions,
        boolean viewerHasEntry,
        Long viewerEntryArtworkId
) {}
