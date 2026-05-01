import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { getToken } from '../lib/auth'

export function AdminRoute({ children }: { children: ReactNode }) {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }
  const { user, loading } = useCurrentUser()
  if (loading && !user) {
    return (
      <p className="py-24 text-center text-on-surface-variant italic" role="status">
        Loading…
      </p>
    )
  }
  const isAdmin = (user?.roles ?? []).includes('ROLE_ADMIN')
  if (!isAdmin) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
