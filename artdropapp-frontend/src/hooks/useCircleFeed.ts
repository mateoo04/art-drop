import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchCircleFeed } from '../api/feedApi'
import type { Artwork } from '../types/artwork'

export const CIRCLE_PAGE_SIZE = 20

export function useCircleFeed() {
  const query = useInfiniteQuery<Artwork[], Error>({
    queryKey: ['artworks', 'circle'],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchCircleFeed({ limit: CIRCLE_PAGE_SIZE, offset: pageParam as number }),
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length < CIRCLE_PAGE_SIZE ? undefined : allPages.length * CIRCLE_PAGE_SIZE,
  })

  return {
    artworks: query.data?.pages.flat() ?? [],
    isLoading: query.isLoading,
    isFetchingNextPage: query.isFetchingNextPage,
    error: query.error?.message ?? null,
    hasNextPage: query.hasNextPage ?? false,
    fetchNextPage: query.fetchNextPage,
    refetch: query.refetch,
  }
}
