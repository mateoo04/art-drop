package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks")
public class ArtworkController {

    private final ArtworkService artworkService;

    public ArtworkController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @GetMapping
    public ResponseEntity<List<ArtworkDTO>> getArtworks(
            @RequestParam(required = false) String medium,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication
    ) {
        String viewer = authentication == null ? null : authentication.getName();
        if (medium != null && !medium.isBlank()) {
            return ResponseEntity.ok(artworkService.findByMedium(medium, viewer, limit, offset));
        }
        return ResponseEntity.ok(artworkService.findAll(viewer, limit, offset));
    }

    @GetMapping("/mediums")
    public ResponseEntity<List<String>> getMediums() {
        return ResponseEntity.ok(artworkService.findDistinctMediums());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ArtworkDTO> getArtworkById(@PathVariable Long id, Authentication authentication) {
        String viewer = authentication == null ? null : authentication.getName();
        return artworkService.findById(id, viewer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<ArtworkDTO> getByTitle(@PathVariable String title, Authentication authentication) {
        String viewer = authentication == null ? null : authentication.getName();
        return artworkService.findOneByTitle(title, viewer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createArtwork(@Valid @RequestBody ArtworkCommand command) {
        if (!artworkService.createArtwork(command)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<Void> like(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        return switch (artworkService.like(id, authentication.getName())) {
            case LIKED -> ResponseEntity.status(HttpStatus.CREATED).build();
            case ALREADY_LIKED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case UNAUTHENTICATED -> ResponseEntity.status(401).build();
            default -> ResponseEntity.internalServerError().build();
        };
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<Void> unlike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(401).build();
        return switch (artworkService.unlike(id, authentication.getName())) {
            case UNLIKED, NOT_LIKED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case UNAUTHENTICATED -> ResponseEntity.status(401).build();
            default -> ResponseEntity.internalServerError().build();
        };
    }

    @PostMapping("/comments")
    public ResponseEntity<Void> createArtworkComment(@Valid @RequestBody ArtworkCommentCommand command) {
        if (!artworkService.createArtworkComment(command)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reviews")
    public ResponseEntity<Void> createArtworkReview(@Valid @RequestBody ArtworkReviewCommand command) {
        if (!artworkService.createArtworkReview(command)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateArtwork(
            @PathVariable Long id,
            @Valid @RequestBody ArtworkUpdateCommand command,
            Authentication authentication
    ) {
        String name = authentication == null ? null : authentication.getName();
        ArtworkService.UpdateResult result = artworkService.updateArtwork(id, command, name);
        return switch (result.outcome()) {
            case OK -> ResponseEntity.ok(result.artwork());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case FORBIDDEN_SALE_GATE -> ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "FORBIDDEN_SALE_GATE"));
        };
    }

    @DeleteMapping("/{title}")
    public ResponseEntity<Void> deleteArtworkByTitle(@PathVariable String title) {
        if (!artworkService.deleteByTitle(title)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
