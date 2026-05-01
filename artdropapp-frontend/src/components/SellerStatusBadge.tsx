import type { SellerStatus } from '../types/seller'

const LABELS: Record<SellerStatus, string> = {
  NONE: 'Not a seller',
  PENDING: 'Pending',
  APPROVED: 'Verified seller',
  REJECTED: 'Rejected',
  REVOKED: 'Revoked',
}

const TONES: Record<SellerStatus, string> = {
  NONE: 'bg-surface-variant text-on-surface-variant',
  PENDING: 'bg-tertiary-container text-on-tertiary-container',
  APPROVED: 'bg-primary-container text-on-primary-container',
  REJECTED: 'bg-error-container text-on-error-container',
  REVOKED: 'bg-error-container text-on-error-container',
}

export function SellerStatusBadge({ status }: { status: SellerStatus }) {
  return (
    <span
      className={`inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-full ${TONES[status]}`}
    >
      {LABELS[status]}
    </span>
  )
}
