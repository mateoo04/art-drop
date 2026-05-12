package hr.tvz.artdrop.artdropapp.exception;

public class StripeIntegrationException extends RuntimeException {
    public StripeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
