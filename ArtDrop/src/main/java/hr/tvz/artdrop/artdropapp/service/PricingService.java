package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.Pricing;

import java.math.BigDecimal;

public interface PricingService {
    Pricing compute(BigDecimal unitPrice, int quantity);
}
