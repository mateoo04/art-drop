package hr.tvz.artdrop.artdropapp.dto;

public record ShippingAddressDTO(
        Long id,
        String recipientName,
        String line1,
        String line2,
        String city,
        String postalCode,
        String country,
        String phone
) {}
