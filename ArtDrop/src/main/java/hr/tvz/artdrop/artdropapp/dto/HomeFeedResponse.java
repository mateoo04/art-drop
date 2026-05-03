package hr.tvz.artdrop.artdropapp.dto;

import java.util.List;

public record HomeFeedResponse(
        List<HomeFeedItemDTO> items,
        String nextCursor,
        boolean hasMore
) {}
