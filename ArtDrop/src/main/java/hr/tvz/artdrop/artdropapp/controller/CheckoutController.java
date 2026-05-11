package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.CheckoutSessionResponse;
import hr.tvz.artdrop.artdropapp.dto.CreateCheckoutSessionCommand;
import hr.tvz.artdrop.artdropapp.exception.InventoryUnavailableException;
import hr.tvz.artdrop.artdropapp.exception.SelfPurchaseException;
import hr.tvz.artdrop.artdropapp.service.CheckoutService;
import hr.tvz.artdrop.artdropapp.service.StripeWebhookService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);

    private final CheckoutService checkoutService;
    private final StripeWebhookService webhookService;

    public CheckoutController(CheckoutService checkoutService, StripeWebhookService webhookService) {
        this.checkoutService = checkoutService;
        this.webhookService = webhookService;
    }

    @PostMapping("/session")
    public ResponseEntity<CheckoutSessionResponse> createSession(
            @Valid @RequestBody CreateCheckoutSessionCommand cmd,
            Authentication auth) {
        return ResponseEntity.ok(checkoutService.createSession(cmd, auth.getName()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        try {
            webhookService.handleEvent(rawBody, signature);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException sigBad) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(sigBad.getMessage());
        } catch (RuntimeException e) {
            // Always 200 on internal errors so Stripe doesn't retry-loop into the same bug.
            log.error("Webhook handler error: {}", e.getMessage(), e);
            return ResponseEntity.ok("logged");
        }
    }

    @ExceptionHandler(SelfPurchaseException.class)
    public ResponseEntity<Map<String, String>> handleSelfPurchase(SelfPurchaseException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "SELF_PURCHASE"));
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleInventory(InventoryUnavailableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "INVENTORY_UNAVAILABLE", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }
}
