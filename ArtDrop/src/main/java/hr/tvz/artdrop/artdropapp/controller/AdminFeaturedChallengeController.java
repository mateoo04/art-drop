package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.FeaturedChallengeStateDTO;
import hr.tvz.artdrop.artdropapp.dto.ScheduleReplacementRequest;
import hr.tvz.artdrop.artdropapp.dto.SetFeaturedCurrentRequest;
import hr.tvz.artdrop.artdropapp.service.FeaturedChallengeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/featured-challenge")
public class AdminFeaturedChallengeController {

    private final FeaturedChallengeService service;

    public AdminFeaturedChallengeController(FeaturedChallengeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FeaturedChallengeStateDTO> get(Authentication authentication) {
        return ResponseEntity.ok(service.getState(authentication == null ? null : authentication.getName()));
    }

    @PutMapping("/current")
    public ResponseEntity<FeaturedChallengeStateDTO> setCurrent(
            Authentication authentication,
            @Valid @RequestBody SetFeaturedCurrentRequest body) {
        service.setCurrent(body.challengeId());
        return ResponseEntity.ok(service.getState(authentication == null ? null : authentication.getName()));
    }

    @DeleteMapping("/current")
    public ResponseEntity<Void> clearCurrent() {
        service.clearCurrent();
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/schedule")
    public ResponseEntity<FeaturedChallengeStateDTO> schedule(
            Authentication authentication,
            @Valid @RequestBody ScheduleReplacementRequest body) {
        service.scheduleReplacement(body.nextChallengeId(), body.triggerType(), body.triggerAt());
        return ResponseEntity.ok(service.getState(authentication == null ? null : authentication.getName()));
    }

    @DeleteMapping("/schedule")
    public ResponseEntity<Void> clearSchedule() {
        service.clearSchedule();
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
