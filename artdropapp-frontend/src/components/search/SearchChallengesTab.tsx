import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { formatChallengeDeadlineShort } from '../../lib/challengeTime'
import { useSearchChallenges } from '../../hooks/useSearchChallenges'
import type { Challenge } from '../../types/challenge'
import { InfiniteScrollSentinel } from '../home/InfiniteScrollSentinel'

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

type Props = { query: string }

export function SearchChallengesTab({ query }: Props) {
  const { t } = useTranslation()
  const { items, isLoading, isFetchingNextPage, error, hasNextPage, fetchNextPage } =
    useSearchChallenges(query)

  if (isLoading) {
    return (
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2" aria-hidden>
        {Array.from({ length: 4 }).map((_, i) => (
          <li
            key={i}
            className="flex h-28 animate-pulse overflow-hidden bg-surface-container-high ring-1 ring-outline-variant/10"
          >
            <div className="h-28 w-28 shrink-0 bg-surface-container-highest" />
            <div className="flex min-w-0 flex-1 flex-col justify-center gap-2 px-4">
              <div className="h-4 max-w-[11rem] bg-surface-container-highest" />
              <div className="h-3 max-w-[7rem] bg-surface-container-highest/70" />
            </div>
          </li>
        ))}
      </ul>
    )
  }
  if (error) {
    return (
      <p
        className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
        role="alert"
      >
        {t('search.error')}
      </p>
    )
  }
  if (items.length === 0) {
    return (
      <p className="py-24 text-center text-on-surface-variant italic">
        {t('search.page.empty', { query })}
      </p>
    )
  }
  return (
    <>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {items.map((challenge) => {
          const visual = cardVisual(challenge)
          const subtitle =
            formatChallengeDeadlineShort(t, challenge.endsAt) ??
            t('home.activeChallenges.timeLeft.openEnded')
          return (
            <li key={challenge.id}>
              <Link
                to={`/challenges/${challenge.id}`}
                className="group flex min-h-[7rem] overflow-hidden bg-[#1c1c1c] outline-none ring-1 ring-white/10 transition-[color,box-shadow] hover:ring-white/20 focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface"
              >
                <div className="relative h-[7rem] w-[7rem] shrink-0 overflow-hidden bg-zinc-800">
                  {visual ? (
                    <img
                      src={visual.src}
                      alt={visual.alt}
                      loading="lazy"
                      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                    />
                  ) : null}
                </div>
                <div className="flex min-w-0 flex-1 flex-col justify-center gap-1 px-4 py-3">
                  <p className="font-headline text-base font-bold leading-tight text-white line-clamp-2">
                    {challenge.title}
                  </p>
                  <p className="text-sm font-light text-white/65">{subtitle}</p>
                </div>
              </Link>
            </li>
          )
        })}
      </ul>
      <InfiniteScrollSentinel
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      />
    </>
  )
}
