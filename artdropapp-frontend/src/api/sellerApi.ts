import { authFetch } from '../lib/authFetch'
import type { AdminPrimaryRole, SellerApplication } from '../types/seller'

function normalizeTimestamp(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value ?? '')
}

function mapApplication(raw: Record<string, unknown>): SellerApplication {
  const applicantRaw = raw.applicant as Record<string, unknown> | null | undefined
  const applicant = applicantRaw == null
    ? null
    : (() => {
        const pr = String(applicantRaw.primaryRole ?? 'USER').toUpperCase() === 'ADMIN' ? 'ADMIN' : 'USER'
        return {
          id: Number(applicantRaw.id),
          username: String(applicantRaw.username ?? ''),
          slug: String(applicantRaw.slug ?? ''),
          displayName: String(applicantRaw.displayName ?? ''),
          email: String(applicantRaw.email ?? ''),
          avatarUrl: applicantRaw.avatarUrl == null ? null : String(applicantRaw.avatarUrl),
          sellerStatus: String(applicantRaw.sellerStatus ?? 'NONE') as SellerApplication['derivedSellerStatus'],
          pendingApplication: null,
          primaryRole: pr as AdminPrimaryRole,
          enabled: applicantRaw.enabled !== false,
        }
      })()
  return {
    id: Number(raw.id),
    userId: Number(raw.userId),
    applicant,
    message: String(raw.message ?? ''),
    status: String(raw.status ?? 'PENDING') as SellerApplication['status'],
    submittedAt: normalizeTimestamp(raw.submittedAt),
    decidedAt: raw.decidedAt == null ? null : normalizeTimestamp(raw.decidedAt),
    decidedByUserId: raw.decidedByUserId == null ? null : Number(raw.decidedByUserId),
    decisionReason: raw.decisionReason == null ? null : String(raw.decisionReason),
    revokedAt: raw.revokedAt == null ? null : normalizeTimestamp(raw.revokedAt),
    revokedByUserId: raw.revokedByUserId == null ? null : Number(raw.revokedByUserId),
    revokeReason: raw.revokeReason == null ? null : String(raw.revokeReason),
    derivedSellerStatus: String(raw.derivedSellerStatus ?? 'NONE') as SellerApplication['derivedSellerStatus'],
    canReapplyAt: raw.canReapplyAt == null ? null : normalizeTimestamp(raw.canReapplyAt),
  }
}

export async function fetchMyApplication(): Promise<SellerApplication | null> {
  const res = await authFetch('/api/users/me/seller-application')
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Failed to load seller application (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export type SubmitApplicationError =
  | { kind: 'ALREADY_PENDING' }
  | { kind: 'ALREADY_SELLER' }
  | { kind: 'COOLDOWN_ACTIVE'; canReapplyAt: string }
  | { kind: 'OTHER'; message: string }

export async function submitApplication(message: string): Promise<SellerApplication> {
  const res = await authFetch('/api/users/me/seller-application', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message }),
  })
  if (res.status === 201) {
    return mapApplication((await res.json()) as Record<string, unknown>)
  }
  let body: Record<string, unknown> = {}
  try {
    body = (await res.json()) as Record<string, unknown>
  } catch {
    body = {}
  }
  const code = String(body.error ?? '')
  if (code === 'ALREADY_PENDING') throw { kind: 'ALREADY_PENDING' } satisfies SubmitApplicationError
  if (code === 'ALREADY_SELLER') throw { kind: 'ALREADY_SELLER' } satisfies SubmitApplicationError
  if (code === 'COOLDOWN_ACTIVE') {
    throw {
      kind: 'COOLDOWN_ACTIVE',
      canReapplyAt: String(body.canReapplyAt ?? ''),
    } satisfies SubmitApplicationError
  }
  throw { kind: 'OTHER', message: `Submit failed (${res.status})` } satisfies SubmitApplicationError
}

export { mapApplication }
