import { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { fetchArtworkById } from '../api/artworksApi'
import { ArtworkDetailComponent } from '../components/ArtworkDetailComponent'
import type { Artwork } from '../types/artwork'

export function ArtworkDetailPage() {
  const { t } = useTranslation()
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
    ? t('artwork.detail.invalidId')
    : error
      ? error.message === 'NOT_FOUND'
        ? t('artwork.detail.notFound')
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
