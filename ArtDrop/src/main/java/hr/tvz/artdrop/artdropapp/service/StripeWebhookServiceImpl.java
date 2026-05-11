package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.model.StripeWebhookEvent;
import hr.tvz.artdrop.artdropapp.repository.StripeWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookServiceImpl.class);

    private final StripeGateway stripeGateway;
    private final OrderService orderService;
    private final StripeWebhookEventRepository eventRepository;
    private final Clock clock;

    public StripeWebhookServiceImpl(StripeGateway stripeGateway,
                                     OrderService orderService,
                                     StripeWebhookEventRepository eventRepository,
                                     Clock clock) {
        this.stripeGateway = stripeGateway;
        this.orderService = orderService;
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Override
    public void handleEvent(String rawPayload, String signatureHeader) {
        Event event;
        try {
            event = stripeGateway.verifyAndParseEvent(rawPayload, signatureHeader);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("invalid stripe signature");
        }

        if (eventRepository.existsById(event.getId())) {
            log.debug("Skipping duplicate stripe event {}", event.getId());
            return;
        }
        try {
            eventRepository.save(new StripeWebhookEvent(event.getId(), event.getType(), LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException dup) {
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> onSessionCompleted(event);
            case "checkout.session.expired", "payment_intent.payment_failed" -> onSessionFailed(event);
            case "charge.refunded" -> onChargeRefunded(event);
            default -> log.debug("Ignoring unhandled stripe event type {}", event.getType());
        }
    }

    private void onSessionCompleted(Event event) {
        Optional<Session> sessionOpt = deserialize(event, Session.class);
        if (sessionOpt.isEmpty()) {
            log.warn("checkout.session.completed missing session object on event {}", event.getId());
            return;
        }
        Session session = sessionOpt.get();
        String orderIdRaw = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
        if (orderIdRaw == null) {
            log.warn("checkout.session.completed missing orderId metadata, session={}", session.getId());
            return;
        }
        Long orderId = Long.parseLong(orderIdRaw);
        try {
            orderService.markPaid(orderId, session.getPaymentIntent());
        } catch (InventoryUnavailableException race) {
            log.error("Race-loss on order {}: {}. Refunding payment intent {}",
                    orderId, race.getMessage(), session.getPaymentIntent());
            try {
                if (session.getPaymentIntent() != null) {
                    stripeGateway.createRefund(session.getPaymentIntent());
                }
            } catch (Exception refundEx) {
                log.error("Failed to refund race-loss payment {}: {}",
                        session.getPaymentIntent(), refundEx.getMessage());
            }
            orderService.markCancelledFromWebhook(session.getId());
        }
    }

    private void onSessionFailed(Event event) {
        Optional<Session> sessionOpt = deserialize(event, Session.class);
        if (sessionOpt.isPresent()) {
            orderService.markCancelledFromWebhook(sessionOpt.get().getId());
            return;
        }
        // payment_intent.payment_failed carries a PaymentIntent, not a Session;
        // find the matching order by PI.
        Optional<PaymentIntent> piOpt = deserialize(event, PaymentIntent.class);
        if (piOpt.isPresent()) {
            // We don't have direct session id here; mark by PI is not implemented.
            // The session.expired event will arrive eventually and clean up.
            log.debug("payment_intent.payment_failed seen for pi={}; awaiting session.expired", piOpt.get().getId());
        }
    }

    private void onChargeRefunded(Event event) {
        Optional<com.stripe.model.Charge> chargeOpt = deserialize(event, com.stripe.model.Charge.class);
        if (chargeOpt.isEmpty() || chargeOpt.get().getPaymentIntent() == null) return;
        orderService.markRefundedFromWebhook(chargeOpt.get().getPaymentIntent());
    }

    // Falls back to deserializeUnsafe() when the SDK's API version differs from the event's
    // (e.g. account on a preview API version). The fallback skips the version compat check;
    // signature has already been verified upstream.
    @SuppressWarnings("unchecked")
    private <T extends StripeObject> Optional<T> deserialize(Event event, Class<T> type) {
        EventDataObjectDeserializer deser = event.getDataObjectDeserializer();
        Optional<StripeObject> obj = deser.getObject();
        if (obj.isEmpty()) {
            try {
                obj = Optional.ofNullable(deser.deserializeUnsafe());
            } catch (Exception e) {
                log.warn("deserializeUnsafe failed for event {}: {}", event.getId(), e.getMessage());
                return Optional.empty();
            }
        }
        return obj.filter(type::isInstance).map(o -> (T) o);
    }
}
