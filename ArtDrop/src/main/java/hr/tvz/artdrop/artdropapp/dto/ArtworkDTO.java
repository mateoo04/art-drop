package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ArtworkDTO(
        Long id,
        String title,
        String medium,
        List<String> tags,
        LocalDateTime publishedAt,
        Integer likeCount,
        Integer commentCount
) {}