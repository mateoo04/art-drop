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
