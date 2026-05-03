import { useState } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import {
  activateAdminChallenge,
  deactivateAdminChallenge,
  deleteAdminChallenge,
  type AdminChallengeFilters,
  type AdminChallengeRow,
} from '../../api/adminApi'
import { useAdminChallenges } from '../../hooks/useAdminChallenges'
import { AdminChallengeExpandPanel } from '../../components/admin/AdminChallengeExpandPanel'
import { ExpandRowToggle } from '../../components/admin/ExpandRowToggle'
import { AdminChallengeFiltersDrawer } from '../../components/admin/AdminChallengeFiltersDrawer'
import { ChallengeStatusBadge } from '../../components/admin/ChallengeStatusBadge'
import { Button } from '../../components/ui/Button'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { Input } from '../../components/ui/Input'
import { Spinner } from '../../components/ui/Spinner'

function formatRange(startsAt: string | null, endsAt: string | null) {
  const fmt = (s: string | null) => {
    if (!s) return '—'
    const d = new Date(s)
    return Number.isNaN(d.getTime()) ? s : d.toLocaleDateString()
  }
  return `${fmt(startsAt)} – ${fmt(endsAt)}`
}

function filterCount(f: AdminChallengeFilters): number {
  let n = 0
  if (f.status != null) n += 1
  if (f.sort && f.sort !== 'starts_desc') n += 1
  return n
}

export function AdminChallengesPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const size = 20
  const [filters, setFilters] = useState<AdminChallengeFilters>({})
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [expandedChallengeId, setExpandedChallengeId] = useState<number | null>(null)
  const [toast, setToast] = useState<string | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<AdminChallengeRow | null>(null)
  const [busy, setBusy] = useState(false)

  const { data, loading, error, refetch } = useAdminChallenges(query, page, size, filters)
  const fc = filterCount(filters)

  return (
    <section>
      <div className="flex gap-3 items-stretch mb-6 flex-wrap">
        <Input
          type="search"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(0) }}
          placeholder={t('admin.challenges.search')}
          className="flex-1 min-w-[12rem]"
        />
        <Button variant="secondary" onClick={() => setDrawerOpen(true)}>
          {fc > 0 ? t('admin.users.filtersWithCount', { count: fc }) : t('admin.users.filters')}
        </Button>
        <Button className="ml-auto shrink-0" onClick={() => navigate('/admin/challenges/new')}>
          {t('admin.challenges.create')}
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
        <p className="text-on-surface-variant italic">{t('admin.challenges.none')}</p>
      ) : (
        <ul className="space-y-4">
          {data.content.map((c) => {
            const expanded = expandedChallengeId === c.id
            return (
              <li
                key={c.id}
                className="border border-outline-variant/15 bg-surface-container-lowest flex flex-col overflow-hidden"
              >
                <div
                  role="button"
                  tabIndex={0}
                  aria-expanded={expanded}
                  aria-label={expanded ? t('admin.challenges.expand.toggleCollapse') : t('admin.challenges.expand.toggleExpand')}
                  className="p-4 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between cursor-pointer select-none rounded-md focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary"
                  onClick={() => setExpandedChallengeId(expanded ? null : c.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      setExpandedChallengeId(expanded ? null : c.id)
                    }
                  }}
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2 mb-1">
                      <h2 className="font-headline text-xl text-on-surface">{c.title}</h2>
                      <ChallengeStatusBadge status={c.status} />
                    </div>
                    {c.description ? (
                      <p className="text-sm text-on-surface-variant line-clamp-2 mb-2">{c.description}</p>
                    ) : null}
                    <p className="text-xs text-on-surface-variant">
                      {formatRange(c.startsAt, c.endsAt)}
                      {' · '}
                      {t('admin.challenges.submissionCount', { count: c.submissionCount })}
                    </p>
                  </div>
                  <ExpandRowToggle
                    className="self-start sm:self-center"
                    expanded={expanded}
                    onToggle={() => setExpandedChallengeId(expanded ? null : c.id)}
                    labelExpand={t('admin.challenges.expand.toggleExpand')}
                    labelCollapse={t('admin.challenges.expand.toggleCollapse')}
                  />
                </div>
                {expanded ? (
                  <AdminChallengeExpandPanel
                    challenge={c}
                    onDeleteRequest={() => setDeleteTarget(c)}
                    onActivate={async () => {
                      try {
                        await activateAdminChallenge(c.id)
                        setToast(t('admin.challenges.toastActivated'))
                        await refetch()
                      } catch (e) {
                        setToast(e instanceof Error ? e.message : t('admin.challenges.actionFailed'))
                      }
                    }}
                    onDeactivate={async () => {
                      try {
                        await deactivateAdminChallenge(c.id)
                        setToast(t('admin.challenges.toastDeactivated'))
                        await refetch()
                      } catch (e) {
                        setToast(e instanceof Error ? e.message : t('admin.challenges.actionFailed'))
                      }
                    }}
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

      <AdminChallengeFiltersDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        filters={filters}
        onApply={(next) => { setFilters(next); setPage(0) }}
      />

      <ConfirmModal
        open={deleteTarget != null}
        title={t('admin.challenges.deleteConfirmTitle')}
        message={deleteTarget ? t('admin.challenges.deleteConfirmBody', { title: deleteTarget.title }) : undefined}
        destructive
        confirmLabel={t('admin.challenges.delete')}
        busy={busy}
        onCancel={() => {
          if (!busy) setDeleteTarget(null)
        }}
        onConfirm={async () => {
          if (!deleteTarget) return
          setBusy(true)
          try {
            await deleteAdminChallenge(deleteTarget.id)
            setToast(t('admin.challenges.toastDeleted'))
            setDeleteTarget(null)
            setExpandedChallengeId((id) => (id === deleteTarget.id ? null : id))
            await refetch()
          } catch (e) {
            setToast(e instanceof Error ? e.message : t('admin.challenges.actionFailed'))
          } finally {
            setBusy(false)
          }
        }}
      />
    </section>
  )
}
