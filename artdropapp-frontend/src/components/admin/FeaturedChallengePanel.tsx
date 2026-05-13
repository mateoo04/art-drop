import { useState } from 'react'
import type { AdminChallengeRow } from '../../api/adminApi'
import { useFeaturedChallenge } from '../../hooks/useFeaturedChallenge'
import { formatEuDateTime } from '../../lib/dateFormat'
import { FeaturedScheduleDialog, type ChallengePickerItem } from './FeaturedScheduleDialog'
import { Button } from '../ui/Button'

interface Props {
  allChallenges: AdminChallengeRow[]
}

export function FeaturedChallengePanel({ allChallenges }: Props) {
  const { state, loading, error, setCurrent, clearCurrent, schedule, clearPending } = useFeaturedChallenge()
  const [picking, setPicking] = useState(false)
  const [scheduling, setScheduling] = useState(false)

  const pickerItems: ChallengePickerItem[] = allChallenges
    .filter(c => c.status !== 'ENDED')
    .map(c => ({ id: c.id, title: c.title }))

  if (loading && state == null) {
    return (
      <div className="border border-outline-variant/15 bg-surface-container-low p-4 mb-6">
        <p className="text-sm text-on-surface-variant">Loading featured challenge…</p>
      </div>
    )
  }

  return (
    <div className="border border-outline-variant/15 bg-surface-container-low p-5 mb-6">
      <h2 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
        Featured Challenge
      </h2>

      {error && (
        <p role="alert" className="text-error text-sm mb-3">{error}</p>
      )}

      {state != null && (
        <div className="space-y-3 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-on-surface-variant text-xs uppercase tracking-wide w-14 shrink-0">Current</span>
            <span className="text-on-surface font-medium">
              {state.current ? state.current.title : <em className="font-normal text-on-surface-variant">(none)</em>}
            </span>
            <div className="flex gap-2 ml-auto">
              <button
                type="button"
                onClick={() => setPicking(true)}
                className="text-xs text-on-surface underline hover:no-underline"
              >
                {state.current ? 'Change' : 'Set featured'}
              </button>
              {state.current && (
                <button
                  type="button"
                  onClick={() => { void clearCurrent() }}
                  className="text-xs text-on-surface-variant underline hover:no-underline"
                >
                  Clear
                </button>
              )}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <span className="text-on-surface-variant text-xs uppercase tracking-wide w-14 shrink-0">Next</span>
            {state.next ? (
              <>
                <span className="text-on-surface font-medium">{state.next.title}</span>
                <span className="text-on-surface-variant text-xs">
                  {state.triggerType === 'AT_TIME' && state.triggerAt
                    ? `— at ${formatEuDateTime(state.triggerAt)}`
                    : '— when current ends'}
                </span>
                <button
                  type="button"
                  onClick={() => { void clearPending() }}
                  className="text-xs text-on-surface-variant underline hover:no-underline ml-auto"
                >
                  Cancel
                </button>
              </>
            ) : (
              <>
                <em className="text-on-surface-variant font-normal">(none)</em>
                <button
                  type="button"
                  onClick={() => setScheduling(true)}
                  className="text-xs text-on-surface underline hover:no-underline ml-auto"
                >
                  Schedule replacement
                </button>
              </>
            )}
          </div>
        </div>
      )}

      {picking && (
        <FeaturedPickerDialog
          challenges={pickerItems}
          currentId={state?.current?.id ?? null}
          onSubmit={async (id) => { await setCurrent(id); setPicking(false) }}
          onCancel={() => setPicking(false)}
        />
      )}

      {scheduling && (
        <FeaturedScheduleDialog
          challenges={pickerItems}
          currentId={state?.current?.id ?? null}
          currentEndsAt={state?.current?.endsAt ?? null}
          onSubmit={async (id, type, at) => { await schedule(id, type, at); setScheduling(false) }}
          onCancel={() => setScheduling(false)}
        />
      )}
    </div>
  )
}

function FeaturedPickerDialog({ challenges, currentId, onSubmit, onCancel }: {
  challenges: ChallengePickerItem[]
  currentId: number | null
  onSubmit: (id: number) => Promise<void>
  onCancel: () => void
}) {
  const [pick, setPick] = useState<number | ''>('')
  const [submitting, setSubmitting] = useState(false)
  const eligible = challenges.filter(c => c.id !== currentId)

  const handleSubmit = async () => {
    if (pick === '') return
    setSubmitting(true)
    try {
      await onSubmit(pick)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="featured-picker-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <button
        type="button"
        aria-label="Close"
        tabIndex={-1}
        onClick={onCancel}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2
          id="featured-picker-dialog-title"
          className="font-display text-2xl text-on-surface mb-5"
        >
          Set Featured Challenge
        </h2>
        <label className="block mb-5">
          <span className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
            Challenge
          </span>
          <select
            className="w-full mt-1 bg-surface-container-lowest border border-outline-variant/15 p-3 font-body text-sm text-on-surface focus:outline-none focus:border-on-surface transition-colors"
            value={pick}
            onChange={e => setPick(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">— pick one —</option>
            {eligible.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}
          </select>
        </label>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
          <Button
            onClick={() => { void handleSubmit() }}
            disabled={pick === '' || submitting}
            loading={submitting}
          >
            Set Featured
          </Button>
        </div>
      </div>
    </div>
  )
}
