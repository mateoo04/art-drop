import { User } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useSearchUsers } from '../../hooks/useSearchUsers'
import { InfiniteScrollSentinel } from '../home/InfiniteScrollSentinel'

type Props = { query: string }

export function SearchArtistsTab({ query }: Props) {
  const { t } = useTranslation()
  const { items, isLoading, isFetchingNextPage, error, hasNextPage, fetchNextPage } =
    useSearchUsers(query)

  if (isLoading) {
    return (
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-hidden>
        {Array.from({ length: 6 }).map((_, i) => (
          <li
            key={i}
            className="flex animate-pulse items-center gap-4 bg-surface-container-high p-4 ring-1 ring-outline-variant/10"
          >
            <div className="size-12 shrink-0 rounded-full bg-surface-container-highest" />
            <div className="h-4 w-32 bg-surface-container-highest" />
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
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((artist) => (
          <li key={artist.id}>
            <Link
              to={`/u/${artist.slug}`}
              className="group flex items-center gap-4 bg-surface-container-low p-4 outline-none ring-1 ring-outline-variant/10 transition-colors hover:bg-surface-container hover:ring-outline-variant/30 focus-visible:ring-2 focus-visible:ring-primary"
            >
              {artist.avatarUrl ? (
                <img
                  src={artist.avatarUrl}
                  alt=""
                  className="size-12 shrink-0 rounded-full object-cover"
                />
              ) : (
                <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-surface-container-highest text-on-surface">
                  <User size={20} aria-hidden />
                </span>
              )}
              <div className="min-w-0 flex-1">
                <p className="truncate text-base font-medium text-on-surface">
                  {artist.displayName}
                </p>
                <p className="truncate text-xs text-on-surface-variant">@{artist.slug}</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
      <InfiniteScrollSentinel
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      />
    </>
  )
}
