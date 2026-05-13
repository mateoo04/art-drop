
export type RegisterRequest = {
  username: string
  email: string
  password: string
  displayName: string
}

export type AuthSessionResponse = {
  username: string
}

export type SignupError =
  | { kind: 'email_taken' }
  | { kind: 'invalid' }
  | { kind: 'network' }

export async function signup(request: RegisterRequest): Promise<AuthSessionResponse> {
  let res: Response
  try {
    res = await fetch(`/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(request),
    })
  } catch {
    const err: SignupError = { kind: 'network' }
    throw err
  }

  if (res.status === 201) {
    return (await res.json()) as AuthSessionResponse
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

export type LoginRequest = {
  email: string
  password: string
}

export type LoginError =
  | { kind: 'bad_credentials' }
  | { kind: 'invalid' }
  | { kind: 'network' }

export async function login(request: LoginRequest): Promise<AuthSessionResponse> {
  let res: Response
  try {
    res = await fetch(`/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username: request.email, password: request.password }),
    })
  } catch {
    const err: LoginError = { kind: 'network' }
    throw err
  }

  if (res.status === 200) {
    return (await res.json()) as AuthSessionResponse
  }
  if (res.status === 401) {
    const err: LoginError = { kind: 'bad_credentials' }
    throw err
  }
  if (res.status === 400) {
    const err: LoginError = { kind: 'invalid' }
    throw err
  }
  const err: LoginError = { kind: 'network' }
  throw err
}

export function isLoginError(value: unknown): value is LoginError {
  return (
    typeof value === 'object' &&
    value !== null &&
    'kind' in value &&
    typeof (value as { kind: unknown }).kind === 'string'
  )
}

export async function logout(): Promise<void> {
  try {
    await fetch(`/api/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      headers: csrfHeader(),
    })
  } catch {
    void 0 // best-effort logout; ignore network failures
  }
}

function csrfHeader(): Record<string, string> {
  const cookie = document.cookie.split(';').find((c) => c.trim().startsWith('XSRF-TOKEN='))
  if (!cookie) return {}
  const value = decodeURIComponent(cookie.split('=')[1] ?? '')
  return value ? { 'X-XSRF-TOKEN': value } : {}
}
