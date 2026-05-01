# Admin Panel & Seller Status Management — Design

**Date:** 2026-05-01
**Status:** Approved (pending implementation plan)

## Summary

Introduce a seller-application workflow and an admin panel. Users apply for seller status with a written message; admins approve or reject from a dedicated queue. Approval grants `ROLE_SELLER`, which is required to list artworks for sale. Admins can also revoke seller status, which auto-unlists the user's currently-listed artworks (sets `sale_status` and `price` to null). The admin panel additionally surfaces a searchable user directory.

## Goals

- Gate the existing `price` / `saleStatus` fields on `Artwork` behind a verified seller role.
- Let users request seller status with a free-text message and re-apply after rejection (14-day cooldown).
- Give admins a focused queue to review pending applications, plus a user directory for general management.
- Lay schema groundwork for future in-app banner / email notifications without requiring them in v1.

## Non-goals (v1)

- In-app or email notifications when an application is decided. Pull-only: users see status when they visit their account.
- Voluntary self step-down from seller status (admin-only revoke).
- Admin role management (granting/revoking `ROLE_ADMIN`).
- Bulk actions in the admin panel.
- A general audit-log table. Revokes are recorded as columns on the latest approved application.

## Data model

### New entity: `SellerApplication`

| Column | Type | Notes |
|---|---|---|
| `id` | bigint, PK | identity |
| `user_id` | bigint, FK → `app_user.id` | not null |
| `message` | varchar(1000) | not null, ≥ 30 chars |
| `status` | varchar(20) | enum: `PENDING`, `APPROVED`, `REJECTED` |
| `submitted_at` | timestamp | not null |
| `decided_at` | timestamp | nullable |
| `decided_by_user_id` | bigint, FK → `app_user.id` | nullable |
| `decision_reason` | varchar(500) | nullable |
| `revoked_at` | timestamp | nullable; only set on the most recent `APPROVED` row when revoke happens |
| `revoked_by_user_id` | bigint, FK → `app_user.id` | nullable |
| `revoke_reason` | varchar(500) | nullable |

**Indexes:**
- `(user_id, submitted_at DESC)` — user history lookup.
- `(status, submitted_at)` — admin queue.

**Invariant:** at most one `PENDING` row per user. Enforced at service level (check before insert) and via partial unique index where the DB supports it.

### New authority: `ROLE_SELLER`

Seeded alongside `ROLE_ADMIN` and `ROLE_USER` in `data.sql`. Granted on approve; removed on revoke.

### Derived `sellerStatus` (per user)

Computed from latest application row + presence of `ROLE_SELLER`:

| Latest application | Has `ROLE_SELLER` | Derived status |
|---|---|---|
| (none) | — | `NONE` |
| `PENDING` | — | `PENDING` |
| `APPROVED` (no `revoked_at`) | yes | `APPROVED` |
| `APPROVED` (has `revoked_at`) | no | `REVOKED` |
| `REJECTED` | no | `REJECTED` (cooldown driven by `decided_at`) |

### Side effects

- **On approve:** add `ROLE_SELLER` to user; set `decided_at`, `decided_by_user_id`, optional `decision_reason`.
- **On reject:** set `decided_at`, `decided_by_user_id`, required `decision_reason`. Do not modify roles.
- **On revoke:** remove `ROLE_SELLER`; set `revoked_at`, `revoked_by_user_id`, required `revoke_reason` on the latest approved row; in the same transaction, for every `Artwork` owned by the user where `sale_status IS NOT NULL` OR `price IS NOT NULL`, set both `sale_status` and `price` to `NULL` ("unlist"). The existing `SaleStatus` enum (`ORIGINAL | EDITION | AVAILABLE | SOLD`) is unchanged; "not for sale" is represented by null columns, matching current code.

### Cooldown

14 days between a `REJECTED` decision and the next allowed application, configurable via property `app.seller.reapply-cooldown-days` (default 14). Same cooldown applies after a revoke (counted from `revoked_at`).

### Future-proofing for notifications

A future `seen_at` (or `dismissed_at`) column on `seller_application` is the natural place to hang a "show banner once" flag when in-app notifications are added. No work in v1.

## Backend API

### User-facing (require `authenticated()`)

- `GET /api/users/me/seller-application` — latest application for the current user. Returns `200` with body, or `404` if none. Body includes the derived `sellerStatus` and, when relevant, `canReapplyAt`.
- `POST /api/users/me/seller-application` — body `{ message: string }`. Creates `PENDING`. Errors:
  - `409` if user already has `PENDING`.
  - `400` with `canReapplyAt` if within cooldown after a `REJECTED` or `REVOKED` decision.
  - `400` if user already has `ROLE_SELLER`.
  - `400` if message fails validation (length 30–1000).

### Admin (require `hasRole('ADMIN')`)

- `GET /api/admin/users?query=&page=&size=` — paginated, case-insensitive search across `username`, `display_name`, `email`. Returns user summary + derived `sellerStatus`.
- `GET /api/admin/users/{id}` — user detail with full application history (newest first).
- `GET /api/admin/seller-applications?status=&page=&size=` — paginated queue. Default filter `PENDING`. Includes embedded user summary.
- `POST /api/admin/seller-applications/{id}/approve` — body `{ reason?: string }`. `409` if not `PENDING`.
- `POST /api/admin/seller-applications/{id}/reject` — body `{ reason: string }` (required). `409` if not `PENDING`.
- `POST /api/admin/users/{id}/revoke-seller` — body `{ reason: string }`. `409` if user lacks `ROLE_SELLER`. Returns count of artworks flipped.

### Security config changes

