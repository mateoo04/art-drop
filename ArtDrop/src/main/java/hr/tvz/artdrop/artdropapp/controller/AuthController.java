package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.AuthSessionResponse;
import hr.tvz.artdrop.artdropapp.dto.JwtResponse;
import hr.tvz.artdrop.artdropapp.dto.LoginRequest;
import hr.tvz.artdrop.artdropapp.dto.RegisterRequest;
import hr.tvz.artdrop.artdropapp.security.AuthCookieService;
import hr.tvz.artdrop.artdropapp.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final boolean demoLoginEnabled;
    private final String demoUsername;
    private final String demoPassword;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            @Value("${app.demo-login.enabled:false}") boolean demoLoginEnabled,
            @Value("${app.demo-login.username:demo}") String demoUsername,
            @Value("${app.demo-login.password:}") String demoPassword
    ) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.demoLoginEnabled = demoLoginEnabled;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwt = authService.login(loginRequest);
        ResponseCookie cookie = authCookieService.build(jwt.accessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthSessionResponse(loginRequest.username()));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthSessionResponse> signup(@Valid @RequestBody RegisterRequest registerRequest) {
        JwtResponse jwt = authService.signup(registerRequest);
        ResponseCookie cookie = authCookieService.build(jwt.accessToken());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthSessionResponse(registerRequest.username()));
    }

    @PostMapping("/demo-login")
    public ResponseEntity<AuthSessionResponse> demoLogin() {
        if (!demoLoginEnabled) {
            return ResponseEntity.notFound().build();
        }
        JwtResponse jwt = authService.login(new LoginRequest(demoUsername, demoPassword));
        ResponseCookie cookie = authCookieService.build(jwt.accessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthSessionResponse(demoUsername));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = authCookieService.buildExpired();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthSessionResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AuthSessionResponse(authentication.getName()));
    }
}
