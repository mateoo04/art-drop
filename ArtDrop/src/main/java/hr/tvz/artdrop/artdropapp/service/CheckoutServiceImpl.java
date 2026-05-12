package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.StripeException;
import hr.tvz.artdrop.artdropapp.dto.CheckoutSessionResponse;
import hr.tvz.artdrop.artdropapp.dto.CreateCheckoutSessionCommand;
import hr.tvz.artdrop.artdropapp.dto.Pricing;
import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.exception.PendingOrderConflictException;
import hr.tvz.artdrop.artdropapp.exception.SelfPurchaseException;
import hr.tvz.artdrop.artdropapp.exception.StripeIntegrationException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.OrderStatus;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private final UserJpaRepository userRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final OrderRepository orderRepository;
    private final AddressService addressService;
    private final ReservationService reservationService;
    private final EditionInventoryService editionInventoryService;
    private final PricingService pricingService;
    private final OrderService orderService;
    private final StripeGateway stripeGateway;
    private final CheckoutService self;
    private final String currency;
    private final String successUrlBase;
    private final String cancelUrlBase;

    public CheckoutServiceImpl(
            UserJpaRepository userRepository,
            ArtworkJpaRepository artworkRepository,
            OrderRepository orderRepository,
            AddressService addressService,
            ReservationService reservationService,
            EditionInventoryService editionInventoryService,
            PricingService pricingService,
            OrderService orderService,
            StripeGateway stripeGateway,
            @Lazy CheckoutService self,
            @Value("${commerce.currency}") String currency,
            @Value("${stripe.success-url-base}") String successUrlBase,
            @Value("${stripe.cancel-url-base}") String cancelUrlBase) {
        this.userRepository = userRepository;
        this.artworkRepository = artworkRepository;
        this.orderRepository = orderRepository;
        this.addressService = addressService;
        this.reservationService = reservationService;
        this.editionInventoryService = editionInventoryService;
        this.pricingService = pricingService;
        this.orderService = orderService;
        this.stripeGateway = stripeGateway;
        this.self = self;
        this.currency = currency;
        this.successUrlBase = successUrlBase;
        this.cancelUrlBase = cancelUrlBase;
    }

    @Override
    public CheckoutSessionResponse createSession(CreateCheckoutSessionCommand cmd, String currentUsername) {
        PreparedOrder prepared = self.preparePendingOrder(cmd, currentUsername);

        StripeGateway.CheckoutSessionRequest req = new StripeGateway.CheckoutSessionRequest(
                String.valueOf(prepared.orderId()),
                prepared.artworkTitle(),
                prepared.total(),
                currency,
                successUrlBase + "/" + prepared.orderId() + "?status=success",
                cancelUrlBase + "/" + prepared.orderId() + "?status=cancelled",
                Map.of("orderId", String.valueOf(prepared.orderId()))
        );

        StripeGateway.CheckoutSessionResult result;
        try {
            result = stripeGateway.createCheckoutSession(req);
        } catch (StripeException e) {
            orderService.deletePendingForBuyerAndArtwork(prepared.buyerId(), prepared.artworkId());
            throw new StripeIntegrationException("failed to create checkout session", e);
        }

        self.attachStripeSession(prepared.orderId(), result.sessionId());

        return new CheckoutSessionResponse(prepared.orderId(), result.url());
    }

    @Override
    @Transactional
    public PreparedOrder preparePendingOrder(CreateCheckoutSessionCommand cmd, String currentUsername) {
        User buyer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("buyer not found"));

        orderRepository.findFirstByBuyerUserIdAndStatus(buyer.getId(), OrderStatus.PENDING_PAYMENT)
                .ifPresent(existing -> {
                    String title = artworkRepository.findById(existing.getArtworkId())
                            .map(Artwork::getTitle)
                            .orElse(null);
                    throw new PendingOrderConflictException(
                            existing.getId(), existing.getArtworkId(), title);
                });

        Artwork artwork = artworkRepository.findById(cmd.artworkId())
                .orElseThrow(() -> new IllegalArgumentException("artwork not found: " + cmd.artworkId()));

        if (artwork.getAuthor() != null && buyer.getId().equals(artwork.getAuthor().getId())) {
            throw new SelfPurchaseException("you cannot purchase your own artwork");
        }
        if (artwork.getPrice() == null) {
            throw new InventoryUnavailableException("artwork is not for sale");
        }
        if (artwork.getSaleState() == SaleState.SOLD || artwork.getSaleState() == SaleState.DRAFT) {
            throw new InventoryUnavailableException("artwork is not currently available");
        }

        int quantity = cmd.quantity() == null ? 1 : cmd.quantity();
        if (artwork.getSaleType() == SaleType.ORIGINAL && quantity != 1) {
            throw new IllegalArgumentException("ORIGINAL artworks must be purchased with quantity 1");
        }
        if (!editionInventoryService.canFulfill(artwork, quantity)) {
            throw new InventoryUnavailableException("requested quantity exceeds available inventory");
        }

        ShippingAddress address = addressService.resolveForOrder(buyer.getId(), cmd.addressId(), cmd.inlineAddress());

        reservationService.reserve(artwork, buyer);

        Pricing pricing = pricingService.compute(artwork.getPrice(), quantity);
        Order order = orderService.createPending(buyer, artwork, quantity, pricing, address, currency);

        return new PreparedOrder(
                order.getId(), artwork.getId(), buyer.getId(),
                artwork.getTitle(), pricing.total());
    }

    @Override
    @Transactional
    public void attachStripeSession(Long orderId, String sessionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + orderId));
        order.setStripeCheckoutSessionId(sessionId);
        orderRepository.save(order);
    }

    public record PreparedOrder(
            Long orderId,
            Long artworkId,
            Long buyerId,
            String artworkTitle,
            BigDecimal total
    ) {}
}
