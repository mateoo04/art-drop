import type { AdminPrimaryRole } from '../../types/seller'

const TONES: Record<AdminPrimaryRole, string> = {
  USER: 'border-outline-variant/30 text-on-surface-variant',
  ADMIN: 'border-on-surface/20 text-on-surface',
}

export function RoleBadge({ role, label }: { role: AdminPrimaryRole; label: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-none border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] bg-transparent ${TONES[role]}`}
    >
      {label}
    </span>
  )
}
