package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Order;
import hr.tvz.artdrop.artdropapp.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByArtworkIdAndStatusIn(Long artworkId, List<OrderStatus> statuses);

    long countByArtworkId(Long artworkId);

    Optional<Order> findByStripeCheckoutSessionId(String sessionId);

    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);

    List<Order> findByBuyerUserIdOrderByCreatedAtDesc(Long buyerUserId, Pageable pageable);

    List<Order> findByArtistUserIdOrderByCreatedAtDesc(Long artistUserId, Pageable pageable);

    Optional<Order> findFirstByBuyerUserIdAndStatus(Long buyerUserId, OrderStatus status);

    Optional<Order> findFirstByBuyerUserIdAndArtworkIdAndStatus(
            Long buyerUserId, Long artworkId, OrderStatus status);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
}
