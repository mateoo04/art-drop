import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query'
import { likeArtwork, unlikeArtwork } from '../api/artworksApi'
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

function isInfiniteArtworkData(value: unknown): value is InfiniteData<Artwork[]> {
  return (
    typeof value === 'object' &&
    value !== null &&
    'pages' in value &&
    Array.isArray((value as InfiniteData<Artwork[]>).pages)
  )
}

export function useLikeArtwork() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, LikeVars, { snapshots: Array<[readonly unknown[], unknown]> }>({
    mutationFn: ({ artworkId, like }) =>
      like ? likeArtwork(artworkId) : unlikeArtwork(artworkId),

    onMutate: ({ artworkId, like }) => {
      const apply = (a: Artwork): Artwork =>
        a.id === artworkId
          ? {
              ...a,
              likedByMe: like,
              likeCount: Math.max(0, a.likeCount + (like ? 1 : -1)),
            }
          : a

      const snapshots = queryClient.getQueriesData<unknown>({ queryKey: ['artworks'] })

      queryClient.setQueriesData<unknown>({ queryKey: ['artworks'] }, (old: unknown) => {
        if (old == null) return old
        if (isInfiniteArtworkData(old)) {
          return { ...old, pages: old.pages.map((page) => page.map(apply)) }
        }
        if (Array.isArray(old)) {
          return (old as Artwork[]).map(apply)
        }
        if (isArtwork(old)) {
          return apply(old)
        }
        return old
      })

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
