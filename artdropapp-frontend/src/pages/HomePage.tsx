import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import { fetchMediums } from '../api/artworksApi'
import { HomeActiveChallengesSection } from '../components/home/HomeActiveChallengesSection'
import { InfiniteScrollSentinel } from '../components/home/InfiniteScrollSentinel'
import { MasonryFeed } from '../components/home/MasonryFeed'
import { MediumFilterBar } from '../components/home/MediumFilterBar'
import { Spinner } from '../components/ui/Spinner'
import { useChallenges } from '../hooks/useChallenges'
import { useDiscoverFeed } from '../hooks/useArtworks'

export function HomePage() {
  const { t } = useTranslation()
  const { data: challenges } = useChallenges()
  const [activeMedium, setActiveMedium] = useState<string>('All')
  const { data: mediums = [] } = useQuery<string[], Error>({
    queryKey: ['artworks', 'mediums'],
    queryFn: fetchMediums,
    staleTime: 5 * 60 * 1000,
  })
  const {
    artworks,
    isLoading,
    isFetchingNextPage,
    error,
    hasNextPage,
    fetchNextPage,
  } = useDiscoverFeed(activeMedium)

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      {challenges && challenges.length > 0 ? (
        <HomeActiveChallengesSection challenges={challenges} />
      ) : null}
      <MediumFilterBar mediums={mediums} active={activeMedium} onChange={setActiveMedium} />

      {isLoading ? (
        <div className="py-12 flex justify-center">
          <Spinner label={t('home.loading')} />
        </div>
      ) : error ? (
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      ) : artworks.length === 0 ? (
        <p className="py-24 text-center text-on-surface-variant italic">
          {t('home.empty')}
        </p>
      ) : (
        <>
          <MasonryFeed artworks={artworks} />
          <InfiniteScrollSentinel
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
            label={t('home.loadingMore')}
          />
        </>
      )}
    </main>
  )
}
