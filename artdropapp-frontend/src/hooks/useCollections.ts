import { useCallback, useEffect, useState } from 'react'
import { fetchCollections } from '../api/collectionsApi'
import type { CollectionDTO } from '../types/collection'

export function useCollections() {
  const [data, setData] = useState<CollectionDTO[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchCollections()
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

  return { data, loading, error }
}
