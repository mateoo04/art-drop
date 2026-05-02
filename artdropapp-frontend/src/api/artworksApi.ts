import { authFetch } from '../lib/authFetch'
import { cloudinaryUrl } from '../lib/cloudinary'
import type {
  Artwork,
  ArtworkImage,
  DimensionUnit,
  ProgressStatus,
  SaleStatus,
} from '../types/artwork'

const PROGRESS_VALUES: ProgressStatus[] = ['WIP', 'FINISHED']
const SALE_VALUES: SaleStatus[] = ['ORIGINAL', 'EDITION', 'AVAILABLE', 'SOLD']
const UNIT_VALUES: DimensionUnit[] = ['CM', 'MM', 'IN', 'PX']

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

function parseUnit(value: unknown): DimensionUnit | null {
  return typeof value === 'string' && (UNIT_VALUES as string[]).includes(value)
    ? (value as DimensionUnit)
    : null
}

function parseNumber(value: unknown): number | null {
  if (value == null) return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function parseImages(raw: unknown): ArtworkImage[] {
  if (!Array.isArray(raw)) return []
  return raw.map((entry): ArtworkImage => {
    const r = entry as Record<string, unknown>
    const publicId = String(r.publicId ?? r.imageUrl ?? '')
    return {
      id: r.id == null ? null : Number(r.id),
      publicId,
      imageUrl: cloudinaryUrl(publicId, { width: 800 }),
      sortOrder: Number(r.sortOrder ?? 0),
      isCover: Boolean(r.isCover),
      caption: r.caption == null ? null : String(r.caption),
    }
  })
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

export function mapApiArtwork(raw: Record<string, unknown>): Artwork {
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
  const images = parseImages(raw.images)
  const coverRaw = raw.coverPublicId ?? raw.coverImageUrl ?? raw.imageUrl
  const coverPublicId = coverRaw != null ? String(coverRaw) : (images[0]?.publicId ?? '')
  const cover = coverPublicId ? cloudinaryUrl(coverPublicId, { width: 800 }) : ''
  return {
    id: Number(raw.id),
    title,
    medium,
    description: raw.description == null ? null : String(raw.description),
    imageUrl: cover,
    coverPublicId,
    imageAlt: String(raw.imageAlt ?? `${title} - ${medium}`),
    aspectRatio: Number(raw.aspectRatio ?? 1),
    images,
    width: parseNumber(raw.width),
    height: parseNumber(raw.height),
    depth: parseNumber(raw.depth),
    dimensionUnit: parseUnit(raw.dimensionUnit),
    price: priceRaw == null ? null : Number(priceRaw),
    progressStatus: parseProgress(raw.progressStatus),
    saleStatus: parseSale(raw.saleStatus),
    artist,
    tags: Array.isArray(raw.tags) ? raw.tags.map(String) : [],
    publishedAt: normalizePublishedAt(raw.publishedAt),
    likeCount: Number(raw.likeCount ?? 0),
    commentCount: Number(raw.commentCount ?? 0),
    likedByMe: Boolean(raw.likedByMe),
  }
}

export type FetchArtworksParams = {
  medium?: string | null
  limit?: number
  offset?: number
}

export async function fetchArtworks({
  medium,
  limit = 20,
  offset = 0,
}: FetchArtworksParams = {}): Promise<Artwork[]> {
  const params = new URLSearchParams()
  params.set('limit', String(limit))
  params.set('offset', String(offset))
  if (medium && medium !== 'All') params.set('medium', medium)
  const res = await authFetch(`/api/artworks?${params.toString()}`)
  if (!res.ok) {
    throw new Error(`Failed to load artworks (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiArtwork(item as Record<string, unknown>))
}

export async function fetchMediums(): Promise<string[]> {
  const res = await authFetch(`/api/artworks/mediums`)
  if (!res.ok) {
    throw new Error(`Failed to load mediums (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map(String)
}

export async function fetchArtworkById(id: number): Promise<Artwork> {
  const res = await authFetch(`/api/artworks/id/${id}`)
  if (res.status === 404) {
    throw new Error('NOT_FOUND')
  }
  if (!res.ok) {
    throw new Error(`Failed to load artwork (${res.status})`)
  }
  const json: unknown = await res.json()
  return mapApiArtwork(json as Record<string, unknown>)
}

export async function likeArtwork(id: number): Promise<void> {
  const res = await authFetch(`/api/artworks/${id}/likes`, { method: 'POST' })
  if (!res.ok && res.status !== 204) {
    throw new Error(`Failed to like (${res.status})`)
  }
}

export async function unlikeArtwork(id: number): Promise<void> {
  const res = await authFetch(`/api/artworks/${id}/likes`, { method: 'DELETE' })
  if (!res.ok && res.status !== 204) {
    throw new Error(`Failed to unlike (${res.status})`)
  }
}

export type ArtworkImageInput = {
  publicId: string
  sortOrder?: number
  isCover?: boolean
  caption?: string | null
}

export type CreateArtworkPayload = {
  title: string
  medium: string
  description?: string
  images: ArtworkImageInput[]
  width?: number | null
  height?: number | null
  depth?: number | null
  dimensionUnit?: DimensionUnit | null
  progressStatus?: ProgressStatus
  tags?: string[]
  price?: number | null
  saleStatus?: SaleStatus | null
}

export async function createArtwork(payload: CreateArtworkPayload): Promise<Artwork | null> {
  const res = await authFetch(`/api/artworks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (res.status === 409) {
    throw new Error('TITLE_TAKEN')
  }
  if (res.status === 401) {
    throw new Error('UNAUTHENTICATED')
  }
  if (res.status === 403) {
    let body: unknown = null
    try {
      body = await res.json()
    } catch {
      body = null
    }
    if (
      body != null &&
      typeof body === 'object' &&
      (body as Record<string, unknown>).error === 'FORBIDDEN_SALE_GATE'
    ) {
      throw new Error('FORBIDDEN_SALE_GATE')
    }
    throw new Error(`Create failed (${res.status})`)
  }
  if (res.status !== 201) {
    throw new Error(`Create failed (${res.status})`)
  }
  try {
    const json: unknown = await res.json()
    return mapApiArtwork(json as Record<string, unknown>)
  } catch {
    return null
  }
}

export type UpdateArtworkPayload = {
  title?: string
  medium?: string
  description?: string
  images?: ArtworkImageInput[]
  width?: number | null
  height?: number | null
  depth?: number | null
  dimensionUnit?: DimensionUnit | null
  price?: number | null
  saleStatus?: 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | null
  unlist?: boolean
}

export async function updateArtwork(id: number, payload: UpdateArtworkPayload): Promise<Artwork> {
  const res = await authFetch(`/api/artworks/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (res.status === 404) {
    throw new Error('Artwork not found')
  }
  if (res.status === 403) {
    let body: unknown = null
    try {
      body = await res.json()
    } catch {
      body = null
    }
    if (
      body != null &&
      typeof body === 'object' &&
      (body as Record<string, unknown>).error === 'FORBIDDEN_SALE_GATE'
    ) {
      throw new Error('FORBIDDEN_SALE_GATE')
    }
    throw new Error(`Update failed (${res.status})`)
  }
  if (!res.ok) {
    throw new Error(`Update failed (${res.status})`)
  }
  const json: unknown = await res.json()
  return mapApiArtwork(json as Record<string, unknown>)
}
