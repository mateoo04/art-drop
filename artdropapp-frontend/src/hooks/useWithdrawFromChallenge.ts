import { useMutation, useQueryClient } from '@tanstack/react-query'
import { withdrawArtworkFromChallenge } from '../api/challengesApi'
import { qk } from '../lib/queryKeys'
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
      const key = qk.artworks.detail(artworkId)
      await queryClient.cancelQueries({ queryKey: key })
      const previous = queryClient.getQueryData<Artwork>(key)
      if (previous) {
        queryClient.setQueryData<Artwork>(key, { ...previous, currentSubmission: null })
      }
      return { previous }
    },

    onError: (_err, { artworkId }, context) => {
      if (context?.previous) {
        queryClient.setQueryData(qk.artworks.detail(artworkId), context.previous)
      }
    },

    onSettled: (_data, _err, { artworkId, challengeId }) => {
      void queryClient.invalidateQueries({ queryKey: qk.artworks.detail(artworkId) })
      void queryClient.invalidateQueries({ queryKey: qk.challenges.detail(challengeId) })
      void queryClient.invalidateQueries({ queryKey: qk.challenges.submissions(challengeId) })
      void queryClient.invalidateQueries({
        queryKey: qk.challenges.eligibleArtworks(challengeId),
      })
      void queryClient.invalidateQueries({
        queryKey: qk.artworks.eligibleChallenges(artworkId),
      })
    },
  })
}
