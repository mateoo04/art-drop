import { type FormEvent, type KeyboardEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createArtwork, fetchMediums } from '../api/artworksApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useMySellerApplication } from '../hooks/useMySellerApplication'
import { SellerApplicationModal } from '../components/SellerApplicationModal'
import { cloudinaryUrl, openCloudinaryUpload } from '../lib/cloudinary'
import type { DimensionUnit, SaleStatus } from '../types/artwork'

type ProgressTab = 'FINISHED' | 'WIP'

type EditionStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE'

const EDITION_LABELS: Record<EditionStatus, string> = {
  ORIGINAL: 'Original (1/1)',
  EDITION: 'Limited Edition',
  AVAILABLE: 'Open Edition',
}

export function ArtworkDropPage() {
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const { application, refetch: refetchApp } = useMySellerApplication()

  type DropImage = { publicId: string; url: string }
  const [images, setImages] = useState<DropImage[]>([])
  const [coverIndex, setCoverIndex] = useState(0)
  const [uploading, setUploading] = useState(false)

  const [title, setTitle] = useState('')
  const [medium, setMedium] = useState('')
  const [progress, setProgress] = useState<ProgressTab>('FINISHED')
  const [description, setDescription] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [tagDraft, setTagDraft] = useState('')
  const [width, setWidth] = useState('')
  const [height, setHeight] = useState('')
  const [depth, setDepth] = useState('')
  const [unit, setUnit] = useState<DimensionUnit>('CM')
  const [listForSale, setListForSale] = useState(true)
  const [priceText, setPriceText] = useState('')
  const [editionStatus, setEditionStatus] = useState<EditionStatus>('ORIGINAL')

  const [mediums, setMediums] = useState<string[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [applyOpen, setApplyOpen] = useState(false)

  const isSeller = (user?.roles ?? []).includes('ROLE_SELLER')
  const sellerStatus = application?.derivedSellerStatus ?? user?.sellerStatus ?? 'NONE'
  const cooldownActive =
    application?.canReapplyAt != null && new Date(application.canReapplyAt) > new Date()

  useEffect(() => {
    let cancelled = false
    fetchMediums()
      .then((data) => {
        if (!cancelled) setMediums(data)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const charCount = description.length
  const maxChars = 500

  async function handleOpenUpload() {
    if (uploading) return
    setMessage(null)
    setUploading(true)
    try {
      const results = await openCloudinaryUpload({ multiple: true, maxFiles: 10 })
      if (results.length > 0) {
        setImages((prev) => {
          const merged = [...prev]
          for (const r of results) {
            if (merged.some((m) => m.publicId === r.publicId)) continue
            merged.push({
              publicId: r.publicId,
              url: r.url || cloudinaryUrl(r.publicId, { width: 1200 }),
            })
          }
          return merged
        })
      }
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  function handleRemoveImage(idx: number) {
    setImages((prev) => prev.filter((_, i) => i !== idx))
    setCoverIndex((prev) => {
      if (prev === idx) return 0
      if (prev > idx) return prev - 1
      return prev
    })
  }

  function handleSetCover(idx: number) {
    setCoverIndex(idx)
  }

  function handleAddTag(raw: string) {
    const next = raw.trim()
    if (!next) return
    if (tags.includes(next)) {
      setTagDraft('')
      return
    }
    setTags([...tags, next])
    setTagDraft('')
  }

  function handleTagKey(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      handleAddTag(tagDraft)
    } else if (e.key === 'Backspace' && tagDraft === '' && tags.length > 0) {
      setTags(tags.slice(0, -1))
    }
  }

  function removeTag(tag: string) {
    setTags(tags.filter((t) => t !== tag))
  }

  function parseDim(value: string): number | null {
    const trimmed = value.trim()
    if (trimmed === '') return null
    const n = Number.parseFloat(trimmed)
    return Number.isFinite(n) ? n : null
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setMessage(null)
    if (images.length === 0) {
      setMessage('Please upload at least one image.')
      return
    }
    if (!title.trim()) {
      setMessage('Please give your piece a title.')
      return
    }
    if (!medium.trim()) {
      setMessage('Please choose a medium.')
      return
    }
    const wantsListing = listForSale && isSeller
    let priceNumber: number | null = null
    let saleStatus: SaleStatus | null = null
    if (wantsListing) {
      priceNumber = priceText.trim() === '' ? null : Number.parseFloat(priceText)
      if (priceNumber == null || !Number.isFinite(priceNumber) || priceNumber < 0) {
        setMessage('Enter a valid price to list this piece for sale.')
        return
      }
      saleStatus = editionStatus
    }
    setSubmitting(true)
    try {
      const w = parseDim(width)
      const h = parseDim(height)
      const d = parseDim(depth)
      const hasAnyDim = w != null || h != null || d != null
      const created = await createArtwork({
        title: title.trim(),
        medium: medium.trim(),
        description: description.trim() || undefined,
        images: images.map((img, i) => ({
          publicId: img.publicId,
          sortOrder: i,
          isCover: i === coverIndex,
        })),
        width: hasAnyDim ? w : null,
        height: hasAnyDim ? h : null,
        depth: hasAnyDim ? d : null,
        dimensionUnit: hasAnyDim ? unit : null,
        progressStatus: progress,
        tags: tags.length > 0 ? tags : undefined,
        price: priceNumber,
        saleStatus,
      })
      if (created?.id) {
        navigate(`/details/${created.id}`)
      } else {
        navigate('/')
      }
    } catch (err) {
      if (err instanceof Error && err.message === 'TITLE_TAKEN') {
        setMessage('That title is already taken — try another.')
      } else if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        setMessage('Listing for sale requires verified seller status.')
      } else if (err instanceof Error && err.message === 'UNAUTHENTICATED') {
        setMessage('Please sign in to drop an artwork.')
      } else {
        setMessage(err instanceof Error ? err.message : 'Something went wrong.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <main className="w-full max-w-[640px] mx-auto flex flex-col pt-16 pb-32 px-6">
        <header className="mb-12">
          <Link
            to="/"
            className="inline-flex items-center text-on-surface-variant hover:text-on-surface transition-colors mb-8 group"
          >
            <span className="material-symbols-outlined mr-2 text-xl group-hover:-translate-x-1 transition-transform">
              arrow_back
            </span>
            <span className="font-label text-sm uppercase tracking-[0.1em]">Cancel Drop</span>
          </Link>
          <h1 className="font-display text-4xl md:text-5xl text-on-surface mb-4 tracking-tight leading-tight">
            Drop a new artwork
          </h1>
          <p className="font-body text-on-surface-variant text-lg max-w-md">
            Share a piece with your circle. Sellers can also list it for sale.
          </p>
        </header>

        <form id="drop-form" className="space-y-16" onSubmit={(e) => void handleSubmit(e)}>
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant">
                Artwork Media
              </span>
              {images.length > 0 ? (
                <span className="font-label text-[10px] uppercase tracking-[0.1em] text-on-surface-variant">
                  {images.length} image{images.length === 1 ? '' : 's'} · cover marked
                </span>
              ) : null}
            </div>
            {images.length === 0 ? (
              <button
                type="button"
                onClick={() => void handleOpenUpload()}
                disabled={uploading}
                className="w-full h-80 border border-dashed border-outline-variant/40 bg-surface-container-low hover:bg-surface-container-highest transition-colors cursor-pointer flex flex-col items-center justify-center group disabled:cursor-wait"
              >
                <span className="material-symbols-outlined text-4xl text-on-surface-variant mb-4 group-hover:scale-110 transition-transform duration-300">
                  {uploading ? 'progress_activity' : 'add_photo_alternate'}
                </span>
                <p className="font-body text-sm text-on-surface text-center px-4">
                  {uploading ? 'Opening uploader…' : 'Click to upload images'}
                </p>
                <p className="font-label text-xs text-on-surface-variant mt-2">
                  JPG, PNG, or WebP · up to 10 files
                </p>
              </button>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {images.map((img, idx) => {
                  const isCover = idx === coverIndex
                  return (
                    <div
                      key={img.publicId}
                      className={
                        isCover
                          ? 'relative aspect-square overflow-hidden bg-surface-container-low ring-2 ring-on-surface'
                          : 'relative aspect-square overflow-hidden bg-surface-container-low'
                      }
                    >
                      <img src={img.url} alt="" className="absolute inset-0 w-full h-full object-cover" />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent opacity-0 hover:opacity-100 transition-opacity flex flex-col justify-end p-2 gap-1">
                        {!isCover ? (
                          <button
                            type="button"
                            onClick={() => handleSetCover(idx)}
                            className="font-label text-[10px] uppercase tracking-[0.1em] bg-on-surface text-surface px-2 py-1 self-start hover:bg-primary transition-colors"
                          >
                            Set cover
                          </button>
                        ) : null}
                        <button
                          type="button"
                          onClick={() => handleRemoveImage(idx)}
                          aria-label="Remove image"
                          className="font-label text-[10px] uppercase tracking-[0.1em] bg-error text-on-error px-2 py-1 self-start hover:opacity-90 transition-opacity"
                        >
                          Remove
                        </button>
                      </div>
                      {isCover ? (
                        <span className="absolute top-2 left-2 px-2 py-0.5 bg-on-surface text-surface font-label text-[10px] uppercase tracking-[0.1em]">
                          Cover
                        </span>
                      ) : null}
                    </div>
                  )
                })}
                {images.length < 10 ? (
                  <button
                    type="button"
                    onClick={() => void handleOpenUpload()}
                    disabled={uploading}
                    className="aspect-square border border-dashed border-outline-variant/40 bg-surface-container-low hover:bg-surface-container-highest transition-colors flex flex-col items-center justify-center disabled:cursor-wait"
                  >
                    <span className="material-symbols-outlined text-3xl text-on-surface-variant">
                      {uploading ? 'progress_activity' : 'add'}
                    </span>
                    <span className="font-label text-[10px] uppercase tracking-[0.1em] text-on-surface-variant mt-1">
                      {uploading ? 'Uploading…' : 'Add more'}
                    </span>
                  </button>
                ) : null}
              </div>
            )}
          </section>

          <section className="space-y-4">
            <label
              htmlFor="title"
              className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant"
            >
              Title
            </label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(ev) => setTitle(ev.target.value)}
              placeholder="Give it a name"
              className="w-full bg-transparent border-b border-outline-variant/30 py-4 px-0 text-xl font-display text-on-surface placeholder:text-outline-variant/50 focus:outline-none focus:border-primary"
              autoComplete="off"
              required
            />
          </section>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <section className="space-y-4">
              <label
                htmlFor="medium"
                className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant"
              >
                Medium
              </label>
              <div className="relative">
                <select
                  id="medium"
                  value={medium}
                  onChange={(ev) => setMedium(ev.target.value)}
                  className="w-full bg-transparent border border-outline-variant/30 py-3 pl-4 pr-10 appearance-none text-sm text-on-surface focus:outline-none focus:border-primary"
                  required
                >
                  <option value="" disabled>
                    Select medium
                  </option>
                  {(mediums.length === 0
                    ? ['Digital', 'Sculpture', 'Painting', 'Photography', 'Mixed Media']
                    : mediums
                  ).map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
                <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">
                  expand_more
                </span>
              </div>
            </section>

            <section className="space-y-4">
              <span className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant">
                Status
              </span>
              <div className="flex border border-outline-variant/30 p-1" role="tablist">
                {(['FINISHED', 'WIP'] as const).map((opt) => (
                  <button
                    key={opt}
                    type="button"
                    role="tab"
                    aria-selected={progress === opt}
                    onClick={() => setProgress(opt)}
                    className={
                      progress === opt
                        ? 'flex-1 py-2 text-sm font-label text-center bg-primary text-on-primary transition-colors'
                        : 'flex-1 py-2 text-sm font-label text-center text-on-surface-variant hover:text-on-surface transition-colors'
                    }
                  >
                    {opt === 'FINISHED' ? 'Finished' : 'WIP'}
                  </button>
                ))}
              </div>
            </section>
          </div>

          <section className="space-y-4">
            <div className="flex justify-between items-end">
              <label
                htmlFor="description"
                className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant"
              >
                Description
              </label>
              <span className="font-label text-[10px] text-outline-variant">
                {charCount} / {maxChars}
              </span>
            </div>
            <textarea
              id="description"
              value={description}
              onChange={(ev) => setDescription(ev.target.value.slice(0, maxChars))}
              rows={4}
              placeholder="What's the story behind this piece?"
              className="w-full bg-transparent border border-outline-variant/30 p-4 text-sm font-body text-on-surface placeholder:text-outline-variant/50 resize-none focus:outline-none focus:border-primary"
            />
          </section>

          <section className="space-y-4">
            <label
              htmlFor="tags"
              className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant"
            >
              Tags
            </label>
            <div className="w-full border border-outline-variant/30 p-2 flex flex-wrap gap-2 items-center bg-transparent focus-within:border-primary transition-colors">
              {tags.map((t) => (
                <span
                  key={t}
                  className="bg-secondary-container text-on-surface px-3 py-1 text-xs font-label flex items-center gap-1"
                >
                  {t}
                  <button
                    type="button"
                    onClick={() => removeTag(t)}
                    aria-label={`Remove ${t}`}
                    className="material-symbols-outlined text-[14px] hover:text-error"
                  >
                    close
                  </button>
                </span>
              ))}
              <input
                id="tags"
                type="text"
                value={tagDraft}
                onChange={(ev) => setTagDraft(ev.target.value)}
                onKeyDown={handleTagKey}
                onBlur={() => handleAddTag(tagDraft)}
                placeholder="Add tags (press Enter)"
                className="flex-1 min-w-[150px] border-none bg-transparent text-sm p-1 focus:outline-none placeholder:text-outline-variant/50"
              />
            </div>
          </section>

          <section className="border-t border-outline-variant/15 pt-8">
            <details className="group">
              <summary className="flex items-center justify-between cursor-pointer list-none">
                <span className="font-label text-sm uppercase tracking-[0.1em] text-on-surface">
                  Add Dimensions{' '}
                  <span className="text-on-surface-variant lowercase">(optional)</span>
                </span>
                <span className="material-symbols-outlined text-on-surface-variant group-open:-scale-y-100 transition-transform">
                  expand_more
                </span>
              </summary>
              <div className="pt-6 grid grid-cols-3 gap-4">
                {[
                  { label: 'Width', value: width, set: setWidth },
                  { label: 'Height', value: height, set: setHeight },
                  { label: 'Depth', value: depth, set: setDepth },
                ].map((field) => (
                  <div key={field.label}>
                    <label className="block font-label text-[10px] uppercase text-outline-variant mb-2">
                      {field.label}
                    </label>
                    <input
                      type="number"
                      min="0"
                      step="0.1"
                      value={field.value}
                      onChange={(ev) => field.set(ev.target.value)}
                      placeholder="0"
                      className="w-full bg-transparent border border-outline-variant/30 py-2 px-3 text-sm focus:outline-none focus:border-primary text-center"
                    />
                  </div>
                ))}
                <div className="col-span-3 flex justify-end mt-2">
                  <div
                    className="flex border border-outline-variant/30 text-[10px] font-label uppercase"
                    role="tablist"
                  >
                    {(['CM', 'IN'] as const).map((u) => (
                      <button
                        key={u}
                        type="button"
                        role="tab"
                        aria-selected={unit === u}
                        onClick={() => setUnit(u)}
                        className={
                          unit === u
                            ? 'px-3 py-1 bg-primary text-on-primary'
                            : 'px-3 py-1 text-on-surface-variant hover:bg-surface-container-low'
                        }
                      >
                        {u}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </details>
          </section>

          <section className="bg-surface-container-low p-8 -mx-6 md:mx-0 relative overflow-hidden">
            <div className="absolute top-0 right-0 p-4">
              <span
                className={
                  isSeller
                    ? 'px-2 py-1 bg-tertiary text-on-tertiary font-label text-[10px] uppercase tracking-wider'
                    : 'px-2 py-1 bg-surface-container-highest text-on-surface-variant font-label text-[10px] uppercase tracking-wider'
                }
              >
                {isSeller ? 'Verified Seller' : 'Seller Required'}
              </span>
            </div>
            <h3 className="font-display text-2xl text-on-surface mb-2 mt-4">List for sale</h3>
            <p className="font-body text-sm text-on-surface-variant mb-8">
              Make this piece available for purchase directly from your profile.
            </p>
            {!isSeller ? (
              <p className="text-sm text-on-surface-variant mb-6">
                {sellerStatus === 'PENDING' ? (
                  'Your seller application is under review.'
                ) : (sellerStatus === 'NONE' ||
                    ((sellerStatus === 'REJECTED' || sellerStatus === 'REVOKED') &&
                      !cooldownActive)) ? (
                  <>
                    Selling artworks requires verified seller status.{' '}
                    <button
                      type="button"
                      onClick={() => setApplyOpen(true)}
                      className="underline"
                    >
                      Apply now
                    </button>
                    .
                  </>
                ) : application?.canReapplyAt ? (
                  `You can re-apply on ${new Date(application.canReapplyAt).toLocaleDateString()}.`
                ) : (
                  'Selling artworks requires verified seller status.'
                )}
              </p>
            ) : null}
            <fieldset className="space-y-6" disabled={!isSeller}>
              <div className="flex items-center space-x-3 mb-6">
                <input
                  id="list_sale"
                  type="checkbox"
                  checked={listForSale}
                  onChange={(ev) => setListForSale(ev.target.checked)}
                  className="form-checkbox h-5 w-5 text-primary border-outline-variant/50 rounded-none focus:ring-primary focus:ring-offset-surface-container-low bg-transparent cursor-pointer disabled:cursor-not-allowed"
                />
                <label
                  htmlFor="list_sale"
                  className="font-label text-sm text-on-surface cursor-pointer"
                >
                  Enable Marketplace Listing
                </label>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-outline-variant/15">
                <div>
                  <label
                    htmlFor="price"
                    className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-3"
                  >
                    Price
                  </label>
                  <div className="relative">
                    <span className="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant font-body">
                      €
                    </span>
                    <input
                      id="price"
                      type="number"
                      min="0"
                      step="0.01"
                      value={priceText}
                      onChange={(ev) => setPriceText(ev.target.value)}
                      placeholder="0.00"
                      className="w-full bg-surface-container-lowest border border-outline-variant/30 py-3 pl-8 pr-4 text-sm text-on-surface focus:outline-none focus:border-primary"
                    />
                  </div>
                </div>
                <div>
                  <label
                    htmlFor="sale_status"
                    className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-3"
                  >
                    Edition Status
                  </label>
                  <div className="relative">
                    <select
                      id="sale_status"
                      value={editionStatus}
                      onChange={(ev) => setEditionStatus(ev.target.value as EditionStatus)}
                      className="w-full bg-surface-container-lowest border border-outline-variant/30 py-3 pl-4 pr-10 appearance-none text-sm text-on-surface focus:outline-none focus:border-primary"
                    >
                      {(['ORIGINAL', 'EDITION', 'AVAILABLE'] as const).map((opt) => (
                        <option key={opt} value={opt}>
                          {EDITION_LABELS[opt]}
                        </option>
                      ))}
                    </select>
                    <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">
                      expand_more
                    </span>
                  </div>
                </div>
              </div>
            </fieldset>
          </section>

          {message ? (
            <p className="text-sm text-error" role="alert">
              {message}
            </p>
          ) : null}
        </form>
      </main>

      <div className="fixed bottom-0 left-0 w-full bg-surface/90 backdrop-blur-[20px] border-t border-outline-variant/10 z-40">
        <div className="max-w-[640px] mx-auto px-6 py-4 flex justify-between items-center">
          <button
            type="button"
            onClick={() => navigate('/')}
            className="font-label text-sm uppercase tracking-[0.1em] text-on-surface-variant hover:text-on-surface transition-colors py-3"
          >
            Cancel
          </button>
          <button
            type="submit"
            form="drop-form"
            disabled={submitting}
            className="bg-on-surface text-surface font-label text-sm uppercase tracking-[0.1em] px-8 py-4 hover:bg-primary transition-colors flex items-center disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {submitting ? 'Dropping…' : 'Drop Artwork'}
            <span className="material-symbols-outlined ml-2 text-[18px]">arrow_forward</span>
          </button>
        </div>
      </div>

      <SellerApplicationModal
        open={applyOpen}
        onClose={() => setApplyOpen(false)}
        onSubmitted={() => void refetchApp()}
      />
    </>
  )
}
