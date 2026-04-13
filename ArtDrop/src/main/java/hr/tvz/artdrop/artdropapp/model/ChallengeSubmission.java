package hr.tvz.artdrop.artdropapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSubmission {
    private Long id;
    private Long challengeId;
    private Long artworkId;
    private Long submittedBy;
    private LocalDateTime submittedAt;
}
