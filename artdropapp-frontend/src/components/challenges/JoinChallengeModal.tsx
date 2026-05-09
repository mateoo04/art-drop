import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useEligibleArtworksForChallenge } from '../../hooks/useEligibleArtworksForChallenge'
import { useSubmitArtworkToChallenge } from '../../hooks/useSubmitArtworkToChallenge'
import { translateChallengeSubmitError } from '../../lib/challengeErrors'
import { cloudinaryUrl } from '../../lib/cloudinary'
import type { Challenge } from '../../types/challenge'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'

type Props = {
  open: boolean
  onClose: () => void
  challenge: Challenge
  onSubmitted: () => void
}

export function JoinChallengeModal({ open, onClose, challenge, onSubmitted }: Props) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { data: eligible = [], isLoading, error: loadError } = useEligibleArtworksForChallenge(
    challenge.id,
    { enabled: open },
  )
  const submitMutation = useSubmitArtworkToChallenge()
  const [pendingId, setPendingId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  function handleSubmitExisting(artworkId: number) {
    if (submitMutation.isPending) return
    setError(null)
    setPendingId(artworkId)
    submitMutation.mutate(
      { challengeId: challenge.id, artworkId },
      {
        onSuccess: () => {
          onSubmitted()
          onClose()
        },
        onError: (e) => setError(translateChallengeSubmitError(e.message, t)),
        onSettled: () => setPendingId(null),
      },
    )
  }

  function handleCreateNew() {
    navigate(`/drop?challengeId=${challenge.id}`)
    onClose()
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="join-challenge-modal-title"
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
      <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto bg-surface-container-lowest border border-outline-variant/15 shadow-[0_20px_60px_rgba(45,52,53,0.18)] p-8">
        <h2 id="join-challenge-modal-title" className="font-display text-2xl text-on-surface mb-2">
          {t('challenges.join.title', { title: challenge.title })}
        </h2>
        <p className="font-body text-sm text-on-surface-variant leading-relaxed mb-8">
          {t('challenges.join.subtitle')}
        </p>

        <section className="mb-8 pb-8 border-b border-outline-variant/15">
          <h3 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-4">
            {t('challenges.join.createNewLabel')}
          </h3>
          <Button onClick={handleCreateNew} disabled={submitMutation.isPending}>
            {t('challenges.join.createNewCta')}
          </Button>
        </section>

        <section>
          <h3 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-4">
            {t('challenges.join.existingLabel')}
          </h3>
          {isLoading ? (
            <div className="py-8 flex justify-center">
              <Spinner label={t('challenges.join.loading')} />
            </div>
          ) : loadError ? (
            <p className="font-body text-sm text-error" role="alert">
              {t('challenges.join.errorLoad')}
            </p>
          ) : eligible.length === 0 ? (
            <p className="font-body text-sm text-on-surface-variant">
              {t('challenges.join.noEligible')}
            </p>
          ) : (
            <ul className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {eligible.map((artwork) => (
                <li key={artwork.id}>
                  <button
                    type="button"
                    onClick={() => handleSubmitExisting(artwork.id)}
                    disabled={submitMutation.isPending}
                    className="group block w-full text-left disabled:opacity-60 disabled:cursor-not-allowed"
                  >
                    <div className="aspect-square overflow-hidden bg-surface-container-low">
                      {artwork.coverPublicId ? (
                        <img
                          src={cloudinaryUrl(artwork.coverPublicId, { width: 400 })}
                          alt={artwork.imageAlt}
                          className="w-full h-full object-cover group-hover:opacity-90 transition-opacity"
                        />
                      ) : null}
                    </div>
                    <p className="mt-2 font-label text-xs text-on-surface truncate">
                      {artwork.title}
                    </p>
                    {pendingId === artwork.id ? (
                      <p className="font-label text-[10px] text-on-surface-variant mt-1">
                        {t('challenges.join.submitting')}
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
        </section>

        <div className="flex justify-end gap-3 mt-8">
          <Button variant="secondary" onClick={onClose} disabled={submitMutation.isPending}>
            {t('common.close')}
          </Button>
        </div>
      </div>
    </div>
  )
}
