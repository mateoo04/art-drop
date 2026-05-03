import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { fetchArtworkById, updateArtwork } from '../api/artworksApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMySellerApplication } from '../hooks/useMySellerApplication'
import { SellerApplicationModal } from '../components/SellerApplicationModal'
import { Spinner } from '../components/ui/Spinner'

type SaleStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | ''

export function ArtworkEditPage() {
  const { t } = useTranslation()
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const { application, refetch: refetchApp } = useMySellerApplication()

  const [title, setTitle] = useState('')
  const [medium, setMedium] = useState('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [priceText, setPriceText] = useState('')
  const [saleStatus, setSaleStatus] = useState<SaleStatus>('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [applyOpen, setApplyOpen] = useState(false)

  const isSeller = (user?.roles ?? []).includes('ROLE_SELLER')
  const sellerStatus = application?.derivedSellerStatus ?? user?.sellerStatus ?? 'NONE'
  const cooldownActive =
    application?.canReapplyAt != null && new Date(application.canReapplyAt) > new Date()

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
        if (cancelled) return
        setTitle(data.title)
        setMedium(data.medium)
        setDescription(data.description ?? '')
        setImageUrl(data.imageUrl)
        setPriceText(data.price == null ? '' : String(data.price))
        setSaleStatus((data.saleStatus ?? '') as SaleStatus)
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

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!Number.isFinite(id)) return
    setSaving(true)
    setMessage(null)
    try {
      const trimmedUrl = imageUrl.trim()
      const priceNumber = priceText.trim() === '' ? null : Number.parseFloat(priceText)
      await updateArtwork(id, {
        title: title.trim(),
        medium: medium.trim(),
        description: description.trim(),
        images: trimmedUrl ? [{ publicId: trimmedUrl, sortOrder: 0, isCover: true }] : undefined,
        price: isSeller ? priceNumber : undefined,
        saleStatus: isSeller ? (saleStatus === '' ? null : saleStatus) : undefined,
      })
      setMessage(t('common.save'))
      navigate(`/details/${id}`)
    } catch (err) {
      if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        setMessage(t('artwork.edit.error.forbiddenSale'))
      } else {
        setMessage(err instanceof Error ? err.message : t('artwork.edit.error.saveFailed'))
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="app-main">
      <h1>{t('artwork.edit.title')}</h1>
      {loading ? (
        <div className="py-12 flex justify-center">
          <Spinner label={t('artwork.edit.loadingLabel')} />
        </div>
      ) : null}
      {error ? <p className="artwork-form__message" role="alert">{error}</p> : null}
      {!loading && !error ? (
        <form className="artwork-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="artwork-form__field">
            {t('artwork.edit.fields.title')}
            <input value={title} onChange={(ev) => setTitle(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            {t('artwork.edit.fields.medium')}
            <input value={medium} onChange={(ev) => setMedium(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            {t('artwork.edit.fields.description')}
            <textarea
              value={description}
              onChange={(ev) => setDescription(ev.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
          <label className="artwork-form__field">
            {t('artwork.edit.fields.imageUrl')}
            <input
              type="url"
              value={imageUrl}
              onChange={(ev) => setImageUrl(ev.target.value)}
              required
              placeholder={t('artwork.edit.fields.imageUrlPlaceholder')}
            />
          </label>

          <fieldset className="artwork-form__field" disabled={!isSeller}>
            <legend>{t('artwork.edit.fields.listing')}</legend>
            {!isSeller ? (
              <p className="text-sm text-on-surface-variant mb-2">
                {sellerStatus === 'PENDING'
                  ? t('artwork.edit.seller.pending')
                  : sellerStatus === 'NONE' || ((sellerStatus === 'REJECTED' || sellerStatus === 'REVOKED') && !cooldownActive)
                  ? <>
                      {t('artwork.edit.seller.requiresStatus')}{' '}
                      <button
                        type="button"
                        onClick={() => setApplyOpen(true)}
                        className="underline"
                      >
                        {t('artwork.edit.seller.applyNow')}
                      </button>.
                    </>
                  : application?.canReapplyAt
                  ? t('artwork.edit.seller.canReapply', {
                      date: new Date(application.canReapplyAt).toLocaleDateString(),
                    })
                  : t('artwork.edit.seller.requiresStatusFallback')}
              </p>
            ) : null}
            <label>
              {t('artwork.edit.fields.price')}
              <input
                type="number"
                step="0.01"
                min="0"
                value={priceText}
                onChange={(ev) => setPriceText(ev.target.value)}
                placeholder={t('artwork.edit.fields.pricePlaceholder')}
              />
            </label>
            <label>
              {t('artwork.edit.fields.saleStatus')}
              <select
                value={saleStatus}
                onChange={(ev) => setSaleStatus(ev.target.value as SaleStatus)}
              >
                <option value="">{t('artwork.edit.fields.saleStatusOptions.notListed')}</option>
                <option value="AVAILABLE">{t('artwork.edit.fields.saleStatusOptions.available')}</option>
                <option value="ORIGINAL">{t('artwork.edit.fields.saleStatusOptions.original')}</option>
                <option value="EDITION">{t('artwork.edit.fields.saleStatusOptions.edition')}</option>
                <option value="SOLD">{t('artwork.edit.fields.saleStatusOptions.sold')}</option>
              </select>
            </label>
          </fieldset>

          <button
            type="submit"
            disabled={saving}
            className="w-full bg-on-surface text-surface font-semibold py-3 px-6 rounded-md hover:bg-on-surface/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? t('artwork.edit.saving') : t('artwork.edit.save')}
          </button>
          {message ? <p className="artwork-form__message" role="alert">{message}</p> : null}
        </form>
      ) : null}

      <SellerApplicationModal
        open={applyOpen}
        onClose={() => setApplyOpen(false)}
        onSubmitted={() => void refetchApp()}
      />
    </main>
  )
}
