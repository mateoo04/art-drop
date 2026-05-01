package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artwork_image")
public class ArtworkImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;
    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;
    @Column(name = "sort_order")
    private Integer sortOrder;
    @Column(name = "is_cover")
    private Boolean isCover;
    private String caption;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