- Add `requestMatchers("/api/admin/**").hasRole("ADMIN")` to `SecurityConfig`.
- Enable method-level security (`@EnableMethodSecurity`) so we can use `@PreAuthorize("hasRole('SELLER')")` on the artwork sale-gate logic.

### Sale-gate enforcement (hard)

In `ArtworkService` create/update paths, when the incoming payload has a non-null `saleStatus` or a non-null `price`, require the current user to have `ROLE_SELLER` — else respond `403`. Non-sellers can still create/update artworks, just with both `price` and `saleStatus` left null. Existing artwork rows are not migrated; this only blocks future writes that flip an artwork into a for-sale state.

## Frontend — user-facing

### `AccountPage.tsx` — "Become a seller" section

Renders by `sellerStatus`:

- **NONE** — copy + "Apply to become a seller" → opens modal.
- **PENDING** — "Application under review" + submitted date + read-only message preview.
- **APPROVED** — "You're a verified seller" badge + approval date.
- **REJECTED, within cooldown** — rejection summary + `decision_reason` + "You can re-apply on YYYY-MM-DD".
- **REJECTED, cooldown elapsed** — same summary + "Apply again" button.
- **REVOKED** — revoke summary + `revoke_reason`; same cooldown rules counted from `revoked_at`.

### `SellerApplicationModal.tsx` (new)

- Textarea labeled "Why do you want to sell on ArtDrop?", char counter, max 1000, min 30.
- Submit posts to `POST /api/users/me/seller-application`. On success: close, refetch, toast. On cooldown error: show `canReapplyAt` inline.

### `ProfilePage.tsx` (own profile)

Small "Become a seller" entry near the edit-profile area. Hidden unless status is `NONE` or `REJECTED` past cooldown or `REVOKED` past cooldown. Opens the same modal.

### `ArtworkEditPage.tsx` — contextual gate

- Disable `price` / `saleStatus` controls if the current user lacks `ROLE_SELLER`.
- Show inline notice above the disabled controls:
  - status `NONE` / past-cooldown `REJECTED`/`REVOKED` → "Selling artworks requires verified seller status. [Apply now]" (opens modal).
  - status `PENDING` → "Your seller application is under review."
  - status `REJECTED`/`REVOKED` within cooldown → "You can re-apply on YYYY-MM-DD."

### New types and modules

- `src/types/seller.ts` — `SellerApplication`, `SellerStatus` enum.
- `src/api/sellerApi.ts` — `getMyApplication`, `submitApplication`.
- `src/hooks/useMySellerApplication.ts` — exposes `status`, `application`, `canReapplyAt`.

## Frontend — admin panel

### Routing and access

- New route `/admin` (nested children) guarded by an `AdminRoute` wrapper (mirrors `ProtectedRoute.tsx`) that checks for `ROLE_ADMIN` on `useCurrentUser()`. Non-admins redirect to `/`.
- The current `me` endpoint must expose authorities (e.g., `roles: string[]` on `UserProfileDTO`). If it doesn't already, add it.

### Layout — `AdminLayoutPage.tsx`

Sidebar / top tabs:
1. **User directory** (`/admin/users`)
2. **Seller requests** (`/admin/seller-requests`)

### `AdminUsersPage.tsx`

- Debounced search input → `/api/admin/users?query=`.
- Paginated table: avatar, username, displayName, email, `SellerStatusBadge`.
- Row click → `/admin/users/:id`.

### `AdminUserDetailPage.tsx`

- Header: avatar, identity, `SellerStatusBadge`.
- "Seller history" section: timeline of all applications, newest first — message, decision, reason, dates.
- Action bar:
  - If user has `ROLE_SELLER` → "Revoke seller status" button → revoke modal. Reason required. Modal also shows count of currently-listed artworks (`sale_status IS NOT NULL` OR `price IS NOT NULL`) that will be unlisted (fetched on open).
  - If user has `PENDING` application → "Review application" link to the queue (or opens decision modal here).

### `AdminSellerRequestsPage.tsx`

- Filter chips: `PENDING` (default) / `APPROVED` / `REJECTED` / `All`.
- For `PENDING` rows: card per application — user summary (avatar, username, displayName), full message, submitted date, inline **Approve** and **Reject** buttons.
  - Approve → small confirm with optional reason.
  - Reject → modal with required reason.
- For `APPROVED` / `REJECTED` rows: read-only history.

### Design choices baked in

- Approve uses a confirm step (with optional reason) rather than one-click, to catch misclicks.
- Revoke modal warns "This will unlist N artworks." Count is fetched when the modal opens.
- No bulk actions in v1.
- Admin role management is out of scope.

### New files

- `src/api/adminApi.ts`
- `src/hooks/useAdminUsers.ts`, `useAdminSellerApplications.ts`, `useAdminUserDetail.ts`
- `src/components/AdminRoute.tsx`
- `src/components/SellerStatusBadge.tsx`
- Pages above under `src/pages/admin/`.

## Testing

- **Backend:** integration tests for each new endpoint covering happy path + the documented error cases (cooldown, double-pending, non-pending decision, revoke-without-role). Include a test that revoke unlists listed artworks (nulls `sale_status` and `price`) in a single transaction.
- **Frontend:** component tests for `SellerApplicationModal` validation/cooldown error rendering, `AdminRoute` redirect for non-admins, `AdminSellerRequestsPage` approve/reject flows.

## Open considerations (out of scope for v1)

- Email or in-app banner notifications on decision (schema accommodates a `seen_at` field later).
- Voluntary self step-down.
- Admin audit log beyond the per-application columns.
- Re-listing existing artworks after revoke (currently auto-unlisted by nulling `sale_status` and `price`; users can manually re-list once they regain seller status).
