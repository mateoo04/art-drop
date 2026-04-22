import { Link, useNavigate } from 'react-router-dom'
import type { Artwork, ProgressStatus, SaleStatus } from '../../types/artwork'

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

function progressLabel(status: ProgressStatus | null): string | null {
  if (status === 'WIP') return 'WIP'
  if (status === 'FINISHED') return 'Finished'
  return null
}

function saleLabel(status: SaleStatus | null): string | null {
  switch (status) {
    case 'AVAILABLE':
      return 'Available'
    case 'ORIGINAL':
      return 'Original'
    case 'EDITION':
      return 'Edition'
    case 'SOLD':
      return 'Sold'
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
  const progress = progressLabel(artwork.progressStatus)
  const sale = saleLabel(artwork.saleStatus)
  const navigate = useNavigate()

  return (
    <article className="masonry-item group cursor-pointer">
      <Link to={`/details/${artwork.id}`} className="block">
        <div className="relative bg-surface-container-lowest overflow-hidden">
          <img
            alt={artwork.imageAlt}
            className={`w-full object-cover ${aspectClass(artwork.aspectRatio)}`}
            src={artwork.imageUrl}
            loading="lazy"
          />
          <div className="absolute top-4 left-4 flex gap-2">
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
          <button
            type="button"
            aria-label={`Edit ${artwork.title}`}
            onClick={(e) => {
              e.preventDefault()
              e.stopPropagation()
              navigate(`/edit/${artwork.id}`)
            }}
            className="material-symbols-outlined absolute top-4 right-4 bg-surface/90 backdrop-blur-md p-2 text-on-surface hover:bg-surface transition-colors"
          >
            edit
          </button>
        </div>

        <div className="mt-6">
          <div className="flex justify-between items-start gap-4">
            <div className="min-w-0">
              <h3 className="font-headline text-2xl text-on-surface leading-tight truncate">
                {artwork.title}
              </h3>
              {artwork.artist ? (
                <p className="font-body text-sm text-on-surface-variant italic">
                  by {artwork.artist.displayName}
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
            <p className="mt-4 text-sm text-on-surface-variant leading-relaxed line-clamp-2 italic">
              “{artwork.description}”
            </p>
          ) : null}
          <div className="mt-6 flex items-center justify-between">
            <div className="flex items-center gap-4 text-on-surface-variant">
              <span className="flex items-center gap-1.5 text-xs">
                <span className="material-symbols-outlined text-sm">favorite</span>
                {formatCount(artwork.likeCount)}
              </span>
              <span className="flex items-center gap-1.5 text-xs">
                <span className="material-symbols-outlined text-sm">chat_bubble</span>
                {formatCount(artwork.commentCount)}
              </span>
            </div>
            <button
              type="button"
              aria-label="Bookmark"
              onClick={(e) => e.preventDefault()}
              className="material-symbols-outlined text-on-surface-variant hover:text-on-surface transition-colors"
            >
              bookmark
            </button>
          </div>
        </div>
      </Link>
    </article>
  )
}
