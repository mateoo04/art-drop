import {
  useMutation,
  useQueryClient,
  type InfiniteData,
  type QueryFilters,
} from '@tanstack/react-query'
import { likeArtwork, unlikeArtwork, type HomeFeedPage } from '../api/artworksApi'
import { qk } from '../lib/queryKeys'
import type { Artwork } from '../types/artwork'

type LikeVars = { artworkId: number; like: boolean }

function isArtwork(value: unknown): value is Artwork {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    'likedByMe' in value &&
    'likeCount' in value
  )
}

function isInfiniteArtworkArrayData(value: unknown): value is InfiniteData<Artwork[]> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'pages' in value &&
    Array.isArray((value as InfiniteData<Artwork[]>).pages) &&
    (value as InfiniteData<Artwork[]>).pages.every((page) => Array.isArray(page))
  )
}

function isInfiniteHomeFeedData(value: unknown): value is InfiniteData<HomeFeedPage> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'pages' in value &&
    Array.isArray((value as InfiniteData<HomeFeedPage>).pages) &&
    (value as InfiniteData<HomeFeedPage>).pages.every((page) =>
      typeof page === 'object' &&
      page !== null &&
      'items' in page &&
      Array.isArray(page.items),
    )
  )
}

export function useLikeArtwork() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, LikeVars, { snapshots: Array<[readonly unknown[], unknown]> }>({
    mutationFn: ({ artworkId, like }) =>
      like ? likeArtwork(artworkId) : unlikeArtwork(artworkId),

    onMutate: async ({ artworkId, like }) => {
      const apply = (a: Artwork): Artwork =>
        a.id === artworkId
          ? {
              ...a,
              likedByMe: like,
              likeCount: Math.max(0, a.likeCount + (like ? 1 : -1)),
            }
          : a

      const updateCachedData = (old: unknown): unknown => {
        if (old == null) return old
        if (isInfiniteArtworkArrayData(old)) {
          return { ...old, pages: old.pages.map((page) => page.map(apply)) }
        }
        if (isInfiniteHomeFeedData(old)) {
          return {
            ...old,
            pages: old.pages.map((page) => ({
              ...page,
              items: page.items.map((item) =>
                item.kind === 'ARTWORK' ? { ...item, artwork: apply(item.artwork) } : item,
              ),
            })),
          }
        }
        if (Array.isArray(old) && old.every(isArtwork)) {
          return old.map(apply)
        }
        if (isArtwork(old)) {
          return apply(old)
        }
        return old
      }

      const cacheFilters: QueryFilters[] = [
        { queryKey: qk.artworks.all },
        { queryKey: qk.feed.all },
        { queryKey: qk.profile.all },
      ]

      await Promise.all(cacheFilters.map((filter) => queryClient.cancelQueries(filter)))

      const snapshots = cacheFilters.flatMap((filter) =>
        queryClient.getQueriesData<unknown>(filter),
      )

      for (const filter of cacheFilters) {
        queryClient.setQueriesData<unknown>(filter, updateCachedData)
      }

      return { snapshots }
    },

    onError: (_err, _vars, context) => {
      if (!context) return
      for (const [key, value] of context.snapshots) {
        queryClient.setQueryData(key, value)
      }
    },
  })
}
