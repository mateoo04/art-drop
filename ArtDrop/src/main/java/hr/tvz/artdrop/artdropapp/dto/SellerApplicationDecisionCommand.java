package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.Size;

public record SellerApplicationDecisionCommand(
        @Size(max = 500)
        String reason
) {}
