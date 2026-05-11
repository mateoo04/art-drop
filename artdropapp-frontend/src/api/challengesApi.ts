import { authFetch } from '../lib/authFetch'
import type {
  Challenge,
  ChallengeStatus,
  SubmissionThumbnail,
} from '../types/challenge'

const STATUS_VALUES: ChallengeStatus[] = ['UPCOMING', 'ACTIVE', 'ENDED']

function parseStatus(value: unknown): ChallengeStatus | null {
  return typeof value === 'string' && (STATUS_VALUES as string[]).includes(value)
    ? (value as ChallengeStatus)
    : null
}

function normalizeDate(value: unknown): string | null {
  if (value == null) return null
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? null : date.toISOString()
  }
  return null
}

function mapThumbnail(raw: Record<string, unknown>): SubmissionThumbnail {
  const title = String(raw.title ?? '')
  return {
    submissionId: Number(raw.submissionId),
    artworkId: Number(raw.artworkId),
    title,
    imageUrl: String(raw.imageUrl ?? ''),
    imageAlt: String(raw.imageAlt ?? title),
    artistDisplayName:
      raw.artistDisplayName == null ? null : String(raw.artistDisplayName),
    artistSlug: raw.artistSlug == null ? null : String(raw.artistSlug),
  }
}

export function mapChallenge(raw: Record<string, unknown>): Challenge {
  return {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    description: raw.description == null ? null : String(raw.description),
    quote: raw.quote == null ? null : String(raw.quote),
    isFeatured: raw.isFeatured === true,
    status: parseStatus(raw.status),
    theme: raw.theme == null ? null : String(raw.theme),
    coverImageUrl: raw.coverImageUrl == null ? null : String(raw.coverImageUrl),
    startsAt: normalizeDate(raw.startsAt),
    endsAt: normalizeDate(raw.endsAt),
    submissionCount: Number(raw.submissionCount ?? 0),
    submissions: Array.isArray(raw.submissions)
      ? raw.submissions.map((s) => mapThumbnail(s as Record<string, unknown>))
      : [],
    viewerHasEntry: Boolean(raw.viewerHasEntry),
    viewerEntryArtworkId:
      raw.viewerEntryArtworkId == null ? null : Number(raw.viewerEntryArtworkId),
  }
}

export async function fetchChallenges(): Promise<Challenge[]> {
  const res = await authFetch(`/api/challenges`)
  if (!res.ok) {
    throw new Error(`Failed to load challenges (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapChallenge(item as Record<string, unknown>))
}

export async function fetchSearchChallenges(
  q: string,
  limit = 20,
  offset = 0,
): Promise<Challenge[]> {
  const params = new URLSearchParams({
    q,
    limit: String(limit),
    offset: String(offset),
  })
  const res = await authFetch(`/api/challenges/search?${params.toString()}`)
  if (!res.ok) {
    throw new Error(`Failed to search challenges (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapChallenge(item as Record<string, unknown>))
}

export async function fetchChallenge(challengeId: number): Promise<Challenge> {
  const res = await authFetch(`/api/challenges/${challengeId}`)
  if (!res.ok) {
    throw new Error(`Failed to load challenge (${res.status})`)
  }
  const json = (await res.json()) as Record<string, unknown>
  return mapChallenge(json)
}

export type SubmissionSort = 'top' | 'recent'

export async function fetchChallengeSubmissions(
  challengeId: number,
  options: { limit?: number; offset?: number; sort?: SubmissionSort } = {},
): Promise<SubmissionThumbnail[]> {
  const { limit = 24, offset = 0, sort = 'recent' } = options
  const params = new URLSearchParams({
    limit: String(limit),
    offset: String(offset),
    sort,
  })
  const res = await authFetch(`/api/challenges/${challengeId}/submissions?${params}`)
  if (!res.ok) {
    throw new Error(`Failed to load submissions (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapThumbnail(item as Record<string, unknown>))
}

export type ChallengeSubmitErrorCode =
  | 'ALREADY_SUBMITTED'
  | 'IN_OTHER_CHALLENGE'
  | 'USER_ALREADY_HAS_ENTRY'
  | 'CHALLENGE_NOT_ACTIVE'
  | 'ARTWORK_TOO_OLD'
  | 'NOT_OWNER'
  | 'UNAUTHENTICATED'
  | 'NOT_FOUND'
  | 'UNKNOWN'

export type ChallengeWithdrawErrorCode =
  | 'CHALLENGE_ENDED'
  | 'NOT_OWNER'
  | 'UNAUTHENTICATED'
  | 'NOT_FOUND'
  | 'UNKNOWN'

async function readErrorCode(res: Response): Promise<string | null> {
  try {
    const body = (await res.json()) as Record<string, unknown> | null
    if (body && typeof body === 'object' && typeof body.error === 'string') {
      return body.error
    }
  } catch {
    // ignore
  }
  return null
}

export async function submitArtworkToChallenge(
  challengeId: number,
  artworkId: number,
): Promise<void> {
  const res = await authFetch(`/api/challenges/${challengeId}/submissions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ artworkId }),
  })
  if (res.status === 201) return
  if (res.status === 401) throw new Error('UNAUTHENTICATED')
  if (res.status === 404) throw new Error('NOT_FOUND')
  if (res.status === 403 || res.status === 409) {
    const code = await readErrorCode(res)
    throw new Error(code ?? 'UNKNOWN')
  }
  throw new Error(`Submit failed (${res.status})`)
}

export async function withdrawArtworkFromChallenge(
  challengeId: number,
  artworkId: number,
): Promise<void> {
  const res = await authFetch(
    `/api/challenges/${challengeId}/submissions/${artworkId}`,
    { method: 'DELETE' },
  )
  if (res.status === 204) return
  if (res.status === 401) throw new Error('UNAUTHENTICATED')
  if (res.status === 404) throw new Error('NOT_FOUND')
  if (res.status === 403) {
    const code = await readErrorCode(res)
    throw new Error(code ?? 'UNKNOWN')
  }
  throw new Error(`Withdraw failed (${res.status})`)
}
