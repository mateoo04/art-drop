package hr.tvz.artdrop.artdropapp.dto;

import java.time.Duration;

public record FeaturedStatusSnapshot(
        String currentTitle,
        Duration timeUntilEnd,
        String nextSwapDescription
) {}
