import { clearToken } from './auth'
import { apiPath } from './apiBase'

export class UnauthorizedError extends Error {
  constructor() {
    super('UNAUTHORIZED')
    this.name = 'UnauthorizedError'
  }
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  for (const part of document.cookie.split(';')) {
    const trimmed = part.trim()
    if (trimmed.startsWith(prefix)) {
      return decodeURIComponent(trimmed.substring(prefix.length))
    }
  }
  return null
}

export async function authFetch(input: string, init: RequestInit = {}): Promise<Response> {
  const headers = new Headers(init.headers ?? {})
  const method = (init.method ?? 'GET').toUpperCase()

  if (!SAFE_METHODS.has(method) && !headers.has('X-XSRF-TOKEN')) {
    const csrf = readCookie('XSRF-TOKEN')
    if (csrf) headers.set('X-XSRF-TOKEN', csrf)
  }

  const requestInput = typeof input === 'string' && input.startsWith('/api') ? apiPath(input) : input
  const res = await fetch(requestInput, { ...init, headers, credentials: 'include' })

  if (res.status === 401) {
    clearToken()
    if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
      window.location.assign('/login')
    }
    throw new UnauthorizedError()
  }

  return res
}
