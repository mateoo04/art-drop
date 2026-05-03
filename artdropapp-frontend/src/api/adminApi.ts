import { authFetch } from '../lib/authFetch'
import type {
  AdminUserDetail,
  AdminUserSummary,
  PageResult,
  SellerApplication,
} from '../types/seller'
import { mapApplication } from './sellerApi'

function mapPage<TOut>(raw: Record<string, unknown>, mapItem: (r: Record<string, unknown>) => TOut): PageResult<TOut> {
  const content = Array.isArray(raw.content)
    ? (raw.content as Record<string, unknown>[]).map(mapItem)
    : []
  return {
    content,
    number: Number(raw.number ?? 0),
    size: Number(raw.size ?? content.length),
    totalElements: Number(raw.totalElements ?? content.length),
    totalPages: Number(raw.totalPages ?? 1),
  }
}

function mapAdminUserSummary(raw: Record<string, unknown>): AdminUserSummary {
  const pr = String(raw.primaryRole ?? 'USER').toUpperCase() === 'ADMIN' ? 'ADMIN' : 'USER'
  return {
    id: Number(raw.id),
    username: String(raw.username ?? ''),
    slug: String(raw.slug ?? ''),
    displayName: String(raw.displayName ?? ''),
    email: String(raw.email ?? ''),
    avatarUrl: raw.avatarUrl == null ? null : String(raw.avatarUrl),
    sellerStatus: String(raw.sellerStatus ?? 'NONE') as AdminUserSummary['sellerStatus'],
    pendingApplication:
      raw.pendingApplication == null
        ? null
        : mapApplication(raw.pendingApplication as Record<string, unknown>),
    primaryRole: pr,
    enabled: raw.enabled !== false,
  }
}

export type AdminUserFilters = {
  sellerStatuses?: ('NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED')[]
  role?: 'ROLE_USER' | 'ROLE_ADMIN' | null
  sort?: 'newest' | 'oldest_pending' | 'username' | 'most_artworks'
}

export async function searchAdminUsers(
  query: string,
  filters: AdminUserFilters,
  page = 0,
  size = 20,
): Promise<PageResult<AdminUserSummary>> {
  const params = new URLSearchParams()
  if (query) params.set('query', query)
  for (const s of filters.sellerStatuses ?? []) params.append('sellerStatus', s)
  if (filters.role) params.set('role', filters.role)
  if (filters.sort) params.set('sort', filters.sort)
  params.set('page', String(page))
  params.set('size', String(size))
  const url = `/api/admin/users?${params.toString()}`
  const res = await authFetch(url)
  if (!res.ok) throw new Error(`Failed to load users (${res.status})`)
  return mapPage((await res.json()) as Record<string, unknown>, mapAdminUserSummary)
}

export async function fetchAdminUserDetail(userId: number): Promise<AdminUserDetail> {
  const res = await authFetch(`/api/admin/users/${userId}`)
  if (!res.ok) throw new Error(`Failed to load user (${res.status})`)
  const raw = (await res.json()) as Record<string, unknown>
  return {
    user: mapAdminUserSummary((raw.user ?? {}) as Record<string, unknown>),
    applicationHistory: Array.isArray(raw.applicationHistory)
      ? (raw.applicationHistory as Record<string, unknown>[]).map(mapApplication)
      : [],
  }
}

export async function fetchListedArtworkCount(userId: number): Promise<number> {
  const res = await authFetch(`/api/admin/users/${userId}/listed-artwork-count`)
  if (!res.ok) throw new Error(`Failed to load count (${res.status})`)
  const raw = (await res.json()) as { count?: unknown }
  return Number(raw.count ?? 0)
}

export async function listSellerApplications(
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL' = 'PENDING',
  page = 0,
  size = 20,
): Promise<PageResult<SellerApplication>> {
  const url = `/api/admin/seller-applications?status=${status}&page=${page}&size=${size}`
  const res = await authFetch(url)
  if (!res.ok) throw new Error(`Failed to load applications (${res.status})`)
  return mapPage((await res.json()) as Record<string, unknown>, mapApplication)
}

export async function approveApplication(id: number, reason?: string): Promise<SellerApplication> {
  const res = await authFetch(`/api/admin/seller-applications/${id}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason: reason ?? null }),
  })
  if (!res.ok) throw new Error(`Approve failed (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export async function rejectApplication(id: number, reason: string): Promise<SellerApplication> {
  const res = await authFetch(`/api/admin/seller-applications/${id}/reject`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!res.ok) throw new Error(`Reject failed (${res.status})`)
  return mapApplication((await res.json()) as Record<string, unknown>)
}

export async function revokeSeller(userId: number, reason: string): Promise<{ unlistedCount: number }> {
  const res = await authFetch(`/api/admin/users/${userId}/revoke-seller`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ reason }),
  })
  if (!res.ok) throw new Error(`Revoke failed (${res.status})`)
  const raw = (await res.json()) as { unlistedCount?: unknown }
  return { unlistedCount: Number(raw.unlistedCount ?? 0) }
}

