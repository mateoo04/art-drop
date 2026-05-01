package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeSellerCommand(
        @NotBlank
        @Size(max = 500)
        String reason
) {}
