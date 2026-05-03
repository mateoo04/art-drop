import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  approveApplication,
  deactivateUser,
  grantSellerRole,
  promoteToAdmin,
  reactivateUser,
  rejectApplication,
  type AdminUserFilters,
} from '../../api/adminApi'
import { AdminFiltersDrawer } from '../../components/admin/AdminFiltersDrawer'
import { AdminUserExpandPanel } from '../../components/admin/AdminUserExpandPanel'
import { ExpandRowToggle } from '../../components/admin/ExpandRowToggle'
import { DecisionModal } from '../../components/admin/DecisionModal'
import { RoleBadge } from '../../components/admin/RoleBadge'
import { SellerStatusBadge } from '../../components/SellerStatusBadge'
import { Button } from '../../components/ui/Button'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { Input } from '../../components/ui/Input'
import { Spinner } from '../../components/ui/Spinner'
import { useAdminUsers } from '../../hooks/useAdminUsers'
import { useCurrentUser } from '../../hooks/useCurrentUser'
import type { AdminUserSummary } from '../../types/seller'

function activeFilterCount(f: AdminUserFilters): number {
  let n = 0
  if (f.sellerStatuses && f.sellerStatuses.length > 0) n += f.sellerStatuses.length
  if (f.role) n += 1
  if (f.sort && f.sort !== 'newest') n += 1
  return n
}

