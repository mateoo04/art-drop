# Admin Panel & Seller Status Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a seller-application workflow gated by a new `ROLE_SELLER`, plus an admin panel with a searchable user directory and a seller-request queue. Approval grants the role; revoke removes the role and unlists the user's for-sale artworks.

**Architecture:** Spring Boot backend adds one new entity (`seller_application`) and one new authority (`ROLE_SELLER`); user-facing endpoints under `/api/users/me/seller-application`, admin endpoints under `/api/admin/**` gated by `hasRole('ADMIN')`. Sale-gate in `ArtworkServiceImpl` blocks non-sellers from setting `price`/`saleStatus`. React frontend adds a "Become a seller" section on `AccountPage`, contextual gating in `ArtworkEditPage`, and a new `/admin` route tree (directory + requests queue + user detail) wrapped in an `AdminRoute` guard.

**Tech Stack:** Spring Boot 4.0.5 + Java 25 + JPA + H2; React 19 + Vite + TypeScript + Tailwind CSS 4 + React Router 7.

**Source spec:** [`docs/superpowers/specs/2026-05-01-admin-seller-management-design.md`](../specs/2026-05-01-admin-seller-management-design.md)

**Note on tests:** Backend integration tests cover the security-critical paths (cooldown, double-pending guard, revoke unlist, sale-gate). Frontend uses manual verification (the project has no JS test setup yet).

---

## Task 1: Schema + seed for `seller_application` and `ROLE_SELLER`

**Files:**
- Modify: `ArtDrop/src/main/resources/schema.sql`
- Modify: `ArtDrop/src/main/resources/data.sql`

- [ ] **Step 1: Append `seller_application` table to schema.sql**

Open `ArtDrop/src/main/resources/schema.sql` and append at the end:

```sql

CREATE TABLE IF NOT EXISTS seller_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    submitted_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP,
    decided_by_user_id BIGINT,
    decision_reason VARCHAR(500),
    revoked_at TIMESTAMP,
    revoked_by_user_id BIGINT,
    revoke_reason VARCHAR(500),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (decided_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL,
    FOREIGN KEY (revoked_by_user_id) REFERENCES app_user(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_seller_application_user ON seller_application(user_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_seller_application_status ON seller_application(status, submitted_at);
```

- [ ] **Step 2: Add `ROLE_SELLER` to data.sql authorities seed**

Open `ArtDrop/src/main/resources/data.sql`. Replace the block at lines 16–18:

```sql
INSERT INTO authority (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_USER');
```

with:

```sql
INSERT INTO authority (id, name) VALUES
(1, 'ROLE_ADMIN'),
(2, 'ROLE_USER'),
(3, 'ROLE_SELLER');
```

- [ ] **Step 3: Add the `RESTART WITH 100` line for the new table**

Find the existing block near the end of `data.sql`:

```sql
ALTER TABLE authority            ALTER COLUMN id RESTART WITH 100;
```

Append immediately after it:

```sql
ALTER TABLE seller_application   ALTER COLUMN id RESTART WITH 100;
```

- [ ] **Step 4: Boot the app to verify schema applies cleanly**

Delete the existing H2 file so the schema rebuilds clean:

```bash
rm -f ArtDrop/data/artdropapp.mv.db ArtDrop/data/artdropapp.trace.db
```

Then run from `ArtDrop/`:

```bash
cd ArtDrop && ./mvnw spring-boot:run
```

Expected: app starts on port 8089 with no SQL errors. Stop it with Ctrl+C once the "Started ArtDropApplication" log appears.

- [ ] **Step 5: Commit**

```bash
git add ArtDrop/src/main/resources/schema.sql ArtDrop/src/main/resources/data.sql
git commit -m "feat: schema and seed for seller_application + ROLE_SELLER

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Configurable cooldown property

**Files:**
- Modify: `ArtDrop/src/main/resources/application.properties`

- [ ] **Step 1: Append cooldown property**

Append to `ArtDrop/src/main/resources/application.properties`:

```properties
app.seller.reapply-cooldown-days=14
```

- [ ] **Step 2: Commit**

```bash
git add ArtDrop/src/main/resources/application.properties
git commit -m "feat: add seller re-apply cooldown property

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: `SellerApplication` entity + status enum

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/model/SellerApplicationStatus.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/model/SellerApplication.java`

- [ ] **Step 1: Create the status enum**

```java
package hr.tvz.artdrop.artdropapp.model;

public enum SellerApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
```

- [ ] **Step 2: Create the entity**

```java
package hr.tvz.artdrop.artdropapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seller_application")
public class SellerApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerApplicationStatus status;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by_user_id")
    private Long decidedByUserId;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;
}
```

- [ ] **Step 3: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/model/SellerApplication.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/model/SellerApplicationStatus.java
git commit -m "feat: SellerApplication entity and status enum

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: `SellerApplicationJpaRepository`

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/SellerApplicationJpaRepository.java`

- [ ] **Step 1: Create the repository**

```java
package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerApplicationJpaRepository extends JpaRepository<SellerApplication, Long> {

    Optional<SellerApplication> findTopByUserIdOrderBySubmittedAtDesc(Long userId);

    List<SellerApplication> findByUserIdOrderBySubmittedAtDesc(Long userId);

    boolean existsByUserIdAndStatus(Long userId, SellerApplicationStatus status);

    Page<SellerApplication> findByStatusOrderBySubmittedAtAsc(SellerApplicationStatus status, Pageable pageable);

    Page<SellerApplication> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @Query("SELECT s FROM SellerApplication s WHERE s.userId = :userId AND s.status = 'APPROVED' AND s.revokedAt IS NULL ORDER BY s.decidedAt DESC")
    Optional<SellerApplication> findCurrentApprovalForUser(Long userId);
}
```

- [ ] **Step 2: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/SellerApplicationJpaRepository.java
git commit -m "feat: SellerApplicationJpaRepository

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: DTOs and commands

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SubmitSellerApplicationCommand.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SellerApplicationDecisionCommand.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/RevokeSellerCommand.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SellerApplicationDTO.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/AdminUserSummaryDTO.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/AdminUserDetailDTO.java`
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/ListedArtworkCountDTO.java`

- [ ] **Step 1: SubmitSellerApplicationCommand**

```java
package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitSellerApplicationCommand(
        @NotBlank
        @Size(min = 30, max = 1000, message = "Message must be 30-1000 characters")
        String message
) {}
```

- [ ] **Step 2: SellerApplicationDecisionCommand**

```java
package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.Size;

public record SellerApplicationDecisionCommand(
        @Size(max = 500)
        String reason
) {}
```

- [ ] **Step 3: RevokeSellerCommand**

```java
package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RevokeSellerCommand(
        @NotBlank
        @Size(max = 500)
        String reason
) {}
```

- [ ] **Step 4: SellerApplicationDTO**

```java
package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;

public record SellerApplicationDTO(
        Long id,
        Long userId,
        AdminUserSummaryDTO applicant,
        String message,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime decidedAt,
        Long decidedByUserId,
        String decisionReason,
        LocalDateTime revokedAt,
        Long revokedByUserId,
        String revokeReason,
        String derivedSellerStatus,
        LocalDateTime canReapplyAt
) {}
```

Note: `applicant` is populated for admin-facing responses (so the queue can show the user without a second fetch); for the user's own `/me` endpoint, leave it `null`.

- [ ] **Step 5: AdminUserSummaryDTO**

```java
package hr.tvz.artdrop.artdropapp.dto;

public record AdminUserSummaryDTO(
        Long id,
        String username,
        String slug,
        String displayName,
        String email,
        String avatarUrl,
        String sellerStatus
) {}
```

- [ ] **Step 6: AdminUserDetailDTO**

```java
package hr.tvz.artdrop.artdropapp.dto;

import java.util.List;

public record AdminUserDetailDTO(
        AdminUserSummaryDTO user,
        List<SellerApplicationDTO> applicationHistory
) {}
```

- [ ] **Step 7: ListedArtworkCountDTO**

```java
package hr.tvz.artdrop.artdropapp.dto;

public record ListedArtworkCountDTO(long count) {}
```

- [ ] **Step 8: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SubmitSellerApplicationCommand.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SellerApplicationDecisionCommand.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/RevokeSellerCommand.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/SellerApplicationDTO.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/AdminUserSummaryDTO.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/AdminUserDetailDTO.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/ListedArtworkCountDTO.java
git commit -m "feat: DTOs and commands for seller application + admin

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Add `findListedByAuthorId` and `unlistAllForAuthor` on `ArtworkJpaRepository`

**Files:**
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/ArtworkJpaRepository.java`

- [ ] **Step 1: Read the existing file**

Read `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/ArtworkJpaRepository.java` to identify the imports block and the position to insert new methods.

- [ ] **Step 2: Add the imports if missing**

Ensure these imports are present at the top:

```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
```

- [ ] **Step 3: Add the two methods inside the interface**

Append these methods inside the `ArtworkJpaRepository` interface (before the closing `}`):

```java
    @Query("SELECT COUNT(a) FROM Artwork a WHERE a.author.id = :authorId AND (a.saleStatus IS NOT NULL OR a.price IS NOT NULL)")
    long countListedByAuthorId(Long authorId);

    @Modifying
    @Transactional
    @Query("UPDATE Artwork a SET a.saleStatus = NULL, a.price = NULL, a.updatedAt = CURRENT_TIMESTAMP WHERE a.author.id = :authorId AND (a.saleStatus IS NOT NULL OR a.price IS NOT NULL)")
    int unlistAllForAuthor(Long authorId);
```

- [ ] **Step 4: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/ArtworkJpaRepository.java
git commit -m "feat: artwork repo helpers for listed count + unlist

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: `SellerApplicationService` interface

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/SellerApplicationService.java`

- [ ] **Step 1: Create the interface**

