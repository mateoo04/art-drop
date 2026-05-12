import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMyReservation, releaseMyReservation } from '../api/reservationsApi'
import { getToken } from '../lib/auth'
import { qk } from '../lib/queryKeys'

export function useMyReservation() {
  return useQuery({
    queryKey: qk.reservations.mine,
    queryFn: fetchMyReservation,
    enabled: !!getToken(),
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  })
}

export function useReleaseMyReservation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: releaseMyReservation,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.reservations.mine })
    },
  })
}
