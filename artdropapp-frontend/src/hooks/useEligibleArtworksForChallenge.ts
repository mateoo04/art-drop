import { useQuery } from '@tanstack/react-query'
import { fetchEligibleArtworksForChallenge } from '../api/artworksApi'
import type { Artwork } from '../types/artwork'

export function useEligibleArtworksForChallenge(
  challengeId: number | null,
  options: { enabled?: boolean } = {},
) {
  const enabled = (options.enabled ?? true) && challengeId != null
  return useQuery<Artwork[], Error>({
    queryKey: ['challenges', 'eligible-artworks', challengeId],
    queryFn: () => fetchEligibleArtworksForChallenge(challengeId as number),
    enabled,
  })
}