```java
package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SellerApplicationService {

    enum SubmitError { ALREADY_SELLER, ALREADY_PENDING, COOLDOWN_ACTIVE, USER_NOT_FOUND }

    sealed interface SubmitResult permits SubmitOk, SubmitFailure {}
    record SubmitOk(SellerApplicationDTO application) implements SubmitResult {}
    record SubmitFailure(SubmitError error, LocalDateTime canReapplyAt) implements SubmitResult {}

    enum DecideError { NOT_FOUND, NOT_PENDING }
    sealed interface DecideResult permits DecideOk, DecideFailure {}
    record DecideOk(SellerApplicationDTO application) implements DecideResult {}
    record DecideFailure(DecideError error) implements DecideResult {}

    enum RevokeError { USER_NOT_FOUND, NOT_A_SELLER }
    sealed interface RevokeResult permits RevokeOk, RevokeFailure {}
    record RevokeOk(int unlistedCount) implements RevokeResult {}
    record RevokeFailure(RevokeError error) implements RevokeResult {}

    Optional<SellerApplicationDTO> findMyLatest(String username);

    SubmitResult submit(String username, SubmitSellerApplicationCommand command);

    Page<SellerApplicationDTO> listApplications(String statusFilter, int page, int size);

    Page<AdminUserSummaryDTO> searchUsers(String query, int page, int size);

    Optional<AdminUserDetailDTO> getUserDetail(Long userId);

    DecideResult approve(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command);

    DecideResult reject(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command);

    RevokeResult revoke(Long userId, String adminUsername, RevokeSellerCommand command);

    ListedArtworkCountDTO countListedArtworks(Long userId);
}
```

- [ ] **Step 2: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/SellerApplicationService.java
git commit -m "feat: SellerApplicationService interface

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: `SellerApplicationServiceImpl`

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/SellerApplicationServiceImpl.java`

- [ ] **Step 1: Create the implementation**

```java
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class SellerApplicationServiceImpl implements SellerApplicationService {

    private static final String ROLE_SELLER = "ROLE_SELLER";

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
    public Page<AdminUserSummaryDTO> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<User> rows;
        if (query == null || query.isBlank()) {
            rows = userRepository.findAll(pageable);
        } else {
            rows = userRepository.searchByUsernameDisplayNameOrEmail(query.trim(), pageable);
        }
        return rows.map(this::toAdminSummary);
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

    private boolean hasRole(User user, String role) {
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
        if (latest == null) return "NONE";
        return switch (latest.getStatus()) {
            case PENDING -> "PENDING";
            case REJECTED -> "REJECTED";
            case APPROVED -> {
                if (latest.getRevokedAt() != null) yield "REVOKED";
                if (user != null && hasRole(user, ROLE_SELLER)) yield "APPROVED";
                yield "REVOKED";
            }
        };
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
                deriveSellerStatusForUser(user)
        );
    }
}
```

- [ ] **Step 2: Add the search query method to `UserJpaRepository`**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/UserJpaRepository.java` and add inside the interface (before the closing `}`):

```java
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "ORDER BY u.username ASC"
    )
    org.springframework.data.domain.Page<User> searchByUsernameDisplayNameOrEmail(
            @org.springframework.data.repository.query.Param("q") String q,
            org.springframework.data.domain.Pageable pageable);
```

- [ ] **Step 3: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/SellerApplicationServiceImpl.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/repository/UserJpaRepository.java
git commit -m "feat: SellerApplicationServiceImpl + user search

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: User-facing controller (`/api/users/me/seller-application`)

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/SellerApplicationController.java`

- [ ] **Step 1: Create the controller**

```java
package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users/me/seller-application")
public class SellerApplicationController {

    private final SellerApplicationService service;

