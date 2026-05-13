import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { createArtwork } from '../api/artworksApi'
import { fetchChallenge } from '../api/challengesApi'
import { ArtworkForm, type ArtworkFormSubmitValues } from '../components/artwork/ArtworkForm'
import type { Challenge } from '../types/challenge'

export function ArtworkDropPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const rawChallengeId = searchParams.get('challengeId')
  const challengeId =
    rawChallengeId != null && Number.isFinite(Number(rawChallengeId))
      ? Number(rawChallengeId)
      : null
  const [challenge, setChallenge] = useState<Challenge | null>(null)
  const [challengeError, setChallengeError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (challengeId == null) {
      setChallenge(null)
      setChallengeError(null)
      return
    }
    let cancelled = false
    fetchChallenge(challengeId)
      .then((c) => {
        if (cancelled) return
        setChallenge(c)
        setChallengeError(c.status !== 'ACTIVE' ? t('artwork.drop.challengeContext.notActive') : null)
      })
      .catch(() => {
        if (!cancelled) setChallengeError(t('artwork.drop.challengeContext.notFound'))
      })
    return () => {
      cancelled = true
    }
  }, [challengeId, t])

  async function handleSubmit(values: ArtworkFormSubmitValues) {
    setMessage(null)
    if (challengeId != null && challengeError != null) {
      setMessage(challengeError)
      return
    }
    try {
      const created = await createArtwork({
        title: values.title,
        medium: values.medium,
        description: values.description || undefined,
        images: values.images ?? [],
        width: values.width,
        height: values.height,
        depth: values.depth,
        dimensionUnit: values.dimensionUnit,
        progressStatus: values.progressStatus,
        tags: values.tags.length > 0 ? values.tags : undefined,
        price: values.price,
        saleType: values.saleType,
        editionSize: values.editionSize,
        challengeId: challengeId ?? undefined,
      })
      if (challengeId != null) {
        navigate(`/challenges/${challengeId}`)
      } else if (created?.id) {
        navigate(`/details/${created.id}`)
      } else {
        navigate('/')
      }
    } catch (err) {
      if (err instanceof Error && err.message === 'TITLE_TAKEN') {
        throw new Error(t('artwork.drop.error.titleTaken'))
      }
      if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        throw new Error(t('artwork.drop.error.forbiddenSale'))
      }
      if (err instanceof Error && err.message === 'UNAUTHENTICATED') {
        throw new Error(t('artwork.drop.error.unauthenticated'))
      }
      if (err instanceof Error && err.message === 'CHALLENGE_NOT_ACTIVE') {
        throw new Error(t('artwork.drop.challengeContext.notActive'))
      }
      if (err instanceof Error && err.message === 'CHALLENGE_NOT_FOUND') {
        throw new Error(t('artwork.drop.challengeContext.notFound'))
      }
      if (err instanceof Error && err.message === 'CHALLENGE_USER_ALREADY_HAS_ENTRY') {
        throw new Error(t('artwork.drop.challengeContext.alreadyEntered'))
      }
      throw err
    }
  }

  return (
    <ArtworkForm
      formId="drop-form"
      title={t('artwork.drop.title')}
      subtitle={t('artwork.drop.subtitle')}
      backTo="/"
      backLabel={t('artwork.drop.cancelLabel')}
      cancelLabel={t('artwork.drop.cancel')}
      submitLabel={t('artwork.drop.submit')}
      submittingLabel={t('artwork.drop.submitting')}
      submitDisabled={challengeId != null && challengeError != null}
      externalMessage={message}
      notice={
        challenge != null ? (
          <div
            className={`mb-8 border px-4 py-3 ${
              challengeError
                ? 'border-error/40 bg-error-container/10 text-error'
                : 'border-tertiary/30 bg-tertiary-container/30 text-on-tertiary-container'
            }`}
            role="status"
          >
            <span className="font-label text-[10px] uppercase tracking-[0.15em] block mb-1">
              {t('artwork.drop.challengeContext.label')}
            </span>
            <p className="font-display text-lg leading-snug">{challenge.title}</p>
            {challengeError ? (
              <p className="font-body text-xs mt-2">{challengeError}</p>
            ) : null}
          </div>
        ) : null
      }
      onCancel={() => navigate('/')}
      onSubmit={handleSubmit}
    />
  )
}
