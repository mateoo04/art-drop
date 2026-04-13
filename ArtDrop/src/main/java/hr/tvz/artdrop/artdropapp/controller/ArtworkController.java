package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkLikeCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artworks")
@CrossOrigin(origins = "http://localhost:5173")
public class ArtworkController {

    private final ArtworkService artworkService;

    public ArtworkController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @GetMapping
    public ResponseEntity<List<ArtworkDTO>> getArtworks(@RequestParam(required = false) String medium) {
        if (medium != null && !medium.isBlank()) {
            List<ArtworkDTO> list = artworkService.findByMedium(medium);
            if (list.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(list);
        }
        return ResponseEntity.ok(artworkService.findAll());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ArtworkDTO> getArtworkById(@PathVariable Long id) {
        return artworkService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/title/{title}")
    public ResponseEntity<ArtworkDTO> getByTitle(@PathVariable String title) {
        return artworkService.findOneByTitle(title)
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

    @PostMapping("/likes")
    public ResponseEntity<Void> createArtworkLike(@Valid @RequestBody ArtworkLikeCommand command) {
        if (!artworkService.createArtworkLike(command)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
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

    @DeleteMapping("/{title}")
    public ResponseEntity<Void> deleteArtworkByTitle(@PathVariable String title) {
        if (!artworkService.deleteByTitle(title)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
