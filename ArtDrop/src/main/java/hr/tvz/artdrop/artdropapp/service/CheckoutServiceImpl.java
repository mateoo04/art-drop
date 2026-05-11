package hr.tvz.artdrop.artdropapp.service;

import com.stripe.exception.StripeException;
import hr.tvz.artdrop.artdropapp.dto.CheckoutSessionResponse;
import hr.tvz.artdrop.artdropapp.dto.CreateCheckoutSessionCommand;
import hr.tvz.artdrop.artdropapp.dto.Pricing;
import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.exception.SelfPurchaseException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        this.currency = currency;
        this.successUrlBase = successUrlBase;
        this.cancelUrlBase = cancelUrlBase;
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createSession(CreateCheckoutSessionCommand cmd, String currentUsername) {
        User buyer = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("buyer not found"));
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

        StripeGateway.CheckoutSessionRequest req = new StripeGateway.CheckoutSessionRequest(
                String.valueOf(order.getId()),
                artwork.getTitle(),
                pricing.total(),
                currency,
                successUrlBase + "/" + order.getId() + "?status=success",
                cancelUrlBase + "/" + order.getId() + "?status=cancelled",
                Map.of("orderId", String.valueOf(order.getId()))
        );

        StripeGateway.CheckoutSessionResult result;
        try {
            result = stripeGateway.createCheckoutSession(req);
        } catch (StripeException e) {
            throw new RuntimeException("failed to create checkout session: " + e.getMessage(), e);
        }

        order.setStripeCheckoutSessionId(result.sessionId());
        orderRepository.save(order);

        return new CheckoutSessionResponse(order.getId(), result.url());
    }
}
