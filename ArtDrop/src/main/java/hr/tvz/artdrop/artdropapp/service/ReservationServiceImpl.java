package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.exception.ReservationConflictException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationService {

    private final ArtworkJpaRepository artworkRepository;
    private final Clock clock;
    private final int reservationMinutes;

    public ReservationServiceImpl(
            ArtworkJpaRepository artworkRepository,
            Clock clock,
            @Value("${commerce.reservation-minutes}") int reservationMinutes) {
        this.artworkRepository = artworkRepository;
        this.clock = clock;
        this.reservationMinutes = reservationMinutes;
    }

    @Override
    @Transactional
    public void reserve(Artwork incoming, User user) {
        if (incoming.getSaleType() == SaleType.EDITION) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);

        Optional<Artwork> existing = artworkRepository.findActiveReservationByUser(user.getId(), now);
        if (existing.isPresent() && !existing.get().getId().equals(incoming.getId())) {
            Artwork e = existing.get();
            throw new ReservationConflictException(e.getId(), e.getTitle());
        }

        Artwork a = artworkRepository.findByIdForUpdate(incoming.getId())
                .orElseThrow(() -> new InventoryUnavailableException("artwork not found"));

        boolean activeReservation =
                a.getSaleState() == SaleState.RESERVED
                && a.getReservedUntil() != null
                && a.getReservedUntil().isAfter(now);

        if (a.getSaleState() == SaleState.SOLD || a.getSaleState() == SaleState.DRAFT) {
            throw new InventoryUnavailableException("artwork not available");
        }
        if (activeReservation && !user.getId().equals(a.getReservedByUserId())) {
            throw new InventoryUnavailableException("artwork is reserved by another buyer");
        }

        LocalDateTime until = now.plusMinutes(reservationMinutes);
        a.setSaleState(SaleState.RESERVED);
        a.setReservedByUserId(user.getId());
        a.setReservedUntil(until);
        artworkRepository.save(a);
        incoming.setSaleState(SaleState.RESERVED);
        incoming.setReservedByUserId(user.getId());
        incoming.setReservedUntil(until);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Artwork> findActiveReservation(Long userId) {
        return artworkRepository.findActiveReservationByUser(userId, LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public void release(Long artworkId) {
        artworkRepository.findByIdForUpdate(artworkId).ifPresent(a -> {
            if (a.getSaleType() == SaleType.ORIGINAL && a.getSaleState() == SaleState.RESERVED) {
                a.setSaleState(SaleState.AVAILABLE);
                a.setReservedByUserId(null);
                a.setReservedUntil(null);
                artworkRepository.save(a);
            }
        });
    }

    @Override
    @Transactional
    public int releaseExpired() {
        List<Artwork> expired = artworkRepository.findExpiredReservations(LocalDateTime.now(clock));
        for (Artwork a : expired) {
            a.setSaleState(SaleState.AVAILABLE);
            a.setReservedByUserId(null);
            a.setReservedUntil(null);
        }
        artworkRepository.saveAll(expired);
        return expired.size();
    }
}
