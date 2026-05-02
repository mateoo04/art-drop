import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCurrentUser } from '../hooks/useCurrentUser'
import { getToken } from '../lib/auth'
import { Spinner } from './ui/Spinner'

export function AdminRoute({ children }: { children: ReactNode }) {
  if (!getToken()) {
    return <Navigate to="/login" replace />
  }
  const { user, loading } = useCurrentUser()
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
