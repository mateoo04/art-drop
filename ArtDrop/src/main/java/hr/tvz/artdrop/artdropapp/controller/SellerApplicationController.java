package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me/seller-application")
public class SellerApplicationController {

    private final SellerApplicationService service;

    public SellerApplicationController(SellerApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SellerApplicationDTO> getMine(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return service.findMyLatest(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitSellerApplicationCommand command
    ) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SellerApplicationService.SubmitResult result = service.submit(authentication.getName(), command);
        return switch (result) {
            case SellerApplicationService.SubmitOk(SellerApplicationDTO app) ->
                    ResponseEntity.status(HttpStatus.CREATED).body(app);
            case SellerApplicationService.SubmitFailure(SellerApplicationService.SubmitError err, var canReapplyAt) ->
                    switch (err) {
                        case ALREADY_PENDING ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "ALREADY_PENDING"));
                        case ALREADY_SELLER ->
                                ResponseEntity.badRequest()
                                        .body(Map.of("error", "ALREADY_SELLER"));
                        case COOLDOWN_ACTIVE ->
                                ResponseEntity.badRequest()
                                        .body(Map.of("error", (Object) "COOLDOWN_ACTIVE", "canReapplyAt", (Object) canReapplyAt));
                        case USER_NOT_FOUND ->
                                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    };
        };
    }
}
