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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int REPLY_PREVIEW_COUNT = 2;

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
        List<Comment> topLevel = commentRepository.findTopLevelByArtwork(artworkId, PageRequest.of(page, safeLimit));
        Long viewerId = viewerUsername == null
                ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);

        if (topLevel.isEmpty()) {
            return List.of();
        }
        List<Long> parentIds = topLevel.stream().map(Comment::getId).toList();
        List<Comment> replies = commentRepository.findRepliesByParentIds(parentIds);
        Map<Long, List<Comment>> repliesByParent = new HashMap<>();
        for (Comment reply : replies) {
            repliesByParent
                    .computeIfAbsent(reply.getParentCommentId(), k -> new ArrayList<>())
                    .add(reply);
        }

        return topLevel.stream()
                .map(c -> {
                    List<Comment> all = repliesByParent.getOrDefault(c.getId(), List.of());
                    List<CommentDTO> preview = all.stream()
                            .limit(REPLY_PREVIEW_COUNT)
                            .map(r -> toDTO(r, viewerId, 0, List.of()))
                            .toList();
                    return toDTO(c, viewerId, all.size(), preview);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> listReplies(Long parentId, String viewerUsername, int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        int page = safeOffset / safeLimit;
        List<Comment> replies = commentRepository.findRepliesByParentId(parentId, PageRequest.of(page, safeLimit));
        Long viewerId = viewerUsername == null
                ? null
                : userRepository.findByUsername(viewerUsername).map(User::getId).orElse(null);
        return replies.stream()
                .map(r -> toDTO(r, viewerId, 0, List.of()))
                .toList();
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

        Long parentId = command.parentCommentId();
        if (parentId != null) {
            Optional<Comment> maybeParent = commentRepository.findById(parentId);
            if (maybeParent.isEmpty()) {
                return Optional.empty();
            }
            Comment parent = maybeParent.get();
            boolean parentBelongsToArtwork = parent.getArtwork() != null
                    && artworkId.equals(parent.getArtwork().getId());
            boolean parentIsTopLevel = parent.getParentCommentId() == null;
            boolean parentIsActive = !Boolean.TRUE.equals(parent.getIsDeleted());
            if (!parentBelongsToArtwork || !parentIsTopLevel || !parentIsActive) {
                return Optional.empty();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Comment comment = new Comment();
        comment.setArtwork(artwork);
        comment.setAuthor(author);
        comment.setText(command.text());
        comment.setParentCommentId(parentId);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setIsDeleted(false);
        Comment saved = commentRepository.save(comment);
        return Optional.of(toDTO(saved, author.getId(), 0, List.of()));
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
        Long authorId = comment.getAuthor() == null ? null : comment.getAuthor().getId();
        if (requester.isEmpty() || authorId == null || !requester.get().getId().equals(authorId)) {
            return DeleteResult.FORBIDDEN;
        }
        comment.setIsDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        return DeleteResult.OK;
    }

    private CommentDTO toDTO(
            Comment comment,
            Long viewerId,
            int replyCount,
            List<CommentDTO> replies
    ) {
        User author = comment.getAuthor();
        Long authorId = author == null ? null : author.getId();
        boolean isAuthor = viewerId != null && authorId != null && viewerId.equals(authorId);
        return new CommentDTO(
                comment.getId(),
                comment.getText(),
                comment.getCreatedAt(),
                authorId,
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug(),
                author == null ? null : author.getAvatarUrl(),
                isAuthor,
                comment.getParentCommentId(),
                replyCount,
                replies
        );
    }
}
