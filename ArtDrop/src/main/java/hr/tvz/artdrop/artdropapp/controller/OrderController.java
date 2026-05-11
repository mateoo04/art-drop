package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.CancelOrderCommand;
import hr.tvz.artdrop.artdropapp.dto.OrderDTO;
import hr.tvz.artdrop.artdropapp.dto.ShipOrderCommand;
import hr.tvz.artdrop.artdropapp.exception.IllegalOrderStateException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final UserJpaRepository userRepository;

    public OrderController(OrderService orderService,
                           OrderRepository orderRepository,
                           ArtworkJpaRepository artworkRepository,
                           UserJpaRepository userRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.artworkRepository = artworkRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> myOrders(Authentication auth) {
        Long uid = requireUserId(auth);
        List<Order> orders = orderRepository.findByBuyerUserIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 200));
        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id, Authentication auth) {
        Long uid = requireUserId(auth);
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();
        boolean isParticipant = order.getBuyerUserId().equals(uid) || order.getArtistUserId().equals(uid);
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isParticipant && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(toDTO(order));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OrderDTO> cancel(@PathVariable Long id,
                                           @Valid @RequestBody(required = false) CancelOrderCommand cmd,
                                           Authentication auth) {
        Long uid = requireUserId(auth);
        Order updated = orderService.cancelByBuyer(id, uid, cmd == null ? null : cmd.reason());
        return ResponseEntity.ok(toDTO(updated));
    }

    @GetMapping("/sales")
    public ResponseEntity<List<OrderDTO>> mySales(Authentication auth) {
        Long uid = requireUserId(auth);
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_SELLER".equals(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Order> orders = orderRepository.findByArtistUserIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 200));
        return ResponseEntity.ok(orders.stream().map(this::toDTO).toList());
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<OrderDTO> ship(@PathVariable Long id,
                                         @Valid @RequestBody(required = false) ShipOrderCommand cmd,
                                         Authentication auth) {
        Long uid = requireUserId(auth);
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_SELLER".equals(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Order updated = orderService.markShipped(id, uid,
                cmd == null ? null : cmd.trackingNumber(),
                cmd == null ? null : cmd.carrier());
        return ResponseEntity.ok(toDTO(updated));
    }

    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<OrderDTO> deliver(@PathVariable Long id, Authentication auth) {
        Long uid = requireUserId(auth);
        if (auth.getAuthorities().stream().noneMatch(a -> "ROLE_SELLER".equals(a.getAuthority()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Order updated = orderService.markDelivered(id, uid);
        return ResponseEntity.ok(toDTO(updated));
    }

    @ExceptionHandler(IllegalOrderStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalOrderStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "ILLEGAL_ORDER_STATE", "message", e.getMessage()));
    }

    private Long requireUserId(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getId();
    }

    private OrderDTO toDTO(Order o) {
        Artwork art = artworkRepository.findById(o.getArtworkId()).orElse(null);
        String title = art == null ? null : art.getTitle();
        String cover = art == null ? null : art.getCoverPublicId();
        OrderDTO.ShippingAddressSnapshot addr = new OrderDTO.ShippingAddressSnapshot(
                o.getShipRecipientName(), o.getShipLine1(), o.getShipLine2(),
                o.getShipCity(), o.getShipPostalCode(), o.getShipCountry(), o.getShipPhone());
        return new OrderDTO(
                o.getId(), o.getBuyerUserId(), o.getArtistUserId(), o.getArtworkId(),
                title, cover, o.getQuantity(), o.getStatus().name(),
                o.getSubtotal(), o.getShippingFee(), o.getPlatformFee(), o.getTotal(),
                o.getCurrency(), addr, o.getTrackingNumber(), o.getShippingCarrier(),
                o.getCancellationReason(), o.getPaidAt(), o.getShippedAt(),
                o.getDeliveredAt(), o.getCancelledAt(), o.getRefundedAt(), o.getCreatedAt());
    }
}
