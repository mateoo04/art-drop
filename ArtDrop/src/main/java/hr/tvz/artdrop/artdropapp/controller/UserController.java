package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.CircleStatusDTO;
import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import hr.tvz.artdrop.artdropapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserJpaRepository userRepository;
    private final ArtworkService artworkService;

    public UserController(UserService userService, UserJpaRepository userRepository, ArtworkService artworkService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.artworkService = artworkService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMe(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.findMe(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileCommand command
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.updateMe(authentication.getName(), command)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/artworks")
    public ResponseEntity<List<ArtworkDTO>> getMyArtworks(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(artworkService.findByAuthorId(user.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<UserProfileDTO> getBySlug(@PathVariable String slug, Authentication authentication) {
        String viewer = authentication == null ? null : authentication.getName();
        return userService.findBySlug(slug, viewer)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/artworks")
    public ResponseEntity<List<ArtworkDTO>> getArtworksBySlug(@PathVariable String slug) {
        return userRepository.findBySlug(slug)
                .map(user -> ResponseEntity.ok(artworkService.findByAuthorId(user.getId())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/circle-status")
    public ResponseEntity<CircleStatusDTO> getCircleStatus(
            @PathVariable String slug,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userService.circleStatus(authentication.getName(), slug)
                .map(inCircle -> ResponseEntity.ok(new CircleStatusDTO(inCircle)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{slug}/circle")
    public ResponseEntity<CircleStatusDTO> joinCircle(
            @PathVariable String slug,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        UserService.CircleAction result = userService.joinCircle(authentication.getName(), slug);
        return switch (result) {
            case OK -> ResponseEntity.ok(new CircleStatusDTO(true));
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case SELF -> ResponseEntity.badRequest().build();
        };
    }

    @DeleteMapping("/{slug}/circle")
    public ResponseEntity<CircleStatusDTO> leaveCircle(
            @PathVariable String slug,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        UserService.CircleAction result = userService.leaveCircle(authentication.getName(), slug);
        return switch (result) {
            case OK -> ResponseEntity.ok(new CircleStatusDTO(false));
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case SELF -> ResponseEntity.badRequest().build();
        };
    }
}
