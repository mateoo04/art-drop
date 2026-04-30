import { Outlet } from 'react-router-dom'
import { AppFooter } from './AppFooter'
import { AppHeader } from './AppHeader'
import { AuthPromptProvider } from '../../contexts/AuthPromptContext'

export function MainLayout() {
  return (
    <AuthPromptProvider>
      <AppHeader />
      <Outlet />
      <AppFooter />
    </AuthPromptProvider>
  )
}
