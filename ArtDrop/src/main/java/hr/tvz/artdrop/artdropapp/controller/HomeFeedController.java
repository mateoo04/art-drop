package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.HomeFeedResponse;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.service.FeedRankingService;
import hr.tvz.artdrop.artdropapp.service.FeedSeenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
public class HomeFeedController {

    private final FeedRankingService feedRankingService;
    private final FeedSeenService feedSeenService;
    private final UserJpaRepository userRepository;

    public HomeFeedController(
            FeedRankingService feedRankingService,
            FeedSeenService feedSeenService,
            UserJpaRepository userRepository
    ) {
        this.feedRankingService = feedRankingService;
        this.feedSeenService = feedSeenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/home")
    public ResponseEntity<HomeFeedResponse> homeFeed(
            Authentication authentication,
            @RequestParam(required = false) String medium,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long viewerId = null;
        String viewerUsername = null;
        if (authentication != null) {
            viewerUsername = authentication.getName();
            viewerId = userRepository.findByUsername(viewerUsername)
                    .map(u -> u.getId())
                    .orElse(null);
        }
        return ResponseEntity.ok(feedRankingService.getHomeFeed(viewerId, viewerUsername, medium, cursor, limit));
    }

    @PostMapping("/seen")
    public ResponseEntity<Void> seen(
            Authentication authentication,
            @Valid @RequestBody SeenRequest body
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        Long viewerId = userRepository.findByUsername(authentication.getName())
                .map(u -> u.getId())
                .orElse(null);
        if (viewerId == null) {
            return ResponseEntity.status(401).build();
        }
        feedSeenService.recordSeen(viewerId, body.artworkIds());
        return ResponseEntity.noContent().build();
    }

    public record SeenRequest(@NotNull List<Long> artworkIds) {}
}
