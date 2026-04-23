package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import hr.tvz.artdrop.artdropapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
}
