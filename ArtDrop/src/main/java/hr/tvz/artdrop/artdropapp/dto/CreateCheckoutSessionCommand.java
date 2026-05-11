package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCheckoutSessionCommand(
        @NotNull Long artworkId,
        @Positive Integer quantity,
        Long addressId,
        @Valid ShippingAddressCommand inlineAddress
) {}
