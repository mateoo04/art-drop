import { cloneElement, isValidElement, type ReactElement, type ReactNode } from 'react'

type FormFieldProps = {
  label: string
  htmlFor: string
  error?: string
  children: ReactNode
}

export function FormField({ label, htmlFor, error, children }: FormFieldProps) {
  const errorId = `${htmlFor}-error`
  const describedBy = error ? errorId : undefined

  const childWithAria =
    isValidElement(children) && describedBy
      ? cloneElement(children as ReactElement<{ 'aria-describedby'?: string }>, {
          'aria-describedby': describedBy,
        })
      : children

  return (
    <div className="space-y-1.5">
      <label
        htmlFor={htmlFor}
        className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant"
      >
        {label}
      </label>
      {childWithAria}
      {error ? (
        <p id={errorId} role="alert" className="font-body text-xs text-error pt-1">
          {error}
        </p>
      ) : null}
    </div>
  )
}
