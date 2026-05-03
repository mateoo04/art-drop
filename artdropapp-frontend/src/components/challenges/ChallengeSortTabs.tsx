import { useTranslation } from 'react-i18next'
import type { SubmissionSort } from '../../api/challengesApi'

type ChallengeSortTabsProps = {
  active: SubmissionSort
  onChange: (sort: SubmissionSort) => void
}

export function ChallengeSortTabs({ active, onChange }: ChallengeSortTabsProps) {
  const { t } = useTranslation()

  const tabs: { key: SubmissionSort; label: string }[] = [
    { key: 'top', label: t('challenges.sort.top') },
    { key: 'recent', label: t('challenges.sort.recent') },
  ]

  return (
    <div
      role="tablist"
      aria-label={t('challenges.sort.label')}
      className="border-b border-outline-variant/30 mb-12"
    >
      <div className="flex gap-10 max-w-[1600px] mx-auto px-8">
        {tabs.map((tab) => {
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
