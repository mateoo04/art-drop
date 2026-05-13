package hr.tvz.artdrop.artdropapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigins,
                     @Value("${app.base-url:}") String appBaseUrl) {
        this.allowedOrigins = normalizeOrigins(allowedOrigins, appBaseUrl);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-XSRF-TOKEN", "Stripe-Signature")
                .allowCredentials(true);
    }

    private static String[] normalizeOrigins(String... values) {
        Set<String> origins = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            for (String origin : value.split(",")) {
                String normalized = origin.trim().replaceAll("/+$", "");
                if (!normalized.isBlank()) {
                    origins.add(normalized);
                }
            }
        }
        if (origins.isEmpty()) {
            origins.add("http://localhost:5173");
        }
        return origins.toArray(String[]::new);
    }
}
