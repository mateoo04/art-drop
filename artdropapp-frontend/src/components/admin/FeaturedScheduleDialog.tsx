import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { FeaturedTriggerType } from '../../api/featuredChallengeApi'
import { Button } from '../ui/Button'

export interface ChallengePickerItem {
  id: number
  title: string
}

interface Props {
  challenges: ChallengePickerItem[]
  currentId: number | null
  currentEndsAt: string | null
  onSubmit: (nextId: number, type: FeaturedTriggerType, at: string | null) => Promise<void>
  onCancel: () => void
}

export function FeaturedScheduleDialog({ challenges, currentId, currentEndsAt, onSubmit, onCancel }: Props) {
  const { t } = useTranslation()
  const [nextId, setNextId] = useState<number | ''>('')
  const [triggerType, setTriggerType] = useState<FeaturedTriggerType>('AT_TIME')
  const [triggerAt, setTriggerAt] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const eligible = challenges.filter(c => c.id !== currentId)
  const canUseWhenEnds = currentId !== null && currentEndsAt !== null

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !submitting) onCancel()
    }
    window.addEventListener('keydown', handler)
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', handler)
      document.body.style.overflow = prev
    }
  }, [submitting, onCancel])

  const submit = async () => {
    if (nextId === '') { setError(t('admin.featuredSchedule.errorPickChallenge')); return }
    if (triggerType === 'AT_TIME' && !triggerAt) { setError(t('admin.featuredSchedule.errorPickTime')); return }
    setSubmitting(true)
    setError(null)
    try {
      await onSubmit(
        nextId,
        triggerType,
        triggerType === 'AT_TIME' ? triggerAt : null,
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="featured-schedule-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <button
        type="button"
        aria-label={t('admin.featuredSchedule.closeLabel')}
        tabIndex={-1}
        onClick={() => { if (!submitting) onCancel() }}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2
          id="featured-schedule-dialog-title"
          className="font-display text-2xl text-on-surface mb-5"
        >
          {t('admin.featuredSchedule.title')}
        </h2>

        <div className="space-y-4">
          <label className="block">
            <span className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
              {t('admin.featuredSchedule.nextChallenge')}
            </span>
            <select
              className="w-full mt-1 bg-surface-container-lowest border border-outline-variant/15 p-3 font-body text-sm text-on-surface focus:outline-none focus:border-on-surface transition-colors"
              value={nextId}
              onChange={e => setNextId(e.target.value === '' ? '' : Number(e.target.value))}
            >
              <option value="">{t('admin.featuredSchedule.pickOne')}</option>
              {eligible.map(c => <option key={c.id} value={c.id}>{c.title}</option>)}
            </select>
          </label>

          <fieldset>
            <legend className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-2">
              {t('admin.featuredSchedule.whenToSwap')}
            </legend>
            <div className="space-y-2">
              <label className="flex items-center gap-2 text-sm text-on-surface cursor-pointer">
                <input
                  type="radio"
                  name="featured-trigger-type"
                  checked={triggerType === 'AT_TIME'}
                  onChange={() => setTriggerType('AT_TIME')}
                  className="accent-on-surface"
                />
                {t('admin.featuredSchedule.atSpecificTime')}
              </label>
              {triggerType === 'AT_TIME' && (
                <input
                  type="datetime-local"
                  className="ml-6 bg-surface-container-lowest border border-outline-variant/15 p-2 font-body text-sm text-on-surface focus:outline-none focus:border-on-surface transition-colors"
                  value={triggerAt}
                  onChange={e => setTriggerAt(e.target.value)}
                />
              )}
              <label
                className={[
                  'flex items-center gap-2 text-sm cursor-pointer',
                  canUseWhenEnds ? 'text-on-surface' : 'text-on-surface-variant opacity-50 cursor-not-allowed',
                ].join(' ')}
                title={canUseWhenEnds ? undefined : t('admin.featuredSchedule.requiresEndDate')}
              >
                <input
                  type="radio"
                  name="featured-trigger-type"
                  disabled={!canUseWhenEnds}
                  checked={triggerType === 'WHEN_CURRENT_ENDS'}
                  onChange={() => setTriggerType('WHEN_CURRENT_ENDS')}
                  className="accent-on-surface"
                />
                {t('admin.featuredSchedule.whenCurrentEnds')}
              </label>
            </div>
          </fieldset>
        </div>

        {error && (
          <p role="alert" className="text-error text-sm mt-3">
            {error}
          </p>
        )}

        <div className="flex justify-end gap-3 mt-6">
          <Button variant="secondary" onClick={onCancel} disabled={submitting}>
            {t('common.cancel')}
          </Button>
          <Button onClick={() => { void submit() }} loading={submitting} disabled={submitting}>
            {t('admin.featuredSchedule.schedule')}
          </Button>
        </div>
      </div>
    </div>
  )
}
