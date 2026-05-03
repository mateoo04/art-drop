import { useTranslation } from 'react-i18next'

type SpinnerProps = {
  label?: string
  className?: string
}

export function Spinner({ label, className }: SpinnerProps) {
  const { t } = useTranslation()
  const finalLabel = label ?? t('common.loading')
  return (
    <span
      role="status"
      aria-label={finalLabel}
      className={className ? `editorial-spinner ${className}` : 'editorial-spinner'}
    />
  )
}
