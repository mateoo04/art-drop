import type { SubmissionSort } from '../../api/challengesApi'

type ChallengeSortTabsProps = {
  active: SubmissionSort
  onChange: (sort: SubmissionSort) => void
}

const TABS: { key: SubmissionSort; label: string }[] = [
  { key: 'top', label: 'Top' },
  { key: 'recent', label: 'Recent' },
]

export function ChallengeSortTabs({ active, onChange }: ChallengeSortTabsProps) {
  return (
    <div
      role="tablist"
      aria-label="Sort submissions"
      className="border-b border-outline-variant/30 mb-12"
    >
      <div className="flex gap-10 max-w-[1600px] mx-auto px-8">
        {TABS.map((tab) => {
          const isActive = tab.key === active
          return (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => onChange(tab.key)}
              className={
                isActive
                  ? 'py-4 -mb-px border-b-2 border-on-surface text-on-surface text-xs uppercase tracking-widest font-bold'
                  : 'py-4 text-on-surface-variant hover:text-on-surface transition-colors text-xs uppercase tracking-widest font-bold'
              }
            >
              {tab.label}
            </button>
          )
        })}
      </div>
    </div>
  )
}
