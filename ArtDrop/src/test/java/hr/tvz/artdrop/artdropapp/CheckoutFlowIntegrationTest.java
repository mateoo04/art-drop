package hr.tvz.artdrop.artdropapp;

import hr.tvz.artdrop.artdropapp.dto.CreateCheckoutSessionCommand;
import hr.tvz.artdrop.artdropapp.dto.ShippingAddressCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Authority;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.OrderStatus;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.AuthorityJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.OrderService;
import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import hr.tvz.artdrop.artdropapp.support.FakeStripeGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private AuthorityJpaRepository authorityRepository;
    @Autowired private OrderService orderService;
    @Autowired private FakeStripeGateway fakeStripe;

    private MockMvc mockMvc;
    private Long buyerArtworkId;
    private Long sellerArtworkId;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        fakeStripe.reset();

        // Clear any leftover active reservations + pending orders for the test buyer so tests start clean.
        Long buyerId = userRepository.findByUsername("user").orElseThrow().getId();
        orderRepository.findByBuyerUserIdOrderByCreatedAtDesc(buyerId,
                        PageRequest.of(0, 200)).stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
                .forEach(orderRepository::delete);
        artworkRepository.findAll().stream()
                .filter(a -> buyerId.equals(a.getReservedByUserId()) && a.getSaleState() == SaleState.RESERVED)
                .forEach(a -> {
                    a.setSaleState(SaleState.AVAILABLE);
                    a.setReservedByUserId(null);
                    a.setReservedUntil(null);
                    artworkRepository.save(a);
                });

        // Ensure buyer user (`user`) and seller (`mateo`) exist with proper roles.
        User seller = userRepository.findByUsername("mateo").orElseThrow();
        Authority sellerRole = authorityRepository.findByName("ROLE_SELLER")
                .orElseThrow();
        if (seller.getAuthorities().stream().noneMatch(a -> "ROLE_SELLER".equals(a.getName()))) {
            seller.getAuthorities().add(sellerRole);
            userRepository.save(seller);
        }

        // Pick a seller-owned artwork that's AVAILABLE and ORIGINAL for buyer to purchase.
        Artwork chosen = artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(seller.getId()).stream()
                .filter(a -> a.getPrice() != null
                        && a.getSaleState() == SaleState.AVAILABLE
                        && a.getSaleType() == SaleType.ORIGINAL)
                .findFirst()
                .orElseGet(() -> {
                    Artwork fallback = artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(seller.getId())
                            .stream().findFirst().orElseThrow();
                    fallback.setPrice(new BigDecimal("100.00"));
                    fallback.setSaleType(SaleType.ORIGINAL);
                    fallback.setSaleState(SaleState.AVAILABLE);
                    return artworkRepository.save(fallback);
                });
        chosen.setPrice(new BigDecimal("100.00"));
        chosen.setSaleType(SaleType.ORIGINAL);
        chosen.setSaleState(SaleState.AVAILABLE);
        chosen.setReservedByUserId(null);
        chosen.setReservedUntil(null);
        sellerArtworkId = artworkRepository.save(chosen).getId();

        // Self-purchase test artwork: owned by the buyer themselves.
        User buyer = userRepository.findByUsername("user").orElseThrow();
        buyerArtworkId = artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(buyer.getId()).stream()
                .findFirst()
                .map(a -> {
                    a.setPrice(new BigDecimal("100.00"));
                    a.setSaleType(SaleType.ORIGINAL);
                    a.setSaleState(SaleState.AVAILABLE);
                    return artworkRepository.save(a).getId();
                })
                .orElse(null);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void happyPathCheckoutPaidShipDelivered() throws Exception {
        CreateCheckoutSessionCommand cmd = new CreateCheckoutSessionCommand(
                sellerArtworkId, 1, null,
                new ShippingAddressCommand("Mateo Buyer", "Ilica 1", null, "Zagreb", "10000", "HR", null));

        String response = mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").exists())
                .andExpect(jsonPath("$.orderId").exists())
                .andReturn().getResponse().getContentAsString();

        Long orderId = objectMapper.readTree(response).get("orderId").asLong();
        Order pending = orderRepository.findById(orderId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(pending.getStripeCheckoutSessionId()).startsWith("cs_test_");

        Artwork reserved = artworkRepository.findById(sellerArtworkId).orElseThrow();
        assertThat(reserved.getSaleState()).isEqualTo(SaleState.RESERVED);

        // Simulate webhook: checkout.session.completed → markPaid.
        orderService.markPaid(orderId, "pi_test_" + pending.getStripeCheckoutSessionId());

        Order paid = orderRepository.findById(orderId).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        Artwork sold = artworkRepository.findById(sellerArtworkId).orElseThrow();
        assertThat(sold.getSaleState()).isEqualTo(SaleState.SOLD);

        // Seller ships and delivers (exercising MockMvc auth path).
        mockMvc.perform(post("/api/orders/" + orderId + "/ship")
                        .with(SecurityMockMvcRequestPostProcessors.user("mateo").roles("USER", "SELLER"))
                        .contentType("application/json")
                        .content("{\"trackingNumber\":\"TRK123\",\"carrier\":\"DHL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/deliver")
                        .with(SecurityMockMvcRequestPostProcessors.user("mateo").roles("USER", "SELLER"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        Order delivered = orderRepository.findById(orderId).orElseThrow();
        assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.getTrackingNumber()).isEqualTo("TRK123");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void selfPurchaseRejected() throws Exception {
        if (buyerArtworkId == null) return; // buyer has no artworks; skip silently
        CreateCheckoutSessionCommand cmd = new CreateCheckoutSessionCommand(
                buyerArtworkId, 1, null,
                new ShippingAddressCommand("Buyer", "L1", null, "Zagreb", "10000", "HR", null));

        mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("SELF_PURCHASE"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void buyerCancelTriggersRefundAndRestoresInventory() throws Exception {
        CreateCheckoutSessionCommand cmd = new CreateCheckoutSessionCommand(
                sellerArtworkId, 1, null,
                new ShippingAddressCommand("Buyer", "L1", null, "Zagreb", "10000", "HR", null));
        String response = mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("orderId").asLong();
        orderService.markPaid(orderId, "pi_test_cancel_" + orderId);

        int refundsBefore = fakeStripe.refundCount;
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .contentType("application/json")
                        .content("{\"reason\":\"changed mind\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        assertThat(fakeStripe.refundCount).isEqualTo(refundsBefore + 1);
        Artwork restored = artworkRepository.findById(sellerArtworkId).orElseThrow();
        assertThat(restored.getSaleState()).isEqualTo(SaleState.AVAILABLE);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void cannotCancelOthersOrder() throws Exception {
        // Create an order owned by `user`.
        CreateCheckoutSessionCommand cmd = new CreateCheckoutSessionCommand(
                sellerArtworkId, 1, null,
                new ShippingAddressCommand("Buyer", "L1", null, "Zagreb", "10000", "HR", null));
        String response = mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long orderId = objectMapper.readTree(response).get("orderId").asLong();
        orderService.markPaid(orderId, "pi_test_other_" + orderId);

        // GET /api/orders/{id} as a third user should be forbidden.
        mockMvc.perform(get("/api/orders/" + orderId)
                        .with(SecurityMockMvcRequestPostProcessors.user("ana").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhookRejectsBadSignature() throws Exception {
        fakeStripe.rejectSignature = true;
        mockMvc.perform(post("/api/checkout/webhook")
                        .contentType("application/json")
                        .header("Stripe-Signature", "t=0,v1=bogus")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void secondCheckoutBlockedByPendingOrderConflict() throws Exception {
        CreateCheckoutSessionCommand cmdA = new CreateCheckoutSessionCommand(
                sellerArtworkId, 1, null,
                new ShippingAddressCommand("Mateo Buyer", "Ilica 1", null, "Zagreb", "10000", "HR", null));
        String responseA = mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmdA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long firstOrderId = objectMapper.readTree(responseA).get("orderId").asLong();

        User seller = userRepository.findByUsername("mateo").orElseThrow();
        Artwork second = artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(seller.getId()).stream()
                .filter(a -> !a.getId().equals(sellerArtworkId))
                .findFirst()
                .orElseThrow();
        second.setPrice(new BigDecimal("150.00"));
        second.setSaleType(SaleType.ORIGINAL);
        second.setSaleState(SaleState.AVAILABLE);
        second.setReservedByUserId(null);
        second.setReservedUntil(null);
        second = artworkRepository.save(second);

        CreateCheckoutSessionCommand cmdB = new CreateCheckoutSessionCommand(
                second.getId(), 1, null,
                new ShippingAddressCommand("Mateo Buyer", "Ilica 1", null, "Zagreb", "10000", "HR", null));
        mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmdB)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PENDING_ORDER_EXISTS"))
                .andExpect(jsonPath("$.existingOrder.id").value(firstOrderId.intValue()))
                .andExpect(jsonPath("$.existingOrder.artworkId").value(sellerArtworkId.intValue()));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void cancelingPendingOrderUnblocksNewCheckout() throws Exception {
        CreateCheckoutSessionCommand cmdA = new CreateCheckoutSessionCommand(
                sellerArtworkId, 1, null,
                new ShippingAddressCommand("Mateo Buyer", "Ilica 1", null, "Zagreb", "10000", "HR", null));
        String responseA = mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmdA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long firstOrderId = objectMapper.readTree(responseA).get("orderId").asLong();
        String firstSessionId = orderRepository.findById(firstOrderId).orElseThrow()
                .getStripeCheckoutSessionId();

        mockMvc.perform(post("/api/orders/" + firstOrderId + "/cancel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(orderRepository.findById(firstOrderId)).isEmpty();
        assertThat(fakeStripe.expiredSessionIds).contains(firstSessionId);
        Artwork releasedFirst = artworkRepository.findById(sellerArtworkId).orElseThrow();
        assertThat(releasedFirst.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(releasedFirst.getReservedByUserId()).isNull();

        User seller = userRepository.findByUsername("mateo").orElseThrow();
        Artwork second = artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(seller.getId()).stream()
                .filter(a -> !a.getId().equals(sellerArtworkId))
                .findFirst()
                .orElseThrow();
        second.setPrice(new BigDecimal("150.00"));
        second.setSaleType(SaleType.ORIGINAL);
        second.setSaleState(SaleState.AVAILABLE);
        second.setReservedByUserId(null);
        second.setReservedUntil(null);
        second = artworkRepository.save(second);

        CreateCheckoutSessionCommand cmdB = new CreateCheckoutSessionCommand(
                second.getId(), 1, null,
                new ShippingAddressCommand("Mateo Buyer", "Ilica 1", null, "Zagreb", "10000", "HR", null));
        mockMvc.perform(post("/api/checkout/session")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmdB)))
                .andExpect(status().isOk());

        Artwork bAfter = artworkRepository.findById(second.getId()).orElseThrow();
        assertThat(bAfter.getSaleState()).isEqualTo(SaleState.RESERVED);
    }
}
