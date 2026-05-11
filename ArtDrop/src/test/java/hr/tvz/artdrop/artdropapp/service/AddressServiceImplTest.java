package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ShippingAddressCommand;
import hr.tvz.artdrop.artdropapp.dto.ShippingAddressDTO;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.repository.ShippingAddressRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddressServiceImplTest {

    private final ShippingAddressRepository repo = mock(ShippingAddressRepository.class);
    private final Clock clock = Clock.fixed(
            LocalDateTime.of(2026, 5, 11, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
    );

    private final AddressServiceImpl svc = new AddressServiceImpl(repo, clock);

    private static ShippingAddressCommand cmd() {
        return new ShippingAddressCommand(
                "Joe Smith", "Ilica 1", "apt 4B", "Zagreb", "10000", "HR", "+385 1 1234567"
        );
    }

    private static ShippingAddress address(Long id, Long userId) {
        ShippingAddress a = new ShippingAddress();
        a.setId(id);
        a.setUserId(userId);
        a.setRecipientName("Joe Smith");
        a.setLine1("Ilica 1");
        a.setLine2("apt 4B");
        a.setCity("Zagreb");
        a.setPostalCode("10000");
        a.setCountry("HR");
        a.setPhone("+385 1 1234567");
        a.setCreatedAt(LocalDateTime.of(2026, 5, 1, 0, 0));
        return a;
    }

    // ----- listForUser -----

    @Test
    void listForUser_returnsMappedDtos() {
        when(repo.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(address(10L, 1L), address(11L, 1L)));

        List<ShippingAddressDTO> result = svc.listForUser(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).recipientName()).isEqualTo("Joe Smith");
    }

    @Test
    void listForUser_returnsEmptyWhenNone() {
        when(repo.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertThat(svc.listForUser(1L)).isEmpty();
    }

    // ----- create -----

    @Test
    void create_savesNewAddressWithClockNow() {
        ArgumentCaptor<ShippingAddress> captor = ArgumentCaptor.forClass(ShippingAddress.class);
        when(repo.save(captor.capture())).thenAnswer(inv -> {
            ShippingAddress saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        ShippingAddressDTO dto = svc.create(1L, cmd());

        assertThat(dto.id()).isEqualTo(99L);
        ShippingAddress saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getRecipientName()).isEqualTo("Joe Smith");
        assertThat(saved.getCity()).isEqualTo("Zagreb");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    // ----- delete -----

    @Test
    void delete_returnsFalseWhenAddressNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.delete(1L, 99L)).isFalse();
        verify(repo, never()).deleteById(any());
    }

    @Test
    void delete_returnsFalseWhenAddressBelongsToDifferentUser() {
        when(repo.findById(10L)).thenReturn(Optional.of(address(10L, 2L)));

        assertThat(svc.delete(1L, 10L)).isFalse();
        verify(repo, never()).deleteById(any());
    }

    @Test
    void delete_returnsTrueAndDeletesWhenOwnedByUser() {
        when(repo.findById(10L)).thenReturn(Optional.of(address(10L, 1L)));

        assertThat(svc.delete(1L, 10L)).isTrue();
        verify(repo).deleteById(10L);
    }

    // ----- resolveForOrder -----

    @Test
    void resolveForOrder_usesExistingAddressWhenIdMatches() {
        when(repo.findById(10L)).thenReturn(Optional.of(address(10L, 1L)));

        ShippingAddress result = svc.resolveForOrder(1L, 10L, null);

        assertThat(result.getId()).isEqualTo(10L);
        verify(repo, never()).save(any());
    }

    @Test
    void resolveForOrder_throwsWhenAddressNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.resolveForOrder(1L, 99L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address not found");
    }

    @Test
    void resolveForOrder_throwsWhenAddressNotOwnedByCaller() {
        when(repo.findById(10L)).thenReturn(Optional.of(address(10L, 2L)));

        assertThatThrownBy(() -> svc.resolveForOrder(1L, 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void resolveForOrder_throwsWhenBothNull() {
        assertThatThrownBy(() -> svc.resolveForOrder(1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void resolveForOrder_savesInlineAddressWhenNoIdProvided() {
        when(repo.save(any())).thenAnswer(inv -> {
            ShippingAddress saved = inv.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        ShippingAddress result = svc.resolveForOrder(1L, null, cmd());

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(repo).save(any());
    }
}
