package hr.tvz.artdrop.artdropapp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import hr.tvz.artdrop.artdropapp.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/signup",
            "/api/auth/demo-login"
    );

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000L;

    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!RATE_LIMITED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientIp(request);
        long now = System.currentTimeMillis();

        Window window = windows.asMap().compute(key, (k, existing) -> {
            if (existing == null || now - existing.start >= WINDOW_MILLIS) {
                return new Window(now);
            }
            return existing;
        });

        if (window.count.incrementAndGet() > MAX_ATTEMPTS) {
            writeTooManyRequests(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma == -1 ? forwarded : forwarded.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorResponse body = ErrorResponse.of(
                "RATE_LIMITED",
                "too many requests, please try again shortly",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();
        Window(long start) { this.start = start; }
    }
}
