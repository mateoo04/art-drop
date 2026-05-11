package hr.tvz.artdrop.artdropapp.exception;

public class PendingOrderConflictException extends RuntimeException {
    private final Long existingOrderId;
    private final Long existingArtworkId;
    private final String existingArtworkTitle;

    public PendingOrderConflictException(Long existingOrderId,
                                         Long existingArtworkId,
                                         String existingArtworkTitle) {
        super("user already has a pending order " + existingOrderId);
        this.existingOrderId = existingOrderId;
        this.existingArtworkId = existingArtworkId;
        this.existingArtworkTitle = existingArtworkTitle;
    }

    public Long getExistingOrderId() { return existingOrderId; }
    public Long getExistingArtworkId() { return existingArtworkId; }
    public String getExistingArtworkTitle() { return existingArtworkTitle; }
}
