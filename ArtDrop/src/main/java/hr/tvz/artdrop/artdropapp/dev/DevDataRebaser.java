package hr.tvz.artdrop.artdropapp.dev;

import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Dev profile: shift seeded artwork/comment/like times so the newest post is near now, preserving relative gaps. */
@Component
@Profile("dev")
public class DevDataRebaser {

    private final JdbcTemplate jdbc;

    public DevDataRebaser(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void rebase() {
        Timestamp anchor = jdbc.queryForObject("SELECT MAX(published_at) FROM artwork", Timestamp.class);
        if (anchor == null) {
            return;
        }
        long deltaMs = System.currentTimeMillis() - anchor.getTime();
        if (deltaMs == 0) {
            return;
        }
        jdbc.update("UPDATE artwork SET published_at = DATEADD('MILLISECOND', ?, published_at)", deltaMs);
        jdbc.update(
                "UPDATE comment SET created_at = DATEADD('MILLISECOND', ?, created_at),"
                        + " updated_at = DATEADD('MILLISECOND', ?, updated_at)",
                deltaMs,
                deltaMs);
        jdbc.update("UPDATE artwork_like SET created_at = DATEADD('MILLISECOND', ?, created_at)", deltaMs);
    }
}
