package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SellerApplicationService service;

    public AdminController(SellerApplicationService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummaryDTO>> searchUsers(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "sellerStatus", required = false) List<String> sellerStatuses,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.searchUsers(query, sellerStatuses, role, sort, page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(@PathVariable Long id) {
        return service.getUserDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/listed-artwork-count")
    public ResponseEntity<ListedArtworkCountDTO> getListedArtworkCount(@PathVariable Long id) {
        return ResponseEntity.ok(service.countListedArtworks(id));
    }

    @GetMapping("/seller-applications")
    public ResponseEntity<Page<SellerApplicationDTO>> listApplications(
            @RequestParam(value = "status", defaultValue = "PENDING") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listApplications(status, page, size));
    }

    @PostMapping("/seller-applications/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody(required = false) SellerApplicationDecisionCommand command
    ) {
        SellerApplicationService.DecideResult result =
                service.approve(id, authentication.getName(), command);
        return mapDecide(result);
    }

    @PostMapping("/seller-applications/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody SellerApplicationDecisionCommand command
    ) {
        SellerApplicationService.DecideResult result =
                service.reject(id, authentication.getName(), command);
        return mapDecide(result);
    }

    @PostMapping("/users/{id}/revoke-seller")
    public ResponseEntity<?> revoke(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody RevokeSellerCommand command
    ) {
        SellerApplicationService.RevokeResult result =
                service.revoke(id, authentication.getName(), command);
        return switch (result) {
            case SellerApplicationService.RevokeOk(int unlistedCount) ->
                    ResponseEntity.ok(Map.of("unlistedCount", unlistedCount));
            case SellerApplicationService.RevokeFailure(SellerApplicationService.RevokeError err) ->
                    switch (err) {
                        case USER_NOT_FOUND -> ResponseEntity.notFound().build();
                        case NOT_A_SELLER ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "NOT_A_SELLER"));
                    };
        };
    }

    private ResponseEntity<?> mapDecide(SellerApplicationService.DecideResult result) {
        return switch (result) {
            case SellerApplicationService.DecideOk(SellerApplicationDTO app) -> ResponseEntity.ok(app);
            case SellerApplicationService.DecideFailure(SellerApplicationService.DecideError err) ->
                    switch (err) {
                        case NOT_FOUND -> ResponseEntity.notFound().build();
                        case NOT_PENDING ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "NOT_PENDING"));
                    };
        };
    }
}
