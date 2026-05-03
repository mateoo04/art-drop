package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.AdminChallengeRowDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminChallengeUpsertDTO;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.service.AdminChallengeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/admin/challenges")
public class AdminChallengeController {

    private final AdminChallengeService adminChallengeService;

    public AdminChallengeController(AdminChallengeService adminChallengeService) {
        this.adminChallengeService = adminChallengeService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminChallengeRowDTO>> list(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        ChallengeStatus st = parseStatus(status);
        return ResponseEntity.ok(adminChallengeService.list(query, st, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminChallengeRowDTO> get(@PathVariable Long id) {
        return adminChallengeService.get(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AdminChallengeRowDTO> create(
            Authentication authentication,
            @Valid @RequestBody AdminChallengeUpsertDTO body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminChallengeService.create(body, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminChallengeRowDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminChallengeUpsertDTO body
    ) {
        return adminChallengeService.update(id, body)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!adminChallengeService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AdminChallengeRowDTO> activate(@PathVariable Long id) {
        return adminChallengeService.setStatus(id, ChallengeStatus.ACTIVE)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<AdminChallengeRowDTO> deactivate(@PathVariable Long id) {
        return adminChallengeService.setStatus(id, ChallengeStatus.ENDED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static ChallengeStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ChallengeStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
