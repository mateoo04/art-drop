package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ShippingAddressCommand;
import hr.tvz.artdrop.artdropapp.dto.ShippingAddressDTO;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;
    private final UserJpaRepository userRepository;

    public AddressController(AddressService addressService, UserJpaRepository userRepository) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<ShippingAddressDTO>> list(Authentication auth) {
        return ResponseEntity.ok(addressService.listForUser(uid(auth)));
    }

    @PostMapping
    public ResponseEntity<ShippingAddressDTO> create(@Valid @RequestBody ShippingAddressCommand cmd,
                                                     Authentication auth) {
        ShippingAddressDTO created = addressService.create(uid(auth), cmd);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        boolean ok = addressService.delete(uid(auth), id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private Long uid(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("user not found"))
                .getId();
    }
}
