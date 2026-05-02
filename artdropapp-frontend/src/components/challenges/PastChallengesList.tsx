import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { Challenge } from '../../types/challenge'

type PastChallengesListProps = {
  challenges: Challenge[]
}

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
  if (challenges.length === 0) return null

  return (
    <section className="max-w-[1600px] mx-auto px-8 py-20 border-t border-surface-container-highest">
      <div className="flex justify-between items-end mb-12">
        <h3 className="text-3xl font-headline text-on-surface">Past Exhibitions</h3>
        <button
          type="button"
          onClick={() => alert('Past challenges archive coming soon.')}
          className="text-xs uppercase tracking-widest font-bold text-on-surface-variant hover:text-on-surface transition-colors"
        >
          View Archive
        </button>
      </div>
      <div className="flex flex-col md:flex-row gap-8 md:gap-16 border-t border-surface-container-high pt-8">
        {challenges.map((challenge) => (
          <Link
            key={challenge.id}
            to={`/challenges/${challenge.id}`}
            className="flex-1 flex justify-between items-center group cursor-pointer border-b border-surface-container-high pb-8 hover:border-on-surface transition-colors text-left"
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
    </section>
  )
}
