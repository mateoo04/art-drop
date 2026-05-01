package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "artwork")
public class Artwork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String medium;

    @Column(length = 2000)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", length = 20)
    private ProgressStatus progressStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_status", length = 20)
    private SaleStatus saleStatus;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<ArtworkImage> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "artwork_tags", joinColumns = @JoinColumn(name = "artwork_id"))
    @Column(name = "tag")
    private List<String> tags;

    @Column(name = "width_value", precision = 10, scale = 2)
    private BigDecimal widthValue;

    @Column(name = "height_value", precision = 10, scale = 2)
    private BigDecimal heightValue;

    @Column(name = "depth_value", precision = 10, scale = 2)
    private BigDecimal depthValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_unit", length = 8)
    private DimensionUnit dimensionUnit;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Formula("(SELECT COUNT(*) FROM artwork_like al WHERE al.artwork_id = id)")
    private Integer likeCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public String getCoverImageUrl() {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                .findFirst()
                .or(() -> images.stream()
                        .min(Comparator.comparing(
                                ArtworkImage::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))))
                .map(ArtworkImage::getImageUrl)
                .orElse(null);
    }
}
