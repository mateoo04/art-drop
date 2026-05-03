import { forwardRef, type ComponentPropsWithoutRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

type Variant = 'primary' | 'secondary' | 'ghost' | 'destructive'

type ButtonProps = ComponentPropsWithoutRef<'button'> & {
  variant?: Variant
  loading?: boolean
  fullWidth?: boolean
  children: ReactNode
}

const VARIANT_CLASSES: Record<Variant, string> = {
  primary:
    'bg-on-surface text-surface hover:opacity-90 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
  secondary:
    'bg-surface-container-lowest text-on-surface border border-outline-variant/15 hover:bg-surface-container-low active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
  ghost:
    'bg-transparent text-on-surface hover:text-outline disabled:opacity-50 disabled:cursor-not-allowed',
  destructive:
    'bg-error text-on-error hover:opacity-90 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', loading = false, fullWidth = false, disabled, className, children, type = 'button', ...rest },
  ref,
) {
  const { t } = useTranslation()
  const widthClass = fullWidth ? 'w-full' : ''
  const base =
    'inline-flex items-center justify-center py-5 px-6 font-label text-[11px] uppercase tracking-[0.2em] font-semibold transition-all duration-200 rounded-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-on-surface focus-visible:ring-offset-2 focus-visible:ring-offset-surface'

  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      aria-busy={loading}
      className={[base, VARIANT_CLASSES[variant], widthClass, className ?? ''].filter(Boolean).join(' ')}
      {...rest}
    >
      {loading ? t('common.pleaseWait') : children}
    </button>
  )
})
