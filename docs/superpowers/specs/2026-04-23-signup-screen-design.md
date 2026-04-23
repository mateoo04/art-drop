# Signup Screen — Design Spec

**Date:** 2026-04-23
**Scope:** Mobile-first signup screen for the ArtDrop frontend, plus the first version of a small UI primitives layer (`Button`, `Input`, `FormField`), plus a targeted backend change to auto-dedupe auto-generated usernames.
**Out of scope (deferred to Lab 8 per `project.md`):** Login page (a stub placeholder ships with this work), `AuthContext`, Axios interceptors, protected routes, RBAC UI, account menu in `AppHeader`, OAuth (Google / Apple) — explicitly dropped per user request.

---

## 1. Goals

1. Ship a mobile-first signup screen matching the Stitch mockup (`stitch-downloads/11157244706319733351/6319003e876d4496bc9a8af34d2e55ab/screenshot.png`), omitting the Google / Apple OAuth buttons and the "Or sign up with" divider.
2. Introduce a small, reusable UI primitives layer (`Button`, `Input`, `FormField`) that the signup page consumes — establishing the pattern big-corp design systems use so that future screens (login, profile edit, artwork create) don't duplicate field markup.
3. Wire the form end-to-end: React Hook Form + Zod → `POST /api/auth/signup` → store JWT in `localStorage` → redirect to `/`.
4. Make the "Already have an account? Sign In" link functional via a one-screen `/login` placeholder page that the real login screen will replace in the follow-up task.

## 2. Non-goals

- No `AuthContext` provider — JWT is stored directly via a tiny helper in `lib/auth.ts`. `AuthContext` is Lab 8.
- No protected-route wrapper. No Axios interceptor. Fetch API is sufficient for one endpoint.
- No password-strength meter, no visibility toggle, no "remember me", no terms-of-service checkbox. These are bolt-ons; none are in the mockup.
- No tests this pass. Project has no testing framework set up yet; Lab 9 is the dedicated testing lab. Manual browser verification instead.

## 3. UX — Fields and validation

The form has **five** fields (UI-visible). These map to the backend `RegisterRequest` DTO as follows:

| UI field           | Backend field   | Source                                             |
|--------------------|-----------------|----------------------------------------------------|
| First Name         | (part of `displayName`) | user input                                 |
| Last Name          | (part of `displayName`) | user input                                 |
| Email Address      | `email`         | user input                                         |
| Password           | `password`      | user input                                         |
| Confirm Password   | (client-only)   | user input — matched against Password              |
| *(not shown in UI)*| `username`      | auto-derived from email local part (see §5.2)      |
| *(not shown in UI)*| `displayName`   | `` `${firstName.trim()} ${lastName.trim()}` ``     |

### Validation (client, via Zod)

- `firstName`: required, 1–50 chars, trimmed
- `lastName`: required, 1–50 chars, trimmed
- `email`: required, valid email format
- `password`: required, min 8 chars (matches backend `@Size(min = 8)`)
- `confirmPassword`: required, must equal `password`

### Validation behavior

- `useForm({ mode: 'onTouched' })` — errors appear when a field loses focus, and re-validate on change once the field has an error (so typing a fix clears the error immediately).
- Submit button disabled + `loading` state while the request is in flight. All input fields are `disabled` during submit.

### Server errors (form-level)

- **201 Created** → store JWT from `JwtResponse.token` into `localStorage` under key `artdrop_token`, `navigate('/')`.
- **409 Conflict** → form-level error "An account with this email already exists. Sign in instead?" with an inline link to `/login`.
- **400 Bad Request** → form-level error "Please check your details and try again."
- **Network / 500** → form-level error "Something went wrong. Please try again."

Form-level errors are rendered in a `role="alert"` container above the submit button.

## 4. Architecture — File layout

All paths relative to `artdropapp-frontend/src/`.

**New files:**

