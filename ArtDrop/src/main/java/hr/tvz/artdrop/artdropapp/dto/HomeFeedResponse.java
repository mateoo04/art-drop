package hr.tvz.artdrop.artdropapp.dto;

import java.util.List;

public record HomeFeedResponse(
        List<ArtworkDTO> items,
        String nextCursor,
        boolean hasMore
) {}
