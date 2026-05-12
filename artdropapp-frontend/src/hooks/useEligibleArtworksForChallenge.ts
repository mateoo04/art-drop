import { useQuery } from '@tanstack/react-query'
import { fetchEligibleArtworksForChallenge } from '../api/artworksApi'
import { qk } from '../lib/queryKeys'
import type { Artwork } from '../types/artwork'

export function useEligibleArtworksForChallenge(
  challengeId: number | null,
  options: { enabled?: boolean } = {},
) {
  const enabled = (options.enabled ?? true) && challengeId != null
  return useQuery<Artwork[], Error>({
    queryKey: qk.challenges.eligibleArtworks(challengeId ?? 0),
    queryFn: () => fetchEligibleArtworksForChallenge(challengeId as number),
    enabled,
  })
}
