import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useEligibleChallengesForArtwork } from '../hooks/useEligibleChallengesForArtwork'
import { useSubmitArtworkToChallenge } from '../hooks/useSubmitArtworkToChallenge'
import { translateChallengeSubmitError } from '../lib/challengeErrors'
import type { Artwork } from '../types/artwork'
import { Button } from './ui/Button'
import { Spinner } from './ui/Spinner'

type Props = {
  open: boolean
  onClose: () => void
  artwork: Artwork
  onSubmitted?: () => void
}

export function SubmitToChallengeModal({ open, onClose, artwork, onSubmitted }: Props) {
  const { t } = useTranslation()
  const { data: eligible = [], isLoading, error: loadError } = useEligibleChallengesForArtwork(
    artwork.id,
    { enabled: open },
  )
  const submitMutation = useSubmitArtworkToChallenge()
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  function handleSubmit(challengeId: number) {
    if (submitMutation.isPending) return
    setError(null)
    setPendingId(challengeId)
    submitMutation.mutate(
      { challengeId, artworkId: artwork.id },
      {
        onSuccess: () => {
          onSubmitted?.()
          onClose()
        },
        onError: (e) => setError(translateChallengeSubmitError(e.message, t)),
        onSettled: () => setPendingId(null),
      },
    )
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="submit-to-challenge-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      <button
        type="button"
        aria-label={t('common.close')}
        tabIndex={-1}
        onClick={() => {
          if (!submitMutation.isPending) onClose()
        }}
        className="absolute inset-0 bg-inverse-surface/40 backdrop-blur-sm cursor-default"
      />
      <div className="relative w-full max-w-md max-h-[90vh] overflow-y-auto bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2 id="submit-to-challenge-modal-title" className="font-display text-2xl text-on-surface mb-2">
          {t('artwork.detail.submitToChallenge.title')}
        </h2>
        <p className="font-body text-sm text-on-surface-variant leading-relaxed mb-8">
          {t('artwork.detail.submitToChallenge.subtitle')}
        </p>

        {isLoading ? (
          <div className="py-8 flex justify-center">
            <Spinner label={t('artwork.detail.submitToChallenge.loading')} />
          </div>
        ) : loadError ? (
          <p className="font-body text-sm text-error" role="alert">
            {t('artwork.detail.submitToChallenge.errorLoad')}
          </p>
        ) : eligible.length === 0 ? (
          <p className="font-body text-sm text-on-surface-variant">
            {t('artwork.detail.submitToChallenge.noEligible')}
          </p>
        ) : (
          <ul className="space-y-3">
            {eligible.map((challenge) => (
              <li key={challenge.id}>
                <button
                  type="button"
                  onClick={() => handleSubmit(challenge.id)}
                  disabled={submitMutation.isPending}
                  className="w-full text-left p-4 border border-outline-variant/30 hover:border-on-surface transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                >
                  <p className="font-display text-lg text-on-surface">{challenge.title}</p>
                  {challenge.description ? (
                    <p className="font-body text-xs text-on-surface-variant mt-1 line-clamp-2">
                      {challenge.description}
                    </p>
                  ) : null}
                  {pendingId === challenge.id ? (
                    <p className="font-label text-[10px] text-on-surface-variant mt-2">
                      {t('artwork.detail.submitToChallenge.submitting')}
                    </p>
                  ) : null}
                </button>
              </li>
            ))}
          </ul>
        )}
        {error ? (
          <p className="mt-4 text-sm text-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="flex justify-end gap-3 mt-8">
          <Button variant="secondary" onClick={onClose} disabled={submitMutation.isPending}>
            {t('common.close')}
          </Button>
        </div>
      </div>
    </div>
  )
}
