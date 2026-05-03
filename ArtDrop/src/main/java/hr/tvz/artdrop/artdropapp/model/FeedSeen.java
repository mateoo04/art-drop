package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "feed_seen",
        indexes = {
                @Index(name = "idx_feed_seen_viewer_time", columnList = "viewer_id, seen_at DESC"),
                @Index(name = "idx_feed_seen_viewer_artwork", columnList = "viewer_id, artwork_id")
        }
)
public class FeedSeen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    @Column(name = "seen_at", nullable = false)
    private LocalDateTime seenAt;
}
