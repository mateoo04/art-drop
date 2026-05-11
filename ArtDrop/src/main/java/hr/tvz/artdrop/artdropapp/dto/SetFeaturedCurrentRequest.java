package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotNull;

public record SetFeaturedCurrentRequest(@NotNull Long challengeId) {}
