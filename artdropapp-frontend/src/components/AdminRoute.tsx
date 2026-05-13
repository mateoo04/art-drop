import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { getToken } from '../lib/auth'
import { Spinner } from './ui/Spinner'

export function AdminRoute({ children }: { children: ReactNode }) {
  const hasToken = Boolean(getToken())
  const { user, loading } = useCurrentUser()
  if (!hasToken) {
    return <Navigate to="/login" replace />
  }
  if (loading && !user) {
    return (
      <div className="py-24 flex justify-center">
        <Spinner />
      </div>
    )
  }
  const isAdmin = (user?.roles ?? []).includes('ROLE_ADMIN')
  if (!isAdmin) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
