package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkLikeCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
import hr.tvz.artdrop.artdropapp.model.Comment;
import hr.tvz.artdrop.artdropapp.model.ProgressStatus;
import hr.tvz.artdrop.artdropapp.model.SaleStatus;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkJpaRepository artworkRepository;
    private final UserJpaRepository userRepository;
    private final Map<String, List<ArtworkCommentCommand>> commentsByTitle = new HashMap<>();
    private final Map<String, List<ArtworkReviewCommand>> reviewsByTitle = new HashMap<>();

    public ArtworkServiceImpl(ArtworkJpaRepository artworkRepository, UserJpaRepository userRepository) {
        this.artworkRepository = artworkRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ArtworkDTO> findAll() {
        return artworkRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<ArtworkDTO> findById(Long id) {
        return artworkRepository.findById(id)
                .map(this::mapToDTO);
    }

    @Override
    public List<ArtworkDTO> findByMedium(String medium) {
        return artworkRepository.findByMediumContainingIgnoreCase(medium)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<ArtworkDTO> findByAuthorId(Long authorId) {
        return artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(authorId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<ArtworkDTO> findCircleFeed(Long viewerId, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        return artworkRepository.findCircleFeed(viewerId, org.springframework.data.domain.PageRequest.of(page, safeLimit))
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<ArtworkDTO> findOneByTitle(String title) {
        return artworkRepository.findByTitleIgnoreCase(title)
                .map(this::mapToDTO);
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
        artwork.setLikeCount(0);
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        ArtworkImage cover = new ArtworkImage(null, artwork, command.imageUrl(), 0, true, "Cover image", LocalDateTime.now());
        artwork.setImages(new ArrayList<>(List.of(cover)));
        artworkRepository.save(artwork);
        return true;
    }

    @Override
    @Transactional
    public boolean createArtworkLike(ArtworkLikeCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findByTitleIgnoreCase(command.title());
        if (maybeArtwork.isEmpty()) {
            return false;
        }

        Artwork artwork = maybeArtwork.get();
        int current = artwork.getLikeCount() == null ? 0 : artwork.getLikeCount();
        int delta = command.delta() == null ? 1 : command.delta();

        if ("UNLIKE".equalsIgnoreCase(command.action())) {
            artwork.setLikeCount(Math.max(0, current - delta));
            return true;
        }

        artwork.setLikeCount(current + delta);
        return true;
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
        return Optional.of(mapToDTO(artwork));
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

    private ArtworkDTO mapToDTO(Artwork artwork) {
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
                artwork.getLikeCount(),
                artwork.getComments() == null ? getCommentCount(artwork.getTitle()) : artwork.getComments().size()
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
