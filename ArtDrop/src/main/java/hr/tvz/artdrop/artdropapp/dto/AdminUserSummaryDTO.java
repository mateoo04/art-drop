package hr.tvz.artdrop.artdropapp.dto;

public record AdminUserSummaryDTO(
        Long id,
        String username,
        String slug,
        String displayName,
        String email,
        String avatarUrl,
        String sellerStatus,
        SellerApplicationDTO pendingApplication,
        String primaryRole,
        boolean enabled
) {
    /** Back-compat constructor for callers that omit pending and role fields. */
    public AdminUserSummaryDTO(
            Long id,
            String username,
            String slug,
            String displayName,
            String email,
            String avatarUrl,
            String sellerStatus
    ) {
        this(id, username, slug, displayName, email, avatarUrl, sellerStatus, null, "USER", true);
    }
}
