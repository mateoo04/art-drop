package hr.tvz.artdrop.artdropapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkLike {
    private Long id;
    private Long artworkId;
    private Long userId;
    private LocalDateTime createdAt;
}
