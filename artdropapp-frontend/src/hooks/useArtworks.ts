import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchArtworks } from '../api/artworksApi'
import type { Artwork } from '../types/artwork'

export const DISCOVER_PAGE_SIZE = 20

export function useDiscoverFeed(medium: string | null) {
  const query = useInfiniteQuery<Artwork[], Error>({
    queryKey: ['artworks', 'discover', medium ?? 'All'],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchArtworks({
        medium: medium === 'All' ? null : medium,
        limit: DISCOVER_PAGE_SIZE,
        offset: pageParam as number,
      }),
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length < DISCOVER_PAGE_SIZE ? undefined : allPages.length * DISCOVER_PAGE_SIZE,
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
