import { API_BASE } from '../config'
import { authFetch } from '../lib/authFetch'
import type { Artwork } from '../types/artwork'
import { mapApiArtwork } from './artworksApi'

export type CircleFeedParams = {
  limit?: number
  offset?: number
}

export async function fetchCircleFeed({
  limit = 20,
  offset = 0,
}: CircleFeedParams = {}): Promise<Artwork[]> {
  const url = `${API_BASE}/api/feed/circle?limit=${limit}&offset=${offset}`
  const res = await authFetch(url)
  if (!res.ok) {
    throw new Error(`Failed to load circle feed (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapApiArtwork(item as Record<string, unknown>))
}
