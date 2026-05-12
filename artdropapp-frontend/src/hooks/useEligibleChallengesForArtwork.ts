import { useQuery } from '@tanstack/react-query'
import { fetchEligibleChallengesForArtwork } from '../api/artworksApi'
import { qk } from '../lib/queryKeys'
import type { Challenge } from '../types/challenge'

export function useEligibleChallengesForArtwork(
  artworkId: number | null,
  options: { enabled?: boolean } = {},
) {
  const enabled = (options.enabled ?? true) && artworkId != null
  return useQuery<Challenge[], Error>({
    queryKey: qk.artworks.eligibleChallenges(artworkId ?? 0),
    queryFn: () => fetchEligibleChallengesForArtwork(artworkId as number),
    enabled,
  })
}
