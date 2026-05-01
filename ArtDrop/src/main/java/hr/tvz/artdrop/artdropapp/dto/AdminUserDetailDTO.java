package hr.tvz.artdrop.artdropapp.dto;

import java.util.List;

public record AdminUserDetailDTO(
        AdminUserSummaryDTO user,
        List<SellerApplicationDTO> applicationHistory
) {}
