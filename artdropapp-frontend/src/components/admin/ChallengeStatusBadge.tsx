import { useTranslation } from 'react-i18next'

const TONES: Record<string, string> = {
  UPCOMING: 'border-tertiary/40 text-tertiary',
  ACTIVE: 'border-emerald-700/30 text-emerald-800',
  ENDED: 'border-outline-variant/30 text-on-surface-variant',
}

export function ChallengeStatusBadge({ status }: { status: string | null }) {
  const { t } = useTranslation()
  const key = status ?? 'ENDED'
  const tone = TONES[key] ?? TONES.ENDED
  const label = t(`admin.challenges.statusLabel.${key}`, { defaultValue: key })
  return (
    <span
      className={`inline-flex items-center rounded-none border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] bg-transparent ${tone}`}
    >
      {label}
    </span>
  )
}
