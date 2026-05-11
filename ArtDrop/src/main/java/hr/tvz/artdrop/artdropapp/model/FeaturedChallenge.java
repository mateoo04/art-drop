package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "featured_challenge")
public class FeaturedChallenge {
    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "current_challenge_id")
    private Long currentChallengeId;

    @Column(name = "next_challenge_id")
    private Long nextChallengeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 20)
    private FeaturedTriggerType triggerType;

    @Column(name = "trigger_at")
    private LocalDateTime triggerAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
