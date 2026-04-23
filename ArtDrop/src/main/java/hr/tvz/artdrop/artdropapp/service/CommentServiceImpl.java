package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CommentDTO;
import hr.tvz.artdrop.artdropapp.dto.CreateCommentCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Comment;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.CommentJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentJpaRepository commentRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final UserJpaRepository userRepository;

    public CommentServiceImpl(
            CommentJpaRepository commentRepository,
            ArtworkJpaRepository artworkRepository,
            UserJpaRepository userRepository
    ) {
        this.commentRepository = commentRepository;
        this.artworkRepository = artworkRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> listForArtwork(Long artworkId, String viewerUsername, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        List<Comment> comments = commentRepository.findTopLevelByArtwork(artworkId, PageRequest.of(page, safeLimit));
        Long viewerId = viewerUsername == null
                ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);
        Map<Long, User> authorCache = new HashMap<>();
        return comments.stream().map(c -> toDTO(c, viewerId, authorCache)).toList();
    }

    @Override
    @Transactional
    public Optional<CommentDTO> create(Long artworkId, String authorUsername, CreateCommentCommand command) {
        Optional<Artwork> maybeArtwork = artworkRepository.findById(artworkId);
        Optional<User> maybeAuthor = userRepository.findByUsername(authorUsername);
        if (maybeArtwork.isEmpty() || maybeAuthor.isEmpty()) {
            return Optional.empty();
        }
        Artwork artwork = maybeArtwork.get();
        User author = maybeAuthor.get();
        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment(null, artwork, author.getId(), command.text(), null, now, now, false);
        Comment saved = commentRepository.save(comment);
        Map<Long, User> cache = new HashMap<>();
        return Optional.of(toDTO(saved, author.getId(), cache));
    }

    @Override
    @Transactional
    public DeleteResult delete(Long commentId, String requesterUsername) {
        Optional<Comment> maybeComment = commentRepository.findById(commentId);
        if (maybeComment.isEmpty()) {
            return DeleteResult.NOT_FOUND;
        }
        Comment comment = maybeComment.get();
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            return DeleteResult.OK;
        }
        Optional<User> requester = userRepository.findByUsername(requesterUsername);
        if (requester.isEmpty() || !requester.get().getId().equals(comment.getAuthorId())) {
            return DeleteResult.FORBIDDEN;
        }
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        return DeleteResult.OK;
    }

    private CommentDTO toDTO(Comment comment, Long viewerId, Map<Long, User> authorCache) {
        User author = null;
        if (comment.getAuthorId() != null) {
            author = authorCache.get(comment.getAuthorId());
            if (author == null) {
                author = userRepository.findById(comment.getAuthorId()).orElse(null);
                if (author != null) {
                    authorCache.put(author.getId(), author);
                }
            }
        }
        boolean isAuthor = viewerId != null && comment.getAuthorId() != null && viewerId.equals(comment.getAuthorId());
        return new CommentDTO(
                comment.getId(),
                comment.getText(),
                comment.getCreatedAt(),
                author == null ? null : author.getId(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug(),
                author == null ? null : author.getAvatarUrl(),
                isAuthor
        );
    }
}
