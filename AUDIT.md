# ArtDrop Audit — Punch List

Findings from the 2026-05-12 audit. Work top-to-bottom; check off as you go.

---

## 🚨 Fix first

### 1. `ArtDrop/.env` is tracked by git
- File: [ArtDrop/.env](ArtDrop/.env)
- It's in `.gitignore` but was already tracked in commit `3a35fc2` — gitignore doesn't untrack.
- **Only `JWT_BASE64_SECRET` was committed** (verified via `git show 3a35fc2:ArtDrop/.env`). The Stripe keys are in your working copy only — never pushed.
- Risk going forward: any future commit to this file would expose whatever's in it (including the Stripe keys currently sitting there).

- [ ] Rotate the JWT secret (generate any new base64 value; old tokens become invalid, which is fine)
- [ ] `git rm --cached ArtDrop/.env && git commit -m "stop tracking ArtDrop/.env"` — this makes future edits invisible to git
- [ ] Optional: scrub the JWT secret from history with `git filter-repo --path ArtDrop/.env --invert-paths` and force-push. If the repo is private / solo, rotating the JWT secret is enough — you don't have to scrub.
- [ ] Stripe keys: **no action needed**, they were never committed.

### 2. No global exception handler (backend)
- No `@ControllerAdvice` / `@RestControllerAdvice` anywhere — unhandled exceptions leak stack traces.
- [ ] Add `GlobalExceptionHandler` with `@RestControllerAdvice`
- [ ] Standard error response shape: `{ error, message, timestamp, path }`
- [ ] Handle: `MethodArgumentNotValidException` (400), `EntityNotFoundException` (404), `AccessDeniedException` (403), `BadCredentialsException` (401), `StripeException` (502), generic `Exception` (500, no stack trace in body)
- [ ] Refactor `AuthController` to throw instead of swallowing `BadCredentialsException`

### 3. EAGER fetch on `Artwork` — ✅ DONE
- Switched all three relations to `LAZY` in [Artwork.java](ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/model/Artwork.java).
- Added `@BatchSize(100)` on `images`, `tags`, `comments` — avoids `MultipleBagFetchException` while still killing N+1 (Hibernate batches lazy collection loads).
- Added `@EntityGraph(attributePaths = {"author"})` on list/feed repository methods.
- Added new `findDetailById` with `@EntityGraph({"author", "images"})` for the detail endpoint; `ArtworkServiceImpl.findById` now uses it.
- Verified: 354/354 tests pass.

### 4. Stripe call inside `@Transactional`
- `CheckoutServiceImpl.createSession()` is `@Transactional` and calls Stripe inside it. If Stripe fails after the order/reservation writes, you can get a half-committed state.
- [ ] Restructure: create order + reservation in a small `@Transactional` method → commit → call Stripe → persist `stripe_session_id` in a second short transaction
- [ ] Don't catch `StripeException` and rethrow as `RuntimeException` without rollback semantics; let it propagate or wrap with `@Transactional(rollbackFor = ...)` if you really need it inside the tx

---

## ⚠️ Important

### 5. JWT stored in `localStorage` (frontend)
- File: [artdropapp-frontend/src/lib/auth.ts](artdropapp-frontend/src/lib/auth.ts)
- XSS-readable. Acceptable for a portfolio project, but reviewers will notice.
- [ ] Either: switch to httpOnly cookie + CSRF token, OR
- [ ] Document the trade-off in README ("known: token in localStorage, would move to httpOnly cookie in production")

### 6. No rate limiting on auth endpoints
- File: [ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java](ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java) (line 35)
- [ ] Add Bucket4j dependency
- [ ] Filter or interceptor on `/api/auth/login` and `/api/auth/signup` — e.g., 5 attempts / minute / IP

