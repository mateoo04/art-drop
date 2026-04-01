package hr.tvz.artdrop.artdropapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Artwork {
    private Long id;
    private String title;
    private String medium;
    private String description;
    private String imageUrl;
    private List<String> tags;
    private LocalDateTime publishedAt;
    private Integer likeCount;

}
