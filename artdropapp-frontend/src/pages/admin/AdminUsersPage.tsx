import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAdminUsers } from '../../hooks/useAdminUsers'
import { SellerStatusBadge } from '../../components/SellerStatusBadge'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { AdminFiltersDrawer } from '../../components/admin/AdminFiltersDrawer'
import { DecisionModal } from '../../components/admin/DecisionModal'
import { approveApplication, rejectApplication, type AdminUserFilters } from '../../api/adminApi'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

function activeFilterCount(f: AdminUserFilters): number {
  let n = 0
  if (f.sellerStatuses && f.sellerStatuses.length > 0) n += f.sellerStatuses.length
  if (f.role) n += 1
  if (f.sort && f.sort !== 'newest') n += 1
  return n
}

export function AdminUsersPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const size = 20
  const [filters, setFilters] = useState<AdminUserFilters>({})
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [decision, setDecision] = useState<{
    mode: 'approve' | 'reject'
    appId: number
    applicantUsername: string
  } | null>(null)
  const [toast, setToast] = useState<string | null>(null)

  const { data, loading, error, refetch } = useAdminUsers(query, page, size, filters)
  const filterCount = activeFilterCount(filters)

  return (
    <section>
      <div className="flex gap-3 items-stretch mb-6">
        <Input
          type="search"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(0) }}
          placeholder="Search by username, name, or email…"
          className="flex-1"
        />
        <Button variant="secondary" onClick={() => setDrawerOpen(true)}>
          Filters{filterCount > 0 ? ` (${filterCount})` : ''}
        </Button>
      </div>

      {toast ? <p className="text-sm text-on-surface mb-4">{toast}</p> : null}

      {loading ? (
        <p className="text-on-surface-variant italic" role="status">Loading…</p>
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : !data || data.content.length === 0 ? (
        <p className="text-on-surface-variant italic">No users found.</p>
      ) : (
        <ul className="divide-y divide-outline-variant/15">
          {data.content.map((u) => {
            const pending = u.pendingApplication
            if (pending) {
              return (
                <li key={u.id} className="py-4">
                  <div className="flex items-center gap-3 mb-3">
                    <Link to={`/admin/users/${u.id}`} className="flex items-center gap-3 flex-1 min-w-0 hover:underline">
                      <img src={u.avatarUrl ?? 'https://i.pravatar.cc/64'} alt="" className="w-10 h-10 rounded-full object-cover" />
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-on-surface">{u.displayName}</div>
                        <div className="text-sm text-on-surface-variant truncate">@{u.username} · {u.email}</div>
                      </div>
                    </Link>
                    <SellerStatusBadge status={u.sellerStatus} />
                  </div>
                  <p className="text-sm text-on-surface bg-surface-container-low p-3 whitespace-pre-wrap mb-3">
                    {pending.message}
                  </p>
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-on-surface-variant">Submitted {formatDate(pending.submittedAt)}</span>
                    <div className="flex gap-2">
                      <Button
                        variant="destructive"
                        onClick={() => setDecision({ mode: 'reject', appId: pending.id, applicantUsername: u.username })}
                      >
                        Reject
                      </Button>
                      <Button
                        onClick={() => setDecision({ mode: 'approve', appId: pending.id, applicantUsername: u.username })}
                      >
                        Approve
                      </Button>
                    </div>
                  </div>
                </li>
              )
            }
            return (
              <li key={u.id}>
                <Link to={`/admin/users/${u.id}`} className="flex items-center gap-3 px-2 py-3 hover:bg-surface-container-low">
                  <img src={u.avatarUrl ?? 'https://i.pravatar.cc/64'} alt="" className="w-10 h-10 rounded-full object-cover" />
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-on-surface">{u.displayName}</div>
                    <div className="text-sm text-on-surface-variant truncate">@{u.username} · {u.email}</div>
                  </div>
                  <SellerStatusBadge status={u.sellerStatus} />
                </Link>
              </li>
            )
          })}
        </ul>
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between mt-6">
          <Button variant="secondary" disabled={data.number === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <span className="text-sm text-on-surface-variant">Page {data.number + 1} of {data.totalPages}</span>
          <Button variant="secondary" disabled={data.number + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button>
        </div>
      ) : null}

      <AdminFiltersDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        filters={filters}
        onApply={(next) => { setFilters(next); setPage(0) }}
      />

      <DecisionModal
        open={decision != null}
        mode={decision?.mode ?? 'approve'}
        applicantUsername={decision?.applicantUsername ?? ''}
        onClose={() => setDecision(null)}
        onConfirm={async (reason) => {
          if (!decision) return
          if (decision.mode === 'approve') {
            await approveApplication(decision.appId, reason || undefined)
            setToast('Application approved.')
          } else {
            await rejectApplication(decision.appId, reason)
            setToast('Application rejected.')
          }
          await refetch()
        }}
      />
    </section>
  )
}
