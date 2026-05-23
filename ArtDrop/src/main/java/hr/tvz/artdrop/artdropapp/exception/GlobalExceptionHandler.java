package hr.tvz.artdrop.artdropapp.exception;

import hr.tvz.artdrop.artdropapp.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Business exceptions: preserve legacy response shapes consumed by the frontend ---

    @ExceptionHandler(SelfPurchaseException.class)
    public ResponseEntity<Map<String, String>> handleSelfPurchase(SelfPurchaseException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "SELF_PURCHASE"));
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleInventory(InventoryUnavailableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "INVENTORY_UNAVAILABLE", "message", e.getMessage()));
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<Map<String, Object>> handleReservationConflict(ReservationConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "RESERVATION_CONFLICT",
                "existingArtwork", Map.of(
                        "id", e.getExistingArtworkId(),
                        "title", e.getExistingArtworkTitle()
                )
        ));
    }

    @ExceptionHandler(PendingOrderConflictException.class)
    public ResponseEntity<Map<String, Object>> handlePendingOrderConflict(PendingOrderConflictException e) {
        Map<String, Object> existingOrder = new HashMap<>();
        existingOrder.put("id", e.getExistingOrderId());
        existingOrder.put("artworkId", e.getExistingArtworkId());
        existingOrder.put("artworkTitle", e.getExistingArtworkTitle());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "PENDING_ORDER_EXISTS",
                "existingOrder", existingOrder
        ));
    }

    @ExceptionHandler(IllegalOrderStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalOrderState(IllegalOrderStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "ILLEGAL_ORDER_STATE", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException e,
                                                              HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("EMAIL_TAKEN", e.getMessage(), 409, req.getRequestURI()));
    }

    @ExceptionHandler(StripeIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleStripe(StripeIntegrationException e,
                                                      HttpServletRequest req) {
        log.error("Stripe integration error at {}: {}", req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of("STRIPE_ERROR", "payment provider unavailable",
                        502, req.getRequestURI()));
    }

    // --- Standard handlers using ErrorResponse ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest req) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", message, 400, req.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                   HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", e.getMessage(), 400, req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e,
                                                               HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("BAD_REQUEST", e.getMessage(), 400, req.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e,
                                                              HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("UNAUTHORIZED", "invalid credentials", 401, req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e,
                                                            HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", "access denied", 403, req.getRequestURI()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e,
                                                              HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", e.getMessage(), 404, req.getRequestURI()));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException e, HttpServletRequest req) {
        log.debug("client aborted request at {}: {}", req.getRequestURI(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e, HttpServletRequest req) {
        if (isClientAbort(e)) {
            log.debug("client aborted request at {}: {}", req.getRequestURI(), e.getMessage());
            return ResponseEntity.noContent().build();
        }

        log.error("unhandled exception at {}: {}", req.getRequestURI(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "an unexpected error occurred",
                        500, req.getRequestURI()));
    }

    private boolean isClientAbort(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if (current instanceof AsyncRequestNotUsableException
                    || className.equals("org.apache.catalina.connector.ClientAbortException")
                    || message != null && isClientAbortMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isClientAbortMessage(String message) {
        return message.contains("Broken pipe")
                || message.contains("Connection reset by peer")
                || message.contains("connection reset by peer");
    }
}
