import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'
import { Spinner } from '../ui/Spinner'
import { RevokeSellerModal } from './RevokeSellerModal'
import { SellerStatusBadge } from '../SellerStatusBadge'
import { RoleBadge } from './RoleBadge'
import { useAdminUserDetail } from '../../hooks/useAdminUserDetail'
import type { AdminUserSummary } from '../../types/seller'

/** Overrides default Button padding (py-5 px-6) for compact admin panel actions */
const PANEL_BTN = '!py-2 !px-3 !text-[10px] !tracking-[0.12em]'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

type Props = {
  userId: number
  summary: AdminUserSummary
  /** Increment after the user list refetches so detail stays in sync (e.g. approve/reject). */
  refreshSignal: number
  currentUserId: number | null
  onPromote: () => void
  onGrantSeller: () => void
  onDeactivateRequest: () => void
  onReactivate: () => void
  onApproveRequest: (appId: number, username: string) => void
  onRejectRequest: (appId: number, username: string) => void
  onListRefetch: () => void
}

export function AdminUserExpandPanel({
  userId,
  summary,
  refreshSignal,
  currentUserId,
  onPromote,
  onGrantSeller,
  onDeactivateRequest,
  onReactivate,
  onApproveRequest,
  onRejectRequest,
  onListRefetch,
}: Props) {
  const { t } = useTranslation()
  const { data, loading, error, refetch } = useAdminUserDetail(userId)
  const [revokeOpen, setRevokeOpen] = useState(false)
  const [localToast, setLocalToast] = useState<string | null>(null)

  const prevRefreshSignal = useRef(refreshSignal)
  useEffect(() => {
    if (prevRefreshSignal.current === refreshSignal) return
    prevRefreshSignal.current = refreshSignal
    void refetch()
  }, [refreshSignal, refetch])

  const u = data?.user ?? summary
  const pending = data?.user.pendingApplication ?? summary.pendingApplication
  const isSeller = u.sellerStatus === 'APPROVED'
  const isSelf = currentUserId != null && u.id === currentUserId
  const showPromote = u.primaryRole !== 'ADMIN'
  const showGrant = u.sellerStatus !== 'APPROVED'
  const showDeactivate = u.enabled && !isSelf
  const showReactivate = !u.enabled

  return (
    <div className="relative w-full border-t border-outline-variant/20 bg-surface-container-low px-4 py-5 space-y-6">
      {localToast ? <p className="text-sm text-on-surface">{localToast}</p> : null}

      {loading ? (
        <div className="py-6 flex justify-center">
          <Spinner />
        </div>
      ) : error ? (
        <p className="text-error text-sm" role="alert">{error}</p>
      ) : (
        <>
          <div className="flex flex-wrap items-start gap-4">
            <img src={u.avatarUrl ?? 'https://i.pravatar.cc/96'} alt="" className="w-14 h-14 rounded-full object-cover shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex flex-wrap items-center gap-2 mb-1">
                <h3 className="font-headline text-lg text-on-surface">{u.displayName}</h3>
                <RoleBadge role={u.primaryRole} label={u.primaryRole === 'ADMIN' ? t('admin.filters.roles.admin') : t('admin.filters.roles.user')} />
                <SellerStatusBadge status={u.sellerStatus} />
              </div>
              <p className="text-sm text-on-surface-variant">@{u.username} · {u.email}</p>
            </div>
          </div>

          <div>
            <h4 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
              {t('admin.users.expand.actions')}
            </h4>
            <div className="flex flex-wrap gap-2">
              {showPromote ? (
                <Button variant="outline" className={PANEL_BTN} onClick={onPromote}>{t('admin.users.promoteAdmin')}</Button>
              ) : null}
              {showGrant ? (
                <Button variant="outline" className={PANEL_BTN} onClick={onGrantSeller}>{t('admin.users.grantSeller')}</Button>
              ) : null}
              {showDeactivate ? (
                <Button variant="outline-destructive" className={PANEL_BTN} onClick={onDeactivateRequest}>{t('admin.users.deactivate')}</Button>
              ) : null}
              {showReactivate ? (
                <Button variant="outline" className={PANEL_BTN} onClick={onReactivate}>{t('admin.users.reactivate')}</Button>
              ) : null}
              {isSeller ? (
                <Button variant="outline-destructive" className={PANEL_BTN} onClick={() => setRevokeOpen(true)}>
                  {t('admin.userDetail.revokeSellerStatus')}
                </Button>
              ) : null}
            </div>
          </div>

          {pending ? (
            <div>
              <h4 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-2">
                {t('admin.users.expand.pendingSellerTitle')}
              </h4>
              <p className="text-sm text-on-surface bg-surface-container-lowest border border-outline-variant/15 p-3 whitespace-pre-wrap mb-3">
                {pending.message}
              </p>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-xs text-on-surface-variant">
                  {t('admin.users.submitted', { date: formatDate(pending.submittedAt) })}
                </span>
                <div className="flex flex-wrap gap-2 w-full sm:w-auto justify-end">
                  <Button
                    variant="outline-destructive"
                    className={PANEL_BTN}
                    onClick={() => onRejectRequest(pending.id, u.username)}
                  >
                    {t('admin.users.reject')}
                  </Button>
                  <Button variant="outline" className={PANEL_BTN} onClick={() => onApproveRequest(pending.id, u.username)}>{t('admin.users.approve')}</Button>
                </div>
              </div>
            </div>
          ) : null}

          <div>
            <h4 className="font-headline text-base text-on-surface mb-3">{t('admin.userDetail.sellerHistory')}</h4>
            {!data || data.applicationHistory.length === 0 ? (
              <p className="text-on-surface-variant italic text-sm">{t('admin.userDetail.noApplications')}</p>
            ) : (
              <ul className="space-y-3">
                {data.applicationHistory.map((app) => (
                  <li key={app.id} className="border border-outline-variant/15 bg-surface-container-lowest p-3">
                    <div className="flex items-center justify-between mb-2 gap-2 flex-wrap">
                      <span className="font-medium text-sm">
                        {app.status}
                        {app.revokedAt ? ` · ${t('admin.userDetail.revokedSuffix')}` : ''}
                      </span>
                      <span className="text-xs text-on-surface-variant">
                        {t('admin.userDetail.submitted', { date: formatDate(app.submittedAt) })}
                      </span>
                    </div>
                    <p className="text-sm text-on-surface bg-surface-variant rounded-md p-2 whitespace-pre-wrap">
                      {app.message}
                    </p>
                    {app.decidedAt ? (
                      <p className="text-xs text-on-surface-variant mt-2">
                        {t('admin.userDetail.decided', { date: formatDate(app.decidedAt) })}
                        {app.decisionReason ? ` — ${app.decisionReason}` : ''}
                      </p>
                    ) : null}
                    {app.revokedAt ? (
                      <p className="text-xs text-error mt-2">
                        {t('admin.userDetail.revoked', { date: formatDate(app.revokedAt) })}
                        {app.revokeReason ? ` — ${app.revokeReason}` : ''}
                      </p>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}

      <RevokeSellerModal
        open={revokeOpen}
        userId={u.id}
        username={u.username}
        onClose={() => setRevokeOpen(false)}
        onRevoked={(count) => {
          setLocalToast(t('admin.userDetail.toastRevoked', { count }))
          void refetch()
          onListRefetch()
        }}
      />
    </div>
  )
}
