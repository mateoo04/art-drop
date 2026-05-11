import { useCallback, useEffect, useState } from 'react'
import {
  getFeaturedState,
  setFeaturedCurrent,
  clearFeaturedCurrent,
  scheduleReplacement,
  clearSchedule,
  type FeaturedChallengeState,
  type FeaturedTriggerType,
} from '../api/featuredChallengeApi'

export function useFeaturedChallenge() {
  const [state, setState] = useState<FeaturedChallengeState | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setState(await getFeaturedState())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void refresh() }, [refresh])

  const setCurrent = useCallback(async (id: number) => {
    setState(await setFeaturedCurrent(id))
  }, [])

  const clearCurrent = useCallback(async () => {
    await clearFeaturedCurrent()
    await refresh()
  }, [refresh])

  const schedule = useCallback(async (nextId: number, type: FeaturedTriggerType, at: string | null) => {
    setState(await scheduleReplacement(nextId, type, at))
  }, [])

  const clearPending = useCallback(async () => {
    await clearSchedule()
    await refresh()
  }, [refresh])

  return { state, loading, error, refresh, setCurrent, clearCurrent, schedule, clearPending }
}
