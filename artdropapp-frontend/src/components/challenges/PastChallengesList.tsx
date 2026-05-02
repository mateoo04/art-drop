import { ArrowRight } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Challenge } from '../../types/challenge'

type PastChallengesListProps = {
  challenges: Challenge[]
}

const INITIAL_COUNT = 6
const PAGE_SIZE = 6

const MONTH_FORMATTER = new Intl.DateTimeFormat('en-US', {
  month: 'long',
  year: 'numeric',
})

function formatEndedOn(endsAt: string | null): string {
  if (!endsAt) return ''
  const date = new Date(endsAt)
  return Number.isNaN(date.getTime()) ? '' : MONTH_FORMATTER.format(date)
}

export function PastChallengesList({ challenges }: PastChallengesListProps) {
  const [visible, setVisible] = useState(INITIAL_COUNT)

  if (challenges.length === 0) return null

  const visibleChallenges = challenges.slice(0, visible)
  const hasMore = visible < challenges.length

  return (
    <section className="max-w-[1600px] mx-auto px-8 py-20 border-t border-surface-container-highest">
      <h3 className="text-3xl font-headline text-on-surface mb-12">Past Exhibitions</h3>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-x-16 gap-y-0 border-t border-surface-container-high pt-8">
        {visibleChallenges.map((challenge) => (
          <Link
            key={challenge.id}
            to={`/challenges/${challenge.id}`}
            className="flex justify-between items-center group cursor-pointer border-b border-surface-container-high py-8 hover:border-on-surface transition-colors text-left"
          >
            <div>
              <h4 className="text-xl font-headline text-on-surface mb-2 group-hover:italic transition-all">
                {challenge.title}
              </h4>
              <p className="text-sm text-on-surface-variant font-light">
                {formatEndedOn(challenge.endsAt)}
              </p>
            </div>
            <ArrowRight
              size={20}
              className="text-on-surface-variant group-hover:text-on-surface transition-colors"
            />
          </Link>
        ))}
      </div>
      {hasMore ? (
        <div className="flex justify-center mt-12">
          <button
            type="button"
            onClick={() => setVisible((v) => v + PAGE_SIZE)}
            className="text-xs uppercase tracking-widest font-bold text-on-surface-variant hover:text-on-surface transition-colors border-b border-outline-variant/30 hover:border-on-surface pb-2"
          >
            Show more past exhibitions
          </button>
        </div>
      ) : null}
    </section>
  )
}