export function AdminUsersPage() {
  const { t } = useTranslation()
  const { user: me } = useCurrentUser()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const size = 20
  const [filters, setFilters] = useState<AdminUserFilters>({})
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [expandedUserId, setExpandedUserId] = useState<number | null>(null)
  const [listRefreshSignal, setListRefreshSignal] = useState(0)
  const [decision, setDecision] = useState<{
    mode: 'approve' | 'reject'
    appId: number
    applicantUsername: string
  } | null>(null)
  const [toast, setToast] = useState<string | null>(null)
  const [deactivateTarget, setDeactivateTarget] = useState<AdminUserSummary | null>(null)
  const [deactivateBusy, setDeactivateBusy] = useState(false)
  const [grantSellerTarget, setGrantSellerTarget] = useState<AdminUserSummary | null>(null)
  const [grantSellerBusy, setGrantSellerBusy] = useState(false)
  const [promoteTarget, setPromoteTarget] = useState<AdminUserSummary | null>(null)
  const [promoteBusy, setPromoteBusy] = useState(false)

  const { data, loading, error, refetch } = useAdminUsers(query, page, size, filters)
  const filterCount = activeFilterCount(filters)

  const currentUserId = me?.id ?? null

  async function refetchListAndBumpPanel() {
    await refetch()
    setListRefreshSignal((s) => s + 1)
  }

  async function runMutation(fn: () => Promise<void>, success: string) {
    try {
      await fn()
      setToast(success)
      await refetchListAndBumpPanel()
    } catch (e) {
      const msg = e instanceof Error ? e.message : t('admin.users.actionFailed')
      setToast(msg === 'SELF_DEACTIVATE' ? t('admin.users.selfDeactivate') : msg)
    }
  }

  return (
    <section>
      <div className="flex gap-3 items-stretch mb-6">
        <Input
          type="search"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(0) }}
          placeholder={t('admin.users.search')}
          className="flex-1"
        />
        <Button variant="secondary" onClick={() => setDrawerOpen(true)}>
          {filterCount > 0 ? t('admin.users.filtersWithCount', { count: filterCount }) : t('admin.users.filters')}
        </Button>
      </div>

      {toast ? <p className="text-sm text-on-surface mb-4">{toast}</p> : null}

      {loading ? (
        <div className="flex justify-center py-16">
          <Spinner />
        </div>
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : !data || data.content.length === 0 ? (
        <p className="text-on-surface-variant italic">{t('admin.users.noUsers')}</p>
      ) : (
        <ul className="divide-y divide-outline-variant/15">
          {data.content.map((u) => {
            const rowMuted = !u.enabled ? 'opacity-60' : ''
            const expanded = expandedUserId === u.id
            const pending = u.pendingApplication
            return (
              <li key={u.id} className={rowMuted}>
                <div
                  role="button"
                  tabIndex={0}
                  aria-expanded={expanded}
                  aria-label={expanded ? t('admin.users.expand.toggleCollapse') : t('admin.users.expand.toggleExpand')}
                  className="flex items-center gap-3 px-2 py-3 hover:bg-surface-container-low flex-wrap cursor-pointer select-none rounded-md focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                  onClick={() => setExpandedUserId(expanded ? null : u.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      setExpandedUserId(expanded ? null : u.id)
                    }
                  }}
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <img src={u.avatarUrl ?? 'https://i.pravatar.cc/64'} alt="" className="w-10 h-10 rounded-full object-cover shrink-0" />
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-on-surface">{u.displayName}</div>
                      <div className="text-sm text-on-surface-variant truncate">@{u.username} · {u.email}</div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0 flex-wrap justify-end">
                    {pending ? (
                      <span className="inline-flex items-center rounded-none border border-tertiary/40 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] bg-transparent text-tertiary">
                        {t('admin.users.expand.pendingBadge')}
                      </span>
                    ) : null}
                    <RoleBadge role={u.primaryRole} label={u.primaryRole === 'ADMIN' ? t('admin.filters.roles.admin') : t('admin.filters.roles.user')} />
                    <SellerStatusBadge status={u.sellerStatus} />
                    <ExpandRowToggle
                      expanded={expanded}
                      onToggle={() => setExpandedUserId(expanded ? null : u.id)}
                      labelExpand={t('admin.users.expand.toggleExpand')}
                      labelCollapse={t('admin.users.expand.toggleCollapse')}
                    />
                  </div>
                </div>
                {expanded ? (
                  <AdminUserExpandPanel
                    userId={u.id}
                    summary={u}
                    refreshSignal={listRefreshSignal}
                    currentUserId={currentUserId}
                    onPromote={() => setPromoteTarget(u)}
                    onGrantSeller={() => setGrantSellerTarget(u)}
                    onDeactivateRequest={() => setDeactivateTarget(u)}
                    onReactivate={() => void runMutation(() => reactivateUser(u.id), t('admin.users.toastReactivated'))}
                    onApproveRequest={(appId, username) => setDecision({ mode: 'approve', appId, applicantUsername: username })}
                    onRejectRequest={(appId, username) => setDecision({ mode: 'reject', appId, applicantUsername: username })}
                    onListRefetch={() => void refetchListAndBumpPanel()}
                  />
                ) : null}
              </li>
            )
          })}
        </ul>
      )}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between mt-6">
          <Button
            variant="secondary"
            type="button"
            className="!p-3 min-w-[2.75rem]"
            disabled={data.number === 0}
            aria-label={t('admin.users.previous')}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            <ChevronLeft size={20} strokeWidth={2} className="shrink-0" aria-hidden />
          </Button>
          <span className="text-sm text-on-surface-variant">{t('admin.users.page', { current: data.number + 1, total: data.totalPages })}</span>
          <Button
            variant="secondary"
            type="button"
            className="!p-3 min-w-[2.75rem]"
            disabled={data.number + 1 >= data.totalPages}
            aria-label={t('admin.users.next')}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={20} strokeWidth={2} className="shrink-0" aria-hidden />
          </Button>
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
            setToast(t('admin.users.toastApproved'))
          } else {
            await rejectApplication(decision.appId, reason)
            setToast(t('admin.users.toastRejected'))
          }
          await refetchListAndBumpPanel()
        }}
      />

      <ConfirmModal
        open={deactivateTarget != null}
        title={t('admin.users.deactivateConfirmTitle')}
        message={deactivateTarget ? t('admin.users.deactivateConfirmBody', { name: deactivateTarget.displayName }) : undefined}
        destructive
        confirmLabel={t('admin.users.deactivate')}
        busy={deactivateBusy}
        onCancel={() => {
          if (!deactivateBusy) setDeactivateTarget(null)
        }}
        onConfirm={async () => {
          if (!deactivateTarget) return
          setDeactivateBusy(true)
          try {
            await deactivateUser(deactivateTarget.id)
            setToast(t('admin.users.toastDeactivated'))
            setDeactivateTarget(null)
            await refetchListAndBumpPanel()
          } catch (e) {
            const msg = e instanceof Error ? e.message : t('admin.users.actionFailed')
            setToast(msg === 'SELF_DEACTIVATE' ? t('admin.users.selfDeactivate') : msg)
          } finally {
            setDeactivateBusy(false)
          }
        }}
      />

      <ConfirmModal
        open={promoteTarget != null}
        title={t('admin.users.promoteAdminConfirmTitle')}
        message={promoteTarget ? t('admin.users.promoteAdminConfirmBody', { name: promoteTarget.displayName }) : undefined}
        confirmLabel={t('admin.users.promoteAdmin')}
        busy={promoteBusy}
        onCancel={() => {
          if (!promoteBusy) setPromoteTarget(null)
        }}
        onConfirm={async () => {
          if (!promoteTarget) return
          setPromoteBusy(true)
          try {
            await promoteToAdmin(promoteTarget.id)
            setToast(t('admin.users.toastPromoted'))
            setPromoteTarget(null)
            await refetchListAndBumpPanel()
          } catch (e) {
            const msg = e instanceof Error ? e.message : t('admin.users.actionFailed')
            setToast(msg)
          } finally {
            setPromoteBusy(false)
          }
        }}
      />

      <ConfirmModal
        open={grantSellerTarget != null}
        title={t('admin.users.grantSellerConfirmTitle')}
        message={grantSellerTarget ? t('admin.users.grantSellerConfirmBody', { name: grantSellerTarget.displayName }) : undefined}
        confirmLabel={t('admin.users.grantSeller')}
        busy={grantSellerBusy}
        onCancel={() => {
          if (!grantSellerBusy) setGrantSellerTarget(null)
        }}
        onConfirm={async () => {
          if (!grantSellerTarget) return
          setGrantSellerBusy(true)
          try {
            await grantSellerRole(grantSellerTarget.id)
            setToast(t('admin.users.toastGrantSeller'))
            setGrantSellerTarget(null)
            await refetchListAndBumpPanel()
          } catch (e) {
            const msg = e instanceof Error ? e.message : t('admin.users.actionFailed')
            setToast(msg)
          } finally {
            setGrantSellerBusy(false)
          }
        }}
      />
    </section>
  )
}
