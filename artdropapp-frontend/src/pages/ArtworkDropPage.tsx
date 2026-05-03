import { ArrowRight, ChevronDown, ImagePlus, Loader, Plus, X } from 'lucide-react'
import { type FormEvent, type KeyboardEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { createArtwork, fetchMediums } from '../api/artworksApi'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { useFooterVisible } from '../hooks/useFooterVisible'
import { useMySellerApplication } from '../hooks/useMySellerApplication'
import { SellerApplicationModal } from '../components/SellerApplicationModal'
import { BackButton } from '../components/ui/BackButton'
import { cloudinaryUrl, openCloudinaryUpload } from '../lib/cloudinary'
import type { DimensionUnit, SaleStatus } from '../types/artwork'

type ProgressTab = 'FINISHED' | 'WIP'

type EditionStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE'

export function ArtworkDropPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const { application, refetch: refetchApp } = useMySellerApplication()
  const footerVisible = useFooterVisible()

  const EDITION_LABELS: Record<EditionStatus, string> = {
    ORIGINAL: t('artwork.drop.edition.original'),
    EDITION: t('artwork.drop.edition.limited'),
    AVAILABLE: t('artwork.drop.edition.open'),
  }

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
      setMessage(err instanceof Error ? err.message : t('artwork.drop.upload.failed'))
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
    setTags(tags.filter((tg) => tg !== tag))
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
      setMessage(t('artwork.drop.error.noImages'))
      return
    }
    if (!title.trim()) {
      setMessage(t('artwork.drop.error.noTitle'))
      return
    }
    if (!medium.trim()) {
      setMessage(t('artwork.drop.error.noMedium'))
      return
    }
    const wantsListing = listForSale && isSeller
    let priceNumber: number | null = null
    let saleStatus: SaleStatus | null = null
    if (wantsListing) {
      priceNumber = priceText.trim() === '' ? null : Number.parseFloat(priceText)
      if (priceNumber == null || !Number.isFinite(priceNumber) || priceNumber < 0) {
        setMessage(t('artwork.drop.error.invalidPrice'))
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
        setMessage(t('artwork.drop.error.titleTaken'))
      } else if (err instanceof Error && err.message === 'FORBIDDEN_SALE_GATE') {
        setMessage(t('artwork.drop.error.forbiddenSale'))
      } else if (err instanceof Error && err.message === 'UNAUTHENTICATED') {
        setMessage(t('artwork.drop.error.unauthenticated'))
      } else {
        setMessage(err instanceof Error ? err.message : t('artwork.drop.error.fallback'))
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <main className="w-full max-w-[640px] mx-auto flex flex-col pt-16 pb-32 px-6">
        <header className="mb-12">
          <BackButton to="/" label={t('artwork.drop.cancelLabel')} className="mb-8" />
          <h1 className="font-display text-4xl md:text-5xl text-on-surface mb-4 tracking-tight leading-tight">
            {t('artwork.drop.title')}
          </h1>
          <p className="font-body text-on-surface-variant text-lg max-w-md">
            {t('artwork.drop.subtitle')}
          </p>
        </header>

        <form id="drop-form" className="space-y-16" onSubmit={(e) => void handleSubmit(e)}>
          <section className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant">
                {t('artwork.drop.fields.media')}
              </span>
              {images.length > 0 ? (
                <span className="font-label text-[10px] uppercase tracking-[0.1em] text-on-surface-variant">
                  {t('artwork.drop.fields.imagesCount', { count: images.length })} {t('artwork.drop.fields.imagesCoverMarked')}
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
                {uploading ? (
                  <Loader
                    size={36}
                    className="text-on-surface-variant mb-4 animate-spin"
                  />
                ) : (
                  <ImagePlus
                    size={36}
                    className="text-on-surface-variant mb-4 group-hover:scale-110 transition-transform duration-300"
                  />
                )}
                <p className="font-body text-sm text-on-surface text-center px-4">
                  {uploading ? t('artwork.drop.upload.opening') : t('artwork.drop.upload.clickToUpload')}
                </p>
                <p className="font-label text-xs text-on-surface-variant mt-2">
                  {t('artwork.drop.upload.fileTypes')}
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
                            {t('artwork.drop.upload.setCover')}
                          </button>
                        ) : null}
                        <button
                          type="button"
                          onClick={() => handleRemoveImage(idx)}
                          aria-label={t('artwork.drop.upload.removeLabel')}
                          className="font-label text-[10px] uppercase tracking-[0.1em] bg-error text-on-error px-2 py-1 self-start hover:opacity-90 transition-opacity"
                        >
                          {t('artwork.drop.upload.remove')}
                        </button>
                      </div>
                      {isCover ? (
                        <span className="absolute top-2 left-2 px-2 py-0.5 bg-on-surface text-surface font-label text-[10px] uppercase tracking-[0.1em]">
                          {t('artwork.drop.upload.cover')}
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
                    {uploading ? (
                      <Loader size={30} className="text-on-surface-variant animate-spin" />
                    ) : (
                      <Plus size={30} className="text-on-surface-variant" />
                    )}
                    <span className="font-label text-[10px] uppercase tracking-[0.1em] text-on-surface-variant mt-1">
                      {uploading ? t('artwork.drop.upload.uploading') : t('artwork.drop.upload.addMore')}
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
              {t('artwork.drop.fields.title')}
            </label>
            <input
              id="title"
              type="text"
              value={title}
              onChange={(ev) => setTitle(ev.target.value)}
              placeholder={t('artwork.drop.fields.titlePlaceholder')}
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
                {t('artwork.drop.fields.medium')}
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
                    {t('artwork.drop.fields.mediumPlaceholder')}
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
                <ChevronDown
                  size={20}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none"
                />
              </div>
            </section>

            <section className="space-y-4">
              <span className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant">
                {t('artwork.drop.fields.status')}
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
                    {opt === 'FINISHED' ? t('artwork.drop.fields.statusFinished') : t('artwork.drop.fields.statusWip')}
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
                {t('artwork.drop.fields.description')}
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
              placeholder={t('artwork.drop.fields.descriptionPlaceholder')}
              className="w-full bg-transparent border border-outline-variant/30 p-4 text-sm font-body text-on-surface placeholder:text-outline-variant/50 resize-none focus:outline-none focus:border-primary"
            />
          </section>

          <section className="space-y-4">
            <label
              htmlFor="tags"
              className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant"
            >
              {t('artwork.drop.fields.tags')}
            </label>
            <div className="w-full border border-outline-variant/30 p-2 flex flex-wrap gap-2 items-center bg-transparent focus-within:border-primary transition-colors">
              {tags.map((tag) => (
                <span
                  key={tag}
                  className="bg-secondary-container text-on-surface px-3 py-1 text-xs font-label flex items-center gap-1"
                >
                  {tag}
                  <button
                    type="button"
                    onClick={() => removeTag(tag)}
                    aria-label={t('artwork.drop.fields.removeTag', { tag })}
                    className="hover:text-error"
                  >
                    <X size={14} />
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
                placeholder={t('artwork.drop.fields.tagsPlaceholder')}
                className="flex-1 min-w-[150px] border-none bg-transparent text-sm p-1 focus:outline-none placeholder:text-outline-variant/50"
              />
            </div>
          </section>

          <section className="border-t border-outline-variant/15 pt-8">
            <details className="group">
              <summary className="flex items-center justify-between cursor-pointer list-none">
                <span className="font-label text-sm uppercase tracking-[0.1em] text-on-surface">
                  {t('artwork.drop.fields.dimensions')}{' '}
                  <span className="text-on-surface-variant lowercase">{t('artwork.drop.fields.dimensionsOptional')}</span>
                </span>
                <ChevronDown
                  size={20}
                  className="text-on-surface-variant group-open:-scale-y-100 transition-transform"
                />
              </summary>
              <div className="pt-6 grid grid-cols-3 gap-4">
                {[
                  { labelKey: 'artwork.drop.fields.dimensionWidth', value: width, set: setWidth },
                  { labelKey: 'artwork.drop.fields.dimensionHeight', value: height, set: setHeight },
                  { labelKey: 'artwork.drop.fields.dimensionDepth', value: depth, set: setDepth },
                ].map((field) => (
                  <div key={field.labelKey}>
                    <label className="block font-label text-[10px] uppercase text-outline-variant mb-2">
                      {t(field.labelKey)}
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
                    {(['CM', 'MM', 'IN', 'PX'] as const).map((u) => (
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
                {isSeller ? t('artwork.drop.seller.verified') : t('artwork.drop.seller.required')}
              </span>
            </div>
            <h3 className="font-display text-2xl text-on-surface mb-2 mt-4">{t('artwork.drop.fields.listing')}</h3>
            <p className="font-body text-sm text-on-surface-variant mb-8">
              {t('artwork.drop.fields.listingSubtitle')}
            </p>
            {!isSeller ? (
              <p className="text-sm text-on-surface-variant mb-6">
                {sellerStatus === 'PENDING' ? (
                  t('artwork.drop.seller.pending')
                ) : (sellerStatus === 'NONE' ||
                    ((sellerStatus === 'REJECTED' || sellerStatus === 'REVOKED') &&
                      !cooldownActive)) ? (
                  <>
                    {t('artwork.drop.seller.requiresStatus')}{' '}
                    <button
                      type="button"
                      onClick={() => setApplyOpen(true)}
                      className="underline"
                    >
                      {t('artwork.drop.seller.applyNow')}
                    </button>
                    .
                  </>
                ) : application?.canReapplyAt ? (
                  t('artwork.drop.seller.canReapply', {
                    date: new Date(application.canReapplyAt).toLocaleDateString(),
                  })
                ) : (
                  t('artwork.drop.seller.requiresStatusFallback')
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
                  {t('artwork.drop.fields.enableListing')}
                </label>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-outline-variant/15">
                <div>
                  <label
                    htmlFor="price"
                    className="block font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-3"
                  >
                    {t('artwork.drop.fields.price')}
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
                    {t('artwork.drop.fields.editionStatus')}
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
                    <ChevronDown
                      size={20}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none"
                    />
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

      <div
        aria-hidden={footerVisible}
        className={`fixed bottom-0 left-0 w-full bg-surface/90 backdrop-blur-[20px] border-t border-outline-variant/10 z-40 transition-transform duration-300 ease-out ${
          footerVisible ? 'translate-y-full pointer-events-none' : 'translate-y-0'
        }`}
      >
        <div className="max-w-[640px] mx-auto px-6 py-4 flex justify-between items-center">
          <button
            type="button"
            onClick={() => navigate('/')}
            className="font-label text-sm uppercase tracking-[0.1em] text-on-surface-variant hover:text-on-surface transition-colors py-3"
          >
            {t('artwork.drop.cancel')}
          </button>
          <button
            type="submit"
            form="drop-form"
            disabled={submitting}
            className="bg-on-surface text-surface font-label text-sm uppercase tracking-[0.1em] px-8 py-4 hover:bg-primary transition-colors flex items-center disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {submitting ? t('artwork.drop.submitting') : t('artwork.drop.submit')}
            <ArrowRight size={18} className="ml-2" />
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
