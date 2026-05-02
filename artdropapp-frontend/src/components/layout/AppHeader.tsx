import { Search, ShieldUser, ShoppingBag, User } from 'lucide-react'
import { NavLink, useNavigate } from 'react-router-dom'
import { getToken } from '../../lib/auth'
import { useCurrentUser } from '../../hooks/useCurrentUser'

const navItems = [
  { to: '/', label: 'Discover', end: true },
  { to: '/circle', label: 'Circle', end: false },
  { to: '/challenges', label: 'Challenges', end: true },
]

export function AppHeader() {
  const navigate = useNavigate()
  const { user } = useCurrentUser()
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

      <nav className="hidden md:flex gap-12 items-center" aria-label="Main navigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              isActive
                ? 'font-headline text-lg tracking-tight text-on-surface border-b-2 border-on-surface pb-1 transition-colors duration-300'
                : 'font-body text-sm tracking-wide uppercase text-primary hover:text-on-surface transition-colors duration-300'
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="flex items-center gap-6">
        <button
          type="button"
          aria-label="Search"
          className="text-on-surface transition-transform active:scale-95"
        >
          <Search size={20} />
        </button>
        <button
          type="button"
          aria-label="Bag"
          className="text-on-surface transition-transform active:scale-95"
        >
          <ShoppingBag size={20} />
        </button>
        {isAdmin ? (
          <button
            type="button"
            aria-label="Admin"
            onClick={() => navigate('/admin')}
            className="text-on-surface transition-transform active:scale-95"
          >
            <ShieldUser size={20} />
          </button>
        ) : null}
        <button
          type="button"
          aria-label="Account"
          onClick={handleAccountClick}
          className="text-on-surface transition-transform active:scale-95"
        >
          <User size={20} />
        </button>
      </div>
    </header>
  )
}
