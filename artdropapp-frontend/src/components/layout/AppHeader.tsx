import { useTranslation } from 'react-i18next'
import { Search, ShieldUser, ShoppingBag, User } from 'lucide-react'
import { NavLink, useNavigate } from 'react-router-dom'
import { getToken } from '../../lib/auth'
import { useCurrentUser } from '../../hooks/useCurrentUser'

export function AppHeader() {
  const navigate = useNavigate()
  const { user } = useCurrentUser()
  const { t } = useTranslation()
  const isAdmin = (user?.roles ?? []).includes('ROLE_ADMIN')

  const handleAccountClick = () => {
    navigate(getToken() ? '/account' : '/login')
  }

  return (
    <header className="bg-surface flex justify-between items-center w-full px-8 py-6 max-w-[1920px] mx-auto sticky top-0 z-50">
      <NavLink
        to="/"
        className="font-headline text-2xl font-bold tracking-tighter text-on-surface"
      >
        ArtDrop
      </NavLink>

      <div className="flex items-center gap-6">
        <button
          type="button"
          aria-label={t('nav.search')}
          className="text-on-surface transition-transform active:scale-95"
        >
          <Search size={20} />
        </button>
        <button
          type="button"
          aria-label={t('nav.bag')}
          className="text-on-surface transition-transform active:scale-95"
        >
          <ShoppingBag size={20} />
        </button>
        {isAdmin ? (
          <button
            type="button"
            aria-label={t('nav.admin')}
            onClick={() => navigate('/admin/users')}
            className="text-on-surface transition-transform active:scale-95"
          >
            <ShieldUser size={20} />
          </button>
        ) : null}
        <button
          type="button"
          aria-label={t('nav.account')}
          onClick={handleAccountClick}
          className="text-on-surface transition-transform active:scale-95"
        >
          <User size={20} />
        </button>
      </div>
    </header>
  )
}
