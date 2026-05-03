import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'
import type { AdminChallengeFilters } from '../../api/adminApi'

type Props = {
  open: boolean
  onClose: () => void
  filters: AdminChallengeFilters
  onApply: (next: AdminChallengeFilters) => void
}

const STATUSES: NonNullable<AdminChallengeFilters['status']>[] = ['UPCOMING', 'ACTIVE', 'ENDED']

export function AdminChallengeFiltersDrawer({ open, onClose, filters, onApply }: Props) {
  const { t } = useTranslation()
  const [draft, setDraft] = useState<AdminChallengeFilters>(filters)

  useEffect(() => {
    if (open) setDraft(filters)
  }, [open, filters])

  useEffect(() => {
    if (!open) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null

  const SORTS: { value: NonNullable<AdminChallengeFilters['sort']>; label: string }[] = [
    { value: 'starts_desc', label: t('admin.challenges.filters.sortStarts') },
    { value: 'title', label: t('admin.challenges.filters.sortTitle') },
  ]

  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex">
      <button
        type="button"
        aria-label={t('admin.filters.closeLabel')}
        tabIndex={-1}
        onClick={onClose}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <aside className="relative w-full max-w-sm h-full bg-surface-container-lowest border-r border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8 overflow-y-auto">
        <h2 className="font-display text-2xl text-on-surface mb-8">{t('admin.challenges.filters.title')}</h2>

        <section className="mb-8">
          <h3 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
            {t('admin.challenges.filters.status')}
          </h3>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setDraft({ ...draft, status: null })}
              className={
                draft.status == null
                  ? 'px-3 py-1.5 rounded-full bg-on-surface text-surface text-xs font-medium'
                  : 'px-3 py-1.5 rounded-full border border-outline-variant/30 text-on-surface-variant text-xs hover:bg-surface-container-low'
              }
            >
              {t('admin.challenges.filters.all')}
            </button>
            {STATUSES.map((s) => {
              const active = draft.status === s
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => setDraft({ ...draft, status: s })}
                  className={
                    active
                      ? 'px-3 py-1.5 rounded-full bg-on-surface text-surface text-xs font-medium'
                      : 'px-3 py-1.5 rounded-full border border-outline-variant/30 text-on-surface-variant text-xs hover:bg-surface-container-low'
                  }
                >
                  {s.charAt(0) + s.slice(1).toLowerCase()}
                </button>
              )
            })}
          </div>
        </section>

        <section className="mb-8">
          <h3 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
            {t('admin.filters.sort')}
          </h3>
          <div className="flex flex-col gap-2">
            {SORTS.map((opt) => {
              const active = (draft.sort ?? 'starts_desc') === opt.value
              return (
                <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="challengeSort"
                    value={opt.value}
                    checked={active}
                    onChange={() => setDraft({ ...draft, sort: opt.value })}
                  />
                  <span className="font-body text-sm text-on-surface">{opt.label}</span>
                </label>
              )
            })}
          </div>
        </section>

        <div className="flex justify-between gap-3 mt-10">
          <Button variant="ghost" onClick={() => setDraft({})}>{t('admin.filters.reset')}</Button>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={onClose}>{t('admin.filters.cancel')}</Button>
            <Button onClick={() => { onApply(draft); onClose() }}>{t('admin.filters.apply')}</Button>
          </div>
        </div>
      </aside>
    </div>
  )
}
