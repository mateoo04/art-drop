package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.MyReservationDTO;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.OrderService;
import hr.tvz.artdrop.artdropapp.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final OrderService orderService;
    private final UserJpaRepository userRepository;

    public ReservationController(ReservationService reservationService,
                                 OrderService orderService,
                                 UserJpaRepository userRepository) {
        this.reservationService = reservationService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<MyReservationDTO> getMyReservation(Authentication auth) {
        Long userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getId();
        Optional<Artwork> reservation = reservationService.findActiveReservation(userId);
        if (reservation.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        Artwork a = reservation.get();
        return ResponseEntity.ok(new MyReservationDTO(
                a.getId(), a.getTitle(), a.getCoverPublicId(), a.getReservedUntil()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyReservation(Authentication auth) {
        Long userId = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getId();
        reservationService.findActiveReservation(userId)
                .ifPresent(a -> {
                    orderService.deletePendingForBuyerAndArtwork(userId, a.getId());
                    reservationService.release(a.getId());
                });
        return ResponseEntity.noContent().build();
    }
}
