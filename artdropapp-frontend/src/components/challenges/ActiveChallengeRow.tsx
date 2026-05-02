import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Swiper, SwiperSlide } from 'swiper/react'
import { FreeMode, Mousewheel, Keyboard, A11y } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/free-mode'
import { cloudinaryUrl } from '../../lib/cloudinary'
import type { Challenge, SubmissionThumbnail } from '../../types/challenge'

type ActiveChallengeRowProps = {
  challenge: Challenge
  bordered: boolean
}

const SLIDE_HEIGHT = 'h-[500px]'
const ASPECT_RATIOS = [3 / 4, 4 / 5, 1, 16 / 9, 4 / 3, 1] as const
const PLACEHOLDER_RATIOS = [3 / 4, 1, 4 / 3, 4 / 5] as const
const PLACEHOLDER_HEIGHT_PX = 500

function SubmissionSlide({
  submission,
  ratio,
}: {
  submission: SubmissionThumbnail
  ratio: number
}) {
  const widthPx = Math.round(PLACEHOLDER_HEIGHT_PX * ratio)
  return (
    <Link
      to={`/details/${submission.artworkId}`}
      style={{ aspectRatio: ratio }}
      className={`relative overflow-hidden group block ${SLIDE_HEIGHT}`}
      aria-label={`View details for ${submission.title}`}
    >
      <img
        alt={submission.imageAlt}
        src={cloudinaryUrl(submission.imageUrl, { width: widthPx * 2 })}
        loading="lazy"
        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
      />
      <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end p-6">
        <div className="text-white">
          <p className="font-headline text-xl">{submission.title}</p>
          {submission.artistDisplayName ? (
            <p className="text-xs font-light text-white/80">
              by {submission.artistDisplayName}
            </p>
          ) : null}
        </div>
      </div>
    </Link>
  )
}

function PlaceholderSlide({ ratio }: { ratio: number }) {
  return (
    <div
      style={{ aspectRatio: ratio }}
      className={`relative overflow-hidden bg-surface-container-high ${SLIDE_HEIGHT}`}
    >
      <div className="absolute inset-0 flex items-center justify-center text-on-surface-variant/50 italic font-headline">
        Awaiting submissions
      </div>
    </div>
  )
}

export function ActiveChallengeRow({ challenge, bordered }: ActiveChallengeRowProps) {
  const submissions = challenge.submissions
  const showPlaceholders = submissions.length === 0
  const slides = showPlaceholders
    ? PLACEHOLDER_RATIOS.map((ratio, i) => (
        <SwiperSlide key={`placeholder-${i}`} className="!w-auto">
          <PlaceholderSlide ratio={ratio} />
        </SwiperSlide>
      ))
    : submissions.map((submission, i) => (
        <SwiperSlide key={submission.submissionId} className="!w-auto">
          <SubmissionSlide
            submission={submission}
            ratio={ASPECT_RATIOS[i % ASPECT_RATIOS.length]}
          />
        </SwiperSlide>
      ))

  return (
    <section
      className={`max-w-[1600px] mx-auto py-16${
        bordered ? ' border-t border-surface-container-highest' : ''
      }`}
    >
      <div className="flex justify-between items-end mb-10 gap-6 px-8">
        <div className="min-w-0">
          <h2 className="text-4xl md:text-5xl font-headline mb-2">{challenge.title}</h2>
          {challenge.description ? (
            <p className="text-on-surface-variant font-light">{challenge.description}</p>
          ) : null}
        </div>
        <Link
          to={`/challenges/${challenge.id}`}
          className="shrink-0 flex items-center gap-2 text-xs uppercase tracking-widest font-bold text-on-surface hover:text-tertiary transition-colors"
        >
          View all
          <ArrowRight size={14} />
        </Link>
      </div>
      <Swiper
        modules={[FreeMode, Mousewheel, Keyboard, A11y]}
        freeMode={{ enabled: true, momentum: true }}
        mousewheel={{ forceToAxis: true }}
        keyboard={{ enabled: true }}
        slidesPerView="auto"
        spaceBetween={24}
        slidesOffsetBefore={32}
        slidesOffsetAfter={32}
        className="w-full pb-8"
      >
        {slides}
      </Swiper>
    </section>
  )
}
