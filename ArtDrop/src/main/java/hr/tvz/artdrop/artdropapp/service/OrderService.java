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

    /**
     * Buyer-initiated cancel.
     * PENDING_PAYMENT → expires the Stripe session, releases the artwork reservation, deletes the row.
     * PAID            → triggers Stripe refund, restores inventory, transitions to REFUNDED.
     */
    Order cancelByBuyer(Long orderId, Long buyerUserId, String reason);

    /**
     * Deletes any PENDING_PAYMENT order this buyer holds for the given artwork.
     * Best-effort Stripe session expiration. Used when a reservation is released externally
     * so that no orphan order is left behind.
     */
    void deletePendingForBuyerAndArtwork(Long buyerUserId, Long artworkId);

    /**
     * Deletes PENDING_PAYMENT orders created more than {@code graceMinutes} ago.
     * Best-effort Stripe session expiration per row. Returns the number of orders deleted.
     */
    int deleteAbandonedPendingOrders(int graceMinutes);

    /** Idempotent webhook handler for charge.refunded. */
    void markRefundedFromWebhook(String paymentIntentId);

    /** session.expired / payment_intent.payment_failed handler. */
    void markCancelledFromWebhook(String sessionId);
}
