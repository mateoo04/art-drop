import { useCallback, useEffect, useState } from 'react'
import { fetchArtworks } from '../api/artworksApi'
import type { Artwork } from '../types/artwork'

export function useArtworks() {
  const [data, setData] = useState<Artwork[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchArtworks()
      setData(list)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return { data, loading, error, refetch, setData }
}
