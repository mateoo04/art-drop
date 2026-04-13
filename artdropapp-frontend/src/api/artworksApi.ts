import { API_BASE } from '../config'
import type { Artwork, ProgressStatus, SaleStatus } from '../types/artwork'

const PROGRESS_VALUES: ProgressStatus[] = ['WIP', 'FINISHED']
const SALE_VALUES: SaleStatus[] = ['ORIGINAL', 'EDITION', 'AVAILABLE', 'SOLD']

function parseProgress(value: unknown): ProgressStatus | null {
  return typeof value === 'string' && (PROGRESS_VALUES as string[]).includes(value)
    ? (value as ProgressStatus)
    : null
}

function parseSale(value: unknown): SaleStatus | null {
  return typeof value === 'string' && (SALE_VALUES as string[]).includes(value)
    ? (value as SaleStatus)
    : null
}

function normalizePublishedAt(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value)
}

function mapApiArtwork(raw: Record<string, unknown>): Artwork {
  const title = String(raw.title)
  const medium = String(raw.medium)
  const artistId = raw.artistId
  const artist =
    artistId != null
      ? {
          id: Number(artistId),
          displayName: String(raw.artistDisplayName ?? ''),
          slug: String(raw.artistSlug ?? ''),
          avatarUrl: raw.artistAvatarUrl == null ? null : String(raw.artistAvatarUrl),
        }
      : null
  const priceRaw = raw.price
  return {
    id: Number(raw.id),
    title,
    medium,
    description: raw.description == null ? null : String(raw.description),
    imageUrl: String(raw.imageUrl ?? ''),
    imageAlt: String(raw.imageAlt ?? `${title} - ${medium}`),
    aspectRatio: Number(raw.aspectRatio ?? 1),
    price: priceRaw == null ? null : Number(priceRaw),
    progressStatus: parseProgress(raw.progressStatus),
    saleStatus: parseSale(raw.saleStatus),
    artist,
    tags: Array.isArray(raw.tags) ? raw.tags.map(String) : [],
    publishedAt: normalizePublishedAt(raw.publishedAt),
    likeCount: Number(raw.likeCount ?? 0),
    commentCount: Number(raw.commentCount ?? 0),
  }
}

export async function fetchArtworks(): Promise<Artwork[]> {
  const res = await fetch(`${API_BASE}/api/artworks`)
  if (!res.ok) {
    throw new Error(`Failed to load artworks (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiArtwork(item as Record<string, unknown>))
}

export async function fetchArtworkById(id: number): Promise<Artwork> {
  const res = await fetch(`${API_BASE}/api/artworks/id/${id}`)
  if (res.status === 404) {
    throw new Error('NOT_FOUND')
  }
  if (!res.ok) {
    throw new Error(`Failed to load artwork (${res.status})`)
  }
  const json: unknown = await res.json()
  return mapApiArtwork(json as Record<string, unknown>)
}

export type CreateArtworkPayload = {
  title: string
  medium: string
  description: string
  imageUrl: string
}

export async function createArtwork(payload: CreateArtworkPayload): Promise<void> {
  const res = await fetch(`${API_BASE}/api/artworks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      ...payload,
      catalogSequence: 0,
    }),
  })
  if (res.status === 409) {
    throw new Error('An artwork with this title already exists')
  }
  if (!res.ok) {
    throw new Error(`Save failed (${res.status})`)
  }
}

export async function deleteArtworkByTitle(title: string): Promise<void> {
  const encoded = encodeURIComponent(title)
  const res = await fetch(`${API_BASE}/api/artworks/${encoded}`, {
    method: 'DELETE',
  })
  if (res.status === 404) {
    throw new Error('Artwork not found')
  }
  if (!res.ok) {
    throw new Error(`Delete failed (${res.status})`)
  }
}
