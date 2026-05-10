import { useMutation, useQueryClient } from '@tanstack/react-query'
import { withdrawArtworkFromChallenge } from '../api/challengesApi'
import type { Artwork } from '../types/artwork'

export function useWithdrawFromChallenge() {
  const queryClient = useQueryClient()

  return useMutation<
    void,
    Error,
    { challengeId: number; artworkId: number },
    { previous: Artwork | undefined }
  >({
    mutationFn: ({ challengeId, artworkId }) =>
      withdrawArtworkFromChallenge(challengeId, artworkId),

    onMutate: async ({ artworkId }) => {
      const key = ['artworks', 'detail', artworkId] as const
      await queryClient.cancelQueries({ queryKey: key })
      const previous = queryClient.getQueryData<Artwork>(key)
      if (previous) {
        queryClient.setQueryData<Artwork>(key, { ...previous, currentSubmission: null })
      }
      return { previous }
    },

    onError: (_err, { artworkId }, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['artworks', 'detail', artworkId], context.previous)
      }
    },

    onSettled: (_data, _err, { artworkId, challengeId }) => {
      void queryClient.invalidateQueries({ queryKey: ['artworks', 'detail', artworkId] })
      void queryClient.invalidateQueries({ queryKey: ['challenges', 'detail', challengeId] })
      void queryClient.invalidateQueries({ queryKey: ['challenge-submissions', challengeId] })
      void queryClient.invalidateQueries({
        queryKey: ['challenges', 'eligible-artworks', challengeId],
      })
      void queryClient.invalidateQueries({
        queryKey: ['artworks', 'eligible-challenges', artworkId],
      })
    },
  })
}
