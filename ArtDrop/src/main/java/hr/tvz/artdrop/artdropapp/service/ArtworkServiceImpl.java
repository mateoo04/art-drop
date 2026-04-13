package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkLikeCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
import hr.tvz.artdrop.artdropapp.model.Comment;
import hr.tvz.artdrop.artdropapp.repository.ArtworkRepository;
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

    private final ArtworkRepository artworkRepository;
    private final Map<String, List<ArtworkCommentCommand>> commentsByTitle = new HashMap<>();
    private final Map<String, List<ArtworkReviewCommand>> reviewsByTitle = new HashMap<>();

    public ArtworkServiceImpl(ArtworkRepository artworkRepository) {
        this.artworkRepository = artworkRepository;
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
        return artworkRepository.findByMedium(medium)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public Optional<ArtworkDTO> findOneByTitle(String title) {
        return artworkRepository.findOneByTitleIgnoreCase(title)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional
    public boolean createArtwork(ArtworkCommand command) {
        if (artworkRepository.existsByTitleIgnoreCase(command.title())) {
            return false;
        }
        Artwork artwork = new Artwork();
        artwork.setAuthorId(1L);
        artwork.setTitle(command.title());
        artwork.setMedium(command.medium());
        artwork.setDescription(command.description());
        artwork.setImageUrl(command.imageUrl());
        artwork.setTags(List.of());
        artwork.setPublishedAt(LocalDateTime.now());
        artwork.setLikeCount(0);
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        ArtworkImage cover = new ArtworkImage(null, artwork, command.imageUrl(), 0, true, "Cover image", LocalDateTime.now());
        artwork.setImages(new ArrayList<>(List.of(cover)));
        artworkRepository.addArtwork(artwork);
        return true;
    }

    @Override
    @Transactional
    public boolean createArtworkLike(ArtworkLikeCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findOneByTitleIgnoreCase(command.title());
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
        Optional<Artwork> maybeArtwork = artworkRepository.findOneByTitleIgnoreCase(command.title());
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
        artworkRepository.addArtwork(artwork);

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
        Optional<Artwork> maybeArtwork = artworkRepository.findOneByTitleIgnoreCase(command.title());
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
    public boolean deleteByTitle(String title) {
        boolean deleted = artworkRepository.deleteByTitleIgnoreCase(title);
        if (deleted) {
            String key = title.toLowerCase();
            commentsByTitle.remove(key);
            reviewsByTitle.remove(key);
        }
        return deleted;
    }

    private ArtworkDTO mapToDTO(Artwork artwork) {
        return new ArtworkDTO(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getMedium(),
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
}
