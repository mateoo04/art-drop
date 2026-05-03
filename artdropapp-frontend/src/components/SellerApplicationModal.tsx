import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { submitApplication, type SubmitApplicationError } from '../api/sellerApi'
import { Button } from './ui/Button'

type Props = {
  open: boolean
  onClose: () => void
  onSubmitted: () => void
}

const MIN_LEN = 30
const MAX_LEN = 400

export function SellerApplicationModal({ open, onClose, onSubmitted }: Props) {
  const { t } = useTranslation()
  const [message, setMessage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [cooldownAt, setCooldownAt] = useState<string | null>(null)

  if (!open) return null

  const trimmed = message.trim()
  const valid = trimmed.length >= MIN_LEN && trimmed.length <= MAX_LEN

  async function handleSubmit() {
    if (!valid || submitting) return
    setSubmitting(true)
    setError(null)
    setCooldownAt(null)
    try {
      await submitApplication(trimmed)
      onSubmitted()
      setMessage('')
      onClose()
    } catch (raw) {
      const e = raw as SubmitApplicationError
      if (e.kind === 'COOLDOWN_ACTIVE') {
        setCooldownAt(e.canReapplyAt)
        setError(t('account.seller.modal.errorCooldown'))
      } else if (e.kind === 'ALREADY_PENDING') {
        setError(t('account.seller.modal.errorAlreadyPending'))
      } else if (e.kind === 'ALREADY_SELLER') {
        setError(t('account.seller.modal.errorAlreadySeller'))
      } else {
        setError(e.message ?? t('account.seller.modal.errorFallback'))
      }
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
        aria-label={t('account.seller.modal.closeLabel')}
        tabIndex={-1}
        onClick={() => {
          if (!submitting) onClose()
        }}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2 className="font-display text-2xl text-on-surface mb-3">{t('account.seller.modal.title')}</h2>
        <p className="font-body text-sm text-on-surface-variant leading-relaxed mb-6">
          {t('account.seller.modal.body', { min: MIN_LEN, max: MAX_LEN })}
        </p>
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          maxLength={MAX_LEN}
          rows={6}
          className="w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border border-outline-variant/15 focus:border-on-surface"
          placeholder={t('account.seller.modal.placeholder')}
        />
        <div className="text-xs text-on-surface-variant mt-1 text-right">
          {trimmed.length}/{MAX_LEN}
        </div>
        {error ? (
          <p className="mt-3 text-sm text-error" role="alert">
            {error}
            {cooldownAt ? ` ${t('account.seller.modal.cooldownSuffix', { date: new Date(cooldownAt).toLocaleDateString() })}` : ''}
          </p>
        ) : null}
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={() => void handleSubmit()}
            loading={submitting}
            disabled={!valid}
          >
            {t('account.seller.modal.submit')}
          </Button>
        </div>
      </div>
    </div>
  )
}
