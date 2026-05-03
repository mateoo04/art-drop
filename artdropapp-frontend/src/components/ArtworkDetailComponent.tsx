import { ChevronLeft, ChevronRight, Heart, Minus, Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Swiper, SwiperSlide } from 'swiper/react'
import { Keyboard, A11y, Pagination } from 'swiper/modules'
import type { Swiper as SwiperType } from 'swiper'
import 'swiper/css'
import 'swiper/css/pagination'

import type { Artwork, DimensionUnit, SaleStatus } from '../types/artwork'
import { useComments } from '../hooks/useComments'
import { useLikeArtwork } from '../hooks/useLikeArtwork'
import { useAuthPrompt } from '../contexts/AuthPromptContext'
import { getToken } from '../lib/auth'
import { CommentComposer } from './artwork/CommentComposer'
import { CommentList } from './artwork/CommentList'
import { Spinner } from './ui/Spinner'

export type ArtworkDetailComponentProps = {
  artwork: Artwork | null
  loading: boolean
  error: string | null
}

function formatPrice(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDimensions(
  w: number | null,
  h: number | null,
  d: number | null,
  unit: DimensionUnit | null,
): string | null {
  if (w == null || h == null || unit == null) return null
  const parts = [w, h]
  if (d != null) parts.push(d)
  const u = unit.toLowerCase()
  return `${parts.map((n) => Number(n).toString()).join(' × ')} ${u}`
}

export function ArtworkDetailComponent({
  artwork,
  loading,
  error,
}: ArtworkDetailComponentProps) {
  const { t } = useTranslation()
  const comments = useComments(artwork?.id ?? null)
  const { promptToAuth } = useAuthPrompt()
  const likeMutation = useLikeArtwork()
  const [animating, setAnimating] = useState(false)
  const [swiper, setSwiper] = useState<SwiperType | null>(null)
  const [quantity, setQuantity] = useState(1)

  const MAX_QUANTITY = 99

  const liked = artwork?.likedByMe ?? false

  const handleLike = () => {
    if (!artwork) return
    if (!getToken()) {
      promptToAuth('like this artwork')
      return
    }
    const next = !liked
    if (next) {
      setAnimating(true)
      window.setTimeout(() => setAnimating(false), 320)
    }
    likeMutation.mutate({ artworkId: artwork.id, like: next })
  }

  function saleRibbon(status: SaleStatus): string {
    switch (status) {
      case 'ORIGINAL':
        return t('artwork.detail.sale.originalAvailable')
      case 'EDITION':
        return t('artwork.detail.sale.editionAvailable')
      case 'AVAILABLE':
        return t('artwork.detail.sale.available')
      case 'SOLD':
        return t('artwork.detail.sale.sold')
    }
  }

  function purchaseLabel(status: SaleStatus): string {
    switch (status) {
      case 'ORIGINAL':
        return t('artwork.detail.sale.purchaseOriginal')
      case 'EDITION':
        return t('artwork.detail.sale.purchaseEdition')
      case 'AVAILABLE':
        return t('artwork.detail.sale.purchase')
      case 'SOLD':
        return t('artwork.detail.sale.sold')
    }
  }

  if (loading) {
    return (
      <div className="px-4 md:px-12 py-16 flex justify-center">
        <Spinner label={t('artwork.edit.loadingLabel')} />
      </div>
    )
  }

  if (error) {
    return (
      <div className="px-4 md:px-12 py-16 text-center text-error" role="alert">
        {error}
      </div>
    )
  }

  if (!artwork) {
    return (
      <div
        className="px-4 md:px-12 py-16 text-center text-on-surface-variant"
        role="region"
        aria-label={t('common.details')}
      >
        {t('artwork.detail.notFound')}
      </div>
    )
  }

  const galleryImages = artwork.images.length > 0
    ? artwork.images
    : artwork.imageUrl
      ? [{ id: null, imageUrl: artwork.imageUrl, sortOrder: 0, isCover: true, caption: null }]
      : []

  const dimensions = formatDimensions(
    artwork.width,
    artwork.height,
    artwork.depth,
    artwork.dimensionUnit,
  )

  const saleStatus = artwork.saleStatus
  const showCommercePanel =
    saleStatus != null && (artwork.price != null || saleStatus === 'SOLD')
  const showPurchaseButton =
    saleStatus != null && saleStatus !== 'SOLD' && artwork.price != null
  const showQuantitySelector = showPurchaseButton && saleStatus === 'EDITION'
  const effectiveQuantity = showQuantitySelector ? quantity : 1

  return (
    <article aria-label={t('common.artworkDetails')}>
      <div className="w-full max-w-[1920px] mx-auto px-4 md:px-8 xl:px-12 py-8 lg:py-16 xl:py-24 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-12 xl:gap-24">
        {/* Gallery */}
        <section className="lg:col-span-7 min-w-0 flex flex-col">
          <div className="w-full bg-surface-container-low relative group overflow-hidden">
            {galleryImages.length > 0 ? (
              <Swiper
                modules={[Keyboard, A11y, Pagination]}
                onSwiper={setSwiper}
                keyboard={{ enabled: true }}
                loop={galleryImages.length > 1}
                pagination={{
                  clickable: true,
                  el: '.gallery-pagination',
                  bulletClass: 'gallery-progress__segment',
                  bulletActiveClass: 'gallery-progress__segment--active',
                }}
                a11y={{
                  prevSlideMessage: t('artwork.detail.gallery.prevImage'),
                  nextSlideMessage: t('artwork.detail.gallery.nextImage'),
                }}
                spaceBetween={0}
                slidesPerView={1}
                className="w-full"
                autoHeight={true}
              >
                {galleryImages.map((img, i) => (
                  <SwiperSlide key={img.id ?? `${img.imageUrl}-${i}`}>
                    <img
                      src={img.imageUrl}
                      alt={img.caption ?? artwork.imageAlt}
                      className="w-full h-auto object-cover select-none"
                      loading={i === 0 ? 'eager' : 'lazy'}
                      draggable={false}
                    />
                  </SwiperSlide>
                ))}
              </Swiper>
            ) : null}

            {galleryImages.length > 1 ? (
              <div className="absolute bottom-3 right-3 flex gap-1 z-10">
                <button
                  type="button"
                  onClick={() => swiper?.slidePrev()}
                  aria-label={t('artwork.detail.gallery.prevImage')}
                  className="w-10 h-10 flex items-center justify-center bg-surface-container-lowest text-on-surface hover:bg-surface-container-low transition-colors shadow-sm"
                >
                  <ChevronLeft size={20} aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={() => swiper?.slideNext()}
                  aria-label={t('artwork.detail.gallery.nextImage')}
                  className="w-10 h-10 flex items-center justify-center bg-surface-container-lowest text-on-surface hover:bg-surface-container-low transition-colors shadow-sm"
                >
                  <ChevronRight size={20} aria-hidden="true" />
                </button>
              </div>
            ) : null}
          </div>
          {galleryImages.length > 1 ? (
            <div className="gallery-pagination" role="tablist" aria-label={t('artwork.detail.gallery.imageNav')} />
          ) : null}
        </section>

        {/* Content */}
        <section className="lg:col-span-5 min-w-0 flex flex-col pt-0 lg:pt-4">
          <div className="mb-10 lg:mb-12">
            <h1 className="font-display text-3xl md:text-4xl lg:text-5xl leading-[1.1] tracking-tight mb-4 text-on-surface">
              {artwork.title}
            </h1>
            {artwork.artist ? (
              <p className="font-display text-lg md:text-xl text-on-surface-variant italic mb-6 lg:mb-8">
                <Link
                  to={`/u/${artwork.artist.slug}`}
                  className="hover:text-on-surface transition-colors"
                >
                  {artwork.artist.displayName}
                </Link>
              </p>
            ) : null}
            <div className="flex flex-wrap gap-3">
              <span className="bg-secondary-container text-on-surface px-4 py-1.5 text-xs font-label uppercase tracking-widest">
                {artwork.medium}
              </span>
              {artwork.tags.map((tag) => (
                <span
                  key={tag}
                  className="bg-secondary-container text-on-surface px-4 py-1.5 text-xs font-label uppercase tracking-widest"
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>

          {artwork.description ? (
            <div className="mb-12 lg:mb-16">
              <h3 className="font-display text-2xl mb-4 text-on-surface">{t('artwork.detail.studioNote')}</h3>
              <p className="font-body text-sm text-on-surface-variant leading-relaxed whitespace-pre-line">
                {artwork.description}
              </p>
            </div>
          ) : null}
          <div className="bg-surface-container-lowest p-6 md:p-8 border border-outline-variant/15 mb-12 lg:mb-16 shadow-[0_10px_40px_rgba(45,52,53,0.06)] relative">
            {showCommercePanel && saleStatus != null ? (
              <div
                className={`absolute top-0 right-0 px-4 py-1 text-xs font-label uppercase tracking-wider translate-x-2 -translate-y-3 lg:translate-x-4 lg:-translate-y-4 ${
                  saleStatus === 'SOLD'
                    ? 'bg-on-surface text-surface'
                    : 'bg-tertiary text-on-tertiary'
                }`}
              >
                {saleRibbon(saleStatus)}
              </div>
            ) : null}
            {showCommercePanel && artwork.price != null ? (
              <div className="flex justify-between items-baseline mb-6 lg:mb-8">
                <span className="font-body text-sm text-on-surface-variant">
                  {effectiveQuantity > 1
                    ? t('artwork.detail.priceWithQuantity', { quantity: effectiveQuantity })
                    : t('artwork.detail.price')}
                </span>
                <span className="font-display text-2xl md:text-3xl text-on-surface">
                  {formatPrice(artwork.price * effectiveQuantity)}
                </span>
              </div>
            ) : null}
            {showQuantitySelector ? (
              <div className="flex items-center justify-between mb-6">
                <span className="font-body text-sm text-on-surface-variant">{t('artwork.detail.quantity')}</span>
                <div
                  className="inline-flex items-stretch border border-outline-variant/40"
                  role="group"
                  aria-label={t('artwork.detail.quantity_selector.label')}
                >
                  <button
                    type="button"
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    disabled={quantity <= 1}
                    aria-label={t('artwork.detail.quantity_selector.decrease')}
                    className="w-10 h-10 flex items-center justify-center text-on-surface hover:bg-surface-container-low disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <Minus size={18} aria-hidden="true" />
                  </button>
                  <span
                    className="min-w-[3rem] flex items-center justify-center font-label text-sm text-on-surface border-x border-outline-variant/40"
                    aria-live="polite"
                  >
                    {quantity}
                  </span>
                  <button
                    type="button"
                    onClick={() => setQuantity((q) => Math.min(MAX_QUANTITY, q + 1))}
                    disabled={quantity >= MAX_QUANTITY}
                    aria-label={t('artwork.detail.quantity_selector.increase')}
                    className="w-10 h-10 flex items-center justify-center text-on-surface hover:bg-surface-container-low disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <Plus size={18} aria-hidden="true" />
                  </button>
                </div>
              </div>
            ) : null}
            {showPurchaseButton && saleStatus != null ? (
              <button
                type="button"
                className="w-full bg-on-surface text-surface py-4 font-label text-sm uppercase tracking-widest hover:bg-on-surface-variant transition-colors mb-6"
              >
                {purchaseLabel(saleStatus)}
              </button>
            ) : null}
            <div
              className={`flex gap-4 ${
                showCommercePanel ? 'border-t border-outline-variant/15 pt-6' : ''
              }`}
            >
              <button
                type="button"
                onClick={() => void handleLike()}
                aria-pressed={liked}
                className={`flex-1 flex items-center justify-center gap-2 py-2 transition-colors ${
                  liked ? 'text-error' : 'text-on-surface-variant hover:text-on-surface'
                }`}
              >
                <Heart
                  size={20}
                  fill={liked ? 'currentColor' : 'none'}
                  aria-hidden="true"
                  className={`transition-transform duration-300 ease-out will-change-transform ${
                    animating ? 'scale-150' : 'scale-100'
                  }`}
                />
                <span className="font-label text-xs uppercase tracking-wide">
                  {liked ? t('artwork.detail.liked') : t('artwork.detail.like')}
                </span>
                <span className="font-label text-xs text-on-surface-variant ml-1">
                  {artwork.likeCount}
                </span>
              </button>
            </div>
          </div>

          <dl className="flex flex-col gap-4 border-t border-outline-variant/15 py-8">
            <div className="flex justify-between items-center">
              <dt className="font-label text-sm text-on-surface-variant">{t('artwork.detail.medium')}</dt>
              <dd className="font-label text-sm text-on-surface">{artwork.medium}</dd>
            </div>
            {dimensions ? (
              <div className="flex justify-between items-center">
                <dt className="font-label text-sm text-on-surface-variant">{t('artwork.detail.dimensions')}</dt>
                <dd className="font-label text-sm text-on-surface">{dimensions}</dd>
              </div>
            ) : null}
            {artwork.progressStatus ? (
              <div className="flex justify-between items-center">
                <dt className="font-label text-sm text-on-surface-variant">{t('artwork.detail.status')}</dt>
                <dd className="font-label text-sm text-on-surface">
                  {artwork.progressStatus === 'WIP'
                    ? t('artwork.detail.progressWip')
                    : t('artwork.detail.progressFinished')}
                </dd>
              </div>
            ) : null}
          </dl>
        </section>
      </div>

      {/* Discussion */}
      <section
        id="comments"
        className="w-full max-w-[1000px] mx-auto px-4 md:px-12 py-12 lg:py-16 bg-surface-container-low mb-16 lg:mb-24 scroll-mt-24"
      >
        <h3 className="font-display text-2xl mb-8 lg:mb-12 text-on-surface">{t('artwork.detail.discussion')}</h3>
        <div className="mb-10 lg:mb-16">
          <CommentComposer
            onSubmit={async (text) => {
              await comments.add(text)
            }}
          />
        </div>
        {comments.loading ? (
          <div className="py-6 flex justify-center">
            <Spinner label={t('comments.loadingComments')} />
          </div>
        ) : comments.error ? (
          <p
            className="py-6 text-center text-error border border-error-container/40 bg-error-container/10 font-body text-sm"
            role="alert"
          >
            {comments.error}
          </p>
        ) : (
          <>
            <CommentList
              comments={comments.data}
              onReply={(text, parentId) => comments.add(text, parentId)}
              onDelete={(id) => comments.remove(id)}
              onLoadMoreReplies={(parentId) => comments.loadMoreReplies(parentId)}
              loadingRepliesFor={comments.loadingRepliesFor}
            />
            {comments.hasMore ? (
              <div className="mt-12 flex justify-center">
                <button
                  type="button"
                  onClick={() => void comments.loadMore()}
                  disabled={comments.loadingMore}
                  className="inline-flex items-center justify-center px-6 py-3 border border-on-surface font-label text-[11px] uppercase tracking-[0.2em] font-semibold text-on-surface hover:bg-on-surface hover:text-surface transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {comments.loadingMore ? (
                    <Spinner label={t('comments.loadingMoreComments')} />
                  ) : (
                    t('artwork.detail.loadMore')
                  )}
                </button>
              </div>
            ) : null}
          </>
        )}
      </section>
    </article>
  )
}
