package hr.tvz.artdrop.artdropapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDTO(
        Long id,
        Long buyerUserId,
        Long artistUserId,
        Long artworkId,
        String artworkTitle,
        String artworkCoverPublicId,
        Integer quantity,
        String status,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal platformFee,
        BigDecimal total,
        String currency,
        ShippingAddressSnapshot shippingAddress,
        String trackingNumber,
        String shippingCarrier,
        String cancellationReason,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt
) {
    public record ShippingAddressSnapshot(
            String recipientName,
            String line1,
            String line2,
            String city,
            String postalCode,
            String country,
            String phone
    ) {}
}
