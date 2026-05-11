package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.Pricing;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingServiceTest {

    private final PricingService pricing = new PricingServiceImpl(
            new BigDecimal("0.10"),
            new BigDecimal("10.00")
    );

    @Test
    void computesOriginalSingleQuantity() {
        Pricing p = pricing.compute(new BigDecimal("100.00"), 1);
        assertThat(p.subtotal()).isEqualByComparingTo("100.00");
        assertThat(p.shippingFee()).isEqualByComparingTo("10.00");
        assertThat(p.platformFee()).isEqualByComparingTo("10.00");
        assertThat(p.total()).isEqualByComparingTo("110.00");
    }

    @Test
    void computesEditionMultipleQuantity() {
        Pricing p = pricing.compute(new BigDecimal("50.00"), 3);
        assertThat(p.subtotal()).isEqualByComparingTo("150.00");
        assertThat(p.shippingFee()).isEqualByComparingTo("10.00");
        assertThat(p.platformFee()).isEqualByComparingTo("15.00");
        assertThat(p.total()).isEqualByComparingTo("160.00");
    }

    @Test
    void roundsPlatformFeeHalfUp() {
        Pricing p = pricing.compute(new BigDecimal("33.33"), 1);
        assertThat(p.platformFee()).isEqualByComparingTo("3.33");
        assertThat(p.total()).isEqualByComparingTo("43.33");
    }

    @Test
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> pricing.compute(new BigDecimal("10.00"), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> pricing.compute(new BigDecimal("10.00"), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullPrice() {
        assertThatThrownBy(() -> pricing.compute(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
