import type { SubmissionThumbnail } from '../../types/challenge'

type ChallengeBentoGridProps = {
  submissions: SubmissionThumbnail[]
}

const CELL_CLASSES = [
  'col-span-2 row-span-2',
  'col-span-1 row-span-1',
  'col-span-1 row-span-2',
  'col-span-2 row-span-1',
  'col-span-1 row-span-1 hidden lg:block',
  'col-span-1 row-span-1 hidden lg:block',
]

export function ChallengeBentoGrid({ submissions }: ChallengeBentoGridProps) {
  if (submissions.length === 0) {
    return (
      <div className="py-16 text-center text-on-surface-variant italic">
        No submissions yet.
      </div>
    )
  }
  const cells = submissions.slice(0, CELL_CLASSES.length)
  return (
    <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4 h-[600px] lg:h-[500px]">
      {cells.map((submission, i) => (
        <div
          key={submission.submissionId}
          className={`${CELL_CLASSES[i]} relative group overflow-hidden bg-surface-container`}
        >
          <img
            alt={submission.imageAlt}
            src={submission.imageUrl}
            loading="lazy"
            className="w-full h-full object-cover grayscale-[0.2] group-hover:scale-105 transition-transform duration-700"
          />
        </div>
      ))}
    </div>
  )
}
