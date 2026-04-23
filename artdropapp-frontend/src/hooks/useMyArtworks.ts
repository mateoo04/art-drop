import { useCallback, useEffect, useState } from 'react'
import { fetchMyArtworks } from '../api/usersApi'
import type { Artwork } from '../types/artwork'

export function useMyArtworks(enabled: boolean) {
  const [data, setData] = useState<Artwork[] | null>(null)
  const [loading, setLoading] = useState(enabled)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchMyArtworks()
      setData(list)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (enabled) void refetch()
  }, [enabled, refetch])

  return { data, loading, error, refetch }
}
