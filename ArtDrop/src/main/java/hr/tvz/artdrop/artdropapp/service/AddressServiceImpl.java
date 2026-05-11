package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ShippingAddressCommand;
import hr.tvz.artdrop.artdropapp.dto.ShippingAddressDTO;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;
import hr.tvz.artdrop.artdropapp.repository.ShippingAddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AddressServiceImpl implements AddressService {

    private final ShippingAddressRepository repository;
    private final Clock clock;

    public AddressServiceImpl(ShippingAddressRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<ShippingAddressDTO> listForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public ShippingAddressDTO create(Long userId, ShippingAddressCommand cmd) {
        ShippingAddress a = build(userId, cmd);
        return toDTO(repository.save(a));
    }

    @Override
    @Transactional
    public boolean delete(Long userId, Long addressId) {
        Optional<ShippingAddress> opt = repository.findById(addressId);
        if (opt.isEmpty() || !opt.get().getUserId().equals(userId)) return false;
        repository.deleteById(addressId);
        return true;
    }

    @Override
    @Transactional
    public ShippingAddress resolveForOrder(Long userId, Long addressId, ShippingAddressCommand inline) {
        if (addressId != null) {
            ShippingAddress a = repository.findById(addressId)
                    .orElseThrow(() -> new IllegalArgumentException("address not found: " + addressId));
            if (!a.getUserId().equals(userId)) {
                throw new IllegalArgumentException("address does not belong to caller");
            }
            return a;
        }
        if (inline == null) {
            throw new IllegalArgumentException("either addressId or inlineAddress is required");
        }
        return repository.save(build(userId, inline));
    }

    private ShippingAddress build(Long userId, ShippingAddressCommand cmd) {
        ShippingAddress a = new ShippingAddress();
        a.setUserId(userId);
        a.setRecipientName(cmd.recipientName());
        a.setLine1(cmd.line1());
        a.setLine2(cmd.line2());
        a.setCity(cmd.city());
        a.setPostalCode(cmd.postalCode());
        a.setCountry(cmd.country());
        a.setPhone(cmd.phone());
        a.setCreatedAt(LocalDateTime.now(clock));
        return a;
    }

    private ShippingAddressDTO toDTO(ShippingAddress a) {
        return new ShippingAddressDTO(
                a.getId(), a.getRecipientName(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getPostalCode(), a.getCountry(), a.getPhone());
    }
}
