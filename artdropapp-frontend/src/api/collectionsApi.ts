import { API_BASE } from '../config'
import type { CollectionDTO } from '../types/collection'

function normalizeCreatedAt(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value)
}

function mapApiCollection(raw: Record<string, unknown>): CollectionDTO {
  return {
    id: Number(raw.id),
    name: String(raw.name),
    description: String(raw.description),
    artworkId: Number(raw.artworkId),
    createdAt: normalizeCreatedAt(raw.createdAt),
    isPublic: Boolean(raw.isPublic),
  }
}

export async function fetchCollections(): Promise<CollectionDTO[]> {
  const res = await fetch(`${API_BASE}/api/collections`)
  if (!res.ok) {
    throw new Error(`Failed to load collections (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiCollection(item as Record<string, unknown>))
}
