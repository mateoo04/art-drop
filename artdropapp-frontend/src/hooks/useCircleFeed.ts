import { useCallback, useEffect, useState } from 'react'
import { fetchCircleFeed } from '../api/feedApi'
import type { Artwork } from '../types/artwork'

const PAGE_SIZE = 20

export function useCircleFeed() {
  const [data, setData] = useState<Artwork[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [hasMore, setHasMore] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchCircleFeed({ limit: PAGE_SIZE, offset: 0 })
      setData(list)
      setHasMore(list.length === PAGE_SIZE)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore) return
    setLoadingMore(true)
    try {
      const list = await fetchCircleFeed({ limit: PAGE_SIZE, offset: data.length })
      setData((prev) => [...prev, ...list])
      setHasMore(list.length === PAGE_SIZE)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoadingMore(false)
    }
  }, [data.length, hasMore, loadingMore])

  return { data, loading, loadingMore, error, hasMore, refetch: load, loadMore }
}
