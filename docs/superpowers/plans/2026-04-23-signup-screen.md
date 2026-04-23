# Signup Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a mobile-first signup screen matching the Stitch mockup, backed by a small reusable UI primitives layer (`Button`, `Input`, `FormField`), with server-side username auto-dedupe.

**Architecture:** Nested-layout routing in React Router 7 (`/signup` and `/login` render outside the main app shell); React Hook Form + Zod for validation with on-touched blur-then-reactive behavior; fetch-based API wrapper writing JWT to `localStorage` on success; one targeted backend change (`AuthServiceImpl.signup()`) to auto-suffix colliding usernames while still 409-ing on email collision.

**Tech Stack:** React 19 + Vite 8 + TypeScript + Tailwind CSS 4 + React Router 7 (frontend already present); `react-hook-form` + `zod` + `@hookform/resolvers` (new); Spring Boot + JPA + H2 (backend already present).

**Source spec:** [`docs/superpowers/specs/2026-04-23-signup-screen-design.md`](../specs/2026-04-23-signup-screen-design.md)

**Note on tests:** The spec explicitly defers automated tests to Lab 9. Each task ends with a concrete manual verification step (compile check, browser render, or API call) and a commit. No step may be marked complete without the verification passing.

---

## Task 1: Install form-library dependencies

**Files:**
- Modify: `artdropapp-frontend/package.json` (via npm)
- Modify: `artdropapp-frontend/package-lock.json` (via npm)

- [ ] **Step 1: Install the three new deps**

Run from the repo root:

```bash
cd artdropapp-frontend && npm install react-hook-form zod @hookform/resolvers
```

Expected: three packages added, no peer-dependency warnings blocking install.

- [ ] **Step 2: Verify versions in package.json**

Read `artdropapp-frontend/package.json`. The `dependencies` block must now contain `react-hook-form`, `zod`, `@hookform/resolvers`. Note the exact installed versions for the commit message.

- [ ] **Step 3: Verify the app still type-checks**

Run from the repo root:

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds, no errors. Abort if the build fails — we need a clean baseline before editing source.

- [ ] **Step 4: Commit**

```bash
cd /Users/mateobubalo/Documents/GitHub/artdropapp
git add artdropapp-frontend/package.json artdropapp-frontend/package-lock.json
git commit -m "feat: add react-hook-form, zod, @hookform/resolvers"
```

---

## Task 2: Backend — username auto-dedupe in AuthServiceImpl

**Files:**
- Modify: `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/AuthServiceImpl.java`

Current behavior (`AuthServiceImpl.java:52-56`): `signup()` throws `IllegalArgumentException` if either username OR email exists. New behavior: throw only on email collision; auto-suffix username collisions.

- [ ] **Step 1: Replace the signup() method body**

Open `ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/AuthServiceImpl.java`.

Replace the entire `signup(...)` method (lines 51–78) with:

```java
    @Override
    public JwtResponse signup(RegisterRequest registerRequest) {
        if (userJpaRepository.existsByEmail(registerRequest.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Authority roleUser = authorityJpaRepository.findByName("ROLE_USER")
                .orElseGet(() -> authorityJpaRepository.save(new Authority(null, "ROLE_USER")));

        String uniqueUsername = generateUniqueUsername(registerRequest.username());
        String slug = generateUniqueSlug(uniqueUsername);
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setUsername(uniqueUsername);
        user.setEmail(registerRequest.email());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setDisplayName(registerRequest.displayName());
        user.setSlug(slug);
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setAuthorities(Set.of(roleUser));
        userJpaRepository.save(user);

        return login(new LoginRequest(uniqueUsername, registerRequest.password()));
    }
```

- [ ] **Step 2: Add the generateUniqueUsername helper**

Immediately above the existing `generateUniqueSlug` method (around line 80), add:

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

- [ ] **Step 3: Verify the backend compiles**

Run from the repo root:

```bash
cd ArtDrop && ./mvnw -q compile
```

