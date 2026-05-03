import { useTranslation } from 'react-i18next'
import type { SellerStatus } from '../types/seller'

const TONES: Record<SellerStatus, string> = {
  NONE: 'bg-surface-variant text-on-surface-variant',
  PENDING: 'bg-tertiary-container text-on-tertiary-container',
  APPROVED: 'bg-primary-container text-on-primary-container',
  REJECTED: 'bg-error-container text-on-error-container',
  REVOKED: 'bg-error-container text-on-error-container',
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
      className={`inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-full ${TONES[status]}`}
    >
      {labels[status]}
    </span>
  )
}
