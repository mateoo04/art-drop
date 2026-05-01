import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchArtworkById, updateArtwork } from '../api/artworksApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMySellerApplication } from '../hooks/useMySellerApplication'
import { SellerApplicationModal } from '../components/SellerApplicationModal'

type SaleStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD' | ''

export function ArtworkEditPage() {
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
      setError('Invalid ID')
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
          setError('Artwork not found.')
        } else {
          setError(err instanceof Error ? err.message : 'Failed to load artwork')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

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
        images: trimmedUrl ? [{ imageUrl: trimmedUrl, sortOrder: 0, isCover: true }] : undefined,
        price: isSeller ? priceNumber : undefined,
        saleStatus: isSeller ? (saleStatus === '' ? null : saleStatus) : undefined,
      })
      setMessage('Saved.')
      navigate(`/details/${id}`)
    } catch (err) {
      if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        setMessage('Selling artworks requires verified seller status.')
      } else {
        setMessage(err instanceof Error ? err.message : 'Save failed')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="app-main">
      <h1>Edit artwork</h1>
      {loading ? <p role="status">Loading…</p> : null}
      {error ? <p className="artwork-form__message" role="alert">{error}</p> : null}
      {!loading && !error ? (
        <form className="artwork-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="artwork-form__field">
            Title
            <input value={title} onChange={(ev) => setTitle(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            Medium
            <input value={medium} onChange={(ev) => setMedium(ev.target.value)} required autoComplete="off" />
          </label>
          <label className="artwork-form__field">
            Description
            <textarea
              value={description}
              onChange={(ev) => setDescription(ev.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
          <label className="artwork-form__field">
            Image URL (https://…)
            <input
              type="url"
              value={imageUrl}
              onChange={(ev) => setImageUrl(ev.target.value)}
              required
              placeholder="https://example.com/image.jpg"
            />
          </label>

          <fieldset className="artwork-form__field" disabled={!isSeller}>
            <legend>Listing</legend>
            {!isSeller ? (
              <p className="text-sm text-on-surface-variant mb-2">
                {sellerStatus === 'PENDING'
                  ? 'Your seller application is under review.'
                  : sellerStatus === 'NONE' || ((sellerStatus === 'REJECTED' || sellerStatus === 'REVOKED') && !cooldownActive)
                  ? <>Selling artworks requires verified seller status.{' '}
                      <button
                        type="button"
                        onClick={() => setApplyOpen(true)}
                        className="underline"
                      >
                        Apply now
                      </button>.
                    </>
                  : application?.canReapplyAt
                  ? `You can re-apply on ${new Date(application.canReapplyAt).toLocaleDateString()}.`
                  : 'Selling artworks requires verified seller status.'}
              </p>
            ) : null}
            <label>
              Price
              <input
                type="number"
                step="0.01"
                min="0"
                value={priceText}
                onChange={(ev) => setPriceText(ev.target.value)}
                placeholder="e.g. 250.00"
              />
            </label>
            <label>
              Sale status
              <select
                value={saleStatus}
                onChange={(ev) => setSaleStatus(ev.target.value as SaleStatus)}
              >
                <option value="">Not listed</option>
                <option value="AVAILABLE">Available</option>
                <option value="ORIGINAL">Original</option>
                <option value="EDITION">Edition</option>
                <option value="SOLD">Sold</option>
              </select>
            </label>
          </fieldset>

          <button
            type="submit"
            disabled={saving}
            className="w-full bg-on-surface text-surface font-semibold py-3 px-6 rounded-md hover:bg-on-surface/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? 'Saving…' : 'Save'}
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
