package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.model.Authority;
import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.AuthorityJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.SellerApplicationJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SellerApplicationServiceImpl implements SellerApplicationService {

    private static final String ROLE_SELLER = "ROLE_SELLER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final SellerApplicationJpaRepository applicationRepository;
    private final UserJpaRepository userRepository;
    private final AuthorityJpaRepository authorityRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final long cooldownDays;

    public SellerApplicationServiceImpl(
            SellerApplicationJpaRepository applicationRepository,
            UserJpaRepository userRepository,
            AuthorityJpaRepository authorityRepository,
            ArtworkJpaRepository artworkRepository,
            @Value("${app.seller.reapply-cooldown-days:14}") long cooldownDays
    ) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.artworkRepository = artworkRepository;
        this.cooldownDays = cooldownDays;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SellerApplicationDTO> findMyLatest(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) return Optional.empty();
        return applicationRepository.findTopByUserIdOrderBySubmittedAtDesc(user.get().getId())
                .map(app -> toDTO(app, user.get()));
    }

    @Override
    @Transactional
    public SubmitResult submit(String username, SubmitSellerApplicationCommand command) {
        Optional<User> maybeUser = userRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return new SubmitFailure(SubmitError.USER_NOT_FOUND, null);
        }
        User user = maybeUser.get();

        if (hasRole(user, ROLE_SELLER)) {
            return new SubmitFailure(SubmitError.ALREADY_SELLER, null);
        }

        if (applicationRepository.existsByUserIdAndStatus(user.getId(), SellerApplicationStatus.PENDING)) {
            return new SubmitFailure(SubmitError.ALREADY_PENDING, null);
        }

        Optional<SellerApplication> latest = applicationRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId());
        if (latest.isPresent()) {
            LocalDateTime cooldownAnchor = cooldownAnchorFor(latest.get());
            if (cooldownAnchor != null) {
                LocalDateTime canReapplyAt = cooldownAnchor.plusDays(cooldownDays);
                if (LocalDateTime.now().isBefore(canReapplyAt)) {
                    return new SubmitFailure(SubmitError.COOLDOWN_ACTIVE, canReapplyAt);
                }
            }
        }

        SellerApplication app = new SellerApplication();
        app.setUserId(user.getId());
        app.setMessage(command.message().trim());
        app.setStatus(SellerApplicationStatus.PENDING);
        app.setSubmittedAt(LocalDateTime.now());
        applicationRepository.save(app);

        return new SubmitOk(toDTO(app, user));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SellerApplicationDTO> listApplications(String statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SellerApplication> rows;
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)) {
            rows = applicationRepository.findAllByOrderBySubmittedAtDesc(pageable);
        } else {
            SellerApplicationStatus parsed;
            try {
                parsed = SellerApplicationStatus.valueOf(statusFilter.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return Page.empty(pageable);
            }
            rows = applicationRepository.findByStatusOrderBySubmittedAtAsc(parsed, pageable);
        }
        return rows.map(app -> toDTO(app, userRepository.findById(app.getUserId()).orElse(null), true));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserSummaryDTO> searchUsers(
            String query,
            List<String> sellerStatuses,
            String role,
            String sort,
            int page,
            int size
    ) {
        // Fetch all matching users (small admin dataset).
        List<User> users;
        if (query == null || query.isBlank()) {
            users = userRepository.findAll(Sort.by("username").ascending());
        } else {
            users = userRepository
                    .searchByUsernameDisplayNameOrEmail(query.trim(), Pageable.unpaged())
                    .getContent();
        }

        // Pre-compute latest application + derived status per user.
        record Enriched(User user, Optional<SellerApplication> latest, String derivedStatus) {}
        List<Enriched> enriched = new ArrayList<>(users.size());
        for (User u : users) {
            Optional<SellerApplication> latest =
                    applicationRepository.findTopByUserIdOrderBySubmittedAtDesc(u.getId());
            String derived = deriveSellerStatus(latest.orElse(null), u);
            enriched.add(new Enriched(u, latest, derived));
        }

        // Filter by sellerStatuses.
        if (sellerStatuses != null && !sellerStatuses.isEmpty()) {
            Set<String> wanted = new HashSet<>();
            for (String s : sellerStatuses) {
                if (s != null && !s.isBlank()) {
                    wanted.add(s.trim().toUpperCase(Locale.ROOT));
                }
            }
            if (!wanted.isEmpty()) {
                enriched = enriched.stream()
                        .filter(e -> wanted.contains(e.derivedStatus()))
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        // Filter by role.
        if (role != null && !role.isBlank()) {
            String wantedRole = role.trim().toUpperCase(Locale.ROOT);
            enriched = enriched.stream()
                    .filter(e -> e.user().getAuthorities() != null
                            && e.user().getAuthorities().stream()
                                    .anyMatch(a -> wantedRole.equals(a.getName())))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Sort.
        String sortKey = (sort == null || sort.isBlank()) ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        Comparator<Enriched> comparator = switch (sortKey) {
            case "oldest_pending" -> (a, b) -> {
                LocalDateTime aPending = a.latest().isPresent()
                        && a.latest().get().getStatus() == SellerApplicationStatus.PENDING
                        ? a.latest().get().getSubmittedAt() : null;
                LocalDateTime bPending = b.latest().isPresent()
                        && b.latest().get().getStatus() == SellerApplicationStatus.PENDING
                        ? b.latest().get().getSubmittedAt() : null;
                if (aPending != null && bPending != null) {
                    return aPending.compareTo(bPending);
                }
                if (aPending != null) return -1;
                if (bPending != null) return 1;
                // Both lack a pending app - fall back to createdAt desc.
                LocalDateTime aCreated = a.user().getCreatedAt();
                LocalDateTime bCreated = b.user().getCreatedAt();
                if (aCreated == null && bCreated == null) return 0;
                if (aCreated == null) return 1;
                if (bCreated == null) return -1;
                return bCreated.compareTo(aCreated);
            };
            case "username" -> Comparator.comparing(
                    (Enriched e) -> e.user().getUsername() == null
                            ? "" : e.user().getUsername().toLowerCase(Locale.ROOT));
            case "most_artworks" -> (a, b) -> {
                long aCount = artworkRepository.countByAuthor_Id(a.user().getId());
                long bCount = artworkRepository.countByAuthor_Id(b.user().getId());
                return Long.compare(bCount, aCount);
            };
            default -> (a, b) -> {
                LocalDateTime aCreated = a.user().getCreatedAt();
                LocalDateTime bCreated = b.user().getCreatedAt();
                if (aCreated == null && bCreated == null) return 0;
                if (aCreated == null) return 1;
                if (bCreated == null) return -1;
                return bCreated.compareTo(aCreated);
            };
        };
        enriched.sort(comparator);

        int total = enriched.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<AdminUserSummaryDTO> content = enriched.subList(from, to).stream()
                .map(e -> toAdminSummaryWithPending(e.user(), e.latest()))
                .toList();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminUserDetailDTO> getUserDetail(Long userId) {
        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) return Optional.empty();
        User user = maybeUser.get();
        List<SellerApplicationDTO> history = applicationRepository
                .findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(app -> toDTO(app, user))
                .toList();
        return Optional.of(new AdminUserDetailDTO(toAdminSummary(user), history));
    }

    @Override
    @Transactional
    public DecideResult approve(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command) {
        Optional<SellerApplication> maybeApp = applicationRepository.findById(applicationId);
        if (maybeApp.isEmpty()) return new DecideFailure(DecideError.NOT_FOUND);
        SellerApplication app = maybeApp.get();
        if (app.getStatus() != SellerApplicationStatus.PENDING) {
            return new DecideFailure(DecideError.NOT_PENDING);
        }

        Optional<User> maybeAdmin = userRepository.findByUsername(adminUsername);
        Optional<User> maybeApplicant = userRepository.findById(app.getUserId());
        if (maybeAdmin.isEmpty() || maybeApplicant.isEmpty()) {
            return new DecideFailure(DecideError.NOT_FOUND);
        }

        app.setStatus(SellerApplicationStatus.APPROVED);
        app.setDecidedAt(LocalDateTime.now());
        app.setDecidedByUserId(maybeAdmin.get().getId());
        app.setDecisionReason(command == null ? null : command.reason());

        User applicant = maybeApplicant.get();
        Authority sellerAuthority = authorityRepository.findByName(ROLE_SELLER)
                .orElseGet(() -> authorityRepository.save(new Authority(null, ROLE_SELLER)));
        Set<Authority> roles = new HashSet<>(applicant.getAuthorities());
        roles.add(sellerAuthority);
        applicant.setAuthorities(roles);
        applicant.setUpdatedAt(LocalDateTime.now());
        userRepository.save(applicant);

        applicationRepository.save(app);
        return new DecideOk(toDTO(app, applicant));
    }

    @Override
    @Transactional
    public DecideResult reject(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command) {
        Optional<SellerApplication> maybeApp = applicationRepository.findById(applicationId);
        if (maybeApp.isEmpty()) return new DecideFailure(DecideError.NOT_FOUND);
        SellerApplication app = maybeApp.get();
        if (app.getStatus() != SellerApplicationStatus.PENDING) {
            return new DecideFailure(DecideError.NOT_PENDING);
        }

        Optional<User> maybeAdmin = userRepository.findByUsername(adminUsername);
        Optional<User> maybeApplicant = userRepository.findById(app.getUserId());
        if (maybeAdmin.isEmpty() || maybeApplicant.isEmpty()) {
            return new DecideFailure(DecideError.NOT_FOUND);
        }

        app.setStatus(SellerApplicationStatus.REJECTED);
        app.setDecidedAt(LocalDateTime.now());
        app.setDecidedByUserId(maybeAdmin.get().getId());
        app.setDecisionReason(command == null ? null : command.reason());
        applicationRepository.save(app);

        return new DecideOk(toDTO(app, maybeApplicant.get()));
    }

    @Override
    @Transactional
    public RevokeResult revoke(Long userId, String adminUsername, RevokeSellerCommand command) {
        Optional<User> maybeUser = userRepository.findById(userId);
        if (maybeUser.isEmpty()) {
            return new RevokeFailure(RevokeError.USER_NOT_FOUND);
        }
        User user = maybeUser.get();
        if (!hasRole(user, ROLE_SELLER)) {
            return new RevokeFailure(RevokeError.NOT_A_SELLER);
        }

        Optional<User> maybeAdmin = userRepository.findByUsername(adminUsername);
        if (maybeAdmin.isEmpty()) {
            return new RevokeFailure(RevokeError.USER_NOT_FOUND);
        }

        Set<Authority> roles = new HashSet<>(user.getAuthorities());
        roles.removeIf(a -> ROLE_SELLER.equals(a.getName()));
        user.setAuthorities(roles);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        applicationRepository.findCurrentApprovalForUser(userId).ifPresent(app -> {
            app.setRevokedAt(LocalDateTime.now());
            app.setRevokedByUserId(maybeAdmin.get().getId());
            app.setRevokeReason(command.reason());
            applicationRepository.save(app);
        });

        int unlisted = artworkRepository.unlistAllForAuthor(userId);
        return new RevokeOk(unlisted);
    }

    @Override
    @Transactional(readOnly = true)
    public ListedArtworkCountDTO countListedArtworks(Long userId) {
        return new ListedArtworkCountDTO(artworkRepository.countListedByAuthorId(userId));
    }

    private static boolean hasRole(User user, String role) {
        return user.getAuthorities() != null
                && user.getAuthorities().stream().anyMatch(a -> role.equals(a.getName()));
    }

    private LocalDateTime cooldownAnchorFor(SellerApplication app) {
        if (app.getStatus() == SellerApplicationStatus.REJECTED && app.getDecidedAt() != null) {
            return app.getDecidedAt();
        }
        if (app.getStatus() == SellerApplicationStatus.APPROVED && app.getRevokedAt() != null) {
            return app.getRevokedAt();
        }
        return null;
    }

    private SellerApplicationDTO toDTO(SellerApplication app, User user) {
        return toDTO(app, user, false);
    }

    private SellerApplicationDTO toDTO(SellerApplication app, User user, boolean includeApplicant) {
        String derived = deriveSellerStatus(app, user);
        LocalDateTime canReapplyAt = null;
        LocalDateTime anchor = cooldownAnchorFor(app);
        if (anchor != null) {
            canReapplyAt = anchor.plusDays(cooldownDays);
        }
        AdminUserSummaryDTO applicant = (includeApplicant && user != null) ? toAdminSummary(user) : null;
        return new SellerApplicationDTO(
                app.getId(),
                app.getUserId(),
                applicant,
                app.getMessage(),
                app.getStatus().name(),
                app.getSubmittedAt(),
                app.getDecidedAt(),
                app.getDecidedByUserId(),
                app.getDecisionReason(),
                app.getRevokedAt(),
                app.getRevokedByUserId(),
                app.getRevokeReason(),
                derived,
                canReapplyAt
        );
    }

    private String deriveSellerStatus(SellerApplication latest, User user) {
        if (latest != null && latest.getStatus() == SellerApplicationStatus.PENDING) {
            return "PENDING";
        }
        if (user != null && hasRole(user, ROLE_SELLER)) {
            return "APPROVED";
        }
        if (latest == null) {
            return "NONE";
        }
        if (latest.getStatus() == SellerApplicationStatus.REJECTED) {
            return "REJECTED";
        }
        return "REVOKED";
    }

    String deriveSellerStatusForUser(User user) {
        Optional<SellerApplication> latest = applicationRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId());
        return deriveSellerStatus(latest.orElse(null), user);
    }

    private AdminUserSummaryDTO toAdminSummary(User user) {
        return new AdminUserSummaryDTO(
                user.getId(),
                user.getUsername(),
                user.getSlug(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl(),
                deriveSellerStatusForUser(user),
                null,
                primaryRoleFor(user),
                user.isEnabled()
        );
    }

    private AdminUserSummaryDTO toAdminSummaryWithPending(User user, Optional<SellerApplication> latest) {
        String derived = deriveSellerStatus(latest.orElse(null), user);
        SellerApplicationDTO pending = null;
        if (latest.isPresent() && latest.get().getStatus() == SellerApplicationStatus.PENDING) {
            pending = toDTO(latest.get(), user, false);
        }
        return new AdminUserSummaryDTO(
                user.getId(),
                user.getUsername(),
                user.getSlug(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAvatarUrl(),
                derived,
                pending,
                primaryRoleFor(user),
                user.isEnabled()
        );
    }

    private static String primaryRoleFor(User user) {
        return hasRole(user, ROLE_ADMIN) ? "ADMIN" : "USER";
    }

    @Override
    @Transactional
    public String promoteToAdmin(Long userId, String actingUsername) {
        Optional<User> maybe = userRepository.findByIdWithAuthorities(userId);
        if (maybe.isEmpty()) {
            return "NOT_FOUND";
        }
        User user = maybe.get();
        if (hasRole(user, ROLE_ADMIN)) {
            return "OK";
        }
        Authority adminAuthority = authorityRepository.findByName(ROLE_ADMIN)
                .orElseGet(() -> authorityRepository.save(new Authority(null, ROLE_ADMIN)));
        Set<Authority> roles = new HashSet<>();
        if (user.getAuthorities() != null) {
            roles.addAll(user.getAuthorities());
        }
        roles.add(adminAuthority);
        user.setAuthorities(roles);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return "OK";
    }

    @Override
    @Transactional
    public String grantSellerRole(Long userId, String actingUsername) {
        Optional<User> maybe = userRepository.findByIdWithAuthorities(userId);
        if (maybe.isEmpty()) {
            return "NOT_FOUND";
        }
        User user = maybe.get();
        if (hasRole(user, ROLE_SELLER)) {
            return "OK";
        }
        Authority sellerAuthority = authorityRepository.findByName(ROLE_SELLER)
                .orElseGet(() -> authorityRepository.save(new Authority(null, ROLE_SELLER)));
        Set<Authority> roles = new HashSet<>();
        if (user.getAuthorities() != null) {
            roles.addAll(user.getAuthorities());
        }
        roles.add(sellerAuthority);
        user.setAuthorities(roles);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return "OK";
    }

    @Override
    @Transactional
    public String deactivateUser(Long userId, String actingUsername) {
        Optional<User> maybe = userRepository.findById(userId);
        if (maybe.isEmpty()) {
            return "NOT_FOUND";
        }
        User user = maybe.get();
        if (actingUsername != null && actingUsername.equals(user.getUsername())) {
            return "SELF_DEACTIVATE";
        }
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return "OK";
    }

    @Override
    @Transactional
    public String reactivateUser(Long userId, String actingUsername) {
        Optional<User> maybe = userRepository.findById(userId);
        if (maybe.isEmpty()) {
            return "NOT_FOUND";
        }
        User user = maybe.get();
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return "OK";
    }
}
