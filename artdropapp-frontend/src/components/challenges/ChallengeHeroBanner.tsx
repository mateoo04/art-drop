import { Timer } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { getChallengeRemaining } from '../../lib/challengeTime'
import type { Challenge, SubmissionThumbnail } from '../../types/challenge'
import { BackButton } from '../ui/BackButton'

type SecondaryAction = {
  label: string
  to?: string
  onClick?: () => void
}

type ChallengeHeroBannerProps = {
  challenge: Challenge
  secondaryAction?: SecondaryAction
  showBack?: boolean
  onJoin?: () => void
}

function pickHeroSubmission(
  submissions: SubmissionThumbnail[],
): SubmissionThumbnail | null {
  if (submissions.length === 0) return null
  const pool = submissions.slice(0, Math.min(submissions.length, 6))
  return pool[Math.floor(Math.random() * pool.length)]
}

export function ChallengeHeroBanner({
  challenge,
  secondaryAction,
  showBack = true,
  onJoin,
}: ChallengeHeroBannerProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const handleBack = () => {
    if (location.key === 'default') {
      navigate('/challenges')
    } else {
      navigate(-1)
    }
  }

  function statusLabel(c: Challenge): string {
    if (c.status === 'ENDED') return t('challenges.hero.status.past')
    if (c.isFeatured) return t('challenges.hero.status.featured')
    if (c.status === 'UPCOMING') return t('challenges.hero.status.upcoming')
    return t('challenges.hero.status.active')
  }

  const heroSubmission = useMemo(
    () => pickHeroSubmission(challenge.submissions),
    [challenge.submissions],
  )

  const endsAt = challenge.endsAt
  const [remaining, setRemaining] = useState(() => getChallengeRemaining(endsAt))
  const [syncedEndsAt, setSyncedEndsAt] = useState(endsAt)
  const hasEnded = challenge.status === 'ENDED' || remaining?.ended === true
  const canJoin = challenge.status === 'ACTIVE' && !hasEnded
  const canViewEntry = challenge.viewerHasEntry && challenge.viewerEntryArtworkId != null
  if (syncedEndsAt !== endsAt) {
    setSyncedEndsAt(endsAt)
    setRemaining(getChallengeRemaining(endsAt))
  }

  useEffect(() => {
    const id = window.setInterval(() => {
      setRemaining(getChallengeRemaining(endsAt))
    }, 60_000)
    return () => window.clearInterval(id)
  }, [endsAt])

  return (
    <section className="w-full relative min-h-[calc(100svh-var(--app-header-height,0px))] md:h-[800px] md:min-h-0 flex items-end overflow-hidden">
      {showBack ? (
        <BackButton
          onClick={handleBack}
          label={t('challenges.hero.backLabel')}
          tone="on-image"
          className="absolute top-6 left-6 z-20 md:top-8 md:left-8"
        />
      ) : null}
      {heroSubmission ? (
        <>
          <img
            alt={heroSubmission.imageAlt}
            src={cloudinaryUrl(heroSubmission.imageUrl, { width: 1920 })}
            className="absolute inset-0 w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
        </>
      ) : (
        <div
          className="absolute inset-0"
          style={{
            background:
              'linear-gradient(135deg, #2d3435 0%, #5a6061 40%, #7d5731 80%, #fac898 100%)',
          }}
        />
      )}
      <div className="relative z-10 w-full px-6 py-12 md:px-8 md:py-24 max-w-[1600px] mx-auto text-white">
        <div className="flex flex-col gap-4 md:gap-6 max-w-3xl">
          <span className="inline-block px-3 py-1 bg-white/20 backdrop-blur-md text-white text-xs uppercase tracking-widest self-start">
            {statusLabel(challenge)}
          </span>
          <h1 className="text-5xl sm:text-6xl md:text-8xl font-headline tracking-tighter leading-[0.9]">
            {challenge.title}
          </h1>
          {challenge.description ? (
            <p className="text-base md:text-xl text-white/90 font-light leading-relaxed md:mt-4">
              {challenge.description}
            </p>
          ) : null}
          {remaining ? (
            <div className="flex items-center gap-4 mt-2 bg-black/40 backdrop-blur-sm px-6 py-4 w-max max-w-full border border-white/20">
              <Timer size={20} className="text-tertiary-container" />
              <div className="flex flex-col">
                <span className="text-xs font-bold uppercase tracking-widest text-white/70">
                  {remaining.ended ? t('challenges.hero.timeClosed') : t('challenges.hero.timeRemaining')}
                </span>
                {!remaining.ended ? (
                  <span className="text-lg font-headline text-white">
                    {t('challenges.hero.timeFormat', {
                      days: remaining.days,
                      hours: remaining.hours,
                      minutes: remaining.minutes,
                    })}
                  </span>
                ) : null}
              </div>
            </div>
          ) : null}
          {canJoin || canViewEntry || secondaryAction ? (
            <div className="flex flex-wrap items-center gap-4 md:gap-6 mt-2 md:mt-8">
              {canJoin || canViewEntry ? (
                canViewEntry ? (
                  <Link
                    to={`/details/${challenge.viewerEntryArtworkId!}`}
                    className="bg-white text-black px-8 py-4 font-label uppercase tracking-widest text-xs font-bold hover:bg-surface-variant transition-colors"
                  >
                    {t('challenges.hero.viewYourEntry')}
                  </Link>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      if (onJoin) {
                        onJoin()
                      } else {
                        alert('Open the challenge to join.')
                      }
                    }}
                    className="bg-white text-black px-8 py-4 font-label uppercase tracking-widest text-xs font-bold hover:bg-surface-variant transition-colors"
                  >
                    {t('challenges.hero.joinChallenge')}
                  </button>
                )
              ) : null}
              {secondaryAction ? (
                secondaryAction.to ? (
                  <Link
                    to={secondaryAction.to}
                    className="px-8 py-4 border border-white/50 text-white font-label uppercase tracking-widest text-xs font-bold hover:bg-white/10 transition-colors"
                  >
                    {secondaryAction.label}
                  </Link>
                ) : (
                  <button
                    type="button"
                    onClick={secondaryAction.onClick}
                    className="px-8 py-4 border border-white/50 text-white font-label uppercase tracking-widest text-xs font-bold hover:bg-white/10 transition-colors"
                  >
                    {secondaryAction.label}
                  </button>
                )
              ) : null}
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}