Expected: BUILD SUCCESS. If it fails, read the error and fix (common issue: missing import).

- [ ] **Step 4: Run the backend and verify a manual signup**

Terminal A (repo root):

```bash
cd ArtDrop && ./mvnw spring-boot:run
```

Wait for `Started ArtdropappApplication`. Then in Terminal B:

```bash
# Happy path — new user
curl -sS -w '\nHTTP %{http_code}\n' -X POST http://localhost:8089/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser1","email":"testuser1@example.com","password":"password123","displayName":"Test One"}'
```
Expected: HTTP 201 with a JSON body like `{"token":"eyJ..."}`.

```bash
# Email collision — same email as a seeded user
curl -sS -w '\nHTTP %{http_code}\n' -X POST http://localhost:8089/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"brandnew","email":"admin@artdrop.local","password":"password123","displayName":"X"}'
```
Expected: HTTP 409, empty body.

```bash
# Username collision — username 'admin' already seeded, but fresh email
curl -sS -w '\nHTTP %{http_code}\n' -X POST http://localhost:8089/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","email":"admin2@example.com","password":"password123","displayName":"New Admin"}'
```
Expected: HTTP 201. The created user's username will be `admin2` (verify in the H2 console at `http://localhost:8089/h2-console` if desired; JDBC URL `jdbc:h2:mem:artdropdb`).

Stop the backend with Ctrl+C in Terminal A before proceeding.

- [ ] **Step 5: Commit**

```bash
git add ArtDrop/src/main/java/hr/tvz/artdrop/artdropapp/service/AuthServiceImpl.java
git commit -m "feat(backend): auto-dedupe username on signup, 409 only on email collision"
```

---

## Task 3: Layout shell — AuthHeader, MainLayout, stub pages, App.tsx refactor

**Files:**
- Create: `artdropapp-frontend/src/components/layout/AuthHeader.tsx`
- Create: `artdropapp-frontend/src/components/layout/MainLayout.tsx`
- Create: `artdropapp-frontend/src/pages/SignupPage.tsx` (stub)
- Create: `artdropapp-frontend/src/pages/LoginPage.tsx` (stub)
- Modify: `artdropapp-frontend/src/App.tsx`

- [ ] **Step 1: Create AuthHeader**

Write `artdropapp-frontend/src/components/layout/AuthHeader.tsx`:

```tsx
export function AuthHeader() {
  return (
    <header className="w-full h-24 flex items-center justify-center bg-surface px-6">
      <h1 className="font-headline text-3xl font-bold tracking-tight text-on-surface">
        ArtDrop
      </h1>
    </header>
  )
}
```

- [ ] **Step 2: Create MainLayout**

Write `artdropapp-frontend/src/components/layout/MainLayout.tsx`:

```tsx
import { Outlet } from 'react-router-dom'
import { AppFooter } from './AppFooter'
import { AppHeader } from './AppHeader'

export function MainLayout() {
  return (
    <>
      <AppHeader />
      <Outlet />
      <AppFooter />
    </>
  )
}
```

- [ ] **Step 3: Create SignupPage stub**

Write `artdropapp-frontend/src/pages/SignupPage.tsx`:

```tsx
import { AuthHeader } from '../components/layout/AuthHeader'

export function SignupPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 flex flex-col flex-grow">
        <p className="font-body text-sm text-on-surface-variant">Signup coming in the next task.</p>
      </main>
    </div>
  )
}
```

- [ ] **Step 4: Create LoginPage stub**

Write `artdropapp-frontend/src/pages/LoginPage.tsx`:

```tsx
import { AuthHeader } from '../components/layout/AuthHeader'

export function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-12 flex flex-col flex-grow">
        <h2 className="font-headline text-4xl font-light tracking-tight mb-2">Log in</h2>
        <p className="font-body text-sm text-on-surface-variant">Login coming soon.</p>
      </main>
    </div>
  )
}
```

