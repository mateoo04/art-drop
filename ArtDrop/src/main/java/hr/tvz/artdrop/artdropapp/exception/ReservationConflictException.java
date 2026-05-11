package hr.tvz.artdrop.artdropapp.exception;

public class ReservationConflictException extends RuntimeException {
    private final Long existingArtworkId;
    private final String existingArtworkTitle;

    public ReservationConflictException(Long existingArtworkId, String existingArtworkTitle) {
        super("user already has an active reservation on artwork " + existingArtworkId);
        this.existingArtworkId = existingArtworkId;
        this.existingArtworkTitle = existingArtworkTitle;
    }

    public Long getExistingArtworkId() { return existingArtworkId; }
    public String getExistingArtworkTitle() { return existingArtworkTitle; }
}
