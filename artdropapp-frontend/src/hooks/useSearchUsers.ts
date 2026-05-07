import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchSearchUsers } from '../api/usersApi'
import type { Artist } from '../types/artwork'
import { SEARCH_PAGE_SIZE } from './useSearchArtworks'

export function useSearchUsers(query: string) {
  const trimmed = query.trim()
  const result = useInfiniteQuery<Artist[], Error>({
    queryKey: ['search', 'users', trimmed],
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchSearchUsers(trimmed, SEARCH_PAGE_SIZE, pageParam as number),
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
