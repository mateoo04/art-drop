package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final UserJpaRepository userRepository;
    private final ArtworkService artworkService;

    public FeedController(UserJpaRepository userRepository, ArtworkService artworkService) {
        this.userRepository = userRepository;
        this.artworkService = artworkService;
    }

    @GetMapping("/circle")
    public ResponseEntity<List<ArtworkDTO>> circleFeed(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(artworkService.findCircleFeed(user.getId(), limit, offset)))
                .orElse(ResponseEntity.status(401).build());
    }
}