- [ ] **Step 5: Refactor App.tsx to nested routes**

Replace the entire contents of `artdropapp-frontend/src/App.tsx` with:

```tsx
import { Route, Routes } from 'react-router-dom'
import { MainLayout } from './components/layout/MainLayout'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworkEditPage } from './pages/ArtworkEditPage'
import { ArtworksPage } from './pages/ArtworksPage'
import { ChallengesPage } from './pages/ChallengesPage'
import { CirclePage } from './pages/CirclePage'
import { CollectionsPage } from './pages/CollectionsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import './App.css'

function App() {
  return (
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
  )
}

export default App
```

Note: `AppHeader` and `AppFooter` are no longer imported here — `MainLayout` owns them.

- [ ] **Step 6: Verify compile + render**

Start the frontend dev server (backend not required for this check):

```bash
cd artdropapp-frontend && npm run dev
```

In a browser:
- Visit `http://localhost:5173/` — expected: `AppHeader` (Discover/Circle/Challenges nav) + home content + `AppFooter`. No regressions.
- Visit `http://localhost:5173/signup` — expected: only the centered "ArtDrop" wordmark + "Signup coming in the next task." No main nav, no footer.
- Visit `http://localhost:5173/login` — expected: only the centered "ArtDrop" wordmark + "Log in" headline + "Login coming soon."
- Visit `http://localhost:5173/circle` — expected: still renders inside MainLayout.

Stop the dev server when done.

- [ ] **Step 7: Commit**

```bash
git add artdropapp-frontend/src/components/layout/AuthHeader.tsx \
        artdropapp-frontend/src/components/layout/MainLayout.tsx \
        artdropapp-frontend/src/pages/SignupPage.tsx \
        artdropapp-frontend/src/pages/LoginPage.tsx \
        artdropapp-frontend/src/App.tsx
git commit -m "feat(frontend): nested layout routes, auth pages outside main shell"
```

---

## Task 4: Auth helpers (`lib/auth.ts`) and API wrapper (`api/authApi.ts`)

**Files:**
- Create: `artdropapp-frontend/src/lib/auth.ts`
- Create: `artdropapp-frontend/src/api/authApi.ts`

- [ ] **Step 1: Create lib/auth.ts**

Write `artdropapp-frontend/src/lib/auth.ts`:

```ts
const TOKEN_KEY = 'artdrop_token'

export function storeToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function deriveUsernameFromEmail(email: string): string {
  const local = email.split('@')[0] ?? ''
  const cleaned = local.toLowerCase().replace(/[^a-z0-9]/g, '')
  return cleaned.length > 0 ? cleaned : 'user'
}
```

- [ ] **Step 2: Create api/authApi.ts**

Write `artdropapp-frontend/src/api/authApi.ts`:

```ts
import { API_BASE } from '../config'

export type RegisterRequest = {
  username: string
  email: string
  password: string
  displayName: string
}

export type JwtResponse = {
  token: string
}

export type SignupError =
  | { kind: 'email_taken' }
  | { kind: 'invalid' }
  | { kind: 'network' }

export async function signup(request: RegisterRequest): Promise<JwtResponse> {
  let res: Response
  try {
    res = await fetch(`${API_BASE}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
  } catch {
    const err: SignupError = { kind: 'network' }
    throw err
  }

  if (res.status === 201) {
    return (await res.json()) as JwtResponse
  }
  if (res.status === 409) {
    const err: SignupError = { kind: 'email_taken' }
    throw err
  }
  if (res.status === 400) {
    const err: SignupError = { kind: 'invalid' }
    throw err
  }
  const err: SignupError = { kind: 'network' }
  throw err
}

