package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record MyReservationDTO(
        Long artworkId,
        String title,
        String coverPublicId,
        LocalDateTime reservedUntil
) {}
