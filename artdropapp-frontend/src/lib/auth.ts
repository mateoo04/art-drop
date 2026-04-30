const TOKEN_KEY = 'artdrop_token'

export function storeToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  if (isTokenExpired(token)) {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
  return token
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

function isTokenExpired(token: string): boolean {
  const parts = token.split('.')
  if (parts.length !== 3) return true
  try {
    const payload = JSON.parse(
      atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')),
    ) as { exp?: number }
    if (typeof payload.exp !== 'number') return false
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

export function deriveUsernameFromEmail(email: string): string {
  const local = email.split('@')[0] ?? ''
  const cleaned = local.toLowerCase().replace(/[^a-z0-9]/g, '')
  return cleaned.length > 0 ? cleaned : 'user'
}
