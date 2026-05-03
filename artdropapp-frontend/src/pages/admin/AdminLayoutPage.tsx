import { useTranslation } from 'react-i18next'
import { NavLink, Outlet } from 'react-router-dom'

export function AdminLayoutPage() {
  const { t } = useTranslation()

  const tabClass = (active: boolean) =>
    [
      'relative pb-3 pt-0.5 text-xs font-semibold uppercase tracking-[0.15em] transition-colors',
      active
        ? 'text-on-surface border-b-[3px] border-on-surface -mb-[2px]'
        : 'text-on-surface-variant border-b-2 border-transparent hover:text-on-surface -mb-px',
    ].join(' ')

  return (
    <main className="max-w-[1440px] mx-auto px-8 pt-4 pb-24">
      <h1 className="font-headline text-3xl text-on-surface mb-6">{t('admin.layout.title')}</h1>
      <nav className="flex gap-10 mb-8 border-b border-outline-variant/35">
        <NavLink to="/admin/users" end className={({ isActive }) => tabClass(isActive)}>{t('admin.layout.userDirectory')}</NavLink>
        <NavLink to="/admin/challenges" end className={({ isActive }) => tabClass(isActive)}>{t('admin.layout.challenges')}</NavLink>
      </nav>
      <Outlet />
    </main>
  )
}
