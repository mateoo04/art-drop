import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMyReservation, releaseMyReservation } from '../api/reservationsApi'
import { getToken } from '../lib/auth'

export const MY_RESERVATION_KEY = ['my-reservation'] as const

export function useMyReservation() {
  return useQuery({
    queryKey: MY_RESERVATION_KEY,
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
      qc.invalidateQueries({ queryKey: MY_RESERVATION_KEY })
    },
  })
}
