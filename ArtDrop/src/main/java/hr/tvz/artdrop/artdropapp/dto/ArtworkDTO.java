package hr.tvz.artdrop.artdropapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ArtworkDTO(
        Long id,
        String title,
        String medium,
        String description,
        String imageUrl,
        String imageAlt,
        Double aspectRatio,
        BigDecimal price,
        String progressStatus,
        String saleStatus,
        Long artistId,
        String artistDisplayName,
        String artistSlug,
        String artistAvatarUrl,
        List<String> tags,
        LocalDateTime publishedAt,
        Integer likeCount,
        Integer commentCount,
        boolean likedByMe
) {}
