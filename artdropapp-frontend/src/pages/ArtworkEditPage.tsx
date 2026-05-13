import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQueryClient } from '@tanstack/react-query'
import {
  fetchArtworkById,
  updateArtwork,
  type UpdateArtworkPayload,
} from '../api/artworksApi'
import {
  ArtworkForm,
  type ArtworkFormInitialValues,
  type ArtworkFormSubmitValues,
} from '../components/artwork/ArtworkForm'
import { Spinner } from '../components/ui/Spinner'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { qk } from '../lib/queryKeys'
import type { Artwork } from '../types/artwork'

function initialValuesFromArtwork(artwork: Artwork): ArtworkFormInitialValues {
  const images = artwork.images.length > 0
    ? artwork.images.map((img) => ({
        publicId: img.publicId,
        url: img.imageUrl,
      }))
    : artwork.coverPublicId
      ? [{ publicId: artwork.coverPublicId, url: artwork.imageUrl }]
      : []
  const coverIndex = Math.max(0, artwork.images.findIndex((img) => img.isCover))
  return {
    images,
    coverIndex,
    title: artwork.title,
    medium: artwork.medium,
    progressStatus: artwork.progressStatus ?? 'FINISHED',
    description: artwork.description ?? '',
    tags: artwork.tags,
    width: artwork.width,
    height: artwork.height,
    depth: artwork.depth,
    dimensionUnit: artwork.dimensionUnit ?? 'CM',
    listForSale: artwork.saleState !== 'DRAFT' && artwork.price != null,
    price: artwork.price,
    saleType: artwork.saleType ?? 'ORIGINAL',
    editionSize: artwork.editionSize,
  }
}

export function ArtworkEditPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const { user, loading: userLoading } = useCurrentUser()
  const [artwork, setArtwork] = useState<Artwork | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError(t('artwork.edit.error.invalidId'))
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchArtworkById(id)
      .then((data) => {
        if (!cancelled) setArtwork(data)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof Error && err.message === 'NOT_FOUND') {
          setError(t('artwork.edit.error.notFound'))
        } else {
          setError(err instanceof Error ? err.message : t('artwork.edit.error.loadFailed'))
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id, t])

  const isOwner =
    artwork != null && user != null && artwork.artist != null && artwork.artist.id === user.id

  async function handleSubmit(values: ArtworkFormSubmitValues) {
    if (!Number.isFinite(id)) return
    const payload: UpdateArtworkPayload = {
      title: values.title,
      medium: values.medium,
      description: values.description,
      width: values.width,
      height: values.height,
      depth: values.depth,
      dimensionUnit: values.dimensionUnit,
      progressStatus: values.progressStatus,
      tags: values.tags,
    }
    if (values.isSeller) {
      if (values.listForSale) {
        payload.price = values.price
        payload.saleType = values.saleType
        payload.editionSize = values.saleType === 'EDITION' ? values.editionSize : null
      } else {
        payload.unlist = true
      }
    }

    try {
      const updated = await updateArtwork(id, payload)
      queryClient.setQueryData(qk.artworks.detail(id), updated)
      void queryClient.invalidateQueries({ queryKey: qk.artworks.all })
      navigate(`/details/${id}`)
    } catch (err) {
      if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        throw new Error(t('artwork.edit.error.forbiddenSale'))
      }
      if (err instanceof Error && err.message === 'FORBIDDEN') {
        throw new Error(t('artwork.edit.error.forbiddenOwner'))
      }
      throw new Error(err instanceof Error ? err.message : t('artwork.edit.error.saveFailed'))
    }
  }

  if (loading || userLoading) {
    return (
      <main className="w-full max-w-[640px] mx-auto px-6 pt-16">
        <div className="py-12 flex justify-center">
          <Spinner label={t('artwork.edit.loadingLabel')} />
        </div>
      </main>
    )
  }

  if (error || !artwork) {
    return (
      <main className="w-full max-w-[640px] mx-auto px-6 pt-16">
        <p className="py-12 text-center text-error" role="alert">
          {error ?? t('artwork.edit.error.notFound')}
        </p>
      </main>
    )
  }

  if (!isOwner) {
    return (
      <main className="w-full max-w-[640px] mx-auto px-6 pt-16">
        <p className="py-12 text-center text-error" role="alert">
          {t('artwork.edit.error.forbiddenOwner')}
        </p>
      </main>
    )
  }

  return (
    <ArtworkForm
      key={artwork.id}
      formId="edit-artwork-form"
      title={t('artwork.edit.title')}
      subtitle={t('artwork.edit.subtitle')}
      backTo={`/details/${artwork.id}`}
      backLabel={t('artwork.edit.cancelLabel')}
      cancelLabel={t('common.cancel')}
      submitLabel={t('artwork.edit.save')}
      submittingLabel={t('artwork.edit.saving')}
      mediaReadonly
      mediaReadonlyNote={t('artwork.edit.mediaLocked')}
      initialValues={initialValuesFromArtwork(artwork)}
      onCancel={() => navigate(`/details/${artwork.id}`)}
      onSubmit={handleSubmit}
    />
  )
}
