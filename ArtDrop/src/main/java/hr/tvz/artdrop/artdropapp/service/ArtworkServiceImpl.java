package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkImageCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkImageDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
import hr.tvz.artdrop.artdropapp.model.ArtworkLike;
import hr.tvz.artdrop.artdropapp.model.Comment;
import hr.tvz.artdrop.artdropapp.model.DimensionUnit;
import hr.tvz.artdrop.artdropapp.model.ProgressStatus;
import hr.tvz.artdrop.artdropapp.model.SaleStatus;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ArtworkLikeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkJpaRepository artworkRepository;
    private final ArtworkLikeJpaRepository likeRepository;
    private final UserJpaRepository userRepository;
    private final Map<String, List<ArtworkCommentCommand>> commentsByTitle = new HashMap<>();
    private final Map<String, List<ArtworkReviewCommand>> reviewsByTitle = new HashMap<>();

    public ArtworkServiceImpl(
            ArtworkJpaRepository artworkRepository,
            ArtworkLikeJpaRepository likeRepository,
            UserJpaRepository userRepository
    ) {
        this.artworkRepository = artworkRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ArtworkDTO> findAll(String viewerUsername) {
        return mapMany(artworkRepository.findAll(), viewerUsername);
    }

    @Override
    public List<ArtworkDTO> findAll(String viewerUsername, int limit, int offset) {
        Pageable page = paged(limit, offset, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Long viewerId = viewerUsername == null
                ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);
        List<Artwork> rows = viewerId == null
                ? artworkRepository.findAll(page).getContent()
                : artworkRepository.findAllExcludingAuthor(viewerId, paged(limit, offset));
        return mapMany(rows, viewerUsername);
    }

    @Override
    public Optional<ArtworkDTO> findById(Long id, String viewerUsername) {
        return artworkRepository.findById(id)
                .map(a -> mapToDTO(a, likedSetFor(viewerUsername, List.of(a))));
    }

    @Override
    public List<ArtworkDTO> findByMedium(String medium, String viewerUsername) {
        return mapMany(artworkRepository.findByMediumContainingIgnoreCase(medium), viewerUsername);
    }

    @Override
    public List<ArtworkDTO> findByMedium(String medium, String viewerUsername, int limit, int offset) {
        Pageable page = paged(limit, offset);
        Long viewerId = viewerUsername == null
                ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);
        List<Artwork> rows = viewerId == null
                ? artworkRepository.findByMediumContainingIgnoreCase(medium, page)
                : artworkRepository.findByMediumExcludingAuthor(medium, viewerId, page);
        return mapMany(rows, viewerUsername);
    }

    private static Pageable paged(int limit, int offset) {
        return paged(limit, offset, Sort.unsorted());
    }

    private static Pageable paged(int limit, int offset, Sort sort) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        return PageRequest.of(safeOffset / safeLimit, safeLimit, sort);
    }

    @Override
    public List<ArtworkDTO> findByAuthorId(Long authorId, String viewerUsername) {
        return mapMany(artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(authorId), viewerUsername);
    }

    @Override
    public List<ArtworkDTO> findCircleFeed(Long viewerId, int limit, int offset) {
        List<Artwork> rows = artworkRepository.findCircleFeed(viewerId, paged(limit, offset));
        Set<Long> likedSet = rows.isEmpty() ? Set.of() : new HashSet<>(
                likeRepository.findArtworkIdsLikedByUser(viewerId, rows.stream().map(Artwork::getId).toList())
        );
        return rows.stream().map(a -> mapToDTO(a, likedSet)).toList();
    }

    @Override
    public List<ArtworkDTO> findByIdsOrdered(List<Long> ids, String viewerUsername) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Artwork> rows = artworkRepository.findByIdIn(ids);
        Map<Long, Artwork> byId = new HashMap<>();
        for (Artwork a : rows) byId.put(a.getId(), a);
        List<Artwork> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Artwork a = byId.get(id);
            if (a != null) ordered.add(a);
        }
        return mapMany(ordered, viewerUsername);
    }

    @Override
    public Optional<ArtworkDTO> findOneByTitle(String title, String viewerUsername) {
        return artworkRepository.findByTitleIgnoreCase(title)
                .map(a -> mapToDTO(a, likedSetFor(viewerUsername, List.of(a))));
    }

    @Override
    @Transactional
    public boolean createArtwork(ArtworkCommand command) {
        return createArtwork(command, null).outcome() == CreateOutcome.CREATED;
    }

    @Override
    @Transactional
    public CreateResult createArtwork(ArtworkCommand command, String authorUsername) {
        if (artworkRepository.existsByTitleIgnoreCase(command.title())) {
            return new CreateResult(CreateOutcome.CONFLICT, null);
        }

        User author = null;
        if (authorUsername != null) {
            author = userRepository.findByUsername(authorUsername).orElse(null);
            if (author == null) {
                return new CreateResult(CreateOutcome.UNAUTHENTICATED, null);
            }
        } else {
            author = userRepository.findById(1L).orElse(null);
        }

        boolean wantsSale = command.price() != null || command.saleStatus() != null;
        if (wantsSale) {
            boolean isSeller = author != null
                    && author.getAuthorities() != null
                    && author.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_SELLER".equals(a.getName()));
            if (!isSeller) {
                return new CreateResult(CreateOutcome.FORBIDDEN_SALE_GATE, null);
            }
        }

        Artwork artwork = new Artwork();
        artwork.setAuthor(author);
        artwork.setTitle(command.title());
        artwork.setMedium(command.medium());
        artwork.setDescription(command.description());
        artwork.setProgressStatus(command.progressStatus() == null
                ? ProgressStatus.FINISHED
                : ProgressStatus.valueOf(command.progressStatus()));
        artwork.setSaleStatus(command.saleStatus() == null
                ? null
                : SaleStatus.valueOf(command.saleStatus()));
        artwork.setPrice(command.price());
        artwork.setTags(command.tags() == null ? List.of() : List.copyOf(command.tags()));
        artwork.setPublishedAt(LocalDateTime.now());
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        applyDimensions(artwork, command.width(), command.height(), command.depth(), command.dimensionUnit());
        artwork.setImages(buildImages(artwork, command.images()));
        Artwork saved = artworkRepository.save(artwork);
        return new CreateResult(CreateOutcome.CREATED, mapToDTO(saved, Set.of()));
    }

    @Override
    @Transactional
    public LikeResult like(Long artworkId, String username) {
        Optional<User> viewer = userRepository.findByUsername(username);
        if (viewer.isEmpty()) return LikeResult.UNAUTHENTICATED;
        if (!artworkRepository.existsById(artworkId)) return LikeResult.NOT_FOUND;
        Long userId = viewer.get().getId();
        if (likeRepository.existsByArtworkIdAndUserId(artworkId, userId)) {
            return LikeResult.ALREADY_LIKED;
        }
        likeRepository.save(new ArtworkLike(null, artworkId, userId, LocalDateTime.now()));
        return LikeResult.LIKED;
    }

    @Override
    @Transactional
    public LikeResult unlike(Long artworkId, String username) {
        Optional<User> viewer = userRepository.findByUsername(username);
        if (viewer.isEmpty()) return LikeResult.UNAUTHENTICATED;
        if (!artworkRepository.existsById(artworkId)) return LikeResult.NOT_FOUND;
        long removed = likeRepository.deleteByArtworkIdAndUserId(artworkId, viewer.get().getId());
        return removed > 0 ? LikeResult.UNLIKED : LikeResult.NOT_LIKED;
    }

    @Override
    @Transactional
    public boolean createArtworkComment(ArtworkCommentCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findByTitleIgnoreCase(command.title());
        if (maybeArtwork.isEmpty()) {
            return false;
        }

        Artwork artwork = maybeArtwork.get();
        Comment comment = new Comment(
                null,
                artwork,
                null,
                command.content(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false
        );
        if (artwork.getComments() == null) {
            artwork.setComments(new ArrayList<>());
        }
        artwork.getComments().add(comment);
        artwork.setUpdatedAt(LocalDateTime.now());
        artworkRepository.save(artwork);

        String key = artwork.getTitle().toLowerCase();
        List<ArtworkCommentCommand> comments = commentsByTitle.get(key);
        if (comments == null) {
            comments = new ArrayList<>();
            commentsByTitle.put(key, comments);
        }
        comments.add(command);
        return true;
    }

    @Override
    @Transactional
    public boolean createArtworkReview(ArtworkReviewCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findByTitleIgnoreCase(command.title());
        if (maybeArtwork.isEmpty()) {
            return false;
        }

        String key = maybeArtwork.get().getTitle().toLowerCase();
        List<ArtworkReviewCommand> reviews = reviewsByTitle.get(key);
        if (reviews == null) {
            reviews = new ArrayList<>();
            reviewsByTitle.put(key, reviews);
        }
        reviews.add(command);
        return true;
    }

    @Override
    @Transactional
    public UpdateResult updateArtwork(Long id, ArtworkUpdateCommand command, String editorUsername) {
        Optional<Artwork> maybeArtwork = artworkRepository.findById(id);
        if (maybeArtwork.isEmpty()) {
            return new UpdateResult(UpdateOutcome.NOT_FOUND, null);
        }
        Artwork artwork = maybeArtwork.get();

        boolean wantsSetSale = command.price() != null || command.saleStatus() != null;
        boolean wantsClearSale = Boolean.TRUE.equals(command.unlist());
        if (wantsSetSale) {
            Optional<User> editor = userRepository.findByUsername(editorUsername);
            boolean isSeller = editor.isPresent()
                    && editor.get().getAuthorities() != null
                    && editor.get().getAuthorities().stream()
                            .anyMatch(a -> "ROLE_SELLER".equals(a.getName()));
            if (!isSeller) {
                return new UpdateResult(UpdateOutcome.FORBIDDEN_SALE_GATE, null);
            }
        }

        if (command.title() != null && !command.title().isBlank()) {
            artwork.setTitle(command.title());
        }
        if (command.medium() != null && !command.medium().isBlank()) {
            artwork.setMedium(command.medium());
        }
        if (command.description() != null) {
            artwork.setDescription(command.description());
        }
        if (command.images() != null && !command.images().isEmpty()) {
            List<ArtworkImage> rebuilt = buildImages(artwork, command.images());
            artwork.getImages().clear();
            artwork.getImages().addAll(rebuilt);
        }
        if (command.width() != null || command.height() != null
                || command.depth() != null || command.dimensionUnit() != null) {
            applyDimensions(artwork, command.width(), command.height(), command.depth(), command.dimensionUnit());
        }
        if (wantsClearSale) {
            artwork.setPrice(null);
            artwork.setSaleStatus(null);
        }
        if (command.price() != null) {
            artwork.setPrice(command.price());
        }
        if (command.saleStatus() != null) {
            artwork.setSaleStatus(SaleStatus.valueOf(command.saleStatus()));
        }
        artwork.setUpdatedAt(LocalDateTime.now());
        artworkRepository.save(artwork);
        return new UpdateResult(UpdateOutcome.OK, mapToDTO(artwork, Set.of()));
    }

    @Override
    public List<String> findDistinctMediums() {
        return artworkRepository.findDistinctMediums();
    }

    @Override
    @Transactional
    public boolean deleteByTitle(String title) {
        boolean deleted = artworkRepository.deleteByTitleIgnoreCase(title) > 0;
        if (deleted) {
            String key = title.toLowerCase();
            commentsByTitle.remove(key);
            reviewsByTitle.remove(key);
        }
        return deleted;
    }

    private List<ArtworkImage> buildImages(Artwork artwork, List<ArtworkImageCommand> commands) {
        List<ArtworkImage> result = new ArrayList<>();
        boolean coverAssigned = false;
        for (int i = 0; i < commands.size(); i++) {
            ArtworkImageCommand c = commands.get(i);
            ArtworkImage img = new ArtworkImage();
            img.setArtwork(artwork);
            img.setPublicId(c.publicId());
            img.setSortOrder(c.sortOrder() != null ? c.sortOrder() : i);
            boolean isCover = Boolean.TRUE.equals(c.isCover()) && !coverAssigned;
            if (isCover) coverAssigned = true;
            img.setIsCover(isCover);
            img.setCaption(c.caption());
            img.setCreatedAt(LocalDateTime.now());
            result.add(img);
        }
        if (!coverAssigned && !result.isEmpty()) {
            result.get(0).setIsCover(true);
        }
        return result;
    }

    private void applyDimensions(Artwork artwork, java.math.BigDecimal w, java.math.BigDecimal h,
                                 java.math.BigDecimal d, String unit) {
        artwork.setWidthValue(w);
        artwork.setHeightValue(h);
        artwork.setDepthValue(d);
        artwork.setDimensionUnit(unit == null ? null : DimensionUnit.valueOf(unit));
    }

    private List<ArtworkDTO> mapMany(List<Artwork> rows, String viewerUsername) {
        Set<Long> likedSet = likedSetFor(viewerUsername, rows);
        return rows.stream().map(a -> mapToDTO(a, likedSet)).toList();
    }

    private Set<Long> likedSetFor(String viewerUsername, List<Artwork> rows) {
        if (viewerUsername == null || rows.isEmpty()) return Set.of();
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return Set.of();
        List<Long> ids = rows.stream().map(Artwork::getId).toList();
        return new HashSet<>(likeRepository.findArtworkIdsLikedByUser(viewer.get().getId(), ids));
    }

    private ArtworkDTO mapToDTO(Artwork artwork, Set<Long> likedByViewer) {
        User author = artwork.getAuthor();
        List<ArtworkImageDTO> imageDtos = artwork.getImages() == null
                ? List.of()
                : artwork.getImages().stream()
                        .map(img -> new ArtworkImageDTO(
                                img.getId(),
                                img.getPublicId(),
                                img.getSortOrder(),
                                Boolean.TRUE.equals(img.getIsCover()),
                                img.getCaption()
                        ))
                        .toList();
        return new ArtworkDTO(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getMedium(),
                artwork.getDescription(),
                artwork.getCoverPublicId(),
                artwork.getTitle() + " - " + artwork.getMedium(),
                estimateAspectRatio(artwork),
                imageDtos,
                artwork.getWidthValue(),
                artwork.getHeightValue(),
                artwork.getDepthValue(),
                artwork.getDimensionUnit() == null ? null : artwork.getDimensionUnit().name(),
                artwork.getPrice(),
                artwork.getProgressStatus() == null ? null : artwork.getProgressStatus().name(),
                artwork.getSaleStatus() == null ? null : artwork.getSaleStatus().name(),
                author == null ? null : author.getId(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug(),
                author == null ? null : author.getAvatarUrl(),
                artwork.getTags(),
                artwork.getPublishedAt(),
                artwork.getLikeCount() == null ? 0 : artwork.getLikeCount(),
                artwork.getComments() == null ? getCommentCount(artwork.getTitle()) : artwork.getComments().size(),
                likedByViewer.contains(artwork.getId())
        );
    }

    private int getCommentCount(String title) {
        if (title == null) {
            return 0;
        }
        List<ArtworkCommentCommand> comments = commentsByTitle.get(title.toLowerCase());
        if (comments == null) {
            return 0;
        }
        return comments.size();
    }

    private double estimateAspectRatio(Artwork artwork) {
        if (artwork.getCoverPublicId() == null) {
            return 1.0;
        }
        long selector = (artwork.getId() == null ? 0 : artwork.getId()) % 5;
        return switch ((int) selector) {
            case 0 -> 1.0;
            case 1 -> 0.78;
            case 2 -> 1.28;
            case 3 -> 0.92;
            default -> 1.14;
        };
    }
}
