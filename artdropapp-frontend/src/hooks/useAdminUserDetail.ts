import { useCallback, useEffect, useState } from 'react'
import { fetchAdminUserDetail } from '../api/adminApi'
import type { AdminUserDetail } from '../types/seller'

export function useAdminUserDetail(userId: number | null) {
  const [data, setData] = useState<AdminUserDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (userId == null || !Number.isFinite(userId)) return
    setLoading(true)
    setError(null)
    try {
      const detail = await fetchAdminUserDetail(userId)
      setData(detail)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void load()
  }, [load])

  return { data, loading, error, refetch: load }
}
