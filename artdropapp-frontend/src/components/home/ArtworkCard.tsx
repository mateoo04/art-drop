import { Bookmark, Heart, MessageCircle } from 'lucide-react'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { useAuthPrompt } from '../../contexts/AuthPromptContext'
import { useLikeArtwork } from '../../hooks/useLikeArtwork'
import { getToken } from '../../lib/auth'
import { cloudinarySrcSet, cloudinaryUrl } from '../../lib/cloudinary'
import type { Artwork, ProgressStatus, SaleStatus } from '../../types/artwork'

const CARD_WIDTHS = [240, 360, 480, 720, 960]
const CARD_SIZES = '(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 25vw'

type ArtworkCardProps = {
  artwork: Artwork
}

function formatCount(n: number): string {
  if (n >= 1000) {
    const k = n / 1000
    return `${k.toFixed(k < 10 ? 1 : 0)}k`
  }
  return String(n)
}

function formatPrice(price: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(price)
}

function aspectClass(ratio: number): string {
  if (ratio >= 1.6) return 'aspect-[16/9]'
  if (ratio >= 1.2) return 'aspect-[4/3]'
  if (ratio >= 0.95) return 'aspect-square'
  if (ratio >= 0.75) return 'aspect-[4/5]'
  if (ratio >= 0.6) return 'aspect-[3/4]'
  return 'aspect-[2/3]'
}

function progressKey(status: ProgressStatus | null): string | null {
  if (status === 'WIP') return 'home.card.progress.wip'
  if (status === 'FINISHED') return 'home.card.progress.finished'
  return null
}

function saleKey(status: SaleStatus | null): string | null {
  switch (status) {
    case 'AVAILABLE':
      return 'home.card.sale.available'
    case 'ORIGINAL':
      return 'home.card.sale.original'
    case 'EDITION':
      return 'home.card.sale.edition'
    case 'SOLD':
      return 'home.card.sale.sold'
    default:
      return null
  }
}

function saleBadgeClasses(status: SaleStatus | null): string {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-tertiary text-on-tertiary'
    case 'ORIGINAL':
      return 'bg-primary text-on-primary'
    case 'EDITION':
      return 'bg-secondary text-on-secondary'
    case 'SOLD':
      return 'bg-inverse-surface text-inverse-on-surface'
    default:
      return 'bg-primary text-on-primary'
  }
}

export function ArtworkCard({ artwork }: ArtworkCardProps) {
  const { t } = useTranslation()
  const progressKey_ = progressKey(artwork.progressStatus)
  const saleKey_ = saleKey(artwork.saleStatus)
  const progress = progressKey_ ? t(progressKey_) : null
  const sale = saleKey_ ? t(saleKey_) : null
  const { promptToAuth } = useAuthPrompt()
  const likeMutation = useLikeArtwork()
  const [animating, setAnimating] = useState(false)

  const liked = artwork.likedByMe

  const handleLike = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
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

  const secondImage = artwork.images.find(
    (img) => img.imageUrl && img.imageUrl !== artwork.imageUrl,
  )

  return (
    <article className="masonry-item group">
      <Link to={`/details/${artwork.id}`} className="block cursor-pointer">
        <div className={`relative bg-surface-container-lowest overflow-hidden ${aspectClass(artwork.aspectRatio)}`}>
          <img
            alt={artwork.imageAlt}
            className="absolute inset-0 w-full h-full object-cover"
            src={artwork.coverPublicId ? cloudinaryUrl(artwork.coverPublicId, { width: 480 }) : artwork.imageUrl}
            srcSet={artwork.coverPublicId ? cloudinarySrcSet(artwork.coverPublicId, CARD_WIDTHS) : undefined}
            sizes={CARD_SIZES}
            loading="lazy"
          />
          {secondImage ? (
            <img
              alt=""
              aria-hidden="true"
              className="hidden md:block absolute inset-0 w-full h-full object-cover opacity-0 group-hover:opacity-100 transition-opacity duration-500 ease-out"
              src={secondImage.publicId ? cloudinaryUrl(secondImage.publicId, { width: 480 }) : secondImage.imageUrl}
              srcSet={secondImage.publicId ? cloudinarySrcSet(secondImage.publicId, CARD_WIDTHS) : undefined}
              sizes={CARD_SIZES}
              loading="lazy"
            />
          ) : null}
          <div className="absolute top-4 left-4 flex gap-2 z-10">
            {progress ? (
              <span className="bg-surface/90 backdrop-blur-md px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-on-surface">
                {progress}
              </span>
            ) : null}
            {sale ? (
              <span
                className={`${saleBadgeClasses(artwork.saleStatus)} px-3 py-1 text-[10px] font-bold uppercase tracking-widest`}
              >
                {sale}
              </span>
            ) : null}
          </div>
        </div>
      </Link>

      <div className="mt-6">
        <div className="flex justify-between items-start gap-4">
          <div className="min-w-0">
            <Link
              to={`/details/${artwork.id}`}
              className="font-headline text-2xl text-on-surface leading-tight truncate block hover:text-outline transition-colors"
            >
              {artwork.title}
            </Link>
            {artwork.artist ? (
              <p className="font-body text-sm text-on-surface-variant italic">
                {t('home.card.by')}{' '}
                <Link
                  to={`/u/${artwork.artist.slug}`}
                  className="hover:text-on-surface transition-colors underline-offset-4 hover:underline"
                >
                  {artwork.artist.displayName}
                </Link>
              </p>
            ) : null}
          </div>
          {artwork.price != null ? (
            <p className="font-body text-lg font-semibold whitespace-nowrap">
              {formatPrice(artwork.price)}
            </p>
          ) : null}
        </div>
        {artwork.description ? (
          <Link
            to={`/details/${artwork.id}`}
            className="mt-4 text-sm text-on-surface-variant leading-relaxed line-clamp-2 italic block"
          >
            “{artwork.description}”
          </Link>
        ) : null}
        <div className="mt-6 flex items-center justify-between">
          <div className="flex items-center gap-4 text-on-surface-variant">
            <button
              type="button"
              onClick={handleLike}
              aria-pressed={liked}
              aria-label={liked ? t('home.card.unlikeArtwork') : t('home.card.likeArtwork')}
              className={`flex items-center gap-1.5 text-xs transition-colors ${
                liked ? 'text-error' : 'hover:text-on-surface'
              }`}
            >
              <Heart
                size={14}
                fill={liked ? 'currentColor' : 'none'}
                className={`transition-transform duration-300 ease-out will-change-transform ${
                  animating ? 'scale-150' : 'scale-100'
                }`}
              />
              {formatCount(artwork.likeCount)}
            </button>
            <Link
              to={`/details/${artwork.id}#comments`}
              aria-label={t('home.card.openComments')}
              className="flex items-center gap-1.5 text-xs hover:text-on-surface transition-colors"
            >
              <MessageCircle size={14} />
              {formatCount(artwork.commentCount)}
            </Link>
          </div>
          <button
            type="button"
            aria-label={t('home.card.bookmark')}
            className="text-on-surface-variant hover:text-on-surface transition-colors"
          >
            <Bookmark size={20} />
          </button>
        </div>
      </div>
    </article>
  )
}
