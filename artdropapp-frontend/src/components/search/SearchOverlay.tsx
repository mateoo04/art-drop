import { ArrowRight, CircleX, Search, User } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Swiper, SwiperSlide } from 'swiper/react'
import { A11y, FreeMode, Keyboard, Mousewheel } from 'swiper/modules'
import 'swiper/css'
import 'swiper/css/free-mode'

import { fetchSearchArtworks } from '../../api/artworksApi'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { useChallenges } from '../../hooks/useChallenges'
import type { Artist, Artwork } from '../../types/artwork'
import type { Challenge } from '../../types/challenge'
import { Spinner } from '../ui/Spinner'

const SEARCH_DEBOUNCE_MS = 320
const MIN_QUERY_LEN = 2
const SEARCH_LIMIT = 8

type SearchOverlayProps = {
  onClose: () => void
}

function useDebouncedValue(value: string, ms: number): string {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const id = window.setTimeout(() => setDebounced(value), ms)
    return () => window.clearTimeout(id)
  }, [value, ms])
  return debounced
}

function thumbImage(artwork: Artwork): string {
  if (artwork.coverPublicId) {
    return cloudinaryUrl(artwork.coverPublicId, { width: 480, crop: 'limit', quality: 'auto' })
  }
  return artwork.imageUrl
}

function uniqueArtists(artworks: Artwork[]): Artist[] {
  const map = new Map<number, Artist>()
  for (const a of artworks) {
    if (a.artist) map.set(a.artist.id, a.artist)
  }
  return [...map.values()].slice(0, 8)
}

function pickChallenges(challenges: Challenge[] | null, query: string): Challenge[] {
  if (!challenges?.length) return []
  const q = query.trim().toLowerCase()
  if (q.length < 2) {
    return challenges.filter((c) => c.status === 'ACTIVE' || c.status === 'UPCOMING').slice(0, 3)
  }
  const scored = challenges.filter(
    (c) =>
      c.title.toLowerCase().includes(q) ||
      (c.description?.toLowerCase().includes(q) ?? false) ||
      (c.theme?.toLowerCase().includes(q) ?? false),
  )
  if (scored.length) return scored.slice(0, 4)
  return challenges.filter((c) => c.status === 'ACTIVE' || c.status === 'UPCOMING').slice(0, 3)
}

function challengeMeta(c: Challenge, t: (k: string, o?: Record<string, unknown>) => string): string {
  if (c.status === 'ENDED') return t('search.challengeEnded')
  if (!c.endsAt) return ''
  const end = new Date(c.endsAt)
  if (Number.isNaN(end.getTime())) return ''
  const now = Date.now()
  if (end.getTime() < now) return t('search.challengeEnded')
  const days = Math.max(1, Math.ceil((end.getTime() - now) / 86_400_000))
  return days === 1 ? t('search.oneDayLeft') : t('search.daysLeft', { count: days })
}

