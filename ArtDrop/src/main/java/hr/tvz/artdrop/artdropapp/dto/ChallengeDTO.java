package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChallengeDTO(
        Long id,
        String title,
        String description,
        String quote,
        String kind,
        String status,
        String theme,
        String coverImageUrl,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        long submissionCount,
        List<SubmissionThumbnailDTO> submissions
) {}
