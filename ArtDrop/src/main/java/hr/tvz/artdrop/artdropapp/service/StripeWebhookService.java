package hr.tvz.artdrop.artdropapp.service;

public interface StripeWebhookService {
    /**
     * Verifies signature, dedupes by event id, dispatches to OrderService.
     * Throws IllegalArgumentException on signature mismatch (controller maps to 400).
     */
    void handleEvent(String rawPayload, String signatureHeader);
}
