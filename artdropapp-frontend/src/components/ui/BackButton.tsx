import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'

type BackButtonProps = {
  to: string
  label: string
  tone?: 'default' | 'on-image'
  className?: string
}

export function BackButton({ to, label, tone = 'default', className }: BackButtonProps) {
  const colors =
    tone === 'on-image'
      ? 'text-white/90 hover:text-white drop-shadow-lg'
      : 'text-on-surface-variant hover:text-on-surface'
  return (
    <Link
      to={to}
      className={`inline-flex items-center group transition-colors ${colors}${
        className ? ` ${className}` : ''
      }`}
    >
      <ArrowLeft
        size={20}
        className="mr-2 group-hover:-translate-x-1 transition-transform"
      />
      <span className="font-label text-sm uppercase tracking-[0.1em]">{label}</span>
    </Link>
  )
}
