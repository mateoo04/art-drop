package hr.tvz.artdrop.artdropapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    private final long tokenValiditySeconds;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(
            @Value("${jwt.token-validity-seconds}") long tokenValiditySeconds,
            @Value("${AUTH_COOKIE_SECURE:${auth.cookie.secure:false}}") boolean secure,
            @Value("${AUTH_COOKIE_SAME_SITE:${auth.cookie.same-site:Lax}}") String sameSite) {
        this.tokenValiditySeconds = tokenValiditySeconds;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie build(String jwt) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, jwt)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(tokenValiditySeconds))
                .build();
    }

    public ResponseCookie buildExpired() {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();
    }
}
