package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitSellerApplicationCommand(
        @NotBlank
        @Size(min = 30, max = 1000, message = "Message must be 30-1000 characters")
        String message
) {}
