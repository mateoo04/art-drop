import { useQuery } from '@tanstack/react-query'
import { fetchChallenge } from '../api/challengesApi'
import { qk } from '../lib/queryKeys'
import type { Challenge } from '../types/challenge'

export function useChallenge(id: number | null) {
  return useQuery<Challenge, Error>({
    queryKey: qk.challenges.detail(id ?? 0),
    queryFn: () => fetchChallenge(id as number),
    enabled: id != null,
  })
}
