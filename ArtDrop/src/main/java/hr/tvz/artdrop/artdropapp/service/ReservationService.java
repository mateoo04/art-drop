package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.User;

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
}
