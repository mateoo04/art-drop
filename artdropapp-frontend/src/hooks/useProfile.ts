import { useCallback, useEffect, useState } from 'react'
import {
  fetchCircleStatus,
  fetchProfileBySlug,
  joinCircle as apiJoinCircle,
  leaveCircle as apiLeaveCircle,
} from '../api/usersApi'
import { getToken } from '../lib/auth'
import type { UserProfile } from '../types/user'

type State = {
  profile: UserProfile | null
  inCircle: boolean | null
  loading: boolean
  error: string | null
}

export function useProfile(slug: string | undefined) {
  const [state, setState] = useState<State>({
    profile: null,
    inCircle: null,
    loading: true,
    error: null,
  })

  const load = useCallback(async () => {
    if (!slug) return
    setState((s) => ({ ...s, loading: true, error: null }))
    try {
      const profile = await fetchProfileBySlug(slug)
      let inCircle: boolean | null = null
      if (getToken() && !profile.isSelf) {
        try {
          inCircle = await fetchCircleStatus(slug)
        } catch {
          inCircle = null
        }
      }
      setState({ profile, inCircle, loading: false, error: null })
    } catch (e) {
      setState({
        profile: null,
        inCircle: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Unknown error',
      })
    }
  }, [slug])

  useEffect(() => {
    void load()
  }, [load])

  const toggleCircle = useCallback(async () => {
    if (!slug || state.inCircle == null) return
    const next = !state.inCircle
    setState((s) => ({ ...s, inCircle: next }))
    try {
      const actual = next ? await apiJoinCircle(slug) : await apiLeaveCircle(slug)
      setState((s) => ({ ...s, inCircle: actual }))
    } catch (e) {
      setState((s) => ({
        ...s,
        inCircle: !next,
        error: e instanceof Error ? e.message : 'Could not update circle',
      }))
    }
  }, [slug, state.inCircle])

  return { ...state, refetch: load, toggleCircle }
}
