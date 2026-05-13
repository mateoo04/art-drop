package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkImageCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkImageDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.ArtworkImage;
import hr.tvz.artdrop.artdropapp.model.ArtworkLike;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.DimensionUnit;
import hr.tvz.artdrop.artdropapp.model.ProgressStatus;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ArtworkLikeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.CommentJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.OrderRepository;
import hr.tvz.artdrop.artdropapp.repository.UserFollowJpaRepository;
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
    private final CommentJpaRepository commentRepository;
    private final ChallengeJpaRepository challengeRepository;
    private final ChallengeSubmissionJpaRepository submissionRepository;
    private final UserFollowJpaRepository followRepository;
    private final OrderRepository orderRepository;

    public ArtworkServiceImpl(
            ArtworkJpaRepository artworkRepository,
            ArtworkLikeJpaRepository likeRepository,
            UserJpaRepository userRepository,
            CommentJpaRepository commentRepository,
            ChallengeJpaRepository challengeRepository,
            ChallengeSubmissionJpaRepository submissionRepository,
            UserFollowJpaRepository followRepository,
            OrderRepository orderRepository
    ) {
        this.artworkRepository = artworkRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
        this.followRepository = followRepository;
        this.orderRepository = orderRepository;
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
        return artworkRepository.findDetailById(id)
                .map(a -> mapToDTO(
                        a,
                        likedSetFor(viewerUsername, List.of(a)),
                        followedAuthorsFor(viewerUsername, List.of(a)),
                        commentCountsFor(List.of(a)),
                        activeSubmissionsFor(List.of(a))
                ));
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

    @Override
    public List<ArtworkDTO> searchArtworks(String query, String viewerUsername, int limit, int offset) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String tsq = toPrefixTsQuery(trimmed);
        if (tsq.isEmpty()) {
            tsq = " ";
        }
        int cappedLimit = Math.max(1, Math.min(limit, 50));
        Pageable page = paged(cappedLimit, offset);
        List<Artwork> rows = artworkRepository.searchArtworks(tsq, trimmed, page);
        return mapMany(rows, viewerUsername);
    }

    private static String toPrefixTsQuery(String input) {
        return java.util.Arrays.stream(input.toLowerCase().split("\\s+"))
                .map(s -> s.replaceAll("[^a-z0-9]", ""))
                .filter(s -> !s.isEmpty())
                .map(s -> s + ":*")
                .collect(java.util.stream.Collectors.joining(" & "));
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
    public List<ArtworkDTO> findByAuthorId(Long authorId, String viewerUsername, int limit, int offset) {
        return mapMany(
                artworkRepository.findByAuthor_IdOrderByPublishedAtDesc(authorId, paged(limit, offset)),
                viewerUsername
        );
    }

    @Override
    public List<ArtworkDTO> findCircleFeed(Long viewerId, int limit, int offset) {
        List<Artwork> rows = artworkRepository.findCircleFeed(viewerId, paged(limit, offset));
        Set<Long> likedSet = rows.isEmpty() ? Set.of() : new HashSet<>(
                likeRepository.findArtworkIdsLikedByUser(viewerId, rows.stream().map(Artwork::getId).toList())
        );
        Set<Long> followedAuthors = rows.stream()
                .map(Artwork::getAuthor)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, Integer> commentCounts = commentCountsFor(rows);
        return rows.stream().map(a -> mapToDTO(a, likedSet, followedAuthors, commentCounts)).toList();
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
                .map(a -> mapToDTO(a, likedSetFor(viewerUsername, List.of(a)), commentCountsFor(List.of(a))));
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

        boolean wantsSale = command.price() != null || command.saleType() != null;
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
        SaleType resolvedType = command.saleType() == null
                ? SaleType.ORIGINAL
                : SaleType.valueOf(command.saleType());
        artwork.setSaleType(resolvedType);
        artwork.setSaleState(wantsSale ? SaleState.AVAILABLE : SaleState.DRAFT);
        if (resolvedType == SaleType.EDITION) {
            artwork.setEditionSize(command.editionSize());
        }
        artwork.setPrice(command.price());
        artwork.setTags(command.tags() == null ? new ArrayList<>() : new ArrayList<>(command.tags()));
        artwork.setPublishedAt(LocalDateTime.now());
        artwork.setCreatedAt(LocalDateTime.now());
        artwork.setUpdatedAt(LocalDateTime.now());
        applyDimensions(artwork, command.width(), command.height(), command.depth(), command.dimensionUnit());
        artwork.setImages(buildImages(artwork, command.images()));
        Artwork saved = artworkRepository.save(artwork);

        Map<Long, ChallengeSubmission> activeSubmission = Map.of();
        if (command.challengeId() != null) {
            Optional<Challenge> challengeOpt = challengeRepository.findById(command.challengeId());
            if (challengeOpt.isEmpty()) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                        .currentTransactionStatus().setRollbackOnly();
                return new CreateResult(CreateOutcome.CHALLENGE_NOT_FOUND, null);
            }
            Challenge challenge = challengeOpt.get();
            if (challenge.getStatus() != ChallengeStatus.ACTIVE) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                        .currentTransactionStatus().setRollbackOnly();
                return new CreateResult(CreateOutcome.CHALLENGE_NOT_ACTIVE, null);
            }
            if (submissionRepository.existsByChallenge_IdAndSubmittedBy(challenge.getId(), author.getId())) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                        .currentTransactionStatus().setRollbackOnly();
                return new CreateResult(CreateOutcome.CHALLENGE_USER_ALREADY_HAS_ENTRY, null);
            }
            ChallengeSubmission submission = submissionRepository.save(new ChallengeSubmission(
                    null, challenge, saved, author.getId(), LocalDateTime.now()
            ));
            activeSubmission = Map.of(saved.getId(), submission);
        }

        return new CreateResult(CreateOutcome.CREATED, mapToDTO(saved, Set.of(), Set.of(), Map.of(), activeSubmission));
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
    public UpdateResult updateArtwork(Long id, ArtworkUpdateCommand command, String editorUsername) {
        if (editorUsername == null) {
            return new UpdateResult(UpdateOutcome.FORBIDDEN, null);
        }
        Optional<User> editor = userRepository.findByUsername(editorUsername);
        if (editor.isEmpty()) {
            return new UpdateResult(UpdateOutcome.FORBIDDEN, null);
        }
        Optional<Artwork> maybeArtwork = artworkRepository.findById(id);
        if (maybeArtwork.isEmpty()) {
            return new UpdateResult(UpdateOutcome.NOT_FOUND, null);
        }
        Artwork artwork = maybeArtwork.get();
        if (!canManageArtwork(artwork, editor.get())) {
            return new UpdateResult(UpdateOutcome.FORBIDDEN, null);
        }

        boolean wantsSetSale = command.price() != null || command.saleType() != null;
        boolean wantsClearSale = Boolean.TRUE.equals(command.unlist());
        if (wantsSetSale) {
            boolean isSeller = hasAuthority(editor.get(), "ROLE_SELLER");
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
        if (command.progressStatus() != null) {
            artwork.setProgressStatus(ProgressStatus.valueOf(command.progressStatus()));
        }
        if (command.tags() != null) {
            artwork.setTags(new ArrayList<>(command.tags()));
        }
        if (wantsClearSale) {
            artwork.setPrice(null);
            artwork.setSaleState(SaleState.DRAFT);
        }
        if (command.price() != null) {
            artwork.setPrice(command.price());
        }
        if (command.saleType() != null) {
            SaleType updatedType = SaleType.valueOf(command.saleType());
            artwork.setSaleType(updatedType);
            if (updatedType == SaleType.EDITION && command.editionSize() != null) {
                artwork.setEditionSize(command.editionSize());
            }
            if (artwork.getSaleState() == SaleState.DRAFT) {
                artwork.setSaleState(SaleState.AVAILABLE);
            }
        } else if (wantsSetSale && artwork.getSaleState() == SaleState.DRAFT) {
            artwork.setSaleState(SaleState.AVAILABLE);
        }
        artwork.setUpdatedAt(LocalDateTime.now());
        artworkRepository.save(artwork);
        return new UpdateResult(UpdateOutcome.OK, mapToDTO(artwork, Set.of(), commentCountsFor(List.of(artwork))));
    }

    @Override
    public List<String> findDistinctMediums() {
        return artworkRepository.findDistinctMediums();
    }

    @Override
    @Transactional
    public DeleteOutcome deleteArtwork(Long id, String requesterUsername) {
        if (requesterUsername == null) {
            return DeleteOutcome.FORBIDDEN;
        }
        Optional<User> requester = userRepository.findByUsername(requesterUsername);
        if (requester.isEmpty()) {
            return DeleteOutcome.FORBIDDEN;
        }
        Optional<Artwork> maybeArtwork = artworkRepository.findById(id);
        if (maybeArtwork.isEmpty()) {
            return DeleteOutcome.NOT_FOUND;
        }
        Artwork artwork = maybeArtwork.get();
        if (!canManageArtwork(artwork, requester.get())) {
            return DeleteOutcome.FORBIDDEN;
        }
        if (orderRepository.countByArtworkId(id) > 0) {
            return DeleteOutcome.HAS_ORDERS;
        }

        submissionRepository.deleteByArtworkId(id);
        likeRepository.deleteByArtworkId(id);
        artworkRepository.delete(artwork);
        return DeleteOutcome.DELETED;
    }

    @Override
    @Transactional
    public boolean deleteByTitle(String title) {
        return artworkRepository.deleteByTitleIgnoreCase(title) > 0;
    }

    private boolean canManageArtwork(Artwork artwork, User user) {
        if (hasAuthority(user, "ROLE_ADMIN")) {
            return true;
        }
        return artwork.getAuthor() != null && artwork.getAuthor().getId().equals(user.getId());
    }

    private boolean hasAuthority(User user, String authority) {
        return user.getAuthorities() != null
                && user.getAuthorities().stream().anyMatch(a -> authority.equals(a.getName()));
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
        Set<Long> followedAuthors = followedAuthorsFor(viewerUsername, rows);
        Map<Long, Integer> commentCounts = commentCountsFor(rows);
        return rows.stream().map(a -> mapToDTO(a, likedSet, followedAuthors, commentCounts)).toList();
    }

    private Set<Long> likedSetFor(String viewerUsername, List<Artwork> rows) {
        if (viewerUsername == null || rows.isEmpty()) return Set.of();
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return Set.of();
        List<Long> ids = rows.stream().map(Artwork::getId).toList();
        return new HashSet<>(likeRepository.findArtworkIdsLikedByUser(viewer.get().getId(), ids));
    }

    private Set<Long> followedAuthorsFor(String viewerUsername, List<Artwork> rows) {
        if (viewerUsername == null || rows.isEmpty()) return Set.of();
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        if (viewer.isEmpty()) return Set.of();
        Long viewerId = viewer.get().getId();
        Set<Long> authorIds = rows.stream()
                .map(Artwork::getAuthor)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .filter(id -> id != null && !id.equals(viewerId))
                .collect(java.util.stream.Collectors.toSet());
        if (authorIds.isEmpty()) return Set.of();
        return new HashSet<>(followRepository.findFolloweeIdsByFollowerIdAndFolloweeIdIn(viewerId, authorIds));
    }

    private Map<Long, ChallengeSubmission> activeSubmissionsFor(List<Artwork> rows) {
        if (rows.isEmpty()) return Map.of();
        Map<Long, ChallengeSubmission> result = new HashMap<>();
        for (Artwork a : rows) {
            if (a.getId() == null) continue;
            submissionRepository
                    .findFirstByArtworkIdAndChallenge_StatusNot(a.getId(), ChallengeStatus.ENDED)
                    .ifPresent(s -> result.put(a.getId(), s));
        }
        return result;
    }

    private Map<Long, Integer> commentCountsFor(List<Artwork> rows) {
        if (rows.isEmpty()) return Map.of();
        List<Long> ids = rows.stream().map(Artwork::getId).filter(java.util.Objects::nonNull).toList();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : commentRepository.countActiveByArtworkIds(ids)) {
            result.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    private ArtworkDTO mapToDTO(Artwork artwork, Set<Long> likedByViewer, Map<Long, Integer> commentCounts) {
        return mapToDTO(artwork, likedByViewer, Set.of(), commentCounts, Map.of());
    }

    private ArtworkDTO mapToDTO(
            Artwork artwork,
            Set<Long> likedByViewer,
            Set<Long> followedAuthorIds,
            Map<Long, Integer> commentCounts
    ) {
        return mapToDTO(artwork, likedByViewer, followedAuthorIds, commentCounts, Map.of());
    }

    private ArtworkDTO mapToDTO(
            Artwork artwork,
            Set<Long> likedByViewer,
            Set<Long> followedAuthorIds,
            Map<Long, Integer> commentCounts,
            Map<Long, ChallengeSubmission> activeSubmissionByArtworkId
    ) {
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
        ChallengeSubmission activeSubmission = activeSubmissionByArtworkId.get(artwork.getId());
        ArtworkDTO.CurrentSubmissionDTO currentSubmission = null;
        if (activeSubmission != null && activeSubmission.getChallenge() != null) {
            Challenge c = activeSubmission.getChallenge();
            currentSubmission = new ArtworkDTO.CurrentSubmissionDTO(c.getId(), c.getTitle());
        }
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
                artwork.getSaleType() == null ? null : artwork.getSaleType().name(),
                artwork.getSaleState() == null ? null : artwork.getSaleState().name(),
                artwork.getEditionSize(),
                null,
                false,
                author == null ? null : author.getId(),
                author == null ? null : author.getDisplayName(),
                author == null ? null : author.getSlug(),
                author == null ? null : author.getAvatarUrl(),
                artwork.getTags(),
                artwork.getPublishedAt(),
                artwork.getLikeCount() == null ? 0 : artwork.getLikeCount(),
                commentCounts.getOrDefault(artwork.getId(), 0),
                likedByViewer.contains(artwork.getId()),
                author != null && author.getId() != null && followedAuthorIds.contains(author.getId()),
                currentSubmission
        );
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
