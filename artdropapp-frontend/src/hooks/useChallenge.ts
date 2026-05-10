import { useQuery } from '@tanstack/react-query'
import { fetchChallenge } from '../api/challengesApi'
import type { Challenge } from '../types/challenge'

export function useChallenge(id: number | null) {
  return useQuery<Challenge, Error>({
    queryKey: ['challenges', 'detail', id],
    queryFn: () => fetchChallenge(id as number),
    enabled: id != null,
  })
}
