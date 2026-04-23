import { forwardRef, type ComponentPropsWithoutRef } from 'react'

type InputProps = ComponentPropsWithoutRef<'input'> & {
  invalid?: boolean
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { invalid = false, className, ...rest },
  ref,
) {
  const base =
    'w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 disabled:cursor-not-allowed'
  const borderClass = invalid
    ? 'border border-error focus:border-error'
    : 'border border-outline-variant/15 focus:border-on-surface'

  return (
    <input
      ref={ref}
      aria-invalid={invalid || undefined}
      className={[base, borderClass, className ?? ''].filter(Boolean).join(' ')}
      {...rest}
    />
  )
})
