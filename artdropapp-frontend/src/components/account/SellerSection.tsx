import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { SellerApplicationModal } from '../SellerApplicationModal'
import { SellerStatusBadge } from '../SellerStatusBadge'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
import { useMySellerApplication } from '../../hooks/useMySellerApplication'
import { formatEuDate } from '../../lib/dateFormat'

export function SellerSection() {
  const { application, loading, error, refetch } = useMySellerApplication()
  const [modalOpen, setModalOpen] = useState(false)
  const { t } = useTranslation()

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
        <h2 className="font-headline text-2xl text-on-surface">{t('account.seller.title')}</h2>
        <SellerStatusBadge status={status} />
      </div>

      {loading ? (
        <Spinner />
      ) : error ? (
        <p className="text-error" role="alert">{error}</p>
      ) : status === 'NONE' ? (
        <div>
          <p className="text-on-surface-variant mb-3">
            {t('account.seller.becomeSeller')}
          </p>
          <Button onClick={() => setModalOpen(true)}>
            {t('account.seller.applyCta')}
          </Button>
        </div>
      ) : status === 'PENDING' ? (
        <div>
          <p className="text-on-surface-variant">
            {t('account.seller.pending', { date: formatEuDate(application!.submittedAt) })}
          </p>
          <p className="mt-3 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
            {application!.message}
          </p>
        </div>
      ) : status === 'APPROVED' ? (
        <p className="text-on-surface-variant">
          {t('account.seller.approved', { date: formatEuDate(application!.decidedAt) })}
        </p>
      ) : (
        <div>
          <p className="text-on-surface-variant">
            {status === 'REVOKED'
              ? t('account.seller.revoked', { date: formatEuDate(application!.revokedAt) })
              : t('account.seller.rejected', { date: formatEuDate(application!.decidedAt) })}
          </p>
          {(status === 'REVOKED' ? application!.revokeReason : application!.decisionReason) ? (
            <p className="mt-2 text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
              {status === 'REVOKED' ? application!.revokeReason : application!.decisionReason}
            </p>
          ) : null}
          {cooldownActive ? (
            <p className="mt-3 text-sm text-on-surface-variant">
              {t('account.seller.cooldown', { date: formatEuDate(application!.canReapplyAt) })}
            </p>
          ) : (
            <div className="mt-3">
              <Button onClick={() => setModalOpen(true)}>
                {t('account.seller.applyAgain')}
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
