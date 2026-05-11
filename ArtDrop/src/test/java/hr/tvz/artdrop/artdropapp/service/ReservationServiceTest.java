package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {

    private final ZoneId zone = ZoneId.systemDefault();
    private final Instant fixedInstant = LocalDateTime.of(2026, 5, 11, 12, 0)
            .atZone(zone).toInstant();
    private final Clock fixed = Clock.fixed(fixedInstant, zone);

    private final ArtworkJpaRepository repo = Mockito.mock(ArtworkJpaRepository.class);
    private final ReservationService svc = new ReservationServiceImpl(repo, fixed, 15);

    private Artwork artwork;
    private User alice;
    private User bob;

    @BeforeEach
    void setup() {
        artwork = new Artwork();
        artwork.setId(1L);
        artwork.setSaleType(SaleType.ORIGINAL);
        artwork.setSaleState(SaleState.AVAILABLE);
        alice = new User();
        alice.setId(10L);
        bob = new User();
        bob.setId(20L);
        Mockito.when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(artwork));
        Mockito.when(repo.save(Mockito.any(Artwork.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reservesAvailableOriginal() {
        svc.reserve(artwork, alice);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.RESERVED);
        assertThat(artwork.getReservedByUserId()).isEqualTo(10L);
        assertThat(artwork.getReservedUntil())
                .isEqualTo(LocalDateTime.now(fixed).plusMinutes(15));
    }

    @Test
    void idempotentForSameUser() {
        svc.reserve(artwork, alice);
        svc.reserve(artwork, alice);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.RESERVED);
        assertThat(artwork.getReservedByUserId()).isEqualTo(10L);
    }

    @Test
    void rejectsWhenAnotherUserHoldsActiveReservation() {
        svc.reserve(artwork, alice);
        assertThatThrownBy(() -> svc.reserve(artwork, bob))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void rejectsSoldArtwork() {
        artwork.setSaleState(SaleState.SOLD);
        assertThatThrownBy(() -> svc.reserve(artwork, alice))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void rejectsDraftArtwork() {
        artwork.setSaleState(SaleState.DRAFT);
        assertThatThrownBy(() -> svc.reserve(artwork, alice))
                .isInstanceOf(InventoryUnavailableException.class);
    }

    @Test
    void noOpForEdition() {
        artwork.setSaleType(SaleType.EDITION);
        artwork.setEditionSize(5);
        svc.reserve(artwork, alice);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(artwork.getReservedByUserId()).isNull();
    }

    @Test
    void releaseRevertsReservedOriginalToAvailable() {
        artwork.setSaleState(SaleState.RESERVED);
        artwork.setReservedByUserId(alice.getId());
        artwork.setReservedUntil(LocalDateTime.now(fixed).plusMinutes(5));
        svc.release(1L);
        assertThat(artwork.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(artwork.getReservedByUserId()).isNull();
        assertThat(artwork.getReservedUntil()).isNull();
    }

    @Test
    void releaseExpiredClearsAllStaleReservations() {
        Artwork stale = new Artwork();
        stale.setId(2L);
        stale.setSaleType(SaleType.ORIGINAL);
        stale.setSaleState(SaleState.RESERVED);
        stale.setReservedByUserId(99L);
        stale.setReservedUntil(LocalDateTime.now(fixed).minusMinutes(1));
        Mockito.when(repo.findExpiredReservations(Mockito.any())).thenReturn(List.of(stale));
        Mockito.when(repo.saveAll(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        int n = svc.releaseExpired();

        assertThat(n).isEqualTo(1);
        assertThat(stale.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(stale.getReservedByUserId()).isNull();
        assertThat(stale.getReservedUntil()).isNull();
    }
}
