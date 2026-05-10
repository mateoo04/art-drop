import { useMutation, useQueryClient } from '@tanstack/react-query'
import { submitArtworkToChallenge } from '../api/challengesApi'

export function useSubmitArtworkToChallenge() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, { challengeId: number; artworkId: number }>({
    mutationFn: ({ challengeId, artworkId }) =>
      submitArtworkToChallenge(challengeId, artworkId),

    onSuccess: (_data, { challengeId, artworkId }) => {
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