```
components/
├── ui/
│   ├── Button.tsx         Primary / secondary / ghost variants; supports loading; forwards ref.
│   ├── Input.tsx          Thin styled <input> with `invalid` prop for error border.
│   └── FormField.tsx      Label + children (input slot) + optional error text, wired with aria-describedby.
└── layout/
    ├── AuthHeader.tsx     Minimal centered "ArtDrop" wordmark. Used by auth routes only.
    └── MainLayout.tsx     Renders <AppHeader /><Outlet /><AppFooter />. Wraps the main app routes.
pages/
├── SignupPage.tsx         The signup screen.
└── LoginPage.tsx          Placeholder stub: "Login coming soon" (real page ships in follow-up task).
api/
└── authApi.ts             `signup(request): Promise<JwtResponse>` — fetch wrapper around /api/auth/signup.
lib/
└── auth.ts                `storeToken`, `getToken`, `clearToken`; `deriveUsernameFromEmail`.
```

**Modified files:**

- `App.tsx` — refactored to nested-layout routing (§5.1).
- `index.html` — no change required; Newsreader + Inter + Material Symbols are already loaded.
- `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/AuthServiceImpl.java` — username auto-dedupe (§6).

## 5. Frontend data flow

### 5.1 Routing — nested layout

```tsx
// App.tsx
<Routes>
  <Route path="/signup" element={<SignupPage />} />
  <Route path="/login" element={<LoginPage />} />
  <Route element={<MainLayout />}>
    <Route path="/" element={<HomePage />} />
    <Route path="/circle" element={<CirclePage />} />
    <Route path="/challenges" element={<ChallengesPage />} />
    <Route path="/artworks" element={<ArtworksPage />} />
    <Route path="/collections" element={<CollectionsPage />} />
    <Route path="/details/:id" element={<ArtworkDetailPage />} />
    <Route path="/edit/:id" element={<ArtworkEditPage />} />
  </Route>
</Routes>
```

