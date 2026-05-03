type Props = {
  expanded: boolean
  onToggle: () => void
  labelExpand: string
  labelCollapse: string
  className?: string
}

export function ExpandRowToggle({ expanded, onToggle, labelExpand, labelCollapse, className }: Props) {
  return (
    <button
      type="button"
      className={[
        'shrink-0 inline-flex items-center justify-center w-8 h-8 rounded-none border-0 bg-transparent text-on-surface-variant transition-colors hover:bg-transparent hover:text-on-surface focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-on-surface focus-visible:ring-offset-2 focus-visible:ring-offset-surface',
        className ?? '',
      ].filter(Boolean).join(' ')}
      aria-expanded={expanded}
      aria-label={expanded ? labelCollapse : labelExpand}
      onClick={(e) => {
        e.stopPropagation()
        onToggle()
      }}
    >
      <svg
        className={`w-4 h-4 transition-transform duration-200 ${expanded ? '-rotate-180' : ''}`}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
      >
        <path d="M6 9l6 6 6-6" />
      </svg>
    </button>
  )
}
