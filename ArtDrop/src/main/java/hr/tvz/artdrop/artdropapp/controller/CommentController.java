package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.CommentDTO;
import hr.tvz.artdrop.artdropapp.dto.CreateCommentCommand;
import hr.tvz.artdrop.artdropapp.service.CommentService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/artworks/{id}/comments")
    public ResponseEntity<List<CommentDTO>> list(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        String viewer = authentication == null ? null : authentication.getName();
        return ResponseEntity.ok(commentService.listForArtwork(id, viewer, limit, offset));
    }

    @PostMapping("/artworks/{id}/comments")
    public ResponseEntity<CommentDTO> create(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody CreateCommentCommand command
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return commentService.create(id, authentication.getName(), command)
                .map(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        CommentService.DeleteResult result = commentService.delete(id, authentication.getName());
        return switch (result) {
            case OK -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case FORBIDDEN -> ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        };
    }
}