export function isSignupError(value: unknown): value is SignupError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'kind' in value &&
    typeof (value as { kind: unknown }).kind === 'string'
  )
}
```

- [ ] **Step 3: Verify both compile**

```bash
cd artdropapp-frontend && npm run build
```

Expected: `tsc -b` passes, `vite build` succeeds. The build won't exercise these yet (no consumers), but type errors would surface here.

- [ ] **Step 4: Commit**

```bash
git add artdropapp-frontend/src/lib/auth.ts artdropapp-frontend/src/api/authApi.ts
git commit -m "feat(frontend): auth helpers and signup API wrapper"
```

---

## Task 5: UI primitive — Button

**Files:**
- Create: `artdropapp-frontend/src/components/ui/Button.tsx`

- [ ] **Step 1: Create Button**

Write `artdropapp-frontend/src/components/ui/Button.tsx`:

```tsx
import { forwardRef, type ComponentPropsWithoutRef, type ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost'

type ButtonProps = ComponentPropsWithoutRef<'button'> & {
  variant?: Variant
  loading?: boolean
  fullWidth?: boolean
  children: ReactNode
}

const VARIANT_CLASSES: Record<Variant, string> = {
  primary:
    'bg-on-surface text-surface hover:opacity-90 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
  secondary:
    'bg-surface-container-lowest text-on-surface border border-outline-variant/15 hover:bg-surface-container-low active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
  ghost:
    'bg-transparent text-on-surface hover:text-outline disabled:opacity-50 disabled:cursor-not-allowed',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', loading = false, fullWidth = false, disabled, className, children, type = 'button', ...rest },
  ref,
) {
  const widthClass = fullWidth ? 'w-full' : ''
  const base =
    'inline-flex items-center justify-center py-5 px-6 font-label text-[11px] uppercase tracking-[0.2em] font-semibold transition-all duration-200 rounded-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-on-surface focus-visible:ring-offset-2 focus-visible:ring-offset-surface'

  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      aria-busy={loading}
      className={[base, VARIANT_CLASSES[variant], widthClass, className ?? ''].filter(Boolean).join(' ')}
      {...rest}
    >
      {loading ? 'Please wait…' : children}
    </button>
  )
})
```

- [ ] **Step 2: Verify build**

```bash
cd artdropapp-frontend && npm run build
```

Expected: passes. No consumers yet.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/components/ui/Button.tsx
git commit -m "feat(ui): Button primitive with primary/secondary/ghost variants"
```

---

## Task 6: UI primitive — Input

**Files:**
- Create: `artdropapp-frontend/src/components/ui/Input.tsx`

- [ ] **Step 1: Create Input**

Write `artdropapp-frontend/src/components/ui/Input.tsx`:

```tsx
import { forwardRef, type ComponentPropsWithoutRef } from 'react'

type InputProps = ComponentPropsWithoutRef<'input'> & {
  invalid?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { invalid = false, className, ...rest },
  ref,
) {
  const base =
    'w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed'
  const borderClass = invalid
    ? 'border border-error focus:border-error'
    : 'border border-outline-variant/15 focus:border-on-surface'

  return (
    <input
      ref={ref}
      aria-invalid={invalid || undefined}
      className={[base, borderClass, className ?? ''].filter(Boolean).join(' ')}
      {...rest}
    />
  )
})
```

- [ ] **Step 2: Verify build**

```bash
cd artdropapp-frontend && npm run build
```

Expected: passes.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/components/ui/Input.tsx
git commit -m "feat(ui): Input primitive with ghost border and invalid state"
```

---

## Task 7: UI primitive — FormField

**Files:**
- Create: `artdropapp-frontend/src/components/ui/FormField.tsx`

- [ ] **Step 1: Create FormField**

Write `artdropapp-frontend/src/components/ui/FormField.tsx`:

```tsx
import { cloneElement, isValidElement, type ReactElement, type ReactNode } from 'react'

type FormFieldProps = {
  label: string
  htmlFor: string
  error?: string
  children: ReactNode
}

