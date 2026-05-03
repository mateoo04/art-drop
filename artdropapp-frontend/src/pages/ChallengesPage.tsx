import { useTranslation } from 'react-i18next'
import { ActiveChallengeRow } from '../components/challenges/ActiveChallengeRow'
import { ChallengeHeroBanner } from '../components/challenges/ChallengeHeroBanner'
import { PastChallengesList } from '../components/challenges/PastChallengesList'
import { Spinner } from '../components/ui/Spinner'
import { useChallenges } from '../hooks/useChallenges'
import type { Challenge } from '../types/challenge'

function partition(challenges: Challenge[]) {
  let featured: Challenge | null = null
  const activeOthers: Challenge[] = []
  const past: Challenge[] = []

  for (const challenge of challenges) {
    if (challenge.status === 'ENDED') {
      past.push(challenge)
    } else if (challenge.kind === 'FEATURED' && !featured) {
      featured = challenge
    } else {
      activeOthers.push(challenge)
    }
  }

  return { featured, activeOthers, past }
}

export function ChallengesPage() {
  const { t } = useTranslation()
  const { data, loading, error } = useChallenges()

  if (loading) {
    return (
      <main className="max-w-[1920px] mx-auto py-32 flex justify-center">
        <Spinner label={t('challenges.loading')} />
      </main>
    )
  }

  if (error) {
    return (
      <main className="max-w-[1920px] mx-auto px-8 py-32">
        <p
          className="py-12 text-center text-error border border-error-container/40 bg-error-container/10"
          role="alert"
        >
          {error}
        </p>
      </main>
    )
  }

  if (!data || data.length === 0) {
    return (
      <main className="max-w-[1920px] mx-auto px-8 py-32 text-center text-on-surface-variant">
        {t('challenges.empty')}
      </main>
    )
  }

  const { featured, activeOthers, past } = partition(data)

  return (
    <main className="max-w-[1920px] mx-auto pb-20">
      {featured ? (
        <ChallengeHeroBanner
          challenge={featured}
          secondaryAction={{ label: t('challenges.learnMore'), to: `/challenges/${featured.id}` }}
        />
      ) : null}
      {activeOthers.map((challenge, i) => (
        <ActiveChallengeRow
          key={challenge.id}
          challenge={challenge}
          bordered={i > 0 || featured !== null}
        />
      ))}
      <PastChallengesList challenges={past} />
    </main>
  )
}
