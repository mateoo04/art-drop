import { Outlet } from 'react-router-dom'
import { AppFooter } from './AppFooter'
import { AppHeader } from './AppHeader'

export function MainLayout() {
  return (
    <>
      <AppHeader />
      <Outlet />
      <AppFooter />
    </>
  )
}