export function FormField({ label, htmlFor, error, children }: FormFieldProps) {
  const errorId = `${htmlFor}-error`
  const describedBy = error ? errorId : undefined

  const childWithAria =
    isValidElement(children) && describedBy
      ? cloneElement(children as ReactElement<{ 'aria-describedby'?: string }>, {
          'aria-describedby': describedBy,
        })
      : children

  return (
    <div className="space-y-1.5">
      <label
        htmlFor={htmlFor}
        className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant"
      >
        {label}
      </label>
      {childWithAria}
      {error ? (
        <p id={errorId} role="alert" className="font-body text-xs text-error pt-1">
          {error}
        </p>
      ) : null}
    </div>
  )
}
```

- [ ] **Step 2: Verify build**

```bash
cd artdropapp-frontend && npm run build
```

Expected: passes.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/components/ui/FormField.tsx
git commit -m "feat(ui): FormField primitive with label, aria-describedby error slot"
```

---

## Task 8: SignupPage — static content (header, hero, headline)

Flesh out the SignupPage stub with everything *above* the form. Form + submit come in later tasks.

**Files:**
- Modify: `artdropapp-frontend/src/pages/SignupPage.tsx`

- [ ] **Step 1: Replace SignupPage contents**

Overwrite `artdropapp-frontend/src/pages/SignupPage.tsx` with:

```tsx
import { AuthHeader } from '../components/layout/AuthHeader'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

export function SignupPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 md:py-12 flex flex-col flex-grow">
        <div className="mb-10 w-full overflow-hidden">
          <img
            className="w-full h-48 md:h-64 object-cover grayscale opacity-90 transition-all duration-700 hover:grayscale-0"
            src={HERO_IMAGE_URL}
            alt="Abstract minimalist painting in muted earth tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Begin your collection.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Join our community of digital curators and discover exceptional artworks.
          </p>
        </section>

        {/* Form comes in Task 9 */}
      </main>
    </div>
  )
}
```

Note: the hero image URL is the one from the Stitch mockup. It's a Google-hosted aida-public asset; in a follow-up we may swap to a curated artwork from our own catalog, but for ship-velocity we use the mockup image directly (spec §7).

- [ ] **Step 2: Verify rendering**

```bash
cd artdropapp-frontend && npm run dev
```

Browser: `http://localhost:5173/signup`. Expected:
- Centered "ArtDrop" wordmark at top
- Grayscale hero image (`h-48` on mobile width, `h-64` when viewport widens past 768px)
- "Begin your collection." in Newsreader serif, large and light
- Subtitle paragraph below

Resize browser from ~375px width up to ~1400px to confirm the `md:` breakpoint hero-height change kicks in at 768px. Stop the dev server when done.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/pages/SignupPage.tsx
git commit -m "feat(signup): header, hero image, headline content"
```

---

## Task 9: SignupPage — form fields with RHF + Zod validation (no submit yet)

**Files:**
- Modify: `artdropapp-frontend/src/pages/SignupPage.tsx`

- [ ] **Step 1: Add the schema and form to SignupPage**

Replace the entire contents of `artdropapp-frontend/src/pages/SignupPage.tsx` with:

```tsx
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

