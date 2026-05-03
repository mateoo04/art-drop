import { useCallback, useEffect, useState } from 'react'
import {
  searchAdminChallenges,
  type AdminChallengeFilters,
  type AdminChallengeRow,
} from '../api/adminApi'
import type { PageResult } from '../types/seller'

export function useAdminChallenges(
  query: string,
  page: number,
  size: number,
  filters: AdminChallengeFilters,
) {
  const [state, setState] = useState<{
    data: PageResult<AdminChallengeRow> | null
    loading: boolean
    error: string | null
  }>({ data: null, loading: true, error: null })
  const [refetchTick, setRefetchTick] = useState(0)

  const filtersKey = JSON.stringify(filters)

  const refetch = useCallback(() => {
    setRefetchTick((t) => t + 1)
  }, [])

  useEffect(() => {
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: null }))
    const debounce = setTimeout(() => {
      searchAdminChallenges(query, filters, page, size)
        .then((data) => {
          if (!cancelled) setState({ data, loading: false, error: null })
        })
        .catch((e: unknown) => {
          if (!cancelled) {
            setState({
              data: null,
              loading: false,
              error: e instanceof Error ? e.message : 'Failed to load',
            })
          }
        })
    }, 250)
    return () => {
      cancelled = true
      clearTimeout(debounce)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, page, size, filtersKey, refetchTick])

  return { ...state, refetch }
}
