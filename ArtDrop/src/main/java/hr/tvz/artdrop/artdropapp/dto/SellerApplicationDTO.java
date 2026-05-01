package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record SellerApplicationDTO(
        Long id,
        Long userId,
        AdminUserSummaryDTO applicant,
        String message,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime decidedAt,
        Long decidedByUserId,
        String decisionReason,
        LocalDateTime revokedAt,
        Long revokedByUserId,
        String revokeReason,
        String derivedSellerStatus,
        LocalDateTime canReapplyAt
) {}
