import type { SubmissionThumbnail } from '../../types/challenge'

type ChallengeHorizontalGridProps = {
  submissions: SubmissionThumbnail[]
}

export function ChallengeHorizontalGrid({ submissions }: ChallengeHorizontalGridProps) {
  if (submissions.length === 0) {
    return (
      <div className="py-16 text-center text-on-surface-variant italic">
        No submissions yet.
      </div>
    )
  }
  const hero = submissions[0]
  const rest = submissions.slice(1, 5)
  return (
    <div className="flex flex-col md:flex-row gap-6 overflow-hidden">
      <div className="w-full md:w-1/3 aspect-[4/5] bg-surface-container-low overflow-hidden">
        {hero ? (
          <img
            alt={hero.imageAlt}
            src={hero.imageUrl}
            loading="lazy"
            className="w-full h-full object-cover"
          />
        ) : null}
      </div>
      <div className="w-full md:w-2/3 flex flex-col gap-6">
        <div className="flex gap-6 h-1/2">
          {rest.slice(0, 2).map((s) => (
            <div
              key={s.submissionId}
              className="w-1/2 aspect-square bg-surface-container-low overflow-hidden"
            >
              <img
                alt={s.imageAlt}
                src={s.imageUrl}
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </div>
          ))}
        </div>
        <div className="flex gap-6 h-1/2">
          {rest.slice(2, 4).map((s, i) => (
            <div
              key={s.submissionId}
              className={`${i === 0 ? 'w-1/3' : 'w-2/3 aspect-[2/1]'} ${i === 0 ? 'aspect-square' : ''} bg-surface-container-low overflow-hidden`}
            >
              <img
                alt={s.imageAlt}
                src={s.imageUrl}
                loading="lazy"
                className="w-full h-full object-cover"
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
