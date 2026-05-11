package hr.tvz.artdrop.artdropapp.dto;

import java.math.BigDecimal;

public record Pricing(
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal platformFee,
        BigDecimal total
) {}