export async function promoteToAdmin(userId: number): Promise<void> {
  const res = await authFetch(`/api/admin/users/${userId}/promote-admin`, { method: 'POST' })
  if (!res.ok) throw new Error(`Promote failed (${res.status})`)
}

export async function grantSellerRole(userId: number): Promise<void> {
  const res = await authFetch(`/api/admin/users/${userId}/grant-seller`, { method: 'POST' })
  if (!res.ok) throw new Error(`Grant seller failed (${res.status})`)
}

export async function deactivateUser(userId: number): Promise<void> {
  const res = await authFetch(`/api/admin/users/${userId}/deactivate`, { method: 'POST' })
  if (res.status === 403) {
    const raw = (await res.json().catch(() => ({}))) as { error?: string }
    throw new Error(raw.error === 'SELF_DEACTIVATE' ? 'SELF_DEACTIVATE' : `Deactivate failed (${res.status})`)
  }
  if (!res.ok) throw new Error(`Deactivate failed (${res.status})`)
}

export async function reactivateUser(userId: number): Promise<void> {
  const res = await authFetch(`/api/admin/users/${userId}/reactivate`, { method: 'POST' })
  if (!res.ok) throw new Error(`Reactivate failed (${res.status})`)
}

export type AdminChallengeRow = {
  id: number
  title: string
  description: string | null
  quote: string | null
  kind: string | null
  status: string | null
  theme: string | null
  coverImageUrl: string | null
  startsAt: string | null
  endsAt: string | null
  submissionCount: number
}

export type AdminChallengeFilters = {
  status?: 'UPCOMING' | 'ACTIVE' | 'ENDED' | null
  sort?: 'starts_desc' | 'title'
}

function mapChallengeRow(raw: Record<string, unknown>): AdminChallengeRow {
  return {
    id: Number(raw.id),
    title: String(raw.title ?? ''),
    description: raw.description == null ? null : String(raw.description),
    quote: raw.quote == null ? null : String(raw.quote),
    kind: raw.kind == null ? null : String(raw.kind),
    status: raw.status == null ? null : String(raw.status),
    theme: raw.theme == null ? null : String(raw.theme),
    coverImageUrl: raw.coverImageUrl == null ? null : String(raw.coverImageUrl),
    startsAt: raw.startsAt == null ? null : String(raw.startsAt),
    endsAt: raw.endsAt == null ? null : String(raw.endsAt),
    submissionCount: Number(raw.submissionCount ?? 0),
  }
}

export type AdminChallengeUpsert = {
  title: string
  description: string | null
  quote: string | null
  kind: 'FEATURED' | 'OPEN'
  status: 'UPCOMING' | 'ACTIVE' | 'ENDED'
  theme: string | null
  coverImageUrl: string | null
  startsAt: string | null
  endsAt: string | null
}

export async function searchAdminChallenges(
  query: string,
  filters: AdminChallengeFilters,
  page = 0,
  size = 20,
): Promise<PageResult<AdminChallengeRow>> {
  const params = new URLSearchParams()
  if (query) params.set('query', query)
  if (filters.status) params.set('status', filters.status)
  if (filters.sort) params.set('sort', filters.sort)
  params.set('page', String(page))
  params.set('size', String(size))
  const res = await authFetch(`/api/admin/challenges?${params.toString()}`)
  if (!res.ok) throw new Error(`Failed to load challenges (${res.status})`)
  return mapPage((await res.json()) as Record<string, unknown>, mapChallengeRow)
}

export async function fetchAdminChallenge(id: number): Promise<AdminChallengeRow> {
  const res = await authFetch(`/api/admin/challenges/${id}`)
  if (!res.ok) throw new Error(`Failed to load challenge (${res.status})`)
  return mapChallengeRow((await res.json()) as Record<string, unknown>)
}

export async function createAdminChallenge(body: AdminChallengeUpsert): Promise<AdminChallengeRow> {
  const res = await authFetch('/api/admin/challenges', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Create failed (${res.status})`)
  return mapChallengeRow((await res.json()) as Record<string, unknown>)
}

export async function updateAdminChallenge(id: number, body: AdminChallengeUpsert): Promise<AdminChallengeRow> {
  const res = await authFetch(`/api/admin/challenges/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`Update failed (${res.status})`)
  return mapChallengeRow((await res.json()) as Record<string, unknown>)
}

export async function deleteAdminChallenge(id: number): Promise<void> {
  const res = await authFetch(`/api/admin/challenges/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(`Delete failed (${res.status})`)
}

export async function activateAdminChallenge(id: number): Promise<AdminChallengeRow> {
  const res = await authFetch(`/api/admin/challenges/${id}/activate`, { method: 'POST' })
  if (!res.ok) throw new Error(`Activate failed (${res.status})`)
  return mapChallengeRow((await res.json()) as Record<string, unknown>)
}

export async function deactivateAdminChallenge(id: number): Promise<AdminChallengeRow> {
  const res = await authFetch(`/api/admin/challenges/${id}/deactivate`, { method: 'POST' })
  if (!res.ok) throw new Error(`Deactivate failed (${res.status})`)
  return mapChallengeRow((await res.json()) as Record<string, unknown>)
}
