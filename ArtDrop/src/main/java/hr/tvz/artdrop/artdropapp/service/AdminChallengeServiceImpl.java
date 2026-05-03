package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminChallengeRowDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminChallengeUpsertDTO;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeKind;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AdminChallengeServiceImpl implements AdminChallengeService {

    private final ChallengeJpaRepository challengeRepository;
    private final ChallengeSubmissionJpaRepository submissionRepository;
    private final UserJpaRepository userRepository;

    public AdminChallengeServiceImpl(
            ChallengeJpaRepository challengeRepository,
            ChallengeSubmissionJpaRepository submissionRepository,
            UserJpaRepository userRepository
    ) {
        this.challengeRepository = challengeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminChallengeRowDTO> list(String query, ChallengeStatus status, int page, int size, String sort) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Challenge> filtered = challengeRepository.findAll().stream()
                .filter(c -> status == null || status.equals(c.getStatus()))
                .filter(c -> q.isEmpty()
                        || (c.getTitle() != null && c.getTitle().toLowerCase(Locale.ROOT).contains(q))
                        || (c.getDescription() != null && c.getDescription().toLowerCase(Locale.ROOT).contains(q)))
                .sorted(resolveComparator(sort))
                .toList();

        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<AdminChallengeRowDTO> slice = filtered.subList(from, to).stream().map(this::toRow).toList();
        return new PageImpl<>(slice, PageRequest.of(page, size), total);
    }

    private static Comparator<Challenge> resolveComparator(String sort) {
        String key = sort == null || sort.isBlank() ? "starts_desc" : sort.trim().toLowerCase(Locale.ROOT);
        Comparator<Challenge> byStarts = Comparator.comparing(
                (Challenge c) -> c.getStartsAt() == null ? LocalDateTime.MIN : c.getStartsAt()
        ).reversed();
        Comparator<Challenge> byTitle = Comparator.comparing(
                (Challenge c) -> c.getTitle() == null ? "" : c.getTitle().toLowerCase(Locale.ROOT)
        );
        return switch (key) {
            case "title" -> byTitle;
            default -> byStarts;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminChallengeRowDTO> get(Long id) {
        return challengeRepository.findById(id).map(this::toRow);
    }

    @Override
    @Transactional
    public AdminChallengeRowDTO create(AdminChallengeUpsertDTO dto, String adminUsername) {
        Challenge c = new Challenge();
        applyUpsert(c, dto);
        c.setCreatedBy(resolveAdminUserId(adminUsername));
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        if (c.getKind() == null) {
            c.setKind(ChallengeKind.OPEN);
        }
        if (c.getStatus() == null) {
            c.setStatus(ChallengeStatus.UPCOMING);
        }
        return toRow(challengeRepository.save(c));
    }

    @Override
    @Transactional
    public Optional<AdminChallengeRowDTO> update(Long id, AdminChallengeUpsertDTO dto) {
        return challengeRepository.findById(id).map(c -> {
            applyUpsert(c, dto);
            c.setUpdatedAt(LocalDateTime.now());
            return toRow(challengeRepository.save(c));
        });
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        if (!challengeRepository.existsById(id)) {
            return false;
        }
        challengeRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    public Optional<AdminChallengeRowDTO> setStatus(Long id, ChallengeStatus status) {
        return challengeRepository.findById(id).map(c -> {
            c.setStatus(status);
            c.setUpdatedAt(LocalDateTime.now());
            return toRow(challengeRepository.save(c));
        });
    }

    private Long resolveAdminUserId(String adminUsername) {
        if (adminUsername == null || adminUsername.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(adminUsername)
                .map(User::getId)
                .orElse(null);
    }

    private static void applyUpsert(Challenge c, AdminChallengeUpsertDTO dto) {
        c.setTitle(dto.title().trim());
        c.setDescription(dto.description());
        c.setQuote(dto.quote());
        c.setKind(dto.kind());
        c.setStatus(dto.status());
        c.setTheme(dto.theme());
        c.setCoverImageUrl(dto.coverImageUrl());
        c.setStartsAt(dto.startsAt());
        c.setEndsAt(dto.endsAt());
    }

    private AdminChallengeRowDTO toRow(Challenge c) {
        long count = submissionRepository.countByChallengeId(c.getId());
        return new AdminChallengeRowDTO(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.getQuote(),
                c.getKind() == null ? null : c.getKind().name(),
                c.getStatus() == null ? null : c.getStatus().name(),
                c.getTheme(),
                c.getCoverImageUrl(),
                c.getStartsAt(),
                c.getEndsAt(),
                count
        );
    }
}
