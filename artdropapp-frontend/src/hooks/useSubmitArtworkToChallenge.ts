import { useMutation, useQueryClient } from '@tanstack/react-query'
import { submitArtworkToChallenge } from '../api/challengesApi'

type SubmitVars = { challengeId: number; artworkId: number }

export function useSubmitArtworkToChallenge() {
  const queryClient = useQueryClient()

  return useMutation<void, Error, SubmitVars>({
    mutationFn: ({ challengeId, artworkId }) =>
      submitArtworkToChallenge(challengeId, artworkId),

    onSuccess: (_data, { challengeId, artworkId }) => {
      void queryClient.invalidateQueries({ queryKey: ['artworks', 'detail', artworkId] })
      void queryClient.invalidateQueries({
        queryKey: ['challenges', 'eligible-artworks', challengeId],
      })
      void queryClient.invalidateQueries({
        queryKey: ['artworks', 'eligible-challenges', artworkId],
      })
    },
  })
}
