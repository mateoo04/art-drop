import type { Challenge, ChallengeKind } from '../../types/challenge'
import { ChallengeBentoGrid } from './ChallengeBentoGrid'
import { ChallengeHorizontalGrid } from './ChallengeHorizontalGrid'

type ChallengeBlockProps = {
  challenge: Challenge
  layout: 'bento' | 'horizontal'
}

function kindLabel(kind: ChallengeKind | null): string {
  switch (kind) {
    case 'FEATURED':
      return 'Featured Challenge'
    case 'COMMUNITY_CHOICE':
      return 'Community Choice'
    case 'OPEN':
      return 'Open Call'
    default:
      return 'Challenge'
  }
}

export function ChallengeBlock({ challenge, layout }: ChallengeBlockProps) {
  return (
    <article className="flex flex-col gap-12">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-end">
        <div className="lg:col-span-7">
          <span className="text-xs uppercase tracking-[0.3em] text-tertiary mb-4 block font-bold">
            {kindLabel(challenge.kind)}
          </span>
          <h2 className="text-5xl md:text-6xl font-headline mb-6">{challenge.title}</h2>
          {challenge.description ? (
            <p className="text-on-surface-variant text-lg leading-relaxed mb-8 max-w-xl">
              {challenge.description}
            </p>
          ) : null}
          {challenge.quote ? (
            <blockquote className="font-headline italic text-3xl md:text-4xl text-on-surface/80 pl-0 max-w-2xl leading-snug">
              “{challenge.quote}”
            </blockquote>
          ) : null}
        </div>
        <div className="lg:col-span-5 flex justify-start lg:justify-end">
          <button
            type="button"
            onClick={() =>
              alert('Submitting artwork will be available once sign-in ships.')
            }
            className="bg-on-surface text-surface px-10 py-4 font-label uppercase tracking-widest text-xs hover:opacity-90 transition-opacity"
          >
            Submit Artwork
          </button>
        </div>
      </div>

      {layout === 'bento' ? (
        <ChallengeBentoGrid submissions={challenge.submissions} />
      ) : (
        <ChallengeHorizontalGrid submissions={challenge.submissions} />
      )}

      {challenge.submissionCount > challenge.submissions.length ? (
        <div className="flex justify-center mt-4">
          <button
            type="button"
            className="group flex items-center gap-2 text-xs uppercase tracking-widest font-bold border-b border-outline-variant/30 pb-2 hover:border-on-surface transition-all"
          >
            Show More Entries
            <span className="material-symbols-outlined text-sm group-hover:translate-x-1 transition-transform">
              arrow_forward
            </span>
          </button>
        </div>
      ) : null}
    </article>
  )
}
