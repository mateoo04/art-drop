import { useState } from 'react'
import { SellerApplicationModal } from '../SellerApplicationModal'
import { SellerStatusBadge } from '../SellerStatusBadge'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
import { useMySellerApplication } from '../../hooks/useMySellerApplication'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

export function SellerSection() {
  const { application, loading, error, refetch } = useMySellerApplication()
  const [modalOpen, setModalOpen] = useState(false)

  const status = application?.derivedSellerStatus ?? 'NONE'
  const cooldownActive =
    application?.canReapplyAt != null && new Date(application.canReapplyAt) > new Date()

  function canApply(): boolean {
    if (status === 'NONE') return true
    if ((status === 'REJECTED' || status === 'REVOKED') && !cooldownActive) return true
    return false
  }

  return (
    <section className="pt-12 pb-8">
      <div className="flex items-center gap-3 mb-4">
        <h2 className="font-headline text-2xl text-on-surface">Seller status</h2>
        <SellerStatusBadge status={status} />
      </div>

      {loading ? (
        <Spinner />
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : status === 'NONE' ? (
        <div>
          <p className="text-on-surface-variant mb-3">
            Become a verified seller to list your artworks for sale.
          </p>
          <Button onClick={() => setModalOpen(true)}>
            Apply to become a seller
          </Button>
        </div>
      ) : status === 'PENDING' ? (
        <div>
          <p className="text-on-surface-variant">
            Your application is under review. Submitted {formatDate(application!.submittedAt)}.
          </p>
          <p className="mt-3 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
            {application!.message}
          </p>
        </div>
      ) : status === 'APPROVED' ? (
        <p className="text-on-surface-variant">
          You're a verified seller (approved {formatDate(application!.decidedAt)}).
        </p>
      ) : (
        <div>
          <p className="text-on-surface-variant">
            {status === 'REVOKED'
              ? `Your seller status was revoked on ${formatDate(application!.revokedAt)}.`
              : `Your application was not approved on ${formatDate(application!.decidedAt)}.`}
          </p>
          {(status === 'REVOKED' ? application!.revokeReason : application!.decisionReason) ? (
            <p className="mt-2 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
              {status === 'REVOKED' ? application!.revokeReason : application!.decisionReason}
            </p>
          ) : null}
          {cooldownActive ? (
            <p className="mt-3 text-sm text-on-surface-variant">
              You can re-apply on {formatDate(application!.canReapplyAt)}.
            </p>
          ) : (
            <div className="mt-3">
              <Button onClick={() => setModalOpen(true)}>
                Apply again
              </Button>
            </div>
          )}
        </div>
      )}

      <SellerApplicationModal
        open={modalOpen && canApply()}
        onClose={() => setModalOpen(false)}
        onSubmitted={() => void refetch()}
      />
    </section>
  )
}
