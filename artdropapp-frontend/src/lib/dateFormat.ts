type DateInput = string | Date | null | undefined

function parseDate(value: DateInput): Date | null {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function fallbackFor(value: DateInput): string {
  return typeof value === 'string' ? value : ''
}

function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

export function formatEuDate(value: DateInput): string {
  const date = parseDate(value)
  if (!date) return fallbackFor(value)

  return `${pad2(date.getDate())}.${pad2(date.getMonth() + 1)}.${date.getFullYear()}.`
}

export function formatEuDateTime(value: DateInput): string {
  const date = parseDate(value)
  if (!date) return fallbackFor(value)

  return `${formatEuDate(date)} ${pad2(date.getHours())}:${pad2(date.getMinutes())}`
}
