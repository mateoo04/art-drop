type MediumFilterBarProps = {
  mediums: string[]
  active: string
  onChange: (medium: string) => void
}

export function MediumFilterBar({ mediums, active, onChange }: MediumFilterBarProps) {
  const options = ['All', ...mediums]
  return (
    <div className="flex items-center gap-3 overflow-x-auto py-8 no-scrollbar border-b border-outline-variant/10 mb-12">
      {options.map((medium) => {
        const isActive = medium === active
        return (
          <button
            key={medium}
            type="button"
            onClick={() => onChange(medium)}
            className={
              isActive
                ? 'bg-primary text-on-primary px-6 py-2 text-xs font-semibold tracking-widest uppercase transition-all whitespace-nowrap'
                : 'bg-secondary-container text-on-secondary-container px-6 py-2 text-xs font-semibold tracking-widest uppercase hover:bg-outline-variant/20 transition-all whitespace-nowrap'
            }
          >
            {medium}
          </button>
        )
      })}
    </div>
  )
}
