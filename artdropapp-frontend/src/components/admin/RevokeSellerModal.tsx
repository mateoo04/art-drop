import { useEffect, useState } from 'react'
import { fetchListedArtworkCount, revokeSeller } from '../../api/adminApi'
import { Button } from '../ui/Button'

type Props = {
  open: boolean
  userId: number
  username: string
  onClose: () => void
  onRevoked: (unlistedCount: number) => void
}

export function RevokeSellerModal({ open, userId, username, onClose, onRevoked }: Props) {
  const [reason, setReason] = useState('')
  const [count, setCount] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    setCount(null)
    fetchListedArtworkCount(userId)
      .then((c) => { if (!cancelled) setCount(c) })
      .catch(() => { if (!cancelled) setCount(0) })
    return () => { cancelled = true }
  }, [open, userId])

  if (!open) return null

  async function handleSubmit() {
    if (reason.trim().length === 0 || submitting) return
    setSubmitting(true)
    setError(null)
    try {
      const res = await revokeSeller(userId, reason.trim())
      onRevoked(res.unlistedCount)
      setReason('')
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Revoke failed')
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
        aria-label="Close"
        tabIndex={-1}
        onClick={() => {
          if (!submitting) onClose()
        }}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2 className="font-display text-2xl text-on-surface mb-3">Revoke seller status</h2>
        <p className="font-body text-sm text-on-surface-variant leading-relaxed mb-6">
          You're about to revoke <strong>@{username}</strong>'s seller status.{' '}
          {count == null
            ? 'Loading listed artwork count…'
            : count === 0
            ? 'No artworks are currently listed.'
            : `This will unlist ${count} artwork${count === 1 ? '' : 's'}.`}
        </p>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">
            Reason (required)
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
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={() => void handleSubmit()}
            loading={submitting}
            disabled={reason.trim().length === 0}
          >
            Revoke seller status
          </Button>
        </div>
      </div>
    </div>
  )
}
