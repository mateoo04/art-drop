import { useTranslation } from 'react-i18next'
import type { SellerStatus } from '../types/seller'

const TONES: Record<SellerStatus, string> = {
  NONE: 'border-outline-variant/30 text-on-surface-variant',
  PENDING: 'border-tertiary/40 text-tertiary',
  APPROVED: 'border-emerald-700/30 text-emerald-800',
  REJECTED: 'border-error/30 text-error',
  REVOKED: 'border-error/30 text-error',
}

export function SellerStatusBadge({ status }: { status: SellerStatus }) {
  const { t } = useTranslation()

  const labels: Record<SellerStatus, string> = {
    NONE: t('account.seller.status.none'),
    PENDING: t('account.seller.status.pending'),
    APPROVED: t('account.seller.status.approved'),
    REJECTED: t('account.seller.status.rejected'),
    REVOKED: t('account.seller.status.revoked'),
  }

  return (
    <span
      className={`inline-flex items-center rounded-none border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] bg-transparent ${TONES[status]}`}
    >
      {labels[status]}
    </span>
  )
}
