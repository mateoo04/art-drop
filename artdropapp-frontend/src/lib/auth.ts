const SESSION_KEY = 'artdrop_session'

export function storeToken(username: string): void {
  localStorage.setItem(SESSION_KEY, username)
}

export function getToken(): string | null {
  return localStorage.getItem(SESSION_KEY)
}

export function clearToken(): void {
  localStorage.removeItem(SESSION_KEY)
}

export function deriveUsernameFromEmail(email: string): string {
  const local = email.split('@')[0] ?? ''
  const cleaned = local.toLowerCase().replace(/[^a-z0-9]/g, '')
  return cleaned.length > 0 ? cleaned : 'user'
}