const signupSchema = z
  .object({
    firstName: z
      .string()
      .trim()
      .min(1, { message: 'First name is required' })
      .max(50, { message: 'First name must be 50 characters or fewer' }),
    lastName: z
      .string()
      .trim()
      .min(1, { message: 'Last name is required' })
      .max(50, { message: 'Last name must be 50 characters or fewer' }),
    email: z
      .string()
      .trim()
      .min(1, { message: 'Email is required' })
      .email({ message: 'Enter a valid email address' }),
    password: z
      .string()
      .min(8, { message: 'Password must be at least 8 characters' }),
    confirmPassword: z.string().min(1, { message: 'Please confirm your password' }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: "Passwords don't match",
  })

type SignupFormValues = z.infer<typeof signupSchema>

export function SignupPage() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({
    mode: 'onTouched',
    resolver: zodResolver(signupSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = handleSubmit((values) => {
    // Wiring in Task 10
    console.log('submit', values)
  })

  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 md:py-12 flex flex-col flex-grow">
        <div className="mb-10 w-full overflow-hidden">
          <img
            className="w-full h-48 md:h-64 object-cover grayscale opacity-90 transition-all duration-700 hover:grayscale-0"
            src={HERO_IMAGE_URL}
            alt="Abstract minimalist painting in muted earth tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Begin your collection.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Join our community of digital curators and discover exceptional artworks.
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label="First Name" htmlFor="firstName" error={errors.firstName?.message}>
            <Input
              id="firstName"
              type="text"
              autoComplete="given-name"
              placeholder="E.g. Elena"
              disabled={isSubmitting}
              invalid={!!errors.firstName}
              {...register('firstName')}
            />
          </FormField>

          <FormField label="Last Name" htmlFor="lastName" error={errors.lastName?.message}>
            <Input
              id="lastName"
              type="text"
              autoComplete="family-name"
              placeholder="E.g. Rossi"
              disabled={isSubmitting}
              invalid={!!errors.lastName}
              {...register('lastName')}
            />
          </FormField>

          <FormField label="Email Address" htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="name@curator.com"
              disabled={isSubmitting}
              invalid={!!errors.email}
              {...register('email')}
            />
          </FormField>

          <FormField label="Password" htmlFor="password" error={errors.password?.message}>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.password}
              {...register('password')}
            />
          </FormField>

          <FormField
            label="Confirm Password"
            htmlFor="confirmPassword"
            error={errors.confirmPassword?.message}
          >
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
          </FormField>

          <div className="pt-2">
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              Create Your Profile
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/login"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            Already have an account? <span className="border-b border-on-surface pb-1">Sign In</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Verify build + validation in browser**

```bash
cd artdropapp-frontend && npm run build
```

Expected: passes.

```bash
cd artdropapp-frontend && npm run dev
```

Browser: `http://localhost:5173/signup`. Exercise each validation case:

1. Click into First Name, then tab out without typing → "First name is required" appears.
2. Type one character → error clears immediately.
3. Email: tab out empty → "Email is required". Type `not-an-email` and tab → "Enter a valid email address". Correct it → clears.
4. Password: type `abc` and tab → "Password must be at least 8 characters". Extend to 8+ chars → clears.
5. Confirm password: type a different value than password and tab → "Passwords don't match". Fix → clears.
6. Hit "Create Your Profile" with all valid fields → check dev console for the `submit { firstName, ... }` log (submit is not yet wired).
7. Hit the "Sign In" footer link → confirm it navigates to `/login` (the stub page).

Stop the dev server.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/pages/SignupPage.tsx
git commit -m "feat(signup): RHF + Zod form fields with on-touched validation"
```

---

## Task 10: SignupPage — wire submit, error handling, redirect

**Files:**
- Modify: `artdropapp-frontend/src/pages/SignupPage.tsx`

- [ ] **Step 1: Add submit wiring and form-level error state**

Replace the entire contents of `artdropapp-frontend/src/pages/SignupPage.tsx` with:

```tsx
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { signup, isSignupError, type SignupError } from '../api/authApi'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'
import { deriveUsernameFromEmail, storeToken } from '../lib/auth'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

const signupSchema = z
  .object({
    firstName: z
      .string()
      .trim()
      .min(1, { message: 'First name is required' })
      .max(50, { message: 'First name must be 50 characters or fewer' }),
    lastName: z
      .string()
      .trim()
      .min(1, { message: 'Last name is required' })
      .max(50, { message: 'Last name must be 50 characters or fewer' }),
    email: z
      .string()
      .trim()
      .min(1, { message: 'Email is required' })
      .email({ message: 'Enter a valid email address' }),
    password: z
      .string()
      .min(8, { message: 'Password must be at least 8 characters' }),
    confirmPassword: z.string().min(1, { message: 'Please confirm your password' }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: "Passwords don't match",
  })

type SignupFormValues = z.infer<typeof signupSchema>

function messageFor(err: SignupError): string {
  if (err.kind === 'email_taken') {
    return 'An account with this email already exists.'
  }
  if (err.kind === 'invalid') {
    return 'Please check your details and try again.'
  }
  return 'Something went wrong. Please try again.'
}

export function SignupPage() {
  const navigate = useNavigate()
  const [formError, setFormError] = useState<SignupError | null>(null)
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({
    mode: 'onTouched',
    resolver: zodResolver(signupSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const response = await signup({
        username: deriveUsernameFromEmail(values.email),
        email: values.email.trim(),
        password: values.password,
        displayName: `${values.firstName.trim()} ${values.lastName.trim()}`,
      })
      storeToken(response.token)
      navigate('/')
    } catch (error) {
      if (isSignupError(error)) {
        setFormError(error)
      } else {
        setFormError({ kind: 'network' })
      }
    }
  })

  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 md:py-12 flex flex-col flex-grow">
        <div className="mb-10 w-full overflow-hidden">
          <img
            className="w-full h-48 md:h-64 object-cover grayscale opacity-90 transition-all duration-700 hover:grayscale-0"
            src={HERO_IMAGE_URL}
            alt="Abstract minimalist painting in muted earth tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Begin your collection.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Join our community of digital curators and discover exceptional artworks.
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label="First Name" htmlFor="firstName" error={errors.firstName?.message}>
            <Input
              id="firstName"
              type="text"
              autoComplete="given-name"
              placeholder="E.g. Elena"
              disabled={isSubmitting}
              invalid={!!errors.firstName}
              {...register('firstName')}
            />
          </FormField>

          <FormField label="Last Name" htmlFor="lastName" error={errors.lastName?.message}>
            <Input
              id="lastName"
              type="text"
              autoComplete="family-name"
              placeholder="E.g. Rossi"
              disabled={isSubmitting}
              invalid={!!errors.lastName}
              {...register('lastName')}
            />
          </FormField>

          <FormField label="Email Address" htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="name@curator.com"
              disabled={isSubmitting}
              invalid={!!errors.email}
              {...register('email')}
            />
          </FormField>

          <FormField label="Password" htmlFor="password" error={errors.password?.message}>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.password}
              {...register('password')}
            />
          </FormField>

          <FormField
            label="Confirm Password"
            htmlFor="confirmPassword"
            error={errors.confirmPassword?.message}
          >
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
          </FormField>

          {formError ? (
            <div
              role="alert"
              className="font-body text-sm text-on-error-container bg-error-container/30 border border-error/30 p-3"
            >
              {messageFor(formError)}{' '}
              {formError.kind === 'email_taken' ? (
                <Link to="/login" className="underline underline-offset-4 hover:text-on-surface">
                  Sign in instead?
                </Link>
              ) : null}
            </div>
          ) : null}

          <div className="pt-2">
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              Create Your Profile
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/login"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            Already have an account? <span className="border-b border-on-surface pb-1">Sign In</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Verify build**

```bash
cd artdropapp-frontend && npm run build
```

Expected: passes.

- [ ] **Step 3: Commit**

```bash
git add artdropapp-frontend/src/pages/SignupPage.tsx
git commit -m "feat(signup): wire submit to /api/auth/signup, handle 409/400/network, redirect on success"
```

---

## Task 11: End-to-end verification

This task is a verification-only pass — no code changes. It completes the spec's §11 verification plan and is the gate before declaring the feature done. Every step below must produce the expected result or the task is blocked.

**Preparation:** The backend and frontend must both be running.

Terminal A (backend):
```bash
cd ArtDrop && ./mvnw spring-boot:run
```
Wait for `Started ArtdropappApplication` and a log line confirming the server is listening on port 8089.

Terminal B (frontend):
```bash
cd artdropapp-frontend && npm run dev
```
Wait for Vite to print the local URL (usually `http://localhost:5173`).

- [ ] **Step 1: Happy path signup**

Browser → `http://localhost:5173/signup`. Fill:
- First Name: `Ada`
- Last Name: `Lovelace`
- Email: `ada@example.com` (must be fresh — if re-running, vary this)
- Password: `password123`
- Confirm Password: `password123`

Click "Create Your Profile". Expected:
- Submit button enters loading state ("Please wait…") and all fields disable.
- Redirect to `/` (home page with main nav).
- DevTools → Application → Local Storage → `http://localhost:5173` → a key `artdrop_token` with a JWT value (three base64 segments separated by dots).

- [ ] **Step 2: Each validation case**

Refresh to `/signup`. Clear `artdrop_token` from localStorage first (DevTools).

Verify each of these produces the expected error on blur and clears on correction:
- First Name empty → "First name is required"
- Last Name empty → "Last name is required"
- Email empty → "Email is required"
- Email `not-an-email` → "Enter a valid email address"
- Password `short` → "Password must be at least 8 characters"
- Password mismatch in Confirm → "Passwords don't match"

- [ ] **Step 3: Duplicate email (409)**

Fill the form with:
- Email: `admin@artdrop.local` (seeded user, guaranteed to exist)
- All other fields: valid

Submit. Expected:
- No navigation.
- Form-level alert: "An account with this email already exists. Sign in instead?" with an inline link.
- Click the "Sign in instead?" link → navigates to `/login`.

- [ ] **Step 4: Username auto-dedupe (collision under the hood)**

Only relevant if no user with the derived username has been created yet. Fill the form with:
- Email: `admin@totally-different.com`  (derives username `admin`, which collides with the seeded admin user)
- All other fields: valid

Submit. Expected: HTTP 201, redirect to `/`, new JWT in localStorage. Verify in the H2 console (`http://localhost:8089/h2-console`, JDBC URL `jdbc:h2:mem:artdropdb`, user `sa`, no password): `SELECT username FROM app_user WHERE email = 'admin@totally-different.com'` returns `admin2`.

- [ ] **Step 5: Network failure**

Stop the backend in Terminal A (Ctrl+C). Fill the form with fresh valid values and submit. Expected:
- Form-level alert: "Something went wrong. Please try again."
- Fields re-enable, user can retry.

Restart the backend before the next step.

- [ ] **Step 6: Responsive check**

DevTools → toggle device toolbar. Sample widths and verify the layout holds:
- 375px (iPhone SE width): hero `h-48`, single-column form, "max-w-md" applied visually.
- 768px (iPad): hero bumps to `h-64`, form still centered and `max-w-md`.
- 1440px (desktop): form remains centered and `max-w-md`, generous top/bottom spacing.

No horizontal scrollbars at any width.

- [ ] **Step 7: Keyboard navigation**

From the signup page, press Tab from the top. Expected focus order: First Name → Last Name → Email → Password → Confirm Password → Create Your Profile → Sign In link. Each focused element shows a visible focus ring.

- [ ] **Step 8: Regression check — existing pages**

Verify the existing app still works:
- `/` → Discover / Circle / Challenges nav visible; home content loads.
- `/circle` → renders inside MainLayout with nav and footer.
- `/challenges` → renders.
- `/artworks` → renders and shows the seeded artworks list (requires backend).

If any of steps 1–8 fail, fix the issue before proceeding. This task does not have a commit — its output is a verification report.

- [ ] **Step 9: Final report**

Write a short summary of what was verified and what (if anything) could not be tested. Example: "Verified all 8 steps; did not test with a screen reader (no VoiceOver access). Found one issue: [description]; fixed in commit [sha]."

---

## Open items deferred to future tasks

- **Login page** (real implementation replacing the stub).
- **AuthContext**, Axios interceptor, protected routes, RBAC UI — Lab 8.
- **Password visibility toggle** — bolt-on nicety.
- **Curated hero image** from the artwork catalog instead of the Stitch-hosted URL — cosmetic polish once we have a preferred artwork.
- **Password strength meter** — YAGNI until we're told otherwise.
