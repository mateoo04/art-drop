import Masonry from 'react-masonry-css'
import { useTranslation } from 'react-i18next'
import { MASONRY_BREAKPOINT_COLS } from './masonryFeedConfig'

const TILE_COUNT = 9

/** Mirrors aspect buckets used in ArtworkCard `aspectClass` for visual variety. */
const ASPECT_CLASSES = [
  'aspect-[4/5]',
  'aspect-square',
  'aspect-[4/3]',
  'aspect-[16/9]',
  'aspect-[3/4]',
  'aspect-[2/3]',
] as const

export function MasonryFeedSkeleton() {
  const { t } = useTranslation()

  return (
    <section aria-busy="true" aria-label={t('home.loading')}>
      <Masonry
        breakpointCols={MASONRY_BREAKPOINT_COLS}
        className="masonry-grid-mc"
        columnClassName="masonry-grid-mc__column"
      >
        {Array.from({ length: TILE_COUNT }, (_, i) => (
          <article key={i} className="masonry-item">
            <div className="pointer-events-none">
              <div
                className={`relative w-full overflow-hidden bg-surface-container-high animate-pulse ${ASPECT_CLASSES[i % ASPECT_CLASSES.length]}`}
                aria-hidden
              />
              <div className="mt-3 space-y-2" aria-hidden>
                <div className="h-4 max-w-[75%] bg-surface-container-high animate-pulse" />
                <div className="h-3 max-w-[40%] bg-surface-container-high/80 animate-pulse" />
                <div className="h-3 max-w-[55%] bg-surface-container-high/60 animate-pulse" />
              </div>
            </div>
          </article>
        ))}
      </Masonry>
    </section>
  )
}
