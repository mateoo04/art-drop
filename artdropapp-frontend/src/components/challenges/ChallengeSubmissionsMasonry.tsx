import Masonry from 'react-masonry-css'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cloudinarySrcSet, cloudinaryUrl } from '../../lib/cloudinary'
import type { SubmissionThumbnail } from '../../types/challenge'

const BREAKPOINTS = {
  default: 3,
  1024: 3,
  768: 2,
  0: 1,
}

const CARD_WIDTHS = [240, 360, 480, 720, 960]
const CARD_SIZES = '(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw'

type ChallengeSubmissionsMasonryProps = {
  submissions: SubmissionThumbnail[]
}

export function ChallengeSubmissionsMasonry({
  submissions,
}: ChallengeSubmissionsMasonryProps) {
  const { t } = useTranslation()

  if (submissions.length === 0) {
    return (
      <div className="py-24 text-center text-on-surface-variant italic font-headline">
        {t('challenges.detail.noSubmissions')}
      </div>
    )
  }

  const byLabel = t('home.card.by')

  return (
    <div className="max-w-[1600px] mx-auto px-8">
      <Masonry
        breakpointCols={BREAKPOINTS}
        className="masonry-grid-mc"
        columnClassName="masonry-grid-mc__column"
      >
        {submissions.map((submission) => (
          <Link
            key={submission.submissionId}
            to={`/details/${submission.artworkId}`}
            className="block relative overflow-hidden bg-surface-container group"
            aria-label={`View details for ${submission.title}`}
          >
            <img
              alt={submission.imageAlt}
              src={cloudinaryUrl(submission.imageUrl, { width: 720 })}
              srcSet={cloudinarySrcSet(submission.imageUrl, CARD_WIDTHS)}
              sizes={CARD_SIZES}
              loading="lazy"
              className="w-full h-auto block group-hover:scale-105 transition-transform duration-700"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/0 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end p-6">
              <div className="text-white">
                <p className="font-headline text-xl">{submission.title}</p>
                {submission.artistDisplayName ? (
                  <p className="text-xs font-light text-white/80">
                    {byLabel} {submission.artistDisplayName}
                  </p>
                ) : null}
              </div>
            </div>
          </Link>
        ))}
      </Masonry>
    </div>
  )
}
