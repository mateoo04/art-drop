package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CollectionDTO(
        Long id,
        String name,
        String description,
        List<Long> artworkIds,
        LocalDateTime createdAt,
        Boolean isPublic
) {}
