package hr.tvz.artdrop.artdropapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeFeedItemDTO(
        String kind,
        ArtworkDTO artwork,
        ChallengeDTO challenge
) {}
