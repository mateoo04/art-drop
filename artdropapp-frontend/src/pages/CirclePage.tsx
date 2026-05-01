import { Link } from 'react-router-dom'
import { InfiniteScrollSentinel } from '../components/home/InfiniteScrollSentinel'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { useCircleFeed } from '../hooks/useCircleFeed'

export function CirclePage() {
  const {
    artworks,
    isLoading,
    isFetchingNextPage,
    error,
    hasNextPage,
    fetchNextPage,
  } = useCircleFeed()

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <header className="pt-8 pb-12 border-b border-outline-variant/15">
        <h1 className="font-headline text-4xl text-on-surface">Your Circle</h1>
        <p className="font-body text-base text-on-surface-variant italic mt-2 max-w-xl">
          The latest drops from artists whose practice you're following.
        </p>
      </header>

      {isLoading ? (
        <p className="py-24 text-center text-on-surface-variant italic" role="status">
          Loading your Circle…
        </p>
      ) : error ? (
        <p
          className="py-24 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      ) : artworks.length === 0 ? (
        <section className="py-24 text-center">
          <p className="font-body text-base text-on-surface-variant italic max-w-xl mx-auto">
            Your Circle is quiet. Follow artists whose practice you want to see more of —
            their drops will show up here, newest first.
          </p>
          <Link
            to="/"
            className="inline-block mt-8 font-label text-[11px] uppercase tracking-[0.2em] text-on-surface border-b-2 border-on-surface pb-1 hover:text-outline hover:border-outline transition-colors"
          >
            Discover artists
          </Link>
        </section>
      ) : (
        <section className="pt-12">
          <MasonryFeed artworks={artworks} />
          <InfiniteScrollSentinel
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />
        </section>
      )}
    </main>
  )
}
