package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.util.Map;

public interface StripeGateway {

    record CheckoutSessionRequest(
            String orderId,
            String artworkTitle,
            BigDecimal totalAmount,
            String currency,
            String successUrl,
            String cancelUrl,
            Map<String, String> metadata
    ) {}

    record CheckoutSessionResult(
            String sessionId,
            String url
    ) {}

    CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest req) throws StripeException;

    Session retrieveSession(String sessionId) throws StripeException;

    void expireSession(String sessionId) throws StripeException;

    String createRefund(String paymentIntentId) throws StripeException;

    Event verifyAndParseEvent(String rawPayload, String signatureHeader) throws SignatureVerificationException;
}
