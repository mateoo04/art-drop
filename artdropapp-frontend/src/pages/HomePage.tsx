import { useState } from 'react'
import { InfiniteScrollSentinel } from '../components/home/InfiniteScrollSentinel'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { MediumFilterBar } from '../components/home/MediumFilterBar'
import { useDiscoverFeed } from '../hooks/useArtworks'

const MEDIUMS = ['Digital', 'Sculpture', 'Mixed Media', 'Acrylic', 'Photography', 'Oil']

export function HomePage() {
  const [activeMedium, setActiveMedium] = useState<string>('All')
  const {
    artworks,
    isLoading,
    isFetchingNextPage,
    error,
    hasNextPage,
    fetchNextPage,
  } = useDiscoverFeed(activeMedium)

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <MediumFilterBar mediums={MEDIUMS} active={activeMedium} onChange={setActiveMedium} />

      {isLoading ? (
        <p className="py-12 text-center text-on-surface-variant italic" role="status">
          Loading artworks…
        </p>
      ) : error ? (
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      ) : artworks.length === 0 ? (
        <p className="py-24 text-center text-on-surface-variant italic">
          No artworks in this medium yet.
        </p>
      ) : (
        <>
          <MasonryFeed artworks={artworks} />
          <InfiniteScrollSentinel
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />
        </>
      )}
    </main>
  )
}