    public SellerApplicationController(SellerApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SellerApplicationDTO> getMine(Authentication authentication) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return service.findMyLatest(authentication.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitSellerApplicationCommand command
    ) {
        if (authentication == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SellerApplicationService.SubmitResult result = service.submit(authentication.getName(), command);
        return switch (result) {
            case SellerApplicationService.SubmitOk(SellerApplicationDTO app) ->
                    ResponseEntity.status(HttpStatus.CREATED).body(app);
            case SellerApplicationService.SubmitFailure(SellerApplicationService.SubmitError err, var canReapplyAt) ->
                    switch (err) {
                        case ALREADY_PENDING ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "ALREADY_PENDING"));
                        case ALREADY_SELLER ->
                                ResponseEntity.badRequest()
                                        .body(Map.of("error", "ALREADY_SELLER"));
                        case COOLDOWN_ACTIVE ->
                                ResponseEntity.badRequest()
                                        .body(Map.of("error", "COOLDOWN_ACTIVE", "canReapplyAt", canReapplyAt));
                        case USER_NOT_FOUND ->
                                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    };
        };
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/SellerApplicationController.java
git commit -m "feat: user-facing seller application endpoints

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Admin controller (`/api/admin/**`)

**Files:**
- Create: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/AdminController.java`

- [ ] **Step 1: Create the controller**

```java
package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SellerApplicationService service;

    public AdminController(SellerApplicationService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummaryDTO>> searchUsers(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.searchUsers(query, page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailDTO> getUserDetail(@PathVariable Long id) {
        return service.getUserDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{id}/listed-artwork-count")
    public ResponseEntity<ListedArtworkCountDTO> getListedArtworkCount(@PathVariable Long id) {
        return ResponseEntity.ok(service.countListedArtworks(id));
    }

    @GetMapping("/seller-applications")
    public ResponseEntity<Page<SellerApplicationDTO>> listApplications(
            @RequestParam(value = "status", defaultValue = "PENDING") String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listApplications(status, page, size));
    }

    @PostMapping("/seller-applications/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody(required = false) SellerApplicationDecisionCommand command
    ) {
        SellerApplicationService.DecideResult result =
                service.approve(id, authentication.getName(), command);
        return mapDecide(result);
    }

    @PostMapping("/seller-applications/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody SellerApplicationDecisionCommand command
    ) {
        SellerApplicationService.DecideResult result =
                service.reject(id, authentication.getName(), command);
        return mapDecide(result);
    }

    @PostMapping("/users/{id}/revoke-seller")
    public ResponseEntity<?> revoke(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody RevokeSellerCommand command
    ) {
        SellerApplicationService.RevokeResult result =
                service.revoke(id, authentication.getName(), command);
        return switch (result) {
            case SellerApplicationService.RevokeOk(int unlistedCount) ->
                    ResponseEntity.ok(Map.of("unlistedCount", unlistedCount));
            case SellerApplicationService.RevokeFailure(SellerApplicationService.RevokeError err) ->
                    switch (err) {
                        case USER_NOT_FOUND -> ResponseEntity.notFound().build();
                        case NOT_A_SELLER ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "NOT_A_SELLER"));
                    };
        };
    }

    private ResponseEntity<?> mapDecide(SellerApplicationService.DecideResult result) {
        return switch (result) {
            case SellerApplicationService.DecideOk(SellerApplicationDTO app) -> ResponseEntity.ok(app);
            case SellerApplicationService.DecideFailure(SellerApplicationService.DecideError err) ->
                    switch (err) {
                        case NOT_FOUND -> ResponseEntity.notFound().build();
                        case NOT_PENDING ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "NOT_PENDING"));
                    };
        };
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/AdminController.java
git commit -m "feat: admin controller for seller management

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Wire admin auth in `SecurityConfig`

**Files:**
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java`

- [ ] **Step 1: Add the admin matcher**

In the `authorizeHttpRequests` block (lines 34-44 of the current file), insert this line **before** the existing `requestMatchers(HttpMethod.GET, "/api/users/me", ...)` line:

```java
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
```

The block should now begin:

```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/signup", "/error", "/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/users/me/**").authenticated()
```

- [ ] **Step 2: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java
git commit -m "feat: gate /api/admin/** behind ROLE_ADMIN

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Expose `roles` and `sellerStatus` on `UserProfileDTO`

**Files:**
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/UserProfileDTO.java`
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/UserServiceImpl.java`

- [ ] **Step 1: Replace `UserProfileDTO`**

Replace the entire contents of `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/UserProfileDTO.java` with:

```java
package hr.tvz.artdrop.artdropapp.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileDTO(
        Long id,
        String username,
        String slug,
        String displayName,
        String bio,
        String avatarUrl,
        LocalDateTime createdAt,
        Integer artworkCount,
        Integer circleSize,
        Integer followingCount,
        boolean isSelf,
        List<String> roles,
        String sellerStatus
) {}
```

- [ ] **Step 2: Update `UserServiceImpl.toProfile`**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/UserServiceImpl.java`. Find the `toProfile(...)` method (currently at lines 121-138) and replace it with:

```java
    private UserProfileDTO toProfile(User user, boolean isSelf) {
        int artworkCount = (int) artworkRepository.countByAuthor_Id(user.getId());
        Integer circleSize = isSelf ? (int) followRepository.countByFolloweeId(user.getId()) : null;
        Integer followingCount = isSelf ? (int) followRepository.countByFollowerId(user.getId()) : null;
        java.util.List<String> roles = user.getAuthorities() == null
                ? java.util.List.of()
                : user.getAuthorities().stream().map(a -> a.getName()).sorted().toList();
        String sellerStatus = sellerApplicationService.deriveSellerStatusForUser(user);
        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getSlug(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                artworkCount,
                circleSize,
                followingCount,
                isSelf,
                roles,
                sellerStatus
        );
    }
```

- [ ] **Step 3: Inject `SellerApplicationServiceImpl` into `UserServiceImpl`**

Open `UserServiceImpl.java`. Find the constructor and the field declarations. Add a new field and constructor parameter for `SellerApplicationServiceImpl`. The field block (just before the constructor) should now include:

```java
    private final SellerApplicationServiceImpl sellerApplicationService;
```

The constructor signature should now include `SellerApplicationServiceImpl sellerApplicationService` as a parameter, and the constructor body should assign `this.sellerApplicationService = sellerApplicationService;` (alongside the existing assignments).

- [ ] **Step 4: Make `deriveSellerStatusForUser` package-private (already is)**

Confirm that `SellerApplicationServiceImpl.deriveSellerStatusForUser` is package-private (no `private`/`public`). It already is per Task 8.

- [ ] **Step 5: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/UserProfileDTO.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/UserServiceImpl.java
git commit -m "feat: expose roles + sellerStatus on UserProfileDTO

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 13: Sale-gate enforcement on artwork update path

**Files:**
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/ArtworkUpdateCommand.java`
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkServiceImpl.java`
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkService.java`
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/ArtworkController.java`

- [ ] **Step 1: Add `price`, `saleStatus`, and a sentinel for clearing them, to `ArtworkUpdateCommand`**

Replace the contents of `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/ArtworkUpdateCommand.java` with:

```java
package hr.tvz.artdrop.artdropapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ArtworkUpdateCommand(
        String title,
        String medium,
        @Size(max = 2000)
        String description,
        @Valid
        List<ArtworkImageCommand> images,
        BigDecimal width,
        BigDecimal height,
        BigDecimal depth,
        @Pattern(regexp = "CM|MM|IN", message = "dimensionUnit must be CM, MM or IN")
        String dimensionUnit,
        BigDecimal price,
        @Pattern(regexp = "ORIGINAL|EDITION|AVAILABLE|SOLD", message = "saleStatus must be ORIGINAL, EDITION, AVAILABLE or SOLD")
        String saleStatus,
        Boolean unlist
) {
}
```

- [ ] **Step 2: Add `SaleNotAuthorizedException` and a result enum to the service interface**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkService.java`. Inside the interface, add this nested enum and update `updateArtwork` to use it:

Replace the existing line:

```java
    Optional<ArtworkDTO> updateArtwork(Long id, ArtworkUpdateCommand command);
```

with:

```java
    enum UpdateOutcome { OK, NOT_FOUND, FORBIDDEN_SALE_GATE }

    record UpdateResult(UpdateOutcome outcome, ArtworkDTO artwork) {}

    UpdateResult updateArtwork(Long id, ArtworkUpdateCommand command, String editorUsername);
```

- [ ] **Step 3: Update `ArtworkServiceImpl.updateArtwork`**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkServiceImpl.java`. Replace the entire current `updateArtwork` method (lines 217-246 of the current file) with:

```java
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
```

- [ ] **Step 4: Add the `User` import to `ArtworkServiceImpl`**

In the imports of `ArtworkServiceImpl.java`, ensure this import is present (add it if missing):

```java
import hr.tvz.artdrop.artdropapp.model.User;
```

- [ ] **Step 5: Update `ArtworkController` to use the new return type**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/ArtworkController.java`. Find the existing `updateArtwork` handler (it currently calls `artworkService.updateArtwork(id, command)`). Replace its body with one that:
1. Reads `authentication.getName()` (or null).
2. Calls `service.updateArtwork(id, command, name)`.
3. Returns:
   - `200` with `result.artwork()` when `OK`
   - `404` when `NOT_FOUND`
   - `403` with `Map.of("error", "FORBIDDEN_SALE_GATE")` when `FORBIDDEN_SALE_GATE`

The handler should look like:

```java
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateArtwork(
            @PathVariable Long id,
            @Valid @RequestBody ArtworkUpdateCommand command,
            org.springframework.security.core.Authentication authentication
    ) {
        String name = authentication == null ? null : authentication.getName();
        ArtworkService.UpdateResult result = artworkService.updateArtwork(id, command, name);
        return switch (result.outcome()) {
            case OK -> ResponseEntity.ok(result.artwork());
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case FORBIDDEN_SALE_GATE -> ResponseEntity
                    .status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(java.util.Map.of("error", "FORBIDDEN_SALE_GATE"));
        };
    }
```

(If the existing handler signature differs — e.g., uses different annotation imports or method name — preserve those and only change the body and return type.)

- [ ] **Step 6: Verify compile**

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS. If existing call sites of `updateArtwork(id, command)` exist outside the controller, update them to pass `null` (or appropriate username) for the new third argument.

- [ ] **Step 7: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/dto/ArtworkUpdateCommand.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkService.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/ArtworkServiceImpl.java \
        ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/controller/ArtworkController.java
git commit -m "feat: gate price/saleStatus updates behind ROLE_SELLER

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: Backend integration tests

**Files:**
- Create: `ArtDrop/src/test/java/hr/tvz/artdrop/artdropapp/SellerFlowIntegrationTest.java`

- [ ] **Step 1: Create the integration test**

```java
package hr.tvz.artdrop.artdropapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleStatus;
import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.SellerApplicationJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SellerFlowIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private SellerApplicationJpaRepository applicationRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @PostConstruct
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitCreatesPendingApplication() throws Exception {
        applicationRepository.deleteAll();
        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("I want to sell because of my long-standing art career.");
        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void doublePendingReturnsConflict() throws Exception {
        applicationRepository.deleteAll();
        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("First application with enough characters to pass validation.");
        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_PENDING"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void cooldownBlocksReapplyAfterRecentRejection() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        SellerApplication recent = new SellerApplication();
        recent.setUserId(userId);
        recent.setMessage("Previously rejected message of sufficient length to pass validation.");
        recent.setStatus(SellerApplicationStatus.REJECTED);
        recent.setSubmittedAt(LocalDateTime.now().minusDays(1));
        recent.setDecidedAt(LocalDateTime.now().minusDays(1));
        applicationRepository.save(recent);

        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("Trying again before the cooldown elapses, please consider me.");
        MvcResult res = mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COOLDOWN_ACTIVE"))
                .andReturn();
        assertThat(res.getResponse().getContentAsString()).contains("canReapplyAt");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveGrantsRoleSeller() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        SellerApplication pending = new SellerApplication();
        pending.setUserId(userId);
        pending.setMessage("Pending application from user awaiting admin decision and review.");
        pending.setStatus(SellerApplicationStatus.PENDING);
        pending.setSubmittedAt(LocalDateTime.now());
        pending = applicationRepository.save(pending);

        mockMvc.perform(post("/api/admin/seller-applications/" + pending.getId() + "/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        var refreshed = userRepository.findByUsername("user").orElseThrow();
        assertThat(refreshed.getAuthorities()).anyMatch(a -> "ROLE_SELLER".equals(a.getName()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void revokeUnlistsArtworks() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();

        // Approve first to grant ROLE_SELLER and create the approved row to revoke.
        SellerApplication pending = new SellerApplication();
        pending.setUserId(userId);
        pending.setMessage("Pending application from user awaiting admin decision and review.");
        pending.setStatus(SellerApplicationStatus.PENDING);
        pending.setSubmittedAt(LocalDateTime.now());
        pending = applicationRepository.save(pending);
        mockMvc.perform(post("/api/admin/seller-applications/" + pending.getId() + "/approve")
                .contentType("application/json").content("{}"))
                .andExpect(status().isOk());

        // Ensure user has at least one listed artwork.
        Artwork a = artworkRepository.findAll().stream()
                .filter(art -> art.getAuthor() != null && userId.equals(art.getAuthor().getId()))
                .findFirst().orElseThrow();
        a.setPrice(new BigDecimal("100.00"));
        a.setSaleStatus(SaleStatus.AVAILABLE);
        artworkRepository.save(a);
        long listedBefore = artworkRepository.countListedByAuthorId(userId);
        assertThat(listedBefore).isGreaterThanOrEqualTo(1);

        RevokeSellerCommand cmd = new RevokeSellerCommand("Reason for revoking seller status in this test.");
        mockMvc.perform(post("/api/admin/users/" + userId + "/revoke-seller")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlistedCount").value((int) listedBefore));

        long listedAfter = artworkRepository.countListedByAuthorId(userId);
        assertThat(listedAfter).isZero();

        var refreshed = userRepository.findByUsername("user").orElseThrow();
        assertThat(refreshed.getAuthorities()).noneMatch(auth -> "ROLE_SELLER".equals(auth.getName()));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void nonSellerCannotSetPriceOnArtworkUpdate() throws Exception {
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        // Strip ROLE_SELLER if present from earlier test ordering.
        var u = userRepository.findByUsername("user").orElseThrow();
        u.getAuthorities().removeIf(auth -> "ROLE_SELLER".equals(auth.getName()));
        userRepository.save(u);

        Artwork a = artworkRepository.findAll().stream()
                .filter(art -> art.getAuthor() != null && userId.equals(art.getAuthor().getId()))
                .findFirst().orElseThrow();

        String body = "{\"price\":250.00,\"saleStatus\":\"AVAILABLE\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/artworks/" + a.getId())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_SALE_GATE"));
    }
}
```

- [ ] **Step 2: Add `spring-security-test` dependency if missing**

Open `ArtDrop/pom.xml` and verify a `spring-security-test` dependency exists in the test scope. If not, add it within the `<dependencies>` block:

```xml
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 3: Run the tests**

```bash
cd ArtDrop && ./mvnw -q test -Dtest=SellerFlowIntegrationTest
```

Expected: all six tests pass. If any fail, read the failure carefully — the most common issues are:
- The `@PreAuthorize` machinery isn't seeing `roles = {"ADMIN"}` because method security isn't enabled. The path-based matcher in Task 11 covers this, so admin tests should work without `@EnableMethodSecurity`.
- Test ordering side effects from shared H2 file. Add `@org.springframework.transaction.annotation.Transactional` at class level if needed for isolation.

- [ ] **Step 4: Commit**

```bash
git add ArtDrop/src/test/java/hr/tvz/artdrop/artdropapp/SellerFlowIntegrationTest.java ArtDrop/pom.xml
git commit -m "test: integration coverage for seller flow + sale gate

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 15: Frontend types, sellerApi, adminApi

**Files:**
- Create: `artdropapp-frontend/src/types/seller.ts`
- Create: `artdropapp-frontend/src/api/sellerApi.ts`
- Create: `artdropapp-frontend/src/api/adminApi.ts`

- [ ] **Step 1: Create `src/types/seller.ts`**

```ts
export type SellerStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED'

export interface SellerApplication {
  id: number
  userId: number
  applicant: AdminUserSummary | null
  message: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  submittedAt: string
  decidedAt: string | null
  decidedByUserId: number | null
  decisionReason: string | null
  revokedAt: string | null
  revokedByUserId: number | null
  revokeReason: string | null
  derivedSellerStatus: SellerStatus
  canReapplyAt: string | null
}

export interface AdminUserSummary {
  id: number
  username: string
  slug: string
  displayName: string
  email: string
  avatarUrl: string | null
  sellerStatus: SellerStatus
}

export interface AdminUserDetail {
  user: AdminUserSummary
  applicationHistory: SellerApplication[]
}

export interface PageResult<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
}
```

- [ ] **Step 2: Create `src/api/sellerApi.ts`**

```ts
import { authFetch } from '../lib/authFetch'
import type { SellerApplication } from '../types/seller'

function normalizeTimestamp(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value ?? '')
}

function mapApplication(raw: Record<string, unknown>): SellerApplication {
  const applicantRaw = raw.applicant as Record<string, unknown> | null | undefined
  const applicant = applicantRaw == null
    ? null
    : {
        id: Number(applicantRaw.id),
        username: String(applicantRaw.username ?? ''),
        slug: String(applicantRaw.slug ?? ''),
        displayName: String(applicantRaw.displayName ?? ''),
        email: String(applicantRaw.email ?? ''),
        avatarUrl: applicantRaw.avatarUrl == null ? null : String(applicantRaw.avatarUrl),
        sellerStatus: String(applicantRaw.sellerStatus ?? 'NONE') as SellerApplication['derivedSellerStatus'],
      }
  return {
    id: Number(raw.id),
    userId: Number(raw.userId),
    applicant,
    message: String(raw.message ?? ''),
    status: String(raw.status ?? 'PENDING') as SellerApplication['status'],
    submittedAt: normalizeTimestamp(raw.submittedAt),
    decidedAt: raw.decidedAt == null ? null : normalizeTimestamp(raw.decidedAt),
    decidedByUserId: raw.decidedByUserId == null ? null : Number(raw.decidedByUserId),
    decisionReason: raw.decisionReason == null ? null : String(raw.decisionReason),
    revokedAt: raw.revokedAt == null ? null : normalizeTimestamp(raw.revokedAt),
    revokedByUserId: raw.revokedByUserId == null ? null : Number(raw.revokedByUserId),
    revokeReason: raw.revokeReason == null ? null : String(raw.revokeReason),
    derivedSellerStatus: String(raw.derivedSellerStatus ?? 'NONE') as SellerApplication['derivedSellerStatus'],
    canReapplyAt: raw.canReapplyAt == null ? null : normalizeTimestamp(raw.canReapplyAt),
  }
}

export async function fetchMyApplication(): Promise<SellerApplication | null> {
  const res = await authFetch('/api/users/me/seller-application')
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Failed to load seller application (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export type SubmitApplicationError =
  | { kind: 'ALREADY_PENDING' }
  | { kind: 'ALREADY_SELLER' }
  | { kind: 'COOLDOWN_ACTIVE'; canReapplyAt: string }
  | { kind: 'OTHER'; message: string }

export async function submitApplication(message: string): Promise<SellerApplication> {
  const res = await authFetch('/api/users/me/seller-application', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  })
  if (res.status === 201) {
    return mapApplication((await res.json()) as Record<string, unknown>)
  }
  let body: Record<string, unknown> = {}
  try {
    body = (await res.json()) as Record<string, unknown>
  } catch {
    body = {}
  }
  const code = String(body.error ?? '')
  if (code === 'ALREADY_PENDING') throw { kind: 'ALREADY_PENDING' } satisfies SubmitApplicationError
  if (code === 'ALREADY_SELLER') throw { kind: 'ALREADY_SELLER' } satisfies SubmitApplicationError
  if (code === 'COOLDOWN_ACTIVE') {
    throw {
      kind: 'COOLDOWN_ACTIVE',
      canReapplyAt: String(body.canReapplyAt ?? ''),
    } satisfies SubmitApplicationError
  }
  throw { kind: 'OTHER', message: `Submit failed (${res.status})` } satisfies SubmitApplicationError
}

export { mapApplication }
```

- [ ] **Step 3: Create `src/api/adminApi.ts`**

```ts
import { authFetch } from '../lib/authFetch'
import type {
  AdminUserDetail,
  AdminUserSummary,
  PageResult,
  SellerApplication,
} from '../types/seller'
import { mapApplication } from './sellerApi'

function mapPage<TIn, TOut>(raw: Record<string, unknown>, mapItem: (r: Record<string, unknown>) => TOut): PageResult<TOut> {
  const content = Array.isArray(raw.content)
    ? (raw.content as Record<string, unknown>[]).map(mapItem)
    : []
  return {
    content,
    number: Number(raw.number ?? 0),
    size: Number(raw.size ?? content.length),
    totalElements: Number(raw.totalElements ?? content.length),
    totalPages: Number(raw.totalPages ?? 1),
  }
}

function mapAdminUserSummary(raw: Record<string, unknown>): AdminUserSummary {
  return {
    id: Number(raw.id),
    username: String(raw.username ?? ''),
    slug: String(raw.slug ?? ''),
    displayName: String(raw.displayName ?? ''),
    email: String(raw.email ?? ''),
    avatarUrl: raw.avatarUrl == null ? null : String(raw.avatarUrl),
    sellerStatus: String(raw.sellerStatus ?? 'NONE') as AdminUserSummary['sellerStatus'],
  }
}

export async function searchAdminUsers(
  query: string,
  page = 0,
  size = 20,
): Promise<PageResult<AdminUserSummary>> {
  const url = `/api/admin/users?query=${encodeURIComponent(query)}&page=${page}&size=${size}`
  const res = await authFetch(url)
  if (!res.ok) throw new Error(`Failed to load users (${res.status})`)
  return mapPage((await res.json()) as Record<string, unknown>, mapAdminUserSummary)
}

export async function fetchAdminUserDetail(userId: number): Promise<AdminUserDetail> {
  const res = await authFetch(`/api/admin/users/${userId}`)
  if (!res.ok) throw new Error(`Failed to load user (${res.status})`)
  const raw = (await res.json()) as Record<string, unknown>
  return {
    user: mapAdminUserSummary((raw.user ?? {}) as Record<string, unknown>),
    applicationHistory: Array.isArray(raw.applicationHistory)
      ? (raw.applicationHistory as Record<string, unknown>[]).map(mapApplication)
      : [],
  }
}

export async function fetchListedArtworkCount(userId: number): Promise<number> {
  const res = await authFetch(`/api/admin/users/${userId}/listed-artwork-count`)
  if (!res.ok) throw new Error(`Failed to load count (${res.status})`)
  const raw = (await res.json()) as { count?: unknown }
  return Number(raw.count ?? 0)
}

export async function listSellerApplications(
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL' = 'PENDING',
  page = 0,
  size = 20,
): Promise<PageResult<SellerApplication>> {
  const url = `/api/admin/seller-applications?status=${status}&page=${page}&size=${size}`
  const res = await authFetch(url)
  if (!res.ok) throw new Error(`Failed to load applications (${res.status})`)
  return mapPage((await res.json()) as Record<string, unknown>, mapApplication)
}

export async function approveApplication(id: number, reason?: string): Promise<SellerApplication> {
  const res = await authFetch(`/api/admin/seller-applications/${id}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason: reason ?? null }),
  })
  if (!res.ok) throw new Error(`Approve failed (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export async function rejectApplication(id: number, reason: string): Promise<SellerApplication> {
  const res = await authFetch(`/api/admin/seller-applications/${id}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!res.ok) throw new Error(`Reject failed (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export async function revokeSeller(userId: number, reason: string): Promise<{ unlistedCount: number }> {
  const res = await authFetch(`/api/admin/users/${userId}/revoke-seller`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!res.ok) throw new Error(`Revoke failed (${res.status})`)
  const raw = (await res.json()) as { unlistedCount?: unknown }
  return { unlistedCount: Number(raw.unlistedCount ?? 0) }
}
```

- [ ] **Step 4: Verify type-check**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds.

- [ ] **Step 5: Commit**

```bash
git add artdropapp-frontend/src/types/seller.ts \
        artdropapp-frontend/src/api/sellerApi.ts \
        artdropapp-frontend/src/api/adminApi.ts
git commit -m "feat: frontend types and API for seller + admin

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 16: Update `UserProfile` type and `useCurrentUser`; add `useMySellerApplication`

**Files:**
- Modify: `artdropapp-frontend/src/types/user.ts`
- Modify: `artdropapp-frontend/src/api/usersApi.ts`
- Create: `artdropapp-frontend/src/hooks/useMySellerApplication.ts`

- [ ] **Step 1: Update `UserProfile` type**

Replace `artdropapp-frontend/src/types/user.ts` with:

```ts
import type { SellerStatus } from './seller'

export interface UserProfile {
  id: number
  username: string
  slug: string
  displayName: string
  bio: string | null
  avatarUrl: string | null
  createdAt: string
  artworkCount: number
  circleSize: number | null
  followingCount: number | null
  isSelf: boolean
  roles: string[]
  sellerStatus: SellerStatus
}
```

- [ ] **Step 2: Update `mapUserProfile` in `usersApi.ts`**

In `artdropapp-frontend/src/api/usersApi.ts`, replace the existing `mapUserProfile` function with:

```ts
function mapUserProfile(raw: Record<string, unknown>): UserProfile {
  const rolesRaw = raw.roles
  const roles = Array.isArray(rolesRaw) ? rolesRaw.map((r) => String(r)) : []
  return {
    id: Number(raw.id),
    username: String(raw.username ?? ''),
    slug: String(raw.slug ?? ''),
    displayName: String(raw.displayName ?? ''),
    bio: raw.bio == null ? null : String(raw.bio),
    avatarUrl: raw.avatarUrl == null ? null : String(raw.avatarUrl),
    createdAt: normalizeCreatedAt(raw.createdAt),
    artworkCount: Number(raw.artworkCount ?? 0),
    circleSize: raw.circleSize == null ? null : Number(raw.circleSize),
    followingCount: raw.followingCount == null ? null : Number(raw.followingCount),
    isSelf: Boolean(raw.isSelf),
    roles,
    sellerStatus: (String(raw.sellerStatus ?? 'NONE')) as UserProfile['sellerStatus'],
  }
}
```

- [ ] **Step 3: Create `useMySellerApplication` hook**

Create `artdropapp-frontend/src/hooks/useMySellerApplication.ts`:

```ts
import { useCallback, useEffect, useState } from 'react'
import { fetchMyApplication } from '../api/sellerApi'
import { getToken } from '../lib/auth'
import type { SellerApplication } from '../types/seller'

type State = {
  application: SellerApplication | null
  loading: boolean
  error: string | null
}

export function useMySellerApplication() {
  const [state, setState] = useState<State>({ application: null, loading: false, error: null })

  const load = useCallback(async () => {
    if (!getToken()) {
      setState({ application: null, loading: false, error: null })
      return
    }
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const app = await fetchMyApplication()
      setState({ application: app, loading: false, error: null })
    } catch (e) {
      setState({
        application: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to load',
      })
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  return { ...state, refetch: load }
}
```

- [ ] **Step 4: Verify type-check**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds.

- [ ] **Step 5: Commit**

```bash
git add artdropapp-frontend/src/types/user.ts \
        artdropapp-frontend/src/api/usersApi.ts \
        artdropapp-frontend/src/hooks/useMySellerApplication.ts
git commit -m "feat: roles + sellerStatus on UserProfile + hook

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 17: Shared frontend components — `SellerStatusBadge`, `AdminRoute`, `SellerApplicationModal`

**Files:**
- Create: `artdropapp-frontend/src/components/SellerStatusBadge.tsx`
- Create: `artdropapp-frontend/src/components/AdminRoute.tsx`
- Create: `artdropapp-frontend/src/components/SellerApplicationModal.tsx`

- [ ] **Step 1: SellerStatusBadge**

```tsx
import type { SellerStatus } from '../types/seller'

const LABELS: Record<SellerStatus, string> = {
  NONE: 'Not a seller',
  PENDING: 'Pending',
  APPROVED: 'Verified seller',
  REJECTED: 'Rejected',
  REVOKED: 'Revoked',
}

const TONES: Record<SellerStatus, string> = {
  NONE: 'bg-surface-variant text-on-surface-variant',
  PENDING: 'bg-tertiary-container text-on-tertiary-container',
  APPROVED: 'bg-primary-container text-on-primary-container',
  REJECTED: 'bg-error-container text-on-error-container',
  REVOKED: 'bg-error-container text-on-error-container',
}

export function SellerStatusBadge({ status }: { status: SellerStatus }) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-full ${TONES[status]}`}
    >
      {LABELS[status]}
    </span>
  )
}
```

- [ ] **Step 2: AdminRoute**

```tsx
import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { getToken } from '../lib/auth'

export function AdminRoute({ children }: { children: ReactNode }) {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }
  const { user, loading } = useCurrentUser()
  if (loading && !user) {
    return (
      <p className="py-24 text-center text-on-surface-variant italic" role="status">
        Loading…
      </p>
    )
  }
  const isAdmin = (user?.roles ?? []).includes('ROLE_ADMIN')
  if (!isAdmin) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
```

- [ ] **Step 3: SellerApplicationModal**

```tsx
import { useState } from 'react'
import { submitApplication, type SubmitApplicationError } from '../api/sellerApi'

type Props = {
  open: boolean
  onClose: () => void
  onSubmitted: () => void
}

const MIN_LEN = 30
const MAX_LEN = 1000

export function SellerApplicationModal({ open, onClose, onSubmitted }: Props) {
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cooldownAt, setCooldownAt] = useState<string | null>(null)

  if (!open) return null

  const trimmed = message.trim()
  const valid = trimmed.length >= MIN_LEN && trimmed.length <= MAX_LEN

  async function handleSubmit() {
    if (!valid || submitting) return
    setSubmitting(true)
    setError(null)
    setCooldownAt(null)
    try {
      await submitApplication(trimmed)
      onSubmitted()
      setMessage('')
      onClose()
    } catch (raw) {
      const e = raw as SubmitApplicationError
      if (e.kind === 'COOLDOWN_ACTIVE') {
        setCooldownAt(e.canReapplyAt)
        setError('You cannot re-apply yet.')
      } else if (e.kind === 'ALREADY_PENDING') {
        setError('You already have a pending application.')
      } else if (e.kind === 'ALREADY_SELLER') {
        setError('You are already a verified seller.')
      } else {
        setError(e.message ?? 'Submission failed.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      role="dialog"
      aria-modal="true"
    >
      <div className="bg-surface text-on-surface w-full max-w-md mx-4 rounded-lg shadow-xl p-6">
        <h2 className="font-headline text-2xl mb-2">Apply to become a seller</h2>
        <p className="text-on-surface-variant text-sm mb-4">
          Tell us why you'd like to sell on ArtDrop. ({MIN_LEN}–{MAX_LEN} characters)
        </p>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          maxLength={MAX_LEN}
          rows={6}
          className="w-full border border-outline rounded-md p-2 bg-surface-variant text-on-surface"
          placeholder="Why do you want to sell on ArtDrop?"
        />
        <div className="text-xs text-on-surface-variant mt-1 text-right">
          {trimmed.length}/{MAX_LEN}
        </div>
        {error ? (
          <p className="mt-3 text-sm text-error" role="alert">
            {error}
            {cooldownAt ? ` You can re-apply on ${new Date(cooldownAt).toLocaleDateString()}.` : ''}
          </p>
        ) : null}
        <div className="flex justify-end gap-2 mt-4">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="px-4 py-2 rounded-md border border-outline text-on-surface"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!valid || submitting}
            className="px-4 py-2 rounded-md bg-on-surface text-surface font-semibold disabled:opacity-50"
          >
            {submitting ? 'Submitting…' : 'Submit application'}
          </button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Verify type-check**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds.

- [ ] **Step 5: Commit**

```bash
git add artdropapp-frontend/src/components/SellerStatusBadge.tsx \
        artdropapp-frontend/src/components/AdminRoute.tsx \
        artdropapp-frontend/src/components/SellerApplicationModal.tsx
git commit -m "feat: SellerStatusBadge + AdminRoute + SellerApplicationModal

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 18: AccountPage seller section + ProfilePage entry

**Files:**
- Create: `artdropapp-frontend/src/components/account/SellerSection.tsx`
- Modify: `artdropapp-frontend/src/pages/AccountPage.tsx`
- Modify: `artdropapp-frontend/src/pages/ProfilePage.tsx`

- [ ] **Step 1: Create `SellerSection`**

```tsx
import { useState } from 'react'
import { SellerApplicationModal } from '../SellerApplicationModal'
import { SellerStatusBadge } from '../SellerStatusBadge'
import { useMySellerApplication } from '../../hooks/useMySellerApplication'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

export function SellerSection() {
  const { application, loading, error, refetch } = useMySellerApplication()
  const [modalOpen, setModalOpen] = useState(false)

  const status = application?.derivedSellerStatus ?? 'NONE'
  const cooldownActive =
    application?.canReapplyAt != null && new Date(application.canReapplyAt) > new Date()

  function canApply(): boolean {
    if (status === 'NONE') return true
    if ((status === 'REJECTED' || status === 'REVOKED') && !cooldownActive) return true
    return false
  }

  return (
    <section className="pt-12 pb-8">
      <div className="flex items-center gap-3 mb-4">
        <h2 className="font-headline text-2xl text-on-surface">Seller status</h2>
        <SellerStatusBadge status={status} />
      </div>

      {loading ? (
        <p className="text-on-surface-variant italic" role="status">Loading…</p>
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : status === 'NONE' ? (
        <div>
          <p className="text-on-surface-variant mb-3">
            Become a verified seller to list your artworks for sale.
          </p>
          <button
            type="button"
            onClick={() => setModalOpen(true)}
            className="px-4 py-2 rounded-md bg-on-surface text-surface font-semibold"
          >
            Apply to become a seller
          </button>
        </div>
      ) : status === 'PENDING' ? (
        <div>
          <p className="text-on-surface-variant">
            Your application is under review. Submitted {formatDate(application!.submittedAt)}.
          </p>
          <p className="mt-3 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
            {application!.message}
          </p>
        </div>
      ) : status === 'APPROVED' ? (
        <p className="text-on-surface-variant">
          You're a verified seller (approved {formatDate(application!.decidedAt)}).
        </p>
      ) : (
        <div>
          <p className="text-on-surface-variant">
            {status === 'REVOKED'
              ? `Your seller status was revoked on ${formatDate(application!.revokedAt)}.`
              : `Your application was not approved on ${formatDate(application!.decidedAt)}.`}
          </p>
          {(status === 'REVOKED' ? application!.revokeReason : application!.decisionReason) ? (
            <p className="mt-2 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
              {status === 'REVOKED' ? application!.revokeReason : application!.decisionReason}
            </p>
          ) : null}
          {cooldownActive ? (
            <p className="mt-3 text-sm text-on-surface-variant">
              You can re-apply on {formatDate(application!.canReapplyAt)}.
            </p>
          ) : (
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="mt-3 px-4 py-2 rounded-md bg-on-surface text-surface font-semibold"
            >
              Apply again
            </button>
          )}
        </div>
      )}

      <SellerApplicationModal
        open={modalOpen && canApply()}
        onClose={() => setModalOpen(false)}
        onSubmitted={() => void refetch()}
      />
    </section>
  )
}
```

- [ ] **Step 2: Embed `SellerSection` in `AccountPage`**

Open `artdropapp-frontend/src/pages/AccountPage.tsx`. Add the import:

```tsx
import { SellerSection } from '../components/account/SellerSection'
```

Place `<SellerSection />` immediately after the existing `<ProfileHeader ... />` block and before the existing `<section className="pt-12">` ("Your drops") block. The render block becomes:

```tsx
      <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
        <ProfileHeader ... />

        <SellerSection />

        <section className="pt-12">
          <h2 className="font-headline text-2xl text-on-surface mb-8">Your drops</h2>
          ...
        </section>
      </main>
```

(Keep all existing `ProfileHeader` props as-is; only insert the new `<SellerSection />` line.)

- [ ] **Step 3: Add the entry point on the user's own ProfilePage**

Open `artdropapp-frontend/src/pages/ProfilePage.tsx`. Read the file to identify how `isSelf` is determined (likely from `user.isSelf` or comparison with current user). Add a "Become a seller" button rendered conditionally:

- Visible only when the profile is the viewer's own profile AND `user.sellerStatus` is one of `NONE`, `REJECTED`, `REVOKED` (cooldown handled inside the modal/account flow).
- On click, navigate to `/account` (where `SellerSection` lives) — this avoids duplicating the modal logic.

Add the navigation import if not already present:

```tsx
import { Link } from 'react-router-dom'
```

Add this block near the existing edit-profile area on the page (find the existing element rendered when the profile is the viewer's own — typically a button or link to `/account`) and insert:

```tsx
{user.isSelf && (user.sellerStatus === 'NONE' || user.sellerStatus === 'REJECTED' || user.sellerStatus === 'REVOKED') ? (
  <Link
    to="/account"
    className="inline-flex items-center px-3 py-1.5 text-sm rounded-md border border-outline text-on-surface hover:bg-surface-variant"
  >
    Become a seller
  </Link>
) : null}
```

- [ ] **Step 4: Verify type-check + render**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds. Then start the dev server (`npm run dev`) and confirm `/account` renders the seller section without errors. Also confirm the modal opens and closes (don't actually submit yet — backend connection comes in the smoke test).

- [ ] **Step 5: Commit**

```bash
git add artdropapp-frontend/src/components/account/SellerSection.tsx \
        artdropapp-frontend/src/pages/AccountPage.tsx \
        artdropapp-frontend/src/pages/ProfilePage.tsx
git commit -m "feat: account seller section + profile entry

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 19: ArtworkEditPage — price/saleStatus controls and gate

**Files:**
- Modify: `artdropapp-frontend/src/api/artworksApi.ts`
- Modify: `artdropapp-frontend/src/pages/ArtworkEditPage.tsx`

- [ ] **Step 1: Extend `updateArtwork` payload type to include `price`, `saleStatus`, `unlist`**

Open `artdropapp-frontend/src/api/artworksApi.ts`. Find the `updateArtwork` function and the type used for its payload. Update the payload type to include:

```ts
  price?: number | null
  saleStatus?: 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | null
  unlist?: boolean
```

If `updateArtwork` returns the artwork JSON directly, ensure it can also surface a 403 with body `{ error: "FORBIDDEN_SALE_GATE" }`. Update the function so that on a non-OK response with status 403, it parses the JSON body and throws `new Error('FORBIDDEN_SALE_GATE')`. For other failures, throw the existing error message as before.

- [ ] **Step 2: Replace `ArtworkEditPage` with the gated version**

Replace the contents of `artdropapp-frontend/src/pages/ArtworkEditPage.tsx` with:

```tsx
import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchArtworkById, updateArtwork } from '../api/artworksApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMySellerApplication } from '../hooks/useMySellerApplication'
import { SellerApplicationModal } from '../components/SellerApplicationModal'

type SaleStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | ''

export function ArtworkEditPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const { application, refetch: refetchApp } = useMySellerApplication()

  const [title, setTitle] = useState('')
  const [medium, setMedium] = useState('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [priceText, setPriceText] = useState('')
  const [saleStatus, setSaleStatus] = useState<SaleStatus>('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [applyOpen, setApplyOpen] = useState(false)

  const isSeller = (user?.roles ?? []).includes('ROLE_SELLER')
  const sellerStatus = application?.derivedSellerStatus ?? user?.sellerStatus ?? 'NONE'
  const cooldownActive =
    application?.canReapplyAt != null && new Date(application.canReapplyAt) > new Date()

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError('Invalid ID')
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchArtworkById(id)
      .then((data) => {
        if (cancelled) return
        setTitle(data.title)
        setMedium(data.medium)
        setDescription(data.description ?? '')
        setImageUrl(data.imageUrl)
        setPriceText(data.price == null ? '' : String(data.price))
        setSaleStatus((data.saleStatus ?? '') as SaleStatus)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof Error && err.message === 'NOT_FOUND') {
          setError('Artwork not found.')
        } else {
          setError(err instanceof Error ? err.message : 'Failed to load artwork')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!Number.isFinite(id)) return
    setSaving(true)
    setMessage(null)
    try {
      const trimmedUrl = imageUrl.trim()
      const priceNumber = priceText.trim() === '' ? null : Number.parseFloat(priceText)
      await updateArtwork(id, {
        title: title.trim(),
        medium: medium.trim(),
        description: description.trim(),
        images: trimmedUrl ? [{ imageUrl: trimmedUrl, sortOrder: 0, isCover: true }] : undefined,
        price: isSeller ? priceNumber : undefined,
        saleStatus: isSeller ? (saleStatus === '' ? null : saleStatus) : undefined,
      })
      setMessage('Saved.')
      navigate(`/details/${id}`)
    } catch (err) {
      if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        setMessage('Selling artworks requires verified seller status.')
      } else {
        setMessage(err instanceof Error ? err.message : 'Save failed')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="app-main">
      <h1>Edit artwork</h1>
      {loading ? <p role="status">Loading…</p> : null}
      {error ? <p className="artwork-form__message" role="alert">{error}</p> : null}
      {!loading && !error ? (
        <form className="artwork-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="artwork-form__field">
            Title
            <input value={title} onChange={(ev) => setTitle(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            Medium
            <input value={medium} onChange={(ev) => setMedium(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            Description
            <textarea
              value={description}
              onChange={(ev) => setDescription(ev.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
          <label className="artwork-form__field">
            Image URL (https://…)
            <input
              type="url"
              value={imageUrl}
              onChange={(ev) => setImageUrl(ev.target.value)}
              required
              placeholder="https://example.com/image.jpg"
            />
          </label>

          <fieldset className="artwork-form__field" disabled={!isSeller}>
            <legend>Listing</legend>
            {!isSeller ? (
              <p className="text-sm text-on-surface-variant mb-2">
                {sellerStatus === 'PENDING'
                  ? 'Your seller application is under review.'
                  : sellerStatus === 'NONE' || ((sellerStatus === 'REJECTED' || sellerStatus === 'REVOKED') && !cooldownActive)
                  ? <>Selling artworks requires verified seller status.{' '}
                      <button
                        type="button"
                        onClick={() => setApplyOpen(true)}
                        className="underline"
                      >
                        Apply now
                      </button>.
                    </>
                  : application?.canReapplyAt
                  ? `You can re-apply on ${new Date(application.canReapplyAt).toLocaleDateString()}.`
                  : 'Selling artworks requires verified seller status.'}
              </p>
            ) : null}
            <label>
              Price
              <input
                type="number"
                step="0.01"
                min="0"
                value={priceText}
                onChange={(ev) => setPriceText(ev.target.value)}
                placeholder="e.g. 250.00"
              />
            </label>
            <label>
              Sale status
              <select
                value={saleStatus}
                onChange={(ev) => setSaleStatus(ev.target.value as SaleStatus)}
              >
                <option value="">Not listed</option>
                <option value="AVAILABLE">Available</option>
                <option value="ORIGINAL">Original</option>
                <option value="EDITION">Edition</option>
                <option value="SOLD">Sold</option>
              </select>
            </label>
          </fieldset>

          <button
            type="submit"
            disabled={saving}
            className="w-full bg-on-surface text-surface font-semibold py-3 px-6 rounded-md hover:bg-on-surface/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
          {message ? <p className="artwork-form__message" role="alert">{message}</p> : null}
        </form>
      ) : null}

      <SellerApplicationModal
        open={applyOpen}
        onClose={() => setApplyOpen(false)}
        onSubmitted={() => void refetchApp()}
      />
    </main>
  )
}
```

(If `Artwork` type from `artworksApi.ts` does not include `price` and `saleStatus` properties, add them in the same file as `price: number | null` and `saleStatus: 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | null`. Update `mapApiArtwork` to populate them from the JSON.)

- [ ] **Step 3: Verify type-check + render**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds. Run `npm run dev` and confirm `/edit/<id>` renders, the listing fieldset is disabled when not a seller, and the "Apply now" button opens the modal.

- [ ] **Step 4: Commit**

```bash
git add artdropapp-frontend/src/api/artworksApi.ts \
        artdropapp-frontend/src/pages/ArtworkEditPage.tsx
git commit -m "feat: artwork edit price/saleStatus controls + seller gate

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 20: Admin layout, routes, AdminUsersPage

**Files:**
- Create: `artdropapp-frontend/src/pages/admin/AdminLayoutPage.tsx`
- Create: `artdropapp-frontend/src/pages/admin/AdminUsersPage.tsx`
- Create: `artdropapp-frontend/src/hooks/useAdminUsers.ts`
- Modify: `artdropapp-frontend/src/App.tsx`

- [ ] **Step 1: useAdminUsers hook**

```ts
import { useEffect, useState } from 'react'
import { searchAdminUsers } from '../api/adminApi'
import type { AdminUserSummary, PageResult } from '../types/seller'

export function useAdminUsers(query: string, page: number, size: number) {
  const [state, setState] = useState<{
    data: PageResult<AdminUserSummary> | null
    loading: boolean
    error: string | null
  }>({ data: null, loading: true, error: null })

  useEffect(() => {
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: null }))
    const debounce = setTimeout(() => {
      searchAdminUsers(query, page, size)
        .then((data) => {
          if (!cancelled) setState({ data, loading: false, error: null })
        })
        .catch((e: unknown) => {
          if (!cancelled) {
            setState({
              data: null,
              loading: false,
              error: e instanceof Error ? e.message : 'Failed to load',
            })
          }
        })
    }, 250)
    return () => {
      cancelled = true
      clearTimeout(debounce)
    }
  }, [query, page, size])

  return state
}
```

- [ ] **Step 2: AdminLayoutPage**

```tsx
import { NavLink, Outlet } from 'react-router-dom'

export function AdminLayoutPage() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-md font-medium ${isActive ? 'bg-primary-container text-on-primary-container' : 'text-on-surface-variant hover:bg-surface-variant'}`

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <h1 className="font-headline text-3xl text-on-surface mb-6">Admin</h1>
      <nav className="flex gap-2 border-b border-outline mb-8 pb-2">
        <NavLink to="/admin/users" end className={linkClass}>User directory</NavLink>
        <NavLink to="/admin/seller-requests" className={linkClass}>Seller requests</NavLink>
      </nav>
      <Outlet />
    </main>
  )
}
```

- [ ] **Step 3: AdminUsersPage**

```tsx
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminUsers } from '../../hooks/useAdminUsers'
import { SellerStatusBadge } from '../../components/SellerStatusBadge'

export function AdminUsersPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const size = 20
  const { data, loading, error } = useAdminUsers(query, page, size)

  return (
    <section>
      <input
        type="search"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value)
          setPage(0)
        }}
        placeholder="Search by username, name, or email…"
        className="w-full mb-6 px-3 py-2 rounded-md border border-outline bg-surface text-on-surface"
      />
      {loading ? (
        <p className="text-on-surface-variant italic" role="status">Loading…</p>
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : !data || data.content.length === 0 ? (
        <p className="text-on-surface-variant italic">No users found.</p>
      ) : (
        <ul className="divide-y divide-outline">
          {data.content.map((u) => (
            <li key={u.id}>
              <Link
                to={`/admin/users/${u.id}`}
                className="flex items-center gap-3 px-2 py-3 hover:bg-surface-variant rounded-md"
              >
                <img
                  src={u.avatarUrl ?? 'https://i.pravatar.cc/64'}
                  alt=""
                  className="w-10 h-10 rounded-full object-cover"
                />
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-on-surface">{u.displayName}</div>
                  <div className="text-sm text-on-surface-variant truncate">
                    @{u.username} · {u.email}
                  </div>
                </div>
                <SellerStatusBadge status={u.sellerStatus} />
              </Link>
            </li>
          ))}
        </ul>
      )}
      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between mt-6">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={data.number === 0}
            className="px-3 py-1.5 rounded-md border border-outline disabled:opacity-50"
          >
            Previous
          </button>
          <span className="text-sm text-on-surface-variant">
            Page {data.number + 1} of {data.totalPages}
          </span>
          <button
            type="button"
            onClick={() => setPage((p) => p + 1)}
            disabled={data.number + 1 >= data.totalPages}
            className="px-3 py-1.5 rounded-md border border-outline disabled:opacity-50"
          >
            Next
          </button>
        </div>
      ) : null}
    </section>
  )
}
```

- [ ] **Step 4: Wire admin routes in `App.tsx`**

Open `artdropapp-frontend/src/App.tsx`. Add imports:

```tsx
import { AdminRoute } from './components/AdminRoute'
import { AdminLayoutPage } from './pages/admin/AdminLayoutPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
```

Inside the existing `<Route element={<MainLayout />}>` block, add (before the closing `</Route>`):

```tsx
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminLayoutPage />
            </AdminRoute>
          }
        >
          <Route index element={<AdminUsersPage />} />
          <Route path="users" element={<AdminUsersPage />} />
        </Route>
```

- [ ] **Step 5: Verify type-check + render**

```bash
cd artdropapp-frontend && npm run build
```

Then run `npm run dev` and visit `/admin` while logged in as `admin`. Expected: user directory shows seeded users with correct seller status. Search for "julian" — list filters. Visiting `/admin` as a non-admin user redirects to `/`.

- [ ] **Step 6: Commit**

```bash
git add artdropapp-frontend/src/pages/admin/AdminLayoutPage.tsx \
        artdropapp-frontend/src/pages/admin/AdminUsersPage.tsx \
        artdropapp-frontend/src/hooks/useAdminUsers.ts \
        artdropapp-frontend/src/App.tsx
git commit -m "feat: admin layout + user directory page

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 21: Admin user detail + revoke modal

**Files:**
- Create: `artdropapp-frontend/src/hooks/useAdminUserDetail.ts`
- Create: `artdropapp-frontend/src/components/admin/RevokeSellerModal.tsx`
- Create: `artdropapp-frontend/src/pages/admin/AdminUserDetailPage.tsx`
- Modify: `artdropapp-frontend/src/App.tsx`

- [ ] **Step 1: useAdminUserDetail hook**

```ts
import { useCallback, useEffect, useState } from 'react'
import { fetchAdminUserDetail } from '../api/adminApi'
import type { AdminUserDetail } from '../types/seller'

export function useAdminUserDetail(userId: number | null) {
  const [data, setData] = useState<AdminUserDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (userId == null || !Number.isFinite(userId)) return
    setLoading(true)
    setError(null)
    try {
      const detail = await fetchAdminUserDetail(userId)
      setData(detail)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void load()
  }, [load])

  return { data, loading, error, refetch: load }
}
```

- [ ] **Step 2: RevokeSellerModal**

```tsx
import { useEffect, useState } from 'react'
import { fetchListedArtworkCount, revokeSeller } from '../../api/adminApi'

type Props = {
  open: boolean
  userId: number
  username: string
  onClose: () => void
  onRevoked: (unlistedCount: number) => void
}

export function RevokeSellerModal({ open, userId, username, onClose, onRevoked }: Props) {
  const [reason, setReason] = useState('')
  const [count, setCount] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    setCount(null)
    fetchListedArtworkCount(userId)
      .then((c) => { if (!cancelled) setCount(c) })
      .catch(() => { if (!cancelled) setCount(0) })
    return () => { cancelled = true }
  }, [open, userId])

  if (!open) return null

  async function handleSubmit() {
    if (reason.trim().length === 0 || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const res = await revokeSeller(userId, reason.trim())
      onRevoked(res.unlistedCount)
      setReason('')
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Revoke failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" role="dialog" aria-modal="true">
      <div className="bg-surface text-on-surface w-full max-w-md mx-4 rounded-lg shadow-xl p-6">
        <h2 className="font-headline text-xl mb-2">Revoke seller status</h2>
        <p className="text-sm text-on-surface-variant mb-3">
          You're about to revoke <strong>@{username}</strong>'s seller status.{' '}
          {count == null
            ? 'Loading listed artwork count…'
            : count === 0
            ? 'No artworks are currently listed.'
            : `This will unlist ${count} artwork${count === 1 ? '' : 's'}.`}
        </p>
        <label className="block">
          <span className="text-sm">Reason (required)</span>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            maxLength={500}
            className="w-full mt-1 border border-outline rounded-md p-2 bg-surface-variant text-on-surface"
          />
        </label>
        {error ? <p className="text-error mt-2 text-sm" role="alert">{error}</p> : null}
        <div className="flex justify-end gap-2 mt-4">
          <button type="button" onClick={onClose} disabled={submitting} className="px-4 py-2 rounded-md border border-outline">Cancel</button>
          <button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={reason.trim().length === 0 || submitting}
            className="px-4 py-2 rounded-md bg-error text-on-error font-semibold disabled:opacity-50"
          >
            {submitting ? 'Revoking…' : 'Revoke seller status'}
          </button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: AdminUserDetailPage**

```tsx
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAdminUserDetail } from '../../hooks/useAdminUserDetail'
import { SellerStatusBadge } from '../../components/SellerStatusBadge'
import { RevokeSellerModal } from '../../components/admin/RevokeSellerModal'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

export function AdminUserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const userId = id ? Number.parseInt(id, 10) : Number.NaN
  const { data, loading, error, refetch } = useAdminUserDetail(Number.isFinite(userId) ? userId : null)
  const [revokeOpen, setRevokeOpen] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  if (loading) {
    return <p className="text-on-surface-variant italic" role="status">Loading…</p>
  }
  if (error) {
    return <p className="text-error" role="alert">{error}</p>
  }
  if (!data) return null

  const u = data.user
  const isSeller = u.sellerStatus === 'APPROVED'

  return (
    <section>
      <Link to="/admin/users" className="text-sm text-on-surface-variant hover:underline">← Back to users</Link>
      <header className="flex items-center gap-4 mt-3 mb-6">
        <img src={u.avatarUrl ?? 'https://i.pravatar.cc/96'} alt="" className="w-16 h-16 rounded-full object-cover" />
        <div className="flex-1">
          <h2 className="font-headline text-2xl text-on-surface">{u.displayName}</h2>
          <p className="text-on-surface-variant">@{u.username} · {u.email}</p>
        </div>
        <SellerStatusBadge status={u.sellerStatus} />
      </header>

      {toast ? <p className="text-sm text-on-surface mb-4">{toast}</p> : null}

      {isSeller ? (
        <button
          type="button"
          onClick={() => setRevokeOpen(true)}
          className="px-4 py-2 rounded-md bg-error text-on-error font-semibold mb-8"
        >
          Revoke seller status
        </button>
      ) : null}

      <h3 className="font-headline text-xl text-on-surface mb-3">Seller history</h3>
      {data.applicationHistory.length === 0 ? (
        <p className="text-on-surface-variant italic">No applications yet.</p>
      ) : (
        <ul className="space-y-4">
          {data.applicationHistory.map((app) => (
            <li key={app.id} className="border border-outline rounded-md p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium">
                  {app.status} {app.revokedAt ? '· revoked' : ''}
                </span>
                <span className="text-sm text-on-surface-variant">
                  Submitted {formatDate(app.submittedAt)}
                </span>
              </div>
              <p className="text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
                {app.message}
              </p>
              {app.decidedAt ? (
                <p className="text-sm text-on-surface-variant mt-2">
                  Decided {formatDate(app.decidedAt)}
                  {app.decisionReason ? ` — ${app.decisionReason}` : ''}
                </p>
              ) : null}
              {app.revokedAt ? (
                <p className="text-sm text-error mt-2">
                  Revoked {formatDate(app.revokedAt)}
                  {app.revokeReason ? ` — ${app.revokeReason}` : ''}
                </p>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      <RevokeSellerModal
        open={revokeOpen}
        userId={u.id}
        username={u.username}
        onClose={() => setRevokeOpen(false)}
        onRevoked={(count) => {
          setToast(`Seller status revoked. ${count} artwork${count === 1 ? '' : 's'} unlisted.`)
          void refetch()
        }}
      />
    </section>
  )
}
```

- [ ] **Step 4: Wire `/admin/users/:id` route in `App.tsx`**

In the admin route block from Task 20, add:

```tsx
          <Route path="users/:id" element={<AdminUserDetailPage />} />
```

Add the import:

```tsx
import { AdminUserDetailPage } from './pages/admin/AdminUserDetailPage'
```

- [ ] **Step 5: Verify build + render**

```bash
cd artdropapp-frontend && npm run build
```

Run `npm run dev` and visit `/admin/users/<id>` (after clicking a user from the directory). Expected: detail renders, seller history shown if any, revoke button appears only when status is `APPROVED`.

- [ ] **Step 6: Commit**

```bash
git add artdropapp-frontend/src/hooks/useAdminUserDetail.ts \
        artdropapp-frontend/src/components/admin/RevokeSellerModal.tsx \
        artdropapp-frontend/src/pages/admin/AdminUserDetailPage.tsx \
        artdropapp-frontend/src/App.tsx
git commit -m "feat: admin user detail page + revoke modal

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 22: Admin seller requests page (queue + decision modals)

**Files:**
- Create: `artdropapp-frontend/src/hooks/useAdminSellerApplications.ts`
- Create: `artdropapp-frontend/src/components/admin/DecisionModal.tsx`
- Create: `artdropapp-frontend/src/pages/admin/AdminSellerRequestsPage.tsx`
- Modify: `artdropapp-frontend/src/App.tsx`

- [ ] **Step 1: useAdminSellerApplications hook**

```ts
import { useCallback, useEffect, useState } from 'react'
import { listSellerApplications } from '../api/adminApi'
import type { PageResult, SellerApplication } from '../types/seller'

export function useAdminSellerApplications(
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL',
  page: number,
  size: number,
) {
  const [data, setData] = useState<PageResult<SellerApplication> | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await listSellerApplications(status, page, size)
      setData(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }, [status, page, size])

  useEffect(() => {
    void load()
  }, [load])

  return { data, loading, error, refetch: load }
}
```

- [ ] **Step 2: DecisionModal (reusable for approve + reject)**

```tsx
import { useState } from 'react'

type Props = {
  open: boolean
  mode: 'approve' | 'reject'
  applicantUsername: string
  onClose: () => void
  onConfirm: (reason: string) => Promise<void>
}

export function DecisionModal({ open, mode, applicantUsername, onClose, onConfirm }: Props) {
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  const reasonRequired = mode === 'reject'
  const valid = !reasonRequired || reason.trim().length > 0

  async function handleSubmit() {
    if (!valid || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await onConfirm(reason.trim())
      setReason('')
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" role="dialog" aria-modal="true">
      <div className="bg-surface text-on-surface w-full max-w-md mx-4 rounded-lg shadow-xl p-6">
        <h2 className="font-headline text-xl mb-2">
          {mode === 'approve' ? 'Approve' : 'Reject'} seller application
        </h2>
        <p className="text-sm text-on-surface-variant mb-3">
          Applicant: <strong>@{applicantUsername}</strong>
        </p>
        <label className="block">
          <span className="text-sm">
            Reason {reasonRequired ? '(required)' : '(optional)'}
          </span>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            maxLength={500}
            className="w-full mt-1 border border-outline rounded-md p-2 bg-surface-variant text-on-surface"
          />
        </label>
        {error ? <p className="text-error mt-2 text-sm" role="alert">{error}</p> : null}
        <div className="flex justify-end gap-2 mt-4">
          <button type="button" onClick={onClose} disabled={submitting} className="px-4 py-2 rounded-md border border-outline">
            Cancel
          </button>
          <button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!valid || submitting}
            className={
              mode === 'approve'
                ? 'px-4 py-2 rounded-md bg-on-surface text-surface font-semibold disabled:opacity-50'
                : 'px-4 py-2 rounded-md bg-error text-on-error font-semibold disabled:opacity-50'
            }
          >
            {submitting
              ? mode === 'approve' ? 'Approving…' : 'Rejecting…'
              : mode === 'approve' ? 'Approve' : 'Reject'}
          </button>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: AdminSellerRequestsPage**

```tsx
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminSellerApplications } from '../../hooks/useAdminSellerApplications'
import { approveApplication, rejectApplication } from '../../api/adminApi'
import { DecisionModal } from '../../components/admin/DecisionModal'

type Filter = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

export function AdminSellerRequestsPage() {
  const [filter, setFilter] = useState<Filter>('PENDING')
  const [page, setPage] = useState(0)
  const size = 20
  const { data, loading, error, refetch } = useAdminSellerApplications(filter, page, size)
  const [decision, setDecision] = useState<{
    mode: 'approve' | 'reject'
    appId: number
    applicantUsername: string
  } | null>(null)
  const [toast, setToast] = useState<string | null>(null)

  const filters: Filter[] = ['PENDING', 'APPROVED', 'REJECTED', 'ALL']

  return (
    <section>
      <div className="flex flex-wrap gap-2 mb-6">
        {filters.map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => {
              setFilter(f)
              setPage(0)
            }}
            className={
              filter === f
                ? 'px-3 py-1.5 rounded-full bg-primary-container text-on-primary-container'
                : 'px-3 py-1.5 rounded-full border border-outline text-on-surface-variant hover:bg-surface-variant'
            }
          >
            {f.charAt(0) + f.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {toast ? <p className="text-sm text-on-surface mb-4">{toast}</p> : null}

      {loading ? (
        <p className="text-on-surface-variant italic" role="status">Loading…</p>
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : !data || data.content.length === 0 ? (
        <p className="text-on-surface-variant italic">No applications.</p>
      ) : (
        <ul className="space-y-4">
          {data.content.map((app) => (
            <li key={app.id} className="border border-outline rounded-md p-4">
              <div className="flex items-center justify-between mb-2 gap-3">
                <Link
                  to={`/admin/users/${app.userId}`}
                  className="flex items-center gap-2 font-medium text-on-surface hover:underline min-w-0"
                >
                  {app.applicant?.avatarUrl ? (
                    <img src={app.applicant.avatarUrl} alt="" className="w-8 h-8 rounded-full object-cover" />
                  ) : null}
                  <span className="truncate">
                    {app.applicant?.displayName ?? `User #${app.userId}`}
                    <span className="text-on-surface-variant ml-1">
                      @{app.applicant?.username ?? app.userId}
                    </span>
                  </span>
                </Link>
                <span className="text-sm text-on-surface-variant whitespace-nowrap">
                  {formatDate(app.submittedAt)} · {app.status}
                </span>
              </div>
              <p className="text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap mb-3">
                {app.message}
              </p>
              {app.status === 'PENDING' ? (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      setDecision({ mode: 'approve', appId: app.id, applicantUsername: app.applicant?.username ?? `user-${app.userId}` })
                    }
                    className="px-3 py-1.5 rounded-md bg-on-surface text-surface font-semibold"
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    onClick={() =>
                      setDecision({ mode: 'reject', appId: app.id, applicantUsername: app.applicant?.username ?? `user-${app.userId}` })
                    }
                    className="px-3 py-1.5 rounded-md bg-error text-on-error font-semibold"
                  >
                    Reject
                  </button>
                </div>
              ) : (
                <div className="text-sm text-on-surface-variant">
                  {app.decidedAt ? `Decided ${formatDate(app.decidedAt)}` : ''}
                  {app.decisionReason ? ` — ${app.decisionReason}` : ''}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between mt-6">
          <button type="button" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={data.number === 0} className="px-3 py-1.5 rounded-md border border-outline disabled:opacity-50">Previous</button>
          <span className="text-sm text-on-surface-variant">Page {data.number + 1} of {data.totalPages}</span>
          <button type="button" onClick={() => setPage((p) => p + 1)} disabled={data.number + 1 >= data.totalPages} className="px-3 py-1.5 rounded-md border border-outline disabled:opacity-50">Next</button>
        </div>
      ) : null}

      <DecisionModal
        open={decision != null}
        mode={decision?.mode ?? 'approve'}
        applicantUsername={decision?.applicantUsername ?? ''}
        onClose={() => setDecision(null)}
        onConfirm={async (reason) => {
          if (!decision) return
          if (decision.mode === 'approve') {
            await approveApplication(decision.appId, reason || undefined)
            setToast('Application approved.')
          } else {
            await rejectApplication(decision.appId, reason)
            setToast('Application rejected.')
          }
          await refetch()
        }}
      />
    </section>
  )
}
```

- [ ] **Step 4: Wire `/admin/seller-requests` route in `App.tsx`**

Add the import:

```tsx
import { AdminSellerRequestsPage } from './pages/admin/AdminSellerRequestsPage'
```

In the admin route block, add:

```tsx
          <Route path="seller-requests" element={<AdminSellerRequestsPage />} />
```

- [ ] **Step 5: Verify build + render**

```bash
cd artdropapp-frontend && npm run build
```

Run `npm run dev` and visit `/admin/seller-requests` as admin. Expected: filter chips work, pending queue empty initially, approve/reject modals render.

- [ ] **Step 6: Commit**

```bash
git add artdropapp-frontend/src/hooks/useAdminSellerApplications.ts \
        artdropapp-frontend/src/components/admin/DecisionModal.tsx \
        artdropapp-frontend/src/pages/admin/AdminSellerRequestsPage.tsx \
        artdropapp-frontend/src/App.tsx
git commit -m "feat: admin seller requests queue + decision modals

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Task 23: End-to-end manual smoke test

**Files:** none (verification only).

- [ ] **Step 1: Start backend and frontend**

In one terminal:

```bash
rm -f ArtDrop/data/artdropapp.mv.db ArtDrop/data/artdropapp.trace.db
cd ArtDrop && ./mvnw spring-boot:run
```

In another:

```bash
cd artdropapp-frontend && npm run dev
```

- [ ] **Step 2: Apply as a regular user**

1. Log in as `julian-vane` / the seeded password (check `data.sql` for the bcrypt hash; password is `password` for non-admin seeded users — confirm by attempting login).
2. Visit `/account`. Confirm seller section reads "Not a seller" with an Apply button.
3. Click Apply. Submit a 30+ character message. Modal closes; status flips to `Pending`.

- [ ] **Step 3: Approve from admin**

1. Log out, log in as `admin`.
2. Visit `/admin/seller-requests`. Confirm Julian's pending application is listed.
3. Click Approve, leave reason blank, confirm. Toast says "Application approved."
4. Visit `/admin/users/<julian's id>`. Status badge reads `Verified seller`. Revoke button visible.

- [ ] **Step 4: List a price as the new seller**

1. Log out, log in as Julian.
2. Visit `/edit/2` (Julian's seeded artwork). Listing fieldset is enabled. Set price to 1500 and saleStatus to AVAILABLE. Save.
3. Confirm save succeeds and redirects to detail page.

- [ ] **Step 5: Revoke as admin and verify unlist**

1. Log in as admin. Visit `/admin/users/<julian's id>`. Click Revoke. Modal shows "This will unlist 1 artwork." Enter a reason, confirm.
2. Visit Julian's profile or refetch — artwork's price/saleStatus should be cleared.

- [ ] **Step 6: Cooldown after rejection**

1. Log in as a fresh user (Sarah). Apply for seller status.
2. As admin, reject the application with a reason.
3. Log back in as Sarah. Visit `/account`. Confirm rejection summary shows the reason and "You can re-apply on …" with a date 14 days out.

- [ ] **Step 7: Sale-gate when no role**

1. As Sarah (still in cooldown, no `ROLE_SELLER`), visit `/edit/3`. Listing fieldset is disabled with the apply CTA.
2. Use the browser devtools network tab to manually `PATCH /api/artworks/3` with `{"price": 100, "saleStatus": "AVAILABLE"}`. Expected: `403` with `{"error":"FORBIDDEN_SALE_GATE"}`.

- [ ] **Step 8: Non-admin can't reach `/admin`**

1. As Sarah, navigate to `/admin`. Expected: redirected to `/`.

- [ ] **Step 9: Final commit (just a marker if anything was tweaked above)**

If any small fixes were needed during smoke test (typos, polish), commit them now. Otherwise skip.

```bash
git status
```

If clean, no commit needed. If small fixes were made:

```bash
git add -A
git commit -m "fix: smoke-test polish

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>"
```

---

## Done

All tasks complete. The plan delivers the spec end-to-end: schema + entity + role; user-facing apply flow with cooldown; admin directory + queue + revoke with artwork unlist; sale-gate enforcement at the API. Backend has integration coverage of the security-critical paths; frontend is verified manually.
