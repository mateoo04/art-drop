import { useEffect, useState } from 'react'
import { Button } from '../ui/Button'
import type { SellerStatus } from '../../types/seller'
import type { AdminUserFilters } from '../../api/adminApi'

type Props = {
  open: boolean
  onClose: () => void
  filters: AdminUserFilters
  onApply: (next: AdminUserFilters) => void
}

const STATUSES: SellerStatus[] = ['NONE', 'PENDING', 'APPROVED', 'REJECTED', 'REVOKED']

const SORTS: { value: NonNullable<AdminUserFilters['sort']>; label: string }[] = [
  { value: 'newest', label: 'Newest joined' },
  { value: 'oldest_pending', label: 'Pending oldest first' },
  { value: 'username', label: 'Username A–Z' },
  { value: 'most_artworks', label: 'Most artworks' },
]

export function AdminFiltersDrawer({ open, onClose, filters, onApply }: Props) {
  const [draft, setDraft] = useState<AdminUserFilters>(filters)

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

  function toggleStatus(s: SellerStatus) {
    const current = draft.sellerStatuses ?? []
    const next = current.includes(s) ? current.filter((x) => x !== s) : [...current, s]
    setDraft({ ...draft, sellerStatuses: next })
  }

  function reset() {
    setDraft({})
  }

  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex">
      <button
        type="button"
        aria-label="Close filters"
        tabIndex={-1}
        onClick={onClose}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <aside className="relative w-full max-w-sm h-full bg-surface-container-lowest border-r border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8 overflow-y-auto">
        <h2 className="font-display text-2xl text-on-surface mb-8">Filters</h2>

        <section className="mb-8">
          <h3 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
            Seller status
          </h3>
          <div className="flex flex-wrap gap-2">
            {STATUSES.map((s) => {
              const active = (draft.sellerStatuses ?? []).includes(s)
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => toggleStatus(s)}
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
            Role
          </h3>
          <div className="flex flex-wrap gap-2">
            {(['ROLE_USER', 'ROLE_ADMIN'] as const).map((r) => {
              const active = draft.role === r
              return (
                <button
                  key={r}
                  type="button"
                  onClick={() => setDraft({ ...draft, role: active ? null : r })}
                  className={
                    active
                      ? 'px-3 py-1.5 rounded-full bg-on-surface text-surface text-xs font-medium'
                      : 'px-3 py-1.5 rounded-full border border-outline-variant/30 text-on-surface-variant text-xs hover:bg-surface-container-low'
                  }
                >
                  {r === 'ROLE_USER' ? 'User' : 'Admin'}
                </button>
              )
            })}
          </div>
        </section>

        <section className="mb-8">
          <h3 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
            Sort
          </h3>
          <div className="flex flex-col gap-2">
            {SORTS.map((opt) => {
              const active = (draft.sort ?? 'newest') === opt.value
              return (
                <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="sort"
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
          <Button variant="ghost" onClick={reset}>Reset</Button>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={onClose}>Cancel</Button>
            <Button onClick={() => { onApply(draft); onClose() }}>Apply</Button>
          </div>
        </div>
      </aside>
    </div>
  )
}
