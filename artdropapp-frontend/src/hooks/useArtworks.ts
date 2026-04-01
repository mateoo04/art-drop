import { useEffect, useState } from 'react'
import { mockArtworks } from '../data/mockArtworkData'
import type { Artwork } from '../types/artwork'

const MOCK_DELAY_MS = 400

export function useArtworks() {
  const [data, setData] = useState<Artwork[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)

    const timeoutId = window.setTimeout(() => {
      setData(mockArtworks)
      setLoading(false)
    }, MOCK_DELAY_MS)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [])

  return { data, loading, error }
}
