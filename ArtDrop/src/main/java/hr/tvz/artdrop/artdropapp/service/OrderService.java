package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.Pricing;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.model.User;

public interface OrderService {

    /** Creates a new order in PENDING_PAYMENT state. Snapshots address + pricing. */
    Order createPending(User buyer, Artwork artwork, int quantity, Pricing pricing,
                        ShippingAddress address, String currency);

    /**
     * Transitions an order from PENDING_PAYMENT → PAID under the artwork's row lock.
     * Re-validates inventory; refunds + cancels on race-loss. Idempotent on already-PAID.
     * @return the updated order
     */
    Order markPaid(Long orderId, String stripePaymentIntentId);

    Order markShipped(Long orderId, Long callerUserId, String trackingNumber, String carrier);

    Order markDelivered(Long orderId, Long callerUserId);

    /** Buyer-initiated cancel of a PAID order. Triggers Stripe refund + inventory restore. */
    Order cancelByBuyer(Long orderId, Long buyerUserId, String reason);

    /** Idempotent webhook handler for charge.refunded. */
    void markRefundedFromWebhook(String paymentIntentId);

    /** session.expired / payment_intent.payment_failed handler. */
    void markCancelledFromWebhook(String sessionId);
}
