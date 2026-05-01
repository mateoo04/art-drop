package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentDTO(
        Long id,
        String text,
        LocalDateTime createdAt,
        Long authorId,
        String authorDisplayName,
        String authorSlug,
        String authorAvatarUrl,
        boolean isAuthor,
        Long parentCommentId,
        int replyCount,
        List<CommentDTO> replies
) {}
