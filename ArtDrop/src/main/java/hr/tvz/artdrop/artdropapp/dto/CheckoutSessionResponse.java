package hr.tvz.artdrop.artdropapp.dto;

public record CheckoutSessionResponse(
        Long orderId,
        String checkoutUrl
) {}
