import { useQuery } from '@tanstack/react-query'
import { fetchMyArtworks } from '../api/usersApi'
import { qk } from '../lib/queryKeys'

export function useMyArtworks(enabled: boolean) {
  const query = useQuery({
    queryKey: qk.artworks.mine,
    queryFn: fetchMyArtworks,
    enabled,
  })

  return {
    data: query.data ?? null,
    loading: query.isLoading,
    error: query.error?.message ?? null,
    refetch: query.refetch,
  }
}
