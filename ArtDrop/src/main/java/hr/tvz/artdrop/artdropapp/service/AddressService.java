package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ShippingAddressCommand;
import hr.tvz.artdrop.artdropapp.dto.ShippingAddressDTO;
import hr.tvz.artdrop.artdropapp.model.ShippingAddress;

import java.util.List;

public interface AddressService {
    List<ShippingAddressDTO> listForUser(Long userId);

    ShippingAddressDTO create(Long userId, ShippingAddressCommand cmd);

    boolean delete(Long userId, Long addressId);

    /** Used by checkout to materialize an address (find existing or create from inline). */
    ShippingAddress resolveForOrder(Long userId, Long addressId, ShippingAddressCommand inline);
}
