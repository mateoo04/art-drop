import { useCallback, useEffect, useState } from 'react'
import { fetchChallenges } from '../api/challengesApi'
import type { Challenge } from '../types/challenge'

export function useChallenges() {
  const [data, setData] = useState<Challenge[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchChallenges()
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

  return { data, loading, error, refetch }
}
