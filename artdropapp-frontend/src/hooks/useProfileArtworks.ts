import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchProfileArtworks } from '../api/usersApi'
import type { Artwork } from '../types/artwork'

export const PROFILE_PAGE_SIZE = 20

export function useProfileArtworks(slug: string | undefined) {
  const result = useInfiniteQuery<Artwork[], Error>({
    queryKey: ['profile', 'artworks', slug ?? ''],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchProfileArtworks(slug ?? '', PROFILE_PAGE_SIZE, pageParam as number),
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length < PROFILE_PAGE_SIZE ? undefined : allPages.length * PROFILE_PAGE_SIZE,
    enabled: Boolean(slug),
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
