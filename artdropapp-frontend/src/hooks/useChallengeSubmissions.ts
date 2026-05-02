import { useInfiniteQuery } from '@tanstack/react-query'
import { fetchChallengeSubmissions, type SubmissionSort } from '../api/challengesApi'
import type { SubmissionThumbnail } from '../types/challenge'

export const SUBMISSIONS_PAGE_SIZE = 24

export function useChallengeSubmissions(
  challengeId: number | null,
  sort: SubmissionSort,
) {
  const enabled = challengeId != null
  const query = useInfiniteQuery<SubmissionThumbnail[], Error>({
    queryKey: ['challenge-submissions', challengeId, sort],
    enabled,
    initialPageParam: 0,
    queryFn: ({ pageParam }) =>
      fetchChallengeSubmissions(challengeId as number, {
        sort,
        limit: SUBMISSIONS_PAGE_SIZE,
        offset: pageParam as number,
      }),
    getNextPageParam: (lastPage, allPages) =>
      lastPage.length < SUBMISSIONS_PAGE_SIZE
        ? undefined
        : allPages.length * SUBMISSIONS_PAGE_SIZE,
  })

  return {
    submissions: query.data?.pages.flat() ?? [],
    isLoading: query.isLoading,
    isFetchingNextPage: query.isFetchingNextPage,
    error: query.error?.message ?? null,
    hasNextPage: query.hasNextPage ?? false,
    fetchNextPage: query.fetchNextPage,
  }
}