export function SearchOverlay({ onClose }: SearchOverlayProps) {
  const { t } = useTranslation()
  const [query, setQuery] = useState('')
  const debounced = useDebouncedValue(query, SEARCH_DEBOUNCE_MS)
  const inputRef = useRef<HTMLInputElement>(null)
  const { data: challenges } = useChallenges()

  const effectiveQuery = debounced.trim()
  const searchEnabled = effectiveQuery.length >= MIN_QUERY_LEN

  const { data, isLoading, isFetching, error } = useQuery({
    queryKey: ['artworks', 'search', effectiveQuery],
    queryFn: () => fetchSearchArtworks(effectiveQuery, SEARCH_LIMIT),
    enabled: searchEnabled,
    staleTime: 30_000,
  })

  const results = useMemo(() => data ?? [], [data])
  const artists = useMemo(() => uniqueArtists(results), [results])
  const challengeRows = useMemo(() => pickChallenges(challenges, effectiveQuery), [challenges, effectiveQuery])

  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [])

  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const busy = searchEnabled && (isLoading || isFetching)

  const headerTop = 'var(--app-header-height, 69px)'

  return (
    <>
      <div
        className="fixed inset-x-0 bottom-0 z-30 bg-background/50 backdrop-blur-[2px]"
        style={{ top: headerTop }}
        aria-label={t('search.closeAria')}
        role="button"
        tabIndex={-1}
        onClick={onClose}
      />
      <div
        className="fixed inset-x-0 z-40 flex max-h-[calc(100vh-var(--app-header-height,69px))] flex-col border-b border-surface-container-high bg-surface-container-lowest/95 font-body shadow-md backdrop-blur-md"
        style={{ top: headerTop }}
        role="dialog"
        aria-modal="true"
        aria-label={t('search.overlayTitle')}
      >
        <div className="mx-auto w-full max-w-screen-2xl">
          <div className="relative border-b border-surface-container-high">
            <Search
              size={24}
              className="absolute left-6 top-1/2 z-10 -translate-y-1/2 text-on-surface-variant"
              aria-hidden
            />
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={t('search.placeholderShort')}
              className="w-full border-none bg-transparent py-5 pl-16 pr-14 text-xl text-on-surface outline-none ring-0 placeholder:text-outline focus:ring-0"
              autoComplete="off"
              spellCheck={false}
            />
            {query ? (
              <button
                type="button"
                onClick={() => {
                  setQuery('')
                  inputRef.current?.focus()
                }}
                className="absolute right-6 top-1/2 z-10 -translate-y-1/2 text-on-surface-variant transition-colors hover:text-on-surface"
                aria-label={t('search.clearAria')}
              >
                <CircleX size={22} aria-hidden />
              </button>
            ) : null}
          </div>

          {!searchEnabled ? (
            <p className="px-8 py-12 text-center text-on-surface-variant">{t('search.minChars')}</p>
          ) : busy ? (
            <div className="flex justify-center px-8 py-16">
              <Spinner />
            </div>
          ) : error ? (
            <p className="px-8 py-12 text-center text-error" role="alert">
              {t('search.error')}
            </p>
          ) : results.length === 0 ? (
            <p className="px-8 py-12 text-center text-on-surface-variant italic">{t('search.empty')}</p>
          ) : (
            <>
              <div className="grid max-h-[70vh] grid-cols-1 overflow-y-auto lg:grid-cols-12">
                <section className="border-b border-surface-container-high py-8 lg:col-span-8 lg:border-b-0 lg:border-r">
                  <p className="mb-6 px-8 text-xs font-semibold uppercase tracking-widest text-on-surface-variant">
                    {t('search.sectionArtworks')}
                  </p>
                  <Swiper
                    modules={[FreeMode, Mousewheel, Keyboard, A11y]}
                    slidesPerView={1.12}
                    breakpoints={{
                      640: { slidesPerView: 2.2, spaceBetween: 32 },
                      1024: { slidesPerView: 3, spaceBetween: 32 },
                    }}
                    spaceBetween={24}
                    slidesOffsetBefore={32}
                    slidesOffsetAfter={32}
                    freeMode
                    mousewheel={{ forceToAxis: true }}
                    keyboard={{ enabled: true }}
                    a11y={{ enabled: true }}
                    className="pb-4"
                  >
                    {results.map((artwork, index) => (
                      <SwiperSlide key={artwork.id}>
                        <Link
                          to={`/details/${artwork.id}`}
                          onClick={onClose}
                          className="block outline-none ring-primary/25 focus-visible:ring-2"
                        >
                          <div
                            className={`mb-4 w-full overflow-hidden bg-surface-container-highest ${
                              index % 3 === 1
                                ? 'aspect-[3/4]'
                                : index % 3 === 2
                                  ? 'aspect-square'
                                  : 'aspect-[4/3]'
                            }`}
                          >
                            <img
                              src={thumbImage(artwork)}
                              alt={artwork.imageAlt}
                              className="h-full w-full object-cover transition-transform duration-500 hover:scale-[1.02]"
                              loading="lazy"
                            />
                          </div>
                          <p className="font-headline text-base font-medium leading-snug text-on-surface line-clamp-2">
                            {artwork.title}
                          </p>
                          <p className="mt-1 text-xs text-on-surface-variant">
                            {artwork.artist
                              ? t('search.byArtist', { name: artwork.artist.displayName })
                              : artwork.medium}
                          </p>
                        </Link>
                      </SwiperSlide>
                    ))}
                  </Swiper>
                </section>

                <aside className="flex flex-col lg:col-span-4">
                  <section className="flex-1 border-b border-surface-container-high p-8">
                    <p className="mb-4 text-xs font-semibold uppercase tracking-widest text-on-surface-variant">
                      {t('search.sectionArtists')}
                    </p>
                    <ul className="space-y-2">
                      {artists.slice(0, 3).map((artist) => (
                        <li key={artist.id}>
                          <Link
                            to={`/u/${artist.slug}`}
                            onClick={onClose}
                            className="-mx-8 flex items-center gap-4 px-8 py-3 text-on-surface outline-none ring-primary/25 transition-colors hover:bg-surface-container-low focus-visible:ring-2"
                          >
                            {artist.avatarUrl ? (
                              <img
                                src={artist.avatarUrl}
                                alt=""
                                className="size-10 shrink-0 rounded-full object-cover"
                              />
                            ) : (
                              <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-surface-container-highest text-on-surface">
                                <User size={18} aria-hidden />
                              </span>
                            )}
                            <span className="truncate text-sm font-medium">{artist.displayName}</span>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </section>

                  <section className="flex-1 p-8">
                    <p className="mb-4 text-xs font-semibold uppercase tracking-widest text-on-surface-variant">
                      {t('search.sectionChallenges')}
                    </p>
                    <ul className="space-y-2">
                      {challengeRows.slice(0, 3).map((c) => (
                        <li key={c.id}>
                          <Link
                            to={`/challenges/${c.id}`}
                            onClick={onClose}
                            className="-mx-8 flex items-center justify-between gap-4 px-8 py-3 outline-none ring-primary/25 transition-colors hover:bg-surface-container-low focus-visible:ring-2"
                          >
                            <span className="text-sm font-medium leading-snug text-on-surface line-clamp-2">
                              {c.title}
                            </span>
                            <span className="shrink-0 text-xs font-medium text-on-surface-variant">
                              {challengeMeta(c, t)}
                            </span>
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </section>
                </aside>
              </div>

              <div className="border-t border-surface-container-high bg-surface-container-lowest p-6">
                <button
                  type="button"
                  className="mx-auto flex items-center gap-2 text-center text-sm font-semibold uppercase tracking-widest text-on-surface transition-colors hover:text-primary"
                >
                  {t('search.viewAllResults')}
                  <ArrowRight size={16} aria-hidden />
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  )
}
