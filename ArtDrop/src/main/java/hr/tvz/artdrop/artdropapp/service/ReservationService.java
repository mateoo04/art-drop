package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.User;

import java.util.Optional;

public interface ReservationService {
    /**
     * For ORIGINAL artworks: acquire an exclusive 15-minute reservation under a pessimistic row lock.
     * Idempotent if the same user already holds an active reservation.
     * Throws InventoryUnavailableException if another user holds an active reservation, or
     * if the artwork is SOLD/DRAFT. No-op for EDITION artworks.
     */
    void reserve(Artwork artwork, User user);

    void release(Long artworkId);

    /** Releases all reservations past their deadline. Returns count released. */
    int releaseExpired();

    /** Returns the user's currently active reservation, if any. */
    Optional<Artwork> findActiveReservation(Long userId);
}
