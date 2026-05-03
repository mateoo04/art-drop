import { ChevronRight } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { formatChallengeDeadlineShort } from '../../lib/challengeTime'
import type { Challenge } from '../../types/challenge'

function sortActiveForHome(list: Challenge[]): Challenge[] {
  return [...list].sort((a, b) => {
    const af = a.kind === 'FEATURED' ? 0 : 1
    const bf = b.kind === 'FEATURED' ? 0 : 1
    if (af !== bf) return af - bf
    const ae = a.endsAt ? new Date(a.endsAt).getTime() : Number.POSITIVE_INFINITY
    const be = b.endsAt ? new Date(b.endsAt).getTime() : Number.POSITIVE_INFINITY
    return ae - be
  })
}

function cardVisual(challenge: Challenge): { src: string; alt: string } | null {
  if (challenge.coverImageUrl) {
    return {
      src: cloudinaryUrl(challenge.coverImageUrl, {
        width: 200,
        height: 200,
        crop: 'fill',
        gravity: 'center',
      }),
      alt: challenge.title,
    }
  }
  const first = challenge.submissions[0]
  if (first) {
    return {
      src: cloudinaryUrl(first.imageUrl, {
        width: 200,
        height: 200,
        crop: 'fill',
        gravity: 'center',
      }),
      alt: first.imageAlt || challenge.title,
    }
  }
  return null
}

type HomeActiveChallengesSectionProps = {
  challenges: Challenge[]
}

export function HomeActiveChallengesSection({ challenges }: HomeActiveChallengesSectionProps) {
  const { t } = useTranslation()
  const active = sortActiveForHome(challenges.filter((c) => c.status !== 'ENDED'))

  if (active.length === 0) return null

  return (
    <section className="mb-10" aria-labelledby="home-active-challenges-heading">
      <h2 id="home-active-challenges-heading" className="mb-4 w-fit max-w-full">
        <Link
          to="/challenges"
          className="group inline-flex items-center gap-1 rounded-sm font-body text-xs font-normal uppercase tracking-widest text-on-surface outline-none ring-offset-2 ring-offset-surface transition-colors hover:text-primary focus-visible:ring-2 focus-visible:ring-primary"
        >
          {t('home.activeChallenges.allChallenges')}
          <ChevronRight
            size={14}
            strokeWidth={2}
            className="shrink-0 transition-transform group-hover:translate-x-0.5"
            aria-hidden
          />
        </Link>
      </h2>
      <div className="-mx-8 flex snap-x snap-mandatory gap-4 overflow-x-auto px-8 pb-2 md:mx-0 md:px-0">
        {active.map((challenge) => {
          const visual = cardVisual(challenge)
          const subtitle =
            formatChallengeDeadlineShort(t, challenge.endsAt) ??
            t('home.activeChallenges.timeLeft.openEnded')

          return (
            <Link
              key={challenge.id}
              to={`/challenges/${challenge.id}`}
              className="group flex min-h-[7rem] w-[min(85vw,20rem)] shrink-0 snap-start overflow-hidden bg-[#1c1c1c] outline-none ring-1 ring-white/10 transition-[color,box-shadow] hover:ring-white/20 focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
            >
              <div className="relative h-[7rem] w-[7rem] shrink-0 overflow-hidden bg-zinc-800">
                {visual ? (
                  <img
                    src={visual.src}
                    alt={visual.alt}
                    loading="lazy"
                    className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                ) : (
                  <div
                    className="flex h-full w-full items-center justify-center text-center font-headline text-xs text-white/40"
                    aria-hidden
                  >
                    {challenge.theme ?? '—'}
                  </div>
                )}
              </div>
              <div className="flex min-w-0 flex-1 flex-col justify-center gap-1 px-4 py-3">
                <p className="font-headline text-base font-bold leading-tight text-white line-clamp-2">
                  {challenge.title}
                </p>
                <p className="text-sm font-light text-white/65">{subtitle}</p>
              </div>
            </Link>
          )
        })}
      </div>
    </section>
  )
}
