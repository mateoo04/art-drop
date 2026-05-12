import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchSearchArtworks } from '../api/artworksApi'
import { qk } from '../lib/queryKeys'
import type { Artwork } from '../types/artwork'

export const SEARCH_PAGE_SIZE = 20

export function useSearchArtworks(query: string) {
  const trimmed = query.trim()
  const result = useInfiniteQuery<Artwork[], Error>({
    queryKey: qk.artworks.search(trimmed),
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchSearchArtworks(trimmed, SEARCH_PAGE_SIZE, pageParam as number),
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length < SEARCH_PAGE_SIZE ? undefined : allPages.length * SEARCH_PAGE_SIZE,
    enabled: trimmed.length >= 1,
    staleTime: 30_000,
  })

  return {
    items: result.data?.pages.flat() ?? [],
    isLoading: result.isLoading,
    isFetchingNextPage: result.isFetchingNextPage,
    error: result.error?.message ?? null,
    hasNextPage: result.hasNextPage ?? false,
    fetchNextPage: result.fetchNextPage,
  }
}
