package hr.tvz.artdrop.artdropapp.support;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import hr.tvz.artdrop.artdropapp.service.StripeGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic test double for StripeGateway. Records calls, returns predictable IDs,
 * and supports priming events that tests can replay into the webhook flow.
 */
@Component
@Profile("test")
public class FakeStripeGateway implements StripeGateway {

    private final AtomicLong sessionSeq = new AtomicLong(1000);
    private final AtomicLong refundSeq = new AtomicLong(2000);
    public final Map<String, CheckoutSessionRequest> sessionsBySessionId = new HashMap<>();
    public final Map<String, String> paymentIntentBySessionId = new HashMap<>();
    public final java.util.Set<String> expiredSessionIds = new java.util.HashSet<>();
    public volatile boolean rejectSignature = false;
    public volatile Event nextEvent;
    public volatile int refundCount = 0;

    @Override
    public CheckoutSessionResult createCheckoutSession(CheckoutSessionRequest req) {
        String sessionId = "cs_test_" + sessionSeq.incrementAndGet();
        String paymentIntentId = "pi_test_" + sessionId;
        sessionsBySessionId.put(sessionId, req);
        paymentIntentBySessionId.put(sessionId, paymentIntentId);
        return new CheckoutSessionResult(sessionId, "https://stripe.test/checkout/" + sessionId);
    }

    @Override
    public Session retrieveSession(String sessionId) {
        Session s = new Session();
        s.setId(sessionId);
        return s;
    }

    @Override
    public void expireSession(String sessionId) {
        expiredSessionIds.add(sessionId);
    }

    @Override
    public String createRefund(String paymentIntentId) {
        refundCount++;
        return "re_test_" + refundSeq.incrementAndGet();
    }

    @Override
    public Event verifyAndParseEvent(String rawPayload, String signatureHeader) throws SignatureVerificationException {
        if (rejectSignature) {
            throw new SignatureVerificationException("test rejection", signatureHeader);
        }
        if (nextEvent == null) {
            throw new IllegalStateException("FakeStripeGateway.nextEvent not primed");
        }
        Event e = nextEvent;
        nextEvent = null;
        return e;
    }

    public void reset() {
        sessionsBySessionId.clear();
        paymentIntentBySessionId.clear();
        expiredSessionIds.clear();
        rejectSignature = false;
        nextEvent = null;
        refundCount = 0;
    }
}