`MainLayout` simply composes the existing `AppHeader` + `<Outlet />` + `AppFooter`. Signup / login render their own `AuthHeader` inside the page component (no shared auth layout yet — the second auth screen doesn't exist yet; YAGNI).

### 5.2 Username derivation

Client-side, in `lib/auth.ts`:

```ts
export function deriveUsernameFromEmail(email: string): string {
  const local = email.split('@')[0] ?? ''
  const cleaned = local.toLowerCase().replace(/[^a-z0-9]/g, '')
  return cleaned.length > 0 ? cleaned : 'user'
}
```

The server is responsible for resolving collisions — the client sends the naive derivation and trusts the server. See §6.

### 5.3 Submit flow

1. RHF `handleSubmit` fires with validated `{ firstName, lastName, email, password, confirmPassword }`.
2. Build `RegisterRequest`:
   ```ts
   {
     username: deriveUsernameFromEmail(email),
     email,
     password,
     displayName: `${firstName.trim()} ${lastName.trim()}`,
   }
   ```
3. `await authApi.signup(request)`.
4. On success: `storeToken(response.token)`, `navigate('/')`.
5. On error: map HTTP status to the form-level message from §3. Never throw past the form — the page always recovers to a valid UI state.

## 6. Backend change — username auto-dedupe

`AuthServiceImpl.signup()` currently throws `IllegalArgumentException("Username or email already exists")` if **either** the username or email is taken. This has a usability problem for our auto-derivation flow: two different users (`elena@curator.com`, `elena@artist.com`) both derive `elena` and would collide even though their emails are distinct.

**Change:**

1. If `existsByEmail(registerRequest.email())` → throw `IllegalArgumentException` (translated by `AuthController` to `409 Conflict`). Email is the user's primary identifier; collision here is a real error surfaced to the user.
2. If `existsByUsername(registerRequest.username())` → do **not** throw. Instead, call a new private method `generateUniqueUsername(String base)` that mirrors the existing `generateUniqueSlug` pattern:

```java
private String generateUniqueUsername(String base) {
    if (!userJpaRepository.existsByUsername(base)) {
        return base;
    }
    int suffix = 2;
    while (userJpaRepository.existsByUsername(base + suffix)) {
        suffix++;
    }
    return base + suffix;
}
```

3. The resolved unique username is used both for `user.setUsername(...)` and as the input to the existing `generateUniqueSlug(...)`.

This is a small, targeted change (~15 lines). It does not change any public DTO or controller contract. The JWT login call at the end of `signup()` continues to work because it uses the resolved (unique) username.

## 7. Styling and design tokens

Tailwind 4 is already configured with the semantic token set used by the gallery screen (`bg-surface`, `text-on-surface`, `surface-container-lowest`, `on-surface-variant`, `outline-variant`, `font-headline`, `font-body`, etc.). The signup screen uses these same tokens directly — no new token definitions.

**Key aesthetic rules matching the Stitch design:**

- **Sharp corners globally** — `rounded-none` on buttons, inputs, cards. No pill or rounded corners anywhere on this screen.
- **Ghost borders** — `border border-outline-variant/15` on inputs; border transitions to `border-on-surface` on focus.
- **Uppercase micro-labels** — `text-[10px] uppercase tracking-[0.15em]` for field labels; `text-[11px] uppercase tracking-[0.2em]` for the submit button.
- **Headline in Newsreader serif** — "Begin your collection." uses `font-headline font-light text-4xl tracking-tight`.
- **Body text in Inter** — everything else.
- **Grayscale hero image** — `grayscale opacity-90 transition-all hover:grayscale-0 h-48 object-cover`.

## 8. Responsive behavior

Mobile-first: base styles target small screens, `md:` and `lg:` breakpoints add enhancements.

| Breakpoint | Behavior                                                                              |
|------------|---------------------------------------------------------------------------------------|
| Base       | Form container `max-w-md mx-auto px-8 py-4`; hero `h-48`. Matches Stitch exactly.     |
| `md:` ≥768 | `py-12`; hero `h-64`. More vertical breathing room; form width unchanged.             |
| `lg:` ≥1024| Same centered layout, generous spacing. No split-screen / multi-column invented.      |

The container uses `min-h-screen flex flex-col` so the page fills the viewport on empty-content devices.

## 9. Accessibility

- Every `<input>` is associated with a `<label>` via `FormField`'s `htmlFor` prop.
- Error messages rendered in a `<p id="{id}-error" role="alert">` element; the corresponding input sets `aria-describedby="{id}-error"` and `aria-invalid={!!error}`.
- Form-level error container also uses `role="alert"` so it's announced to screen readers when it appears after submit.
- Submit button has a visible focus ring (`focus-visible:ring-2 focus-visible:ring-on-surface`).
- Semantic landmarks: `<header>` for `AuthHeader`, `<main>` for the page content, `<footer>` for the "Already have an account" link.

## 10. Dependencies

New npm packages added to `artdropapp-frontend/package.json`:

- `react-hook-form` — form state management.
- `zod` — schema validation.
- `@hookform/resolvers` — bridge between RHF and Zod (`zodResolver`).

All three are tiny, zero-dependency, and de facto industry standard. No other dependencies needed.

## 11. Verification plan (manual, in browser)

When implementation is complete I will, with the dev server running:

1. Happy path: fill valid details → submit → verify 201 response, token written to `localStorage`, redirect to `/`.
2. Validation: each field empty → error on blur; invalid email format → error on blur; password <8 chars → error; password / confirm mismatch → error. Type a fix → error clears.
3. Duplicate email: sign up with a seeded email from `data.sql` → verify 409 → form-level error shown with `/login` link.
4. Username collision: sign up with `elena@curator.com`, then `elena@artist.com` → verify both succeed, second user's username is `elena2` (check H2 console at `/h2-console`).
5. Network failure: stop the backend → submit → verify generic network error message, form stays usable.
6. Responsive: verify layout at mobile (≤480px), tablet (768px), desktop (≥1024px).
7. Keyboard: tab through the form → verify focus ring visible on every field and the submit button.
8. Screen-reader spot-check: trigger an error → verify VoiceOver / announces the error (macOS: Cmd+F5).

I will report what I actually exercised vs. what I claim works. Unverified claims of "works" are not allowed — if I can't test a case (e.g. no screen reader available), I will say so explicitly.

## 12. Open questions

None. All decisions locked in during brainstorm.
