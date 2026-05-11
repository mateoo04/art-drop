package hr.tvz.artdrop.artdropapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    // 32-byte base64 secret (256 bits) for HS256.
    private static final String BASE64_SECRET = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWZvci1qd3QtMzJieXRlcw==";

    private final JwtTokenProvider provider = new JwtTokenProvider(BASE64_SECRET, 3600);

    @Test
    void generateToken_producesValidToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joe", null, List.of());

        String token = provider.generateToken(auth);

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void getUsernameFromToken_returnsSubject() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joe", null, List.of());
        String token = provider.generateToken(auth);

        String username = provider.getUsernameFromToken(token);

        assertThat(username).isEqualTo("joe");
    }

    @Test
    void validateToken_returnsFalseForMalformed() {
        assertThat(provider.validateToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void validateToken_returnsFalseForTokenSignedWithDifferentKey() {
        JwtTokenProvider other = new JwtTokenProvider(
                "YW5vdGhlci1zZWNyZXQtZm9yLXRlc3RpbmctMzItYnl0ZXMtbG9uZw==", 3600);
        Authentication auth = new UsernamePasswordAuthenticationToken("joe", null, List.of());
        String foreignToken = other.generateToken(auth);

        assertThat(provider.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(BASE64_SECRET, 0);
        Authentication auth = new UsernamePasswordAuthenticationToken("joe", null, List.of());
        String token = shortLived.generateToken(auth);
        Thread.sleep(50);

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
