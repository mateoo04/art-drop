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
let loadGeneration = 0
const listeners = new Set<Listener>()

function setState(next: State) {
  currentState = next
  for (const fn of listeners) fn(currentState)
}

async function load(): Promise<void> {
  const token = getToken()
  if (!token) {
    loadGeneration += 1
    inflight = null
    setState({ user: null, loading: false, error: null })
    return
  }
  if (inflight) return inflight
  const generation = ++loadGeneration
  const nextUser = currentState.user?.username === token ? currentState.user : null
  setState({ user: nextUser, loading: true, error: null })
  inflight = (async () => {
    try {
      const user = await fetchMe()
      if (generation !== loadGeneration) return
      if (getToken() !== token) {
        setState({ user: null, loading: false, error: null })
        return
      }
      setState({ user, loading: false, error: null })
    } catch (e) {
      if (generation !== loadGeneration) return
      if (getToken() !== token) {
        setState({ user: null, loading: false, error: null })
        return
      }
      setState({
        user: null,
        loading: false,
        error: e instanceof Error ? e.message : 'Unknown error',
      })
    } finally {
      if (generation === loadGeneration) {
        inflight = null
      }
    }
  })()
  return inflight
}

export function useCurrentUser() {
  const [state, setLocal] = useState<State>(currentState)

  useEffect(() => {
    const listener: Listener = (s) => setLocal(s)
    listeners.add(listener)
    const token = getToken()
    if (!token && currentState.user != null) {
      resetCurrentUser()
    } else if (token && !currentState.loading && currentState.user?.username !== token) {
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
  loadGeneration += 1
  inflight = null
  setState({ user: null, loading: false, error: null })
}
