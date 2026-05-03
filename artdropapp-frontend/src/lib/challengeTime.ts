import type { TFunction } from 'i18next'

export type ChallengeRemaining = {
  days: number
  hours: number
  minutes: number
  ended: boolean
}

/** Parses ends-at into remaining parts; null if unknown date. */
export function getChallengeRemaining(endsAt: string | null): ChallengeRemaining | null {
  if (!endsAt) return null
  const end = new Date(endsAt).getTime()
  if (Number.isNaN(end)) return null
  const diff = end - Date.now()
  if (diff <= 0) return { days: 0, hours: 0, minutes: 0, ended: true }
  const minutes = Math.floor(diff / 60_000) % 60
  const hours = Math.floor(diff / 3_600_000) % 24
  const days = Math.floor(diff / 86_400_000)
  return { days, hours, minutes, ended: false }
}

/** Compact label for cards (e.g. "2 days left", "1 week left"). */
export function formatChallengeDeadlineShort(
  t: TFunction,
  endsAt: string | null,
): string | null {
  const r = getChallengeRemaining(endsAt)
  if (!r) return null
  if (r.ended) return t('home.activeChallenges.timeLeft.ended')
  const diffMs =
    endsAt && !Number.isNaN(new Date(endsAt).getTime())
      ? new Date(endsAt).getTime() - Date.now()
      : 0
  const totalDaysFloat = diffMs / 86_400_000
  const totalHours = Math.floor(diffMs / 3_600_000)

  if (totalDaysFloat >= 7) {
    const weeks = Math.floor(totalDaysFloat / 7)
    return t('home.activeChallenges.timeLeft.weeks', { count: Math.max(1, weeks) })
  }
  if (totalDaysFloat >= 1) {
    const days = Math.max(1, Math.floor(totalDaysFloat))
    return t('home.activeChallenges.timeLeft.days', { count: days })
  }
  return t('home.activeChallenges.timeLeft.hours', { count: Math.max(1, totalHours) })
}
