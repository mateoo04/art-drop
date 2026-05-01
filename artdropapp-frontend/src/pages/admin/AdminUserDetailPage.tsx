import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAdminUserDetail } from '../../hooks/useAdminUserDetail'
import { SellerStatusBadge } from '../../components/SellerStatusBadge'
import { RevokeSellerModal } from '../../components/admin/RevokeSellerModal'
import { Button } from '../../components/ui/Button'

function formatDate(value: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString()
}

export function AdminUserDetailPage() {
  const { id } = useParams<{ id: string }>()
  const userId = id ? Number.parseInt(id, 10) : Number.NaN
  const { data, loading, error, refetch } = useAdminUserDetail(Number.isFinite(userId) ? userId : null)
  const [revokeOpen, setRevokeOpen] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  if (loading) {
    return <p className="text-on-surface-variant italic" role="status">Loading…</p>
  }
  if (error) {
    return <p className="text-error" role="alert">{error}</p>
  }
  if (!data) return null

  const u = data.user
  const isSeller = u.sellerStatus === 'APPROVED'

  return (
    <section>
      <Link to="/admin/users" className="text-sm text-on-surface-variant hover:underline">← Back to users</Link>
      <header className="flex items-center gap-4 mt-3 mb-6">
        <img src={u.avatarUrl ?? 'https://i.pravatar.cc/96'} alt="" className="w-16 h-16 rounded-full object-cover" />
        <div className="flex-1">
          <h2 className="font-headline text-2xl text-on-surface">{u.displayName}</h2>
          <p className="text-on-surface-variant">@{u.username} · {u.email}</p>
        </div>
        <SellerStatusBadge status={u.sellerStatus} />
      </header>

      {toast ? <p className="text-sm text-on-surface mb-4">{toast}</p> : null}

      {isSeller ? (
        <div className="mb-8">
          <Button variant="destructive" onClick={() => setRevokeOpen(true)}>
            Revoke seller status
          </Button>
        </div>
      ) : null}

      <h3 className="font-headline text-xl text-on-surface mb-3">Seller history</h3>
      {data.applicationHistory.length === 0 ? (
        <p className="text-on-surface-variant italic">No applications yet.</p>
      ) : (
        <ul className="space-y-4">
          {data.applicationHistory.map((app) => (
            <li key={app.id} className="border border-outline rounded-md p-4">
              <div className="flex items-center justify-between mb-2">
                <span className="font-medium">
                  {app.status} {app.revokedAt ? '· revoked' : ''}
                </span>
                <span className="text-sm text-on-surface-variant">
                  Submitted {formatDate(app.submittedAt)}
                </span>
              </div>
              <p className="text-sm text-on-surface bg-surface-variant rounded-md p-3 whitespace-pre-wrap">
                {app.message}
              </p>
              {app.decidedAt ? (
                <p className="text-sm text-on-surface-variant mt-2">
                  Decided {formatDate(app.decidedAt)}
                  {app.decisionReason ? ` — ${app.decisionReason}` : ''}
                </p>
              ) : null}
              {app.revokedAt ? (
                <p className="text-sm text-error mt-2">
                  Revoked {formatDate(app.revokedAt)}
                  {app.revokeReason ? ` — ${app.revokeReason}` : ''}
                </p>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      <RevokeSellerModal
        open={revokeOpen}
        userId={u.id}
        username={u.username}
        onClose={() => setRevokeOpen(false)}
        onRevoked={(count) => {
          setToast(`Seller status revoked. ${count} artwork${count === 1 ? '' : 's'} unlisted.`)
          void refetch()
        }}
      />
    </section>
  )
}
