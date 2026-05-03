import { useCallback, useEffect, useRef } from 'react'
import { postSeenArtworks } from '../api/artworksApi'
import { getToken } from '../lib/auth'

const FLUSH_AFTER_MS = 2000
const MAX_BATCH = 20

export function useSeenReporter() {
  const queueRef = useRef<Set<number>>(new Set())
  const timerRef = useRef<number | null>(null)

  const flush = useCallback(() => {
    if (timerRef.current != null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
    const ids = Array.from(queueRef.current)
    queueRef.current.clear()
    if (ids.length === 0) return
    if (!getToken()) return
    void postSeenArtworks(ids).catch(() => {
      // swallow — telemetry-style endpoint, fire-and-forget
    })
  }, [])

  const reportSeen = useCallback(
    (artworkId: number) => {
      if (!getToken()) return
      queueRef.current.add(artworkId)
      if (queueRef.current.size >= MAX_BATCH) {
        flush()
        return
      }
      if (timerRef.current == null) {
        timerRef.current = window.setTimeout(flush, FLUSH_AFTER_MS)
      }
    },
    [flush],
  )

  useEffect(() => {
    return () => {
      flush()
    }
  }, [flush])

  return { reportSeen }
}
