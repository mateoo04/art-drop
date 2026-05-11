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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

class OrderServiceStateMachineTest {

    private final OrderRepository orderRepo = Mockito.mock(OrderRepository.class);
    private final ArtworkJpaRepository artworkRepo = Mockito.mock(ArtworkJpaRepository.class);
    private final EditionInventoryService editionInventoryService = Mockito.mock(EditionInventoryService.class);
    private final StripeGateway stripeGateway = Mockito.mock(StripeGateway.class);
    private final Clock fixed = Clock.fixed(
            LocalDateTime.of(2026, 5, 11, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());
    private final OrderService svc = new OrderServiceImpl(
            orderRepo, artworkRepo, editionInventoryService, stripeGateway, fixed);

    private Order order;
    private Artwork artwork;
    private User buyer;
    private User artist;

    @BeforeEach
    void setup() {
        buyer = new User();
        buyer.setId(10L);
        artist = new User();
        artist.setId(20L);

        artwork = new Artwork();
        artwork.setId(100L);
        artwork.setSaleType(SaleType.ORIGINAL);
        artwork.setSaleState(SaleState.RESERVED);
        artwork.setReservedByUserId(buyer.getId());
        artwork.setAuthor(artist);

        order = new Order();
        order.setId(1L);
        order.setBuyerUserId(buyer.getId());
        order.setArtistUserId(artist.getId());
        order.setArtworkId(artwork.getId());
        order.setQuantity(1);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setStripePaymentIntentId("pi_test_123");
        order.setSubtotal(new BigDecimal("100.00"));
        order.setShippingFee(new BigDecimal("10.00"));
        order.setPlatformFee(new BigDecimal("10.00"));
        order.setTotal(new BigDecimal("110.00"));
        order.setCurrency("EUR");
        order.setCreatedAt(LocalDateTime.now(fixed));
        order.setUpdatedAt(LocalDateTime.now(fixed));

        Mockito.when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        Mockito.when(orderRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(artworkRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(artwork));
        Mockito.when(artworkRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createPending ----------

    @Test
    void createPendingSnapshotsAddressAndPricing() {
        ShippingAddress addr = new ShippingAddress(
                null, buyer.getId(), "Mateo", "Ilica 1", null, "Zagreb", "10000", "HR", null,
                LocalDateTime.now(fixed));
        Pricing p = new Pricing(new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("10.00"), new BigDecimal("110.00"));
        Mockito.when(orderRepo.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(42L);
            return o;
        });

        Order saved = svc.createPending(buyer, artwork, 1, p, addr, "EUR");

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(saved.getBuyerUserId()).isEqualTo(10L);
        assertThat(saved.getArtistUserId()).isEqualTo(20L);
        assertThat(saved.getShipRecipientName()).isEqualTo("Mateo");
        assertThat(saved.getShipCity()).isEqualTo("Zagreb");
        assertThat(saved.getShipCountry()).isEqualTo("HR");
        assertThat(saved.getTotal()).isEqualByComparingTo("110.00");
    }

    // ---------- markPaid ----------

    @Test
    void markPaidTransitionsAndMarksOriginalSold() {
        svc.markPaid(1L, "pi_test_new");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isEqualTo(LocalDateTime.now(fixed));
        assertThat(order.getStripePaymentIntentId()).isEqualTo("pi_test_new");
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.SOLD);
        assertThat(artwork.getReservedByUserId()).isNull();
    }

    @Test
    void markPaidIsIdempotentOnAlreadyPaid() {
        order.setStatus(OrderStatus.PAID);
        Order returned = svc.markPaid(1L, "pi_test_new");
        assertThat(returned.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markPaidRejectsRaceLossForOriginalAlreadySold() {
        artwork.setSaleState(SaleState.SOLD);
        artwork.setReservedByUserId(999L);
        assertThatThrownBy(() -> svc.markPaid(1L, "pi_test_new"))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void markPaidRejectsRaceLossForEditionExhausted() {
        artwork.setSaleType(SaleType.EDITION);
        artwork.setEditionSize(5);
        artwork.setSaleState(SaleState.AVAILABLE);
        Mockito.when(editionInventoryService.canFulfill(any(), Mockito.eq(1))).thenReturn(false);

        assertThatThrownBy(() -> svc.markPaid(1L, "pi_test_new"))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void markPaidEditionStaysAvailableWhenInventoryRemains() {
        artwork.setSaleType(SaleType.EDITION);
        artwork.setEditionSize(10);
        artwork.setSaleState(SaleState.AVAILABLE);
        Mockito.when(editionInventoryService.canFulfill(any(), Mockito.eq(1))).thenReturn(true);
        Mockito.when(editionInventoryService.remaining(any())).thenReturn(7);

        svc.markPaid(1L, "pi_test_new");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
    }

    @Test
    void markPaidEditionMovesToSoldWhenLastUnitSold() {
        artwork.setSaleType(SaleType.EDITION);
        artwork.setEditionSize(1);
        artwork.setSaleState(SaleState.AVAILABLE);
        Mockito.when(editionInventoryService.canFulfill(any(), Mockito.eq(1))).thenReturn(true);
        Mockito.when(editionInventoryService.remaining(any())).thenReturn(1);

        svc.markPaid(1L, "pi_test_new");

        assertThat(artwork.getSaleState()).isEqualTo(SaleState.SOLD);
    }

    @Test
    void markPaidRejectsFromCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> svc.markPaid(1L, "pi_test_new"))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    // ---------- markShipped ----------

    @Test
    void markShippedTransitionsFromPaid() {
        order.setStatus(OrderStatus.PAID);
        svc.markShipped(1L, artist.getId(), "TRACK123", "DHL");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("TRACK123");
        assertThat(order.getShippingCarrier()).isEqualTo("DHL");
        assertThat(order.getShippedAt()).isEqualTo(LocalDateTime.now(fixed));
    }

    @Test
    void markShippedRejectsNonArtist() {
        order.setStatus(OrderStatus.PAID);
        assertThatThrownBy(() -> svc.markShipped(1L, 999L, "TRACK", "DHL"))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void markShippedRejectsFromPendingPayment() {
        assertThatThrownBy(() -> svc.markShipped(1L, artist.getId(), "TRACK", "DHL"))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    // ---------- markDelivered ----------

    @Test
    void markDeliveredTransitionsFromShipped() {
        order.setStatus(OrderStatus.SHIPPED);
        svc.markDelivered(1L, artist.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isEqualTo(LocalDateTime.now(fixed));
    }

    @Test
    void markDeliveredRejectsFromPaid() {
        order.setStatus(OrderStatus.PAID);
        assertThatThrownBy(() -> svc.markDelivered(1L, artist.getId()))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    // ---------- cancelByBuyer ----------

    @Test
    void cancelByBuyerRefundsAndRestoresOriginalInventory() throws StripeException {
        order.setStatus(OrderStatus.PAID);
        artwork.setSaleState(SaleState.SOLD);
        Mockito.when(stripeGateway.createRefund("pi_test_123")).thenReturn("re_test_456");

        svc.cancelByBuyer(1L, buyer.getId(), "changed my mind");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getRefundedAt()).isEqualTo(LocalDateTime.now(fixed));
        assertThat(order.getCancellationReason()).isEqualTo("changed my mind");
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);

        ArgumentCaptor<String> piCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(stripeGateway).createRefund(piCaptor.capture());
        assertThat(piCaptor.getValue()).isEqualTo("pi_test_123");
    }

    @Test
    void cancelByBuyerRejectsNonOwner() {
        order.setStatus(OrderStatus.PAID);
        assertThatThrownBy(() -> svc.cancelByBuyer(1L, 999L, null))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void cancelByBuyerRejectsAfterShipped() {
        order.setStatus(OrderStatus.SHIPPED);
        assertThatThrownBy(() -> svc.cancelByBuyer(1L, buyer.getId(), null))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    @Test
    void cancelByBuyerOnPendingDeletesRowAndReleasesReservation() throws StripeException {
        order.setStripeCheckoutSessionId("cs_test_pending");
        Mockito.doNothing().when(stripeGateway).expireSession("cs_test_pending");

        svc.cancelByBuyer(1L, buyer.getId(), null);

        Mockito.verify(stripeGateway).expireSession("cs_test_pending");
        Mockito.verify(orderRepo).delete(order);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(artwork.getReservedByUserId()).isNull();
    }

    @Test
    void cancelByBuyerOnPendingDeletesEvenIfStripeExpireFails() throws StripeException {
        order.setStripeCheckoutSessionId("cs_test_pending");
        Mockito.doThrow(new StripeException("boom", null, null, 500) {})
                .when(stripeGateway).expireSession("cs_test_pending");

        svc.cancelByBuyer(1L, buyer.getId(), null);

        Mockito.verify(orderRepo).delete(order);
    }

    @Test
    void cancelByBuyerRejectsFromCancelled() {
        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> svc.cancelByBuyer(1L, buyer.getId(), null))
                .isInstanceOf(IllegalOrderStateException.class);
    }

    // ---------- pending order cleanup ----------

    @Test
    void deletePendingForBuyerAndArtworkRemovesMatchingRow() throws StripeException {
        order.setStripeCheckoutSessionId("cs_test_pending");
        Mockito.when(orderRepo.findFirstByBuyerUserIdAndArtworkIdAndStatus(
                        buyer.getId(), artwork.getId(), OrderStatus.PENDING_PAYMENT))
                .thenReturn(Optional.of(order));

        svc.deletePendingForBuyerAndArtwork(buyer.getId(), artwork.getId());

        Mockito.verify(stripeGateway).expireSession("cs_test_pending");
        Mockito.verify(orderRepo).delete(order);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
    }

    @Test
    void deletePendingForBuyerAndArtworkIsNoOpWhenNoMatch() {
        Mockito.when(orderRepo.findFirstByBuyerUserIdAndArtworkIdAndStatus(
                        Mockito.anyLong(), Mockito.anyLong(), Mockito.any()))
                .thenReturn(Optional.empty());

        svc.deletePendingForBuyerAndArtwork(buyer.getId(), 999L);

        Mockito.verify(orderRepo, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void deleteAbandonedPendingOrdersDeletesByCutoff() throws StripeException {
        order.setStripeCheckoutSessionId("cs_test_old");
        Mockito.when(orderRepo.findByStatusAndCreatedAtBefore(
                        Mockito.eq(OrderStatus.PENDING_PAYMENT), Mockito.any()))
                .thenReturn(java.util.List.of(order));

        int n = svc.deleteAbandonedPendingOrders(30);

        assertThat(n).isEqualTo(1);
        Mockito.verify(stripeGateway).expireSession("cs_test_old");
        Mockito.verify(orderRepo).delete(order);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        Mockito.verify(orderRepo).findByStatusAndCreatedAtBefore(
                Mockito.eq(OrderStatus.PENDING_PAYMENT), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(LocalDateTime.now(fixed).minusMinutes(30));
    }

    // ---------- webhook handlers ----------

    @Test
    void markRefundedFromWebhookIsIdempotent() {
        order.setStatus(OrderStatus.REFUNDED);
        order.setRefundedAt(LocalDateTime.now(fixed).minusHours(1));
        Mockito.when(orderRepo.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(order));

        svc.markRefundedFromWebhook("pi_test_123");

        assertThat(order.getRefundedAt()).isEqualTo(LocalDateTime.now(fixed).minusHours(1));
    }

    @Test
    void markRefundedFromWebhookTransitionsPaidOrder() {
        order.setStatus(OrderStatus.PAID);
        Mockito.when(orderRepo.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(order));

        svc.markRefundedFromWebhook("pi_test_123");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getRefundedAt()).isEqualTo(LocalDateTime.now(fixed));
    }

    @Test
    void markCancelledFromWebhookReleasesReservation() {
        order.setStripeCheckoutSessionId("cs_test_abc");
        Mockito.when(orderRepo.findByStripeCheckoutSessionId("cs_test_abc"))
                .thenReturn(Optional.of(order));

        svc.markCancelledFromWebhook("cs_test_abc");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(artwork.getReservedByUserId()).isNull();
    }

    @Test
    void markCancelledFromWebhookNoOpForNonPendingOrder() {
        order.setStatus(OrderStatus.PAID);
        order.setStripeCheckoutSessionId("cs_test_abc");
        Mockito.when(orderRepo.findByStripeCheckoutSessionId("cs_test_abc"))
                .thenReturn(Optional.of(order));

        svc.markCancelledFromWebhook("cs_test_abc");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
