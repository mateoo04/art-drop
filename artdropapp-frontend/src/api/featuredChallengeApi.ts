import { authFetch } from '../lib/authFetch'

export type FeaturedTriggerType = 'AT_TIME' | 'WHEN_CURRENT_ENDS'

export interface FeaturedChallengeItem {
  id: number
  title: string
  endsAt: string | null
}

export interface FeaturedChallengeState {
  current: FeaturedChallengeItem | null
  next: FeaturedChallengeItem | null
  triggerType: FeaturedTriggerType | null
  triggerAt: string | null
}

const BASE = '/api/admin/featured-challenge'

function mapItem(raw: Record<string, unknown>): FeaturedChallengeItem {
  return {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    endsAt: raw.endsAt == null ? null : String(raw.endsAt),
  }
}

function mapState(raw: Record<string, unknown>): FeaturedChallengeState {
  return {
    current: raw.current == null ? null : mapItem(raw.current as Record<string, unknown>),
    next: raw.next == null ? null : mapItem(raw.next as Record<string, unknown>),
    triggerType: raw.triggerType == null ? null : (raw.triggerType as FeaturedTriggerType),
    triggerAt: raw.triggerAt == null ? null : String(raw.triggerAt),
  }
}

export async function getFeaturedState(): Promise<FeaturedChallengeState> {
  const res = await authFetch(BASE)
  if (!res.ok) throw new Error(`Failed to load featured state (${res.status})`)
  return mapState((await res.json()) as Record<string, unknown>)
}

export async function setFeaturedCurrent(challengeId: number): Promise<FeaturedChallengeState> {
  const res = await authFetch(`${BASE}/current`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ challengeId }),
  })
  if (!res.ok) throw new Error(`Failed to set featured current (${res.status})`)
  return mapState((await res.json()) as Record<string, unknown>)
}

export async function clearFeaturedCurrent(): Promise<void> {
  const res = await authFetch(`${BASE}/current`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`Failed to clear featured current (${res.status})`)
}

export async function scheduleReplacement(
  nextChallengeId: number,
  triggerType: FeaturedTriggerType,
  triggerAt: string | null,
): Promise<FeaturedChallengeState> {
  const res = await authFetch(`${BASE}/schedule`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nextChallengeId, triggerType, triggerAt }),
  })
  if (!res.ok) throw new Error(`Failed to schedule replacement (${res.status})`)
  return mapState((await res.json()) as Record<string, unknown>)
}

export async function clearSchedule(): Promise<void> {
  const res = await authFetch(`${BASE}/schedule`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`Failed to clear schedule (${res.status})`)
}
