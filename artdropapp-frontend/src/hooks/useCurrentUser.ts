import { useCallback, useEffect, useState } from 'react'
import { fetchMe } from '../api/usersApi'
import { getToken } from '../lib/auth'
import type { UserProfile } from '../types/user'

type State = {
  user: UserProfile | null
  loading: boolean
  error: string | null
}

type Listener = (state: State) => void

let currentState: State = { user: null, loading: false, error: null }
let inflight: Promise<void> | null = null
const listeners = new Set<Listener>()

function setState(next: State) {
  currentState = next
  for (const fn of listeners) fn(currentState)
}

async function load(): Promise<void> {
  if (!getToken()) {
    setState({ user: null, loading: false, error: null })
    return
  }
  if (inflight) return inflight
  setState({ ...currentState, loading: true, error: null })
  inflight = (async () => {
    try {
      const user = await fetchMe()
      setState({ user, loading: false, error: null })
    } catch (e) {
      setState({
        user: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Unknown error',
      })
    } finally {
      inflight = null
    }
  })()
  return inflight
}

export function useCurrentUser() {
  const [state, setLocal] = useState<State>(currentState)

  useEffect(() => {
    const listener: Listener = (s) => setLocal(s)
    listeners.add(listener)
    if (currentState.user == null && !currentState.loading && getToken()) {
      void load()
    }
    return () => {
      listeners.delete(listener)
    }
  }, [])

  const refetch = useCallback(() => load(), [])

  const setUser = useCallback((user: UserProfile | null) => {
    setState({ ...currentState, user })
  }, [])

  return { ...state, refetch, setUser }
}

export function resetCurrentUser() {
  setState({ user: null, loading: false, error: null })
}
