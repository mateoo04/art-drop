import { API_BASE } from '../config'
import { authFetch } from '../lib/authFetch'
import type { UserProfile } from '../types/user'
import type { Artwork } from '../types/artwork'
import { mapApiArtwork } from './artworksApi'

export type UpdateProfilePayload = {
  displayName?: string
  bio?: string | null
  avatarUrl?: string | null
}

function normalizeCreatedAt(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value ?? '')
}

function mapUserProfile(raw: Record<string, unknown>): UserProfile {
  return {
    id: Number(raw.id),
    username: String(raw.username ?? ''),
    slug: String(raw.slug ?? ''),
    displayName: String(raw.displayName ?? ''),
    bio: raw.bio == null ? null : String(raw.bio),
    avatarUrl: raw.avatarUrl == null ? null : String(raw.avatarUrl),
    createdAt: normalizeCreatedAt(raw.createdAt),
    artworkCount: Number(raw.artworkCount ?? 0),
    circleSize: raw.circleSize == null ? null : Number(raw.circleSize),
    followingCount: raw.followingCount == null ? null : Number(raw.followingCount),
    isSelf: Boolean(raw.isSelf),
  }
}

export async function fetchMe(): Promise<UserProfile> {
  const res = await authFetch(`${API_BASE}/api/users/me`)
  if (!res.ok) {
    throw new Error(`Failed to load profile (${res.status})`)
  }
  const json = (await res.json()) as Record<string, unknown>
  return mapUserProfile(json)
}

export async function updateMe(payload: UpdateProfilePayload): Promise<UserProfile> {
  const res = await authFetch(`${API_BASE}/api/users/me`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!res.ok) {
    throw new Error(`Update failed (${res.status})`)
  }
  const json = (await res.json()) as Record<string, unknown>
  return mapUserProfile(json)
}

export async function fetchMyArtworks(): Promise<Artwork[]> {
  const res = await authFetch(`${API_BASE}/api/users/me/artworks`)
  if (!res.ok) {
    throw new Error(`Failed to load your artworks (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiArtwork(item as Record<string, unknown>))
}

export async function fetchProfileBySlug(slug: string): Promise<UserProfile> {
  const res = await authFetch(`${API_BASE}/api/users/${encodeURIComponent(slug)}`)
  if (res.status === 404) {
    throw new Error('NOT_FOUND')
  }
  if (!res.ok) {
    throw new Error(`Failed to load profile (${res.status})`)
  }
  const json = (await res.json()) as Record<string, unknown>
  return mapUserProfile(json)
}

export async function fetchProfileArtworks(slug: string): Promise<Artwork[]> {
  const res = await authFetch(`${API_BASE}/api/users/${encodeURIComponent(slug)}/artworks`)
  if (!res.ok) {
    throw new Error(`Failed to load artworks (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiArtwork(item as Record<string, unknown>))
}

export async function fetchCircleStatus(slug: string): Promise<boolean> {
  const res = await authFetch(`${API_BASE}/api/users/${encodeURIComponent(slug)}/circle-status`)
  if (!res.ok) {
    throw new Error(`Failed to load circle status (${res.status})`)
  }
  const json = (await res.json()) as { inCircle?: unknown }
  return Boolean(json.inCircle)
}

export async function joinCircle(slug: string): Promise<boolean> {
  const res = await authFetch(`${API_BASE}/api/users/${encodeURIComponent(slug)}/circle`, {
    method: 'POST',
  })
  if (!res.ok) {
    throw new Error(`Failed to join circle (${res.status})`)
  }
  const json = (await res.json()) as { inCircle?: unknown }
  return Boolean(json.inCircle)
}

export async function leaveCircle(slug: string): Promise<boolean> {
  const res = await authFetch(`${API_BASE}/api/users/${encodeURIComponent(slug)}/circle`, {
    method: 'DELETE',
  })
  if (!res.ok) {
    throw new Error(`Failed to leave circle (${res.status})`)
  }
  const json = (await res.json()) as { inCircle?: unknown }
  return Boolean(json.inCircle)
}
