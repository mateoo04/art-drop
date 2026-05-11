package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class EditionInventoryServiceTest {

    private final OrderRepository repo = Mockito.mock(OrderRepository.class);
    private final EditionInventoryService svc = new EditionInventoryServiceImpl(repo);

    @Test
    void returnsNullForOriginal() {
        Artwork a = new Artwork();
        a.setId(1L);
        a.setSaleType(SaleType.ORIGINAL);
        assertThat(svc.remaining(a)).isNull();
    }

    @Test
    void computesRemainingForEdition() {
        Artwork a = new Artwork();
        a.setId(7L);
        a.setSaleType(SaleType.EDITION);
        a.setEditionSize(10);
        Mockito.when(repo.countByArtworkIdAndStatusIn(eq(7L), any())).thenReturn(3L);
        assertThat(svc.remaining(a)).isEqualTo(7);
    }

    @Test
    void remainingClampedToZeroWhenOverSold() {
        Artwork a = new Artwork();
        a.setId(7L);
        a.setSaleType(SaleType.EDITION);
        a.setEditionSize(5);
        Mockito.when(repo.countByArtworkIdAndStatusIn(eq(7L), any())).thenReturn(7L);
        assertThat(svc.remaining(a)).isZero();
    }

    @Test
    void canFulfillEditionWhenEnoughRemaining() {
        Artwork a = new Artwork();
        a.setId(7L);
        a.setSaleType(SaleType.EDITION);
        a.setEditionSize(10);
        Mockito.when(repo.countByArtworkIdAndStatusIn(eq(7L), any())).thenReturn(8L);
        assertThat(svc.canFulfill(a, 2)).isTrue();
        assertThat(svc.canFulfill(a, 3)).isFalse();
    }

    @Test
    void canFulfillOriginalOnlyAtQuantityOne() {
        Artwork a = new Artwork();
        a.setId(1L);
        a.setSaleType(SaleType.ORIGINAL);
        assertThat(svc.canFulfill(a, 1)).isTrue();
        assertThat(svc.canFulfill(a, 2)).isFalse();
        assertThat(svc.canFulfill(a, 0)).isFalse();
    }
}
