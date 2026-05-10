package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ChallengeDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmissionThumbnailDTO;
import hr.tvz.artdrop.artdropapp.service.ChallengeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping
    public ResponseEntity<List<ChallengeDTO>> getAll(Authentication authentication) {
        String viewer = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(challengeService.findAll(viewer));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChallengeDTO>> searchChallenges(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication
    ) {
        String viewer = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(challengeService.searchChallenges(q, limit, offset, viewer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChallengeDTO> getById(@PathVariable Long id, Authentication authentication) {
        String viewer = authentication == null ? null : authentication.getName();
        return challengeService.findById(id, viewer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<SubmissionThumbnailDTO>> getSubmissions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort
    ) {
        return ResponseEntity.ok(challengeService.findSubmissions(id, limit, offset, sort));
    }

    public record SubmitArtworkRequest(Long artworkId) {}

    @PostMapping("/{id}/submissions")
    public ResponseEntity<?> submitArtwork(
            @PathVariable Long id,
            @RequestBody SubmitArtworkRequest body,
            Authentication authentication
    ) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (body == null || body.artworkId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "MISSING_ARTWORK_ID"));
        }
        ChallengeService.SubmitResult result = challengeService.submitArtwork(
                id, body.artworkId(), authentication.getName());
        return switch (result.outcome()) {
            case CREATED -> ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("submissionId", result.submissionId()));
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case UNAUTHENTICATED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case FORBIDDEN_NOT_OWNER -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(Map.of("error", "NOT_OWNER"));
            case FORBIDDEN_CHALLENGE_NOT_ACTIVE -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(Map.of("error", "CHALLENGE_NOT_ACTIVE"));
            case FORBIDDEN_ARTWORK_TOO_OLD -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(Map.of("error", "ARTWORK_TOO_OLD"));
            case CONFLICT_ALREADY_SUBMITTED -> ResponseEntity
                    .status(HttpStatus.CONFLICT).body(Map.of("error", "ALREADY_SUBMITTED"));
            case CONFLICT_IN_OTHER_CHALLENGE -> ResponseEntity
                    .status(HttpStatus.CONFLICT).body(Map.of("error", "IN_OTHER_CHALLENGE"));
            case CONFLICT_USER_ALREADY_HAS_ENTRY -> ResponseEntity
                    .status(HttpStatus.CONFLICT).body(Map.of("error", "USER_ALREADY_HAS_ENTRY"));
        };
    }

    @DeleteMapping("/{id}/submissions/{artworkId}")
    public ResponseEntity<?> withdrawSubmission(
            @PathVariable Long id,
            @PathVariable Long artworkId,
            Authentication authentication
    ) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ChallengeService.WithdrawResult result = challengeService.withdrawSubmission(
                id, artworkId, authentication.getName());
        return switch (result.outcome()) {
            case OK -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case UNAUTHENTICATED -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            case FORBIDDEN_NOT_OWNER -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(Map.of("error", "NOT_OWNER"));
            case FORBIDDEN_CHALLENGE_ENDED -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN).body(Map.of("error", "CHALLENGE_ENDED"));
        };
    }

    @GetMapping("/{id}/eligible-artworks")
    public ResponseEntity<List<ArtworkDTO>> getEligibleArtworks(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(challengeService.findEligibleArtworksForChallenge(id, authentication.getName()));
    }
}
