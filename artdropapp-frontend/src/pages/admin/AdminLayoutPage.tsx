import { NavLink, Outlet } from 'react-router-dom'

export function AdminLayoutPage() {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-4 py-2 rounded-md font-medium ${isActive ? 'bg-primary-container text-on-primary-container' : 'text-on-surface-variant hover:bg-surface-variant'}`

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <h1 className="font-headline text-3xl text-on-surface mb-6">Admin</h1>
      <nav className="flex gap-2 border-b border-outline mb-8 pb-2">
        <NavLink to="/admin/users" end className={linkClass}>User directory</NavLink>
      </nav>
      <Outlet />
    </main>
  )
}
