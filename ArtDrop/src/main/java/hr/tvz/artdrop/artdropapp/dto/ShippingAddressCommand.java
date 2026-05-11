package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressCommand(
        @NotBlank @Size(max = 200) String recipientName,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 120) String city,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "country must be ISO-3166-alpha-2 (e.g. HR, US)")
        String country,
        @Size(max = 40) String phone
) {}
