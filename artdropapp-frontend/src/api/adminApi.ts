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
