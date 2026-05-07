import { useTranslation } from 'react-i18next'
import { homeFeedItemsFromArtworks } from '../../api/artworksApi'
import { InfiniteScrollSentinel } from '../home/InfiniteScrollSentinel'
import { MasonryFeed } from '../home/MasonryFeed'
import { MasonryFeedSkeleton } from '../home/MasonryFeedSkeleton'
import { useSearchArtworks } from '../../hooks/useSearchArtworks'

type Props = { query: string }

export function SearchArtworksTab({ query }: Props) {
  const { t } = useTranslation()
  const { items, isLoading, isFetchingNextPage, error, hasNextPage, fetchNextPage } =
    useSearchArtworks(query)

  if (isLoading) return <MasonryFeedSkeleton />
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
      <MasonryFeed items={homeFeedItemsFromArtworks(items)} />
      <InfiniteScrollSentinel
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
        label={t('home.loadingMore')}
      />
    </>
  )
}
