import { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchArtworkById } from '../api/artworksApi'
import { ArtworkDetailComponent } from '../components/ArtworkDetailComponent'
import type { Artwork } from '../types/artwork'

export function ArtworkDetailPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const { hash } = useLocation()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const enabled = Number.isFinite(id)

  const { data, isLoading, error } = useQuery<Artwork, Error>({
    queryKey: ['artworks', 'detail', id],
    queryFn: () => fetchArtworkById(id),
    enabled,
  })

  useEffect(() => {
    if (isLoading || !hash) return
    const target = document.getElementById(hash.slice(1))
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [isLoading, hash, data?.id])

  const errorMessage = !enabled
    ? 'Invalid ID'
    : error
      ? error.message === 'NOT_FOUND'
        ? 'Artwork not found.'
        : error.message
      : null

  return (
    <main>
      <ArtworkDetailComponent
        artwork={data ?? null}
        loading={enabled && isLoading}
        error={errorMessage}
      />
    </main>
  )
}
