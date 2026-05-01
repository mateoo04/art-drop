package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
import hr.tvz.artdrop.artdropapp.model.ArtworkLike;
import hr.tvz.artdrop.artdropapp.model.Comment;
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
        List<Artwork> rows = artworkRepository.findAll(page).getContent();
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
        List<Artwork> rows = artworkRepository.findByMediumContainingIgnoreCase(medium, page);
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
    public Optional<ArtworkDTO> findOneByTitle(String title, String viewerUsername) {
        return artworkRepository.findByTitleIgnoreCase(title)
                .map(a -> mapToDTO(a, likedSetFor(viewerUsername, List.of(a))));
    }

    @Override
    @Transactional
    public boolean createArtwork(ArtworkCommand command) {
        if (artworkRepository.existsByTitleIgnoreCase(command.title())) {
            return false;
        }
        Artwork artwork = new Artwork();
        User defaultAuthor = userRepository.findById(1L).orElse(null);
        artwork.setAuthor(defaultAuthor);
        artwork.setTitle(command.title());
        artwork.setMedium(command.medium());
        artwork.setDescription(command.description());
        artwork.setImageUrl(command.imageUrl());
        artwork.setProgressStatus(ProgressStatus.FINISHED);
        artwork.setSaleStatus(SaleStatus.AVAILABLE);
        artwork.setTags(List.of());
        artwork.setPublishedAt(LocalDateTime.now());
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        ArtworkImage cover = new ArtworkImage(null, artwork, command.imageUrl(), 0, true, "Cover image", LocalDateTime.now());
        artwork.setImages(new ArrayList<>(List.of(cover)));
        artworkRepository.save(artwork);
        return true;
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
    public Optional<ArtworkDTO> updateArtwork(Long id, ArtworkUpdateCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findById(id);
        if (maybeArtwork.isEmpty()) {
            return Optional.empty();
        }
        Artwork artwork = maybeArtwork.get();
        if (command.title() != null && !command.title().isBlank()) {
            artwork.setTitle(command.title());
        }
        if (command.medium() != null && !command.medium().isBlank()) {
            artwork.setMedium(command.medium());
        }
        if (command.description() != null) {
            artwork.setDescription(command.description());
        }
        if (command.imageUrl() != null && !command.imageUrl().isBlank()) {
            artwork.setImageUrl(command.imageUrl());
        }
        artwork.setUpdatedAt(LocalDateTime.now());
        artworkRepository.save(artwork);
        return Optional.of(mapToDTO(artwork, Set.of()));
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
        return new ArtworkDTO(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getMedium(),
                artwork.getDescription(),
                artwork.getImageUrl(),
                artwork.getTitle() + " - " + artwork.getMedium(),
                estimateAspectRatio(artwork),
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
        if (artwork.getImageUrl() == null) {
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
