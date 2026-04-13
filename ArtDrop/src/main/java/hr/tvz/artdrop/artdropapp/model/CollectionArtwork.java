package hr.tvz.artdrop.artdropapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionArtwork {
    private Long collectionId;
    private Long artworkId;
    private Integer position;
    private LocalDateTime addedAt;
}
