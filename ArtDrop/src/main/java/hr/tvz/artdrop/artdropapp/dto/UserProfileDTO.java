package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record UserProfileDTO(
        Long id,
        String username,
        String slug,
        String displayName,
        String bio,
        String avatarUrl,
        LocalDateTime createdAt,
        Integer artworkCount,
        Integer circleSize,
        Integer followingCount,
        boolean isSelf
) {}
