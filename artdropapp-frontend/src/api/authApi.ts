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
