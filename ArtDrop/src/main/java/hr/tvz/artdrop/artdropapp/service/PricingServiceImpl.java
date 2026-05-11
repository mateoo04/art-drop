package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.Pricing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingServiceImpl implements PricingService {

    private final BigDecimal commissionRate;
    private final BigDecimal shippingFee;

    public PricingServiceImpl(
            @Value("${commerce.platform-commission-rate}") BigDecimal commissionRate,
            @Value("${commerce.shipping-fee-eur}") BigDecimal shippingFee) {
        this.commissionRate = commissionRate;
        this.shippingFee = shippingFee;
    }

    @Override
    public Pricing compute(BigDecimal unitPrice, int quantity) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                                       .setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = subtotal.multiply(commissionRate)
                                         .setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shippingFee).setScale(2, RoundingMode.HALF_UP);
        return new Pricing(subtotal, shippingFee, platformFee, total);
    }
}
