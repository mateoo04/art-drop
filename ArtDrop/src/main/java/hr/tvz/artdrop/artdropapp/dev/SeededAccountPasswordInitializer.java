package hr.tvz.artdrop.artdropapp.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeededAccountPasswordInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeededAccountPasswordInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String seededAccountsPassword;
    private final String demoUsername;
    private final String demoPassword;
    private final String adminUsername;
    private final String adminPassword;

    public SeededAccountPasswordInitializer(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.seeded-accounts.password:}") String seededAccountsPassword,
            @Value("${app.demo-login.username:demo}") String demoUsername,
            @Value("${app.demo-login.password:}") String demoPassword,
            @Value("${app.seeded-admin.username:mateo}") String adminUsername,
            @Value("${app.seeded-admin.password:}") String adminPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.seededAccountsPassword = seededAccountsPassword;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hasText(seededAccountsPassword)) {
            int updated = jdbcTemplate.update("""
                    UPDATE app_user
                    SET password_hash = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE email LIKE '%@artdrop.local'
                    """, passwordEncoder.encode(seededAccountsPassword));
            log.info("Updated {} seeded account password hashes from SEEDED_ACCOUNT_PASSWORD", updated);
            return;
        }

        updateSeededUserPassword(demoUsername, demoPassword, "demo");
        updateSeededUserPassword(adminUsername, adminPassword, "admin");
    }

    private void updateSeededUserPassword(String username, String rawPassword, String label) {
        if (!hasText(username) || !hasText(rawPassword)) {
            return;
        }
        int updated = jdbcTemplate.update("""
                UPDATE app_user
                SET password_hash = ?, updated_at = CURRENT_TIMESTAMP
                WHERE username = ?
                """, passwordEncoder.encode(rawPassword), username);
        if (updated == 0) {
            log.warn("Seeded {} account '{}' was not found; password was not updated", label, username);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
