package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CheckoutSessionResponse;
import hr.tvz.artdrop.artdropapp.dto.CreateCheckoutSessionCommand;

public interface CheckoutService {
    CheckoutSessionResponse createSession(CreateCheckoutSessionCommand cmd, String currentUsername);

    CheckoutServiceImpl.PreparedOrder preparePendingOrder(CreateCheckoutSessionCommand cmd, String currentUsername);

    void attachStripeSession(Long orderId, String sessionId);
}
