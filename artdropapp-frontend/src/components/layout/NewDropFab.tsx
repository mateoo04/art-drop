import { Plus } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getToken } from '../../lib/auth'
import { useCurrentUser } from '../../hooks/useCurrentUser'

export function NewDropFab() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useCurrentUser()

  if (location.pathname === '/drop') return null
  if (!getToken() || !user) return null

  return (
    <button
      type="button"
      onClick={() => navigate('/drop')}
      aria-label="New drop"
      className="fixed bottom-8 right-8 z-40 flex items-center gap-2 bg-on-surface text-surface font-label text-sm uppercase tracking-[0.1em] px-6 py-4 shadow-lg hover:bg-primary transition-colors active:scale-95"
    >
      <Plus size={20} />
      New Drop
    </button>
  )
}
