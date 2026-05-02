type SpinnerProps = {
  label?: string
  className?: string
}

export function Spinner({ label = 'Loading', className }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label={label}
      className={className ? `editorial-spinner ${className}` : 'editorial-spinner'}
    />
  )
}
