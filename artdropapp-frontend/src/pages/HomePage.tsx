import { useMemo, useState } from 'react'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { MediumFilterBar } from '../components/home/MediumFilterBar'
import { useArtworks } from '../hooks/useArtworks'

export function HomePage() {
  const { data, loading, error } = useArtworks()
  const [activeMedium, setActiveMedium] = useState<string>('All')

  const mediums = useMemo(() => {
    const set = new Set<string>()
    for (const a of data ?? []) set.add(a.medium)
    return Array.from(set).sort()
  }, [data])

  const artworks = useMemo(() => {
    const list = data ?? []
    const filtered = activeMedium === 'All' ? list : list.filter((a) => a.medium === activeMedium)
    return [...filtered].sort(
      (a, b) => new Date(b.publishedAt).getTime() - new Date(a.publishedAt).getTime(),
    )
  }, [activeMedium, data])

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4">
      <MediumFilterBar mediums={mediums} active={activeMedium} onChange={setActiveMedium} />

      {loading ? (
        <p className="py-12 text-center text-on-surface-variant italic" role="status">
          Loading artworks…
        </p>
      ) : null}
      {error ? (
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      ) : null}
      {!loading && !error ? <MasonryFeed artworks={artworks} /> : null}
    </main>
  )
}
