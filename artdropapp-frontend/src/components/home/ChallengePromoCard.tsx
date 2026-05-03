import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { formatChallengeDeadlineShort } from '../../lib/challengeTime'
import type { Challenge } from '../../types/challenge'
import { Button } from '../ui/Button'

type ChallengePromoCardProps = {
  challenge: Challenge
}

function coverSrc(challenge: Challenge): string | null {
  if (challenge.coverImageUrl) {
    return cloudinaryUrl(challenge.coverImageUrl, {
      width: 720,
      height: 480,
      crop: 'fill',
      gravity: 'center',
    })
  }
  const first = challenge.submissions[0]
  if (first?.imageUrl) {
    return cloudinaryUrl(first.imageUrl, {
      width: 720,
      height: 480,
      crop: 'fill',
      gravity: 'center',
    })
  }
  return null
}

export function ChallengePromoCard({ challenge }: ChallengePromoCardProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const src = coverSrc(challenge)
  const deadline = formatChallengeDeadlineShort(t, challenge.endsAt)
  const detailHref = `/challenges/${challenge.id}`
  const count = challenge.submissionCount

  return (
    <article className="masonry-item overflow-hidden ring-1 ring-outline-variant/15">
      <div className="relative aspect-[5/3] w-full overflow-hidden bg-surface-container-high">
        {src ? (
          <img
            src={src}
            alt={challenge.title}
            className="absolute inset-0 h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div
            className="absolute inset-0 bg-gradient-to-br from-primary/40 via-tertiary/30 to-secondary/35"
            aria-hidden
          />
        )}
        <div className="absolute left-4 top-4 z-10">
          <span className="inline-block bg-surface/90 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-on-surface backdrop-blur-md">
            {t('home.challengePromo.submissionsCount', { count })}
          </span>
        </div>
      </div>
      <div className="bg-[#1A1A1A] px-4 pb-4 pt-4 text-on-surface">
        <h3 className="font-serif text-lg font-medium leading-snug tracking-tight text-white md:text-xl">
          {t('home.challengePromo.joinTitle', { title: challenge.title })}
        </h3>
        {challenge.description ? (
          <p className="mt-2 line-clamp-3 text-sm leading-relaxed text-zinc-400">
            {challenge.description}
          </p>
        ) : null}
        <div className="mt-4 flex items-end justify-between gap-3">
          <p className="min-w-0 text-xs text-zinc-500">
            {deadline ?? t('home.challengePromo.noDeadline')}
          </p>
          <Button
            type="button"
            variant="inverse"
            className="shrink-0 py-3 px-4"
            onClick={() => navigate(detailHref)}
          >
            {t('home.challengePromo.joinChallengeCta')}
          </Button>
        </div>
      </div>
    </article>
  )
}
