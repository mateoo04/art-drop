package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.Map;

@Component
@Profile("!test")
public class StripeGatewayImpl implements StripeGateway {

    private final String webhookSecret;

    public StripeGatewayImpl(@Value("${stripe.webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest req) throws StripeException {
        long amountInMinor = req.totalAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(req.successUrl())
                .setCancelUrl(req.cancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(req.currency().toLowerCase())
                                .setUnitAmount(amountInMinor)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(req.artworkTitle())
                                        .build())
                                .build())
                        .build());

        if (req.metadata() != null) {
            for (Map.Entry<String, String> e : req.metadata().entrySet()) {
                builder.putMetadata(e.getKey(), e.getValue());
            }
        }

        Session session = Session.create(builder.build());
        return new CheckoutSessionResult(session.getId(), session.getUrl());
    }

    @Override
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }

    @Override
    public void expireSession(String sessionId) throws StripeException {
        Session.retrieve(sessionId).expire();
    }

    @Override
    public String createRefund(String paymentIntentId) throws StripeException {
        Refund refund = Refund.create(RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build());
        return refund.getId();
    }

    @Override
    public Event verifyAndParseEvent(String rawPayload, String signatureHeader) throws SignatureVerificationException {
        return Webhook.constructEvent(rawPayload, signatureHeader, webhookSecret);
    }
}
