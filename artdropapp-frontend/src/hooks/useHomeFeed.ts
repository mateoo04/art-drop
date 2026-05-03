import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchHomeFeed, type HomeFeedPage } from '../api/artworksApi'

export const HOME_FEED_PAGE_SIZE = 20

export function useHomeFeed(medium: string | null) {
  const query = useInfiniteQuery<HomeFeedPage, Error>({
    queryKey: ['feed', 'home', medium ?? 'All'],
    initialPageParam: null as string | null,
    queryFn: ({ pageParam }) =>
      fetchHomeFeed({
        medium: medium === 'All' ? null : medium,
        cursor: pageParam as string | null,
        limit: HOME_FEED_PAGE_SIZE,
      }),
    getNextPageParam: (lastPage) =>
      lastPage.hasMore && lastPage.nextCursor ? lastPage.nextCursor : undefined,
  })

  return {
    items: query.data?.pages.flatMap((p) => p.items) ?? [],
    isLoading: query.isLoading,
    isFetchingNextPage: query.isFetchingNextPage,
    error: query.error?.message ?? null,
    hasNextPage: query.hasNextPage ?? false,
    fetchNextPage: query.fetchNextPage,
    refetch: query.refetch,
  }
}
