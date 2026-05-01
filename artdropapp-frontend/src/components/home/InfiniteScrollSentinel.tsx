import { useEffect } from 'react'
import { useInView } from 'react-intersection-observer'

type InfiniteScrollSentinelProps = {
  hasNextPage: boolean
  isFetchingNextPage: boolean
  onLoadMore: () => void
}

export function InfiniteScrollSentinel({
  hasNextPage,
  isFetchingNextPage,
  onLoadMore,
}: InfiniteScrollSentinelProps) {
  const { ref, inView } = useInView({ rootMargin: '600px 0px' })

  useEffect(() => {
    if (inView && hasNextPage && !isFetchingNextPage) {
      onLoadMore()
    }
  }, [inView, hasNextPage, isFetchingNextPage, onLoadMore])

  if (!hasNextPage) return null

  return (
    <div ref={ref} className="py-12 flex justify-center" aria-hidden={!isFetchingNextPage}>
      {isFetchingNextPage ? (
        <p className="font-body text-sm text-on-surface-variant italic" role="status">
          Loading more…
        </p>
      ) : null}
    </div>
  )
}
