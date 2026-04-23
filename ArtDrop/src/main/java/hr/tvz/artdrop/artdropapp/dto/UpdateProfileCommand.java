package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileCommand(
        @Size(min = 2, max = 100)
        String displayName,
        @Size(max = 1000)
        String bio,
        @Size(max = 1000)
        String avatarUrl
) {}
