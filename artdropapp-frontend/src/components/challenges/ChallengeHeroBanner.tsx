import { Timer } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
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
  backTo?: string
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
  backTo,
}: ChallengeHeroBannerProps) {
  const { t } = useTranslation()

  function statusLabel(c: Challenge): string {
    if (c.status === 'ENDED') return t('challenges.hero.status.past')
    if (c.kind === 'FEATURED') return t('challenges.hero.status.featured')
    if (c.status === 'UPCOMING') return t('challenges.hero.status.upcoming')
    return t('challenges.hero.status.active')
  }

  const heroSubmission = useMemo(
    () => pickHeroSubmission(challenge.submissions),
    [challenge.id, challenge.submissions],
  )

  const [remaining, setRemaining] = useState(() => getChallengeRemaining(challenge.endsAt))
  useEffect(() => {
    setRemaining(getChallengeRemaining(challenge.endsAt))
    const id = window.setInterval(() => {
      setRemaining(getChallengeRemaining(challenge.endsAt))
    }, 60_000)
    return () => window.clearInterval(id)
  }, [challenge.endsAt])

  return (
    <section className="w-full relative h-[600px] md:h-[800px] flex items-end overflow-hidden">
      {backTo ? (
        <BackButton
          to={backTo}
          label={t('challenges.hero.backLabel')}
          tone="on-image"
          className="absolute top-8 left-8 z-20"
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
      <div className="relative z-10 w-full px-8 py-16 md:py-24 max-w-[1600px] mx-auto text-white">
        <div className="flex flex-col gap-6 max-w-3xl">
          <span className="inline-block px-3 py-1 bg-white/20 backdrop-blur-md text-white text-xs uppercase tracking-widest self-start mb-2">
            {statusLabel(challenge)}
          </span>
          <h1 className="text-6xl md:text-8xl font-headline tracking-tighter leading-[0.9]">
            {challenge.title}
          </h1>
          {challenge.description ? (
            <p className="text-lg md:text-xl text-white/90 font-light leading-relaxed mt-4">
              {challenge.description}
            </p>
          ) : null}
          {remaining ? (
            <div className="flex items-center gap-4 mt-4 bg-black/40 backdrop-blur-sm px-6 py-4 w-max border border-white/20">
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
          {challenge.status !== 'ENDED' || secondaryAction ? (
            <div className="flex items-center gap-6 mt-8">
              {challenge.status !== 'ENDED' ? (
                <button
                  type="button"
                  onClick={() =>
                    alert('Submitting artwork will be available once sign-in ships.')
                  }
                  className="bg-white text-black px-8 py-4 font-label uppercase tracking-widest text-xs font-bold hover:bg-surface-variant transition-colors"
                >
                  {t('challenges.hero.joinChallenge')}
                </button>
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
