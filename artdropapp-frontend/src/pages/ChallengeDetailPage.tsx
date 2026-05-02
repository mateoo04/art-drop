import { useState } from 'react'
import { useParams } from 'react-router-dom'
import type { SubmissionSort } from '../api/challengesApi'
import { ChallengeHeroBanner } from '../components/challenges/ChallengeHeroBanner'
import { ChallengeSortTabs } from '../components/challenges/ChallengeSortTabs'
import { ChallengeSubmissionsMasonry } from '../components/challenges/ChallengeSubmissionsMasonry'
import { InfiniteScrollSentinel } from '../components/home/InfiniteScrollSentinel'
import { Spinner } from '../components/ui/Spinner'
import { useChallenge } from '../hooks/useChallenge'
import { useChallengeSubmissions } from '../hooks/useChallengeSubmissions'

export function ChallengeDetailPage() {
  const { id } = useParams<{ id: string }>()
  const challengeId = id ? Number(id) : null
  const validId = challengeId != null && Number.isFinite(challengeId) ? challengeId : null

  const [sort, setSort] = useState<SubmissionSort>('top')
  const { data: challenge, loading, error } = useChallenge(validId)
  const {
    submissions,
    isLoading: submissionsLoading,
    isFetchingNextPage,
    error: submissionsError,
    hasNextPage,
    fetchNextPage,
  } = useChallengeSubmissions(validId, sort)

  if (loading) {
    return (
      <main className="max-w-[1920px] mx-auto py-32 flex justify-center">
        <Spinner label="Loading challenge" />
      </main>
    )
  }

  if (error || !challenge) {
    return (
      <main className="max-w-[1920px] mx-auto px-8 py-32">
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error ?? 'Challenge not found.'}
        </p>
      </main>
    )
  }

  return (
    <main className="max-w-[1920px] mx-auto pb-20">
      <ChallengeHeroBanner challenge={challenge} backTo="/challenges" />
      <section className="py-16">
        <ChallengeSortTabs active={sort} onChange={setSort} />
        {submissionsLoading ? (
          <div className="py-12 flex justify-center">
            <Spinner label="Loading submissions" />
          </div>
        ) : submissionsError ? (
          <p
            className="max-w-[1600px] mx-auto px-8 py-12 text-center text-error border border-error-container/40 bg-error-container/10"
            role="alert"
          >
            {submissionsError}
          </p>
        ) : (
          <>
            <ChallengeSubmissionsMasonry submissions={submissions} />
            <InfiniteScrollSentinel
              hasNextPage={hasNextPage}
              isFetchingNextPage={isFetchingNextPage}
              onLoadMore={fetchNextPage}
              label="Loading more submissions"
            />
          </>
        )}
      </section>
    </main>
  )
}
