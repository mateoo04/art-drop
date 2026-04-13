import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { fetchArtworkById } from '../api/artworksApi'
import { ArtworkDetailComponent } from '../components/ArtworkDetailComponent'
import type { Artwork } from '../types/artwork'

export function ArtworkDetailPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const [artwork, setArtwork] = useState<Artwork | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setArtwork(null)
      setError('Invalid ID')
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)

    fetchArtworkById(id)
      .then((data) => {
        if (!cancelled) {
          setArtwork(data)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          if (err instanceof Error && err.message === 'NOT_FOUND') {
            setError('Artwork not found.')
          } else {
            setError(err instanceof Error ? err.message : 'Failed to load artwork')
          }
          setArtwork(null)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [id])

  return (
    <main className="app-main">
      <h1>Artwork details</h1>
      <ArtworkDetailComponent artwork={artwork} loading={loading} error={error} />
    </main>
  )
}