### 7. No CI
- No `.github/workflows/` directory.
- [ ] Add `.github/workflows/ci.yml`:
  - Backend job: `./mvnw test` (Testcontainers needs Docker — use `services: postgres` or rely on GH runner's Docker)
  - Frontend job: `npm ci && npm run build && npm run lint`
- [ ] Add a status badge to README

### 8. No error boundary, no 404 page, no global toast (frontend)
- `sonner` is in `package.json` but unused.
- [ ] Wire `sonner` `<Toaster />` into the root layout
- [ ] Set `QueryClient` default `onError` to surface errors via toast
- [ ] Add root-level `<ErrorBoundary>` (or a route-level one in `App.tsx`)
- [ ] Add a `*` catch-all route → `NotFoundPage`

### 9. Inconsistent React Query keys
- `['search', 'artworks', query]` in [artdropapp-frontend/src/hooks/useSearchArtworks.ts](artdropapp-frontend/src/hooks/useSearchArtworks.ts)
- `['artworks', 'search', query]` in [artdropapp-frontend/src/components/search/SearchOverlay.tsx](artdropapp-frontend/src/components/search/SearchOverlay.tsx)
- [ ] Create `src/lib/queryKeys.ts` with constants (e.g., `qk.artworks.search(query)`)
- [ ] Replace all hardcoded keys
- [ ] Audit mutation `invalidateQueries` calls against new key constants

### 10. `DEBUG` logging on `org.springframework.web` in default profile
- File: [ArtDrop/src/main/resources/application.properties](ArtDrop/src/main/resources/application.properties) (line 6)
- [ ] Move to `INFO` in `application.properties`
- [ ] Add `application-dev.properties` with `logging.level.org.springframework.web=DEBUG` if needed locally

### 11. No `LICENSE`
- [ ] Add MIT or Apache-2.0 LICENSE file at repo root

### 12. Validation gaps on request DTOs
- Search param `q` on `ArtworkController` is unvalidated.
- Some command DTOs (`UpdateProfileCommand`, etc.) may lack `@NotBlank` / `@Size`.
- [ ] Sweep all `@RequestBody` DTOs — add `@Valid` on controller params, add constraints on the DTO fields
- [ ] Validate `@RequestParam` strings with `@Size` / `@Pattern` where they're used in queries

---

## ✅ Nice to have

### 13. OpenAPI / Swagger
- [ ] Add `springdoc-openapi-starter-webmvc-ui` to `pom.xml`
- [ ] Bonus: generate TS types for the frontend with `openapi-typescript` to kill DTO drift

### 14. Spring Boot Actuator
- [ ] Add `spring-boot-starter-actuator`
- [ ] Expose `/actuator/health`, `/actuator/info`; lock others behind ADMIN role

### 15. App Dockerfile
- [ ] Multi-stage Dockerfile for the Spring app
- [ ] Add `app` service to `docker-compose.yml` so `docker compose up` runs everything

### 16. Frontend tests
- [ ] One Vitest + RTL smoke test on the auth flow (login form submit, error display)
- [ ] Optional: a Playwright E2E hitting the dev server

### 17. Page titles / meta
- [ ] `react-helmet-async` or simple `useEffect(() => { document.title = ... })` hook per route

### 18. Pagination response shape
- List endpoints return `List<DTO>` — wrap in `{ content, page, size, totalPages, totalElements }` (Spring's `Page<T>` serializes to this natively)
- [ ] Update controllers + frontend hooks to use the new shape

### 19. Dead config: `/h2-console/**` in `SecurityConfig`
- File: [ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java](ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/SecurityConfig.java) (line 35)
- H2 isn't in `pom.xml`, so this is harmless but dead. Just remove the path.

### 20. Skeleton loaders on more pages
- Only `MasonryFeedSkeleton` exists. Detail pages, admin lists, checkout — all could use them.

### 21. CORS `allowedHeaders("*")`
- File: [ArtDrop/src/main/java/.../WebConfig.java](ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/config/WebConfig.java) (line 15)
- [ ] Tighten to explicit list: `Authorization`, `Content-Type`

### 22. Admin form validation
- [artdropapp-frontend/src/pages/admin/AdminChallengeFormPage.tsx](artdropapp-frontend/src/pages/admin/AdminChallengeFormPage.tsx) only checks `title.trim()`. Auth forms use Zod + react-hook-form — adopt the same pattern here.

### 23. Prettier config
- ESLint is configured; no Prettier. Add `.prettierrc` for consistency.

---

## Notes / overruled claims

- The audit suggested `.env.example` was missing — it isn't, it exists at the repo root.
- The audit flagged `/h2-console/**` permitAll as a security risk — it isn't, since H2 is not in `pom.xml` (no console to expose). Demoted to "dead config, clean up."
- CSRF is disabled in `SecurityConfig` — fine for a stateless JWT API, no action needed.
