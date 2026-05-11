package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.Size;

public record ShipOrderCommand(
        @Size(max = 100) String trackingNumber,
        @Size(max = 80) String carrier
) {}
