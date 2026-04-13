package hr.tvz.artdrop.artdropapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Collection {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private List<Long> artworkIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isPublic;
}
