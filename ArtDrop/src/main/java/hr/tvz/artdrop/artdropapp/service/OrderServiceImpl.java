package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.StripeException;
import hr.tvz.artdrop.artdropapp.dto.Pricing;
import hr.tvz.artdrop.artdropapp.exception.IllegalOrderStateException;
import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.OrderStatus;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final EditionInventoryService editionInventoryService;
    private final StripeGateway stripeGateway;
    private final Clock clock;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ArtworkJpaRepository artworkRepository,
                            EditionInventoryService editionInventoryService,
                            StripeGateway stripeGateway,
                            Clock clock) {
        this.orderRepository = orderRepository;
        this.artworkRepository = artworkRepository;
        this.editionInventoryService = editionInventoryService;
        this.stripeGateway = stripeGateway;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Order createPending(User buyer, Artwork artwork, int quantity, Pricing pricing,
                               ShippingAddress address, String currency) {
        LocalDateTime now = LocalDateTime.now(clock);
        Order o = new Order();
        o.setBuyerUserId(buyer.getId());
        o.setArtistUserId(artwork.getAuthor().getId());
        o.setArtworkId(artwork.getId());
        o.setQuantity(quantity);
        o.setStatus(OrderStatus.PENDING_PAYMENT);
        o.setSubtotal(pricing.subtotal());
        o.setShippingFee(pricing.shippingFee());
        o.setPlatformFee(pricing.platformFee());
        o.setTotal(pricing.total());
        o.setCurrency(currency);
        o.setShipRecipientName(address.getRecipientName());
        o.setShipLine1(address.getLine1());
        o.setShipLine2(address.getLine2());
        o.setShipCity(address.getCity());
        o.setShipPostalCode(address.getPostalCode());
        o.setShipCountry(address.getCountry());
        o.setShipPhone(address.getPhone());
        o.setCreatedAt(now);
        o.setUpdatedAt(now);
        return orderRepository.save(o);
    }

    @Override
    @Transactional
    public Order markPaid(Long orderId, String stripePaymentIntentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalOrderStateException("order not found: " + orderId));
        if (order.getStatus() == OrderStatus.PAID
                || order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED) {
            return order; // idempotent
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalOrderStateException(
                    "cannot mark PAID from " + order.getStatus());
        }

        Artwork artwork = artworkRepository.findByIdForUpdate(order.getArtworkId())
                .orElseThrow(() -> new IllegalOrderStateException(
                        "artwork missing for order " + orderId));

        // Race guard
        if (artwork.getSaleType() == SaleType.ORIGINAL) {
            if (artwork.getSaleState() == SaleState.SOLD) {
                throw new InventoryUnavailableException(
                        "original already sold to a different buyer");
            }
        } else {
            if (!editionInventoryService.canFulfill(artwork, order.getQuantity())) {
                throw new InventoryUnavailableException(
                        "edition exhausted while payment was processing");
            }
        }

        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        order.setStripePaymentIntentId(stripePaymentIntentId);
        order.setUpdatedAt(now);

        if (artwork.getSaleType() == SaleType.ORIGINAL) {
            artwork.setSaleState(SaleState.SOLD);
            artwork.setReservedByUserId(null);
            artwork.setReservedUntil(null);
        } else {
            Integer remaining = editionInventoryService.remaining(artwork);
            if (remaining != null && remaining - order.getQuantity() <= 0) {
                artwork.setSaleState(SaleState.SOLD);
            }
        }
        artworkRepository.save(artwork);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order markShipped(Long orderId, Long callerUserId, String trackingNumber, String carrier) {
        Order order = loadAndAssertArtist(orderId, callerUserId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalOrderStateException(
                    "cannot ship from " + order.getStatus());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(now);
        order.setTrackingNumber(trackingNumber);
        order.setShippingCarrier(carrier);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order markDelivered(Long orderId, Long callerUserId) {
        Order order = loadAndAssertArtist(orderId, callerUserId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalOrderStateException(
                    "cannot deliver from " + order.getStatus());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(now);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order cancelByBuyer(Long orderId, Long buyerUserId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalOrderStateException("order not found: " + orderId));
        if (!order.getBuyerUserId().equals(buyerUserId)) {
            throw new IllegalOrderStateException("order does not belong to caller");
        }

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            deletePendingOrder(order);
            return order;
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalOrderStateException(
                    "cannot cancel from " + order.getStatus()
                            + "; only PENDING_PAYMENT or PAID is cancellable by buyer");
        }
        if (order.getStripePaymentIntentId() == null) {
            throw new IllegalOrderStateException("missing payment intent on PAID order");
        }

        try {
            stripeGateway.createRefund(order.getStripePaymentIntentId());
        } catch (StripeException e) {
            throw new IllegalOrderStateException("refund failed: " + e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.REFUNDED);
        order.setCancelledAt(now);
        order.setRefundedAt(now);
        order.setCancellationReason(reason);
        order.setUpdatedAt(now);

        Artwork artwork = artworkRepository.findByIdForUpdate(order.getArtworkId()).orElse(null);
        if (artwork != null) {
            if (artwork.getSaleType() == SaleType.ORIGINAL) {
                artwork.setSaleState(SaleState.AVAILABLE);
                artwork.setReservedByUserId(null);
                artwork.setReservedUntil(null);
            } else {
                if (artwork.getSaleState() == SaleState.SOLD) {
                    artwork.setSaleState(SaleState.AVAILABLE);
                }
            }
            artworkRepository.save(artwork);
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deletePendingForBuyerAndArtwork(Long buyerUserId, Long artworkId) {
        orderRepository.findFirstByBuyerUserIdAndArtworkIdAndStatus(
                        buyerUserId, artworkId, OrderStatus.PENDING_PAYMENT)
                .ifPresent(this::deletePendingOrder);
    }

    @Override
    @Transactional
    public int deleteAbandonedPendingOrders(int graceMinutes) {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusMinutes(graceMinutes);
        List<Order> abandoned = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT, cutoff);
        for (Order o : abandoned) {
            deletePendingOrder(o);
        }
        return abandoned.size();
    }

    private void deletePendingOrder(Order order) {
        if (order.getStripeCheckoutSessionId() != null) {
            try {
                stripeGateway.expireSession(order.getStripeCheckoutSessionId());
            } catch (StripeException e) {
                log.warn("Failed to expire Stripe session {} for order {}: {}",
                        order.getStripeCheckoutSessionId(), order.getId(), e.getMessage());
            }
        }
        Artwork artwork = artworkRepository.findByIdForUpdate(order.getArtworkId()).orElse(null);
        if (artwork != null
                && artwork.getSaleType() == SaleType.ORIGINAL
                && artwork.getSaleState() == SaleState.RESERVED
                && order.getBuyerUserId().equals(artwork.getReservedByUserId())) {
            artwork.setSaleState(SaleState.AVAILABLE);
            artwork.setReservedByUserId(null);
            artwork.setReservedUntil(null);
            artworkRepository.save(artwork);
        }
        orderRepository.delete(order);
    }

    @Override
    @Transactional
    public void markRefundedFromWebhook(String paymentIntentId) {
        Optional<Order> opt = orderRepository.findByStripePaymentIntentId(paymentIntentId);
        if (opt.isEmpty()) return;
        Order order = opt.get();
        if (order.getStatus() == OrderStatus.REFUNDED) return; // idempotent
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.REFUNDED);
        if (order.getRefundedAt() == null) order.setRefundedAt(now);
        order.setUpdatedAt(now);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void markCancelledFromWebhook(String sessionId) {
        Optional<Order> opt = orderRepository.findByStripeCheckoutSessionId(sessionId);
        if (opt.isEmpty()) return;
        Order order = opt.get();
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) return;
        LocalDateTime now = LocalDateTime.now(clock);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        // Release ORIGINAL reservation if held by this order's buyer.
        Artwork artwork = artworkRepository.findByIdForUpdate(order.getArtworkId()).orElse(null);
        if (artwork != null
                && artwork.getSaleType() == SaleType.ORIGINAL
                && artwork.getSaleState() == SaleState.RESERVED
                && order.getBuyerUserId().equals(artwork.getReservedByUserId())) {
            artwork.setSaleState(SaleState.AVAILABLE);
            artwork.setReservedByUserId(null);
            artwork.setReservedUntil(null);
            artworkRepository.save(artwork);
        }
    }

    private Order loadAndAssertArtist(Long orderId, Long callerUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalOrderStateException("order not found: " + orderId));
        if (!order.getArtistUserId().equals(callerUserId)) {
            throw new IllegalOrderStateException("only the artist can transition this order");
        }
        return order;
    }
}
