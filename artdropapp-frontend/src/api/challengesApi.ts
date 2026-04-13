import { API_BASE } from '../config'
import type {
  Challenge,
  ChallengeKind,
  ChallengeStatus,
  SubmissionThumbnail,
} from '../types/challenge'

const KIND_VALUES: ChallengeKind[] = ['FEATURED', 'COMMUNITY_CHOICE', 'OPEN']
const STATUS_VALUES: ChallengeStatus[] = ['UPCOMING', 'ACTIVE', 'ENDED']

function parseKind(value: unknown): ChallengeKind | null {
  return typeof value === 'string' && (KIND_VALUES as string[]).includes(value)
    ? (value as ChallengeKind)
    : null
}

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

function mapChallenge(raw: Record<string, unknown>): Challenge {
  return {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    description: raw.description == null ? null : String(raw.description),
    quote: raw.quote == null ? null : String(raw.quote),
    kind: parseKind(raw.kind),
    status: parseStatus(raw.status),
    theme: raw.theme == null ? null : String(raw.theme),
    coverImageUrl: raw.coverImageUrl == null ? null : String(raw.coverImageUrl),
    startsAt: normalizeDate(raw.startsAt),
    endsAt: normalizeDate(raw.endsAt),
    submissionCount: Number(raw.submissionCount ?? 0),
    submissions: Array.isArray(raw.submissions)
      ? raw.submissions.map((s) => mapThumbnail(s as Record<string, unknown>))
      : [],
  }
}

export async function fetchChallenges(): Promise<Challenge[]> {
  const res = await fetch(`${API_BASE}/api/challenges`)
  if (!res.ok) {
    throw new Error(`Failed to load challenges (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapChallenge(item as Record<string, unknown>))
}

export async function fetchChallengeSubmissions(
  challengeId: number,
  limit = 12,
  offset = 0,
): Promise<SubmissionThumbnail[]> {
  const res = await fetch(
    `${API_BASE}/api/challenges/${challengeId}/submissions?limit=${limit}&offset=${offset}`,
  )
  if (!res.ok) {
    throw new Error(`Failed to load submissions (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapThumbnail(item as Record<string, unknown>))
}
