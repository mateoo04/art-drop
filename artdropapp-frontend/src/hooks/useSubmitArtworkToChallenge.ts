import { useMutation, useQueryClient } from '@tanstack/react-query'
import { submitArtworkToChallenge } from '../api/challengesApi'
import { qk } from '../lib/queryKeys'

export function useSubmitArtworkToChallenge() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, { challengeId: number; artworkId: number }>({
    mutationFn: ({ challengeId, artworkId }) =>
      submitArtworkToChallenge(challengeId, artworkId),

    onSuccess: (_data, { challengeId, artworkId }) => {
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
