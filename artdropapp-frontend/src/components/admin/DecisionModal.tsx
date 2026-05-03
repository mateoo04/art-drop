import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'

type Props = {
  open: boolean
  mode: 'approve' | 'reject'
  applicantUsername: string
  onClose: () => void
  onConfirm: (reason: string) => Promise<void>
}

export function DecisionModal({ open, mode, applicantUsername, onClose, onConfirm }: Props) {
  const { t } = useTranslation()
  const [reason, setReason] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  const reasonRequired = mode === 'reject'
  const valid = !reasonRequired || reason.trim().length > 0

  async function handleSubmit() {
    if (!valid || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      await onConfirm(reason.trim())
      setReason('')
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : t('admin.decision.failed'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <button
        type="button"
        aria-label={t('common.close')}
        tabIndex={-1}
        onClick={() => {
          if (!submitting) onClose()
        }}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2 className="font-display text-2xl text-on-surface mb-3">
          {mode === 'approve' ? t('admin.decision.titleApprove') : t('admin.decision.titleReject')}
        </h2>
        <p className="font-body text-sm text-on-surface-variant leading-relaxed mb-6">
          {t('admin.decision.applicant')}: <strong>@{applicantUsername}</strong>
        </p>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">
            {t('admin.decision.reasonLabel')} {reasonRequired ? t('admin.decision.reasonRequired') : t('admin.decision.reasonOptional')}
          </span>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            rows={4}
            maxLength={500}
            className="w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border border-outline-variant/15 focus:border-on-surface"
          />
        </label>
        {error ? <p className="text-error mt-2 text-sm" role="alert">{error}</p> : null}
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            {t('admin.decision.cancel')}
          </Button>
          {mode === 'approve' ? (
            <Button
              onClick={() => void handleSubmit()}
              loading={submitting}
              disabled={!valid}
            >
              {t('admin.decision.approve')}
            </Button>
          ) : (
            <Button
              variant="destructive"
              onClick={() => void handleSubmit()}
              loading={submitting}
              disabled={!valid}
            >
              {t('admin.decision.reject')}
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}
