import { useCallback, useEffect, useState } from 'react'
import { fetchMyApplication } from '../api/sellerApi'
import { getToken } from '../lib/auth'
import type { SellerApplication } from '../types/seller'

type State = {
  application: SellerApplication | null
  loading: boolean
  error: string | null
}

export function useMySellerApplication() {
  const [state, setState] = useState<State>({ application: null, loading: false, error: null })

  const load = useCallback(async () => {
    if (!getToken()) {
      setState({ application: null, loading: false, error: null })
      return
    }
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const app = await fetchMyApplication()
      setState({ application: app, loading: false, error: null })
    } catch (e) {
      setState({
        application: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Failed to load',
      })
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  return { ...state, refetch: load }
}
