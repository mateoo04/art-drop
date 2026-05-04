import { useLayoutEffect, useRef } from 'react'
import { Outlet } from 'react-router-dom'
import { AppFooter } from './AppFooter'
import { AppHeader } from './AppHeader'
import { NewDropFab } from './NewDropFab'
import { AuthPromptProvider } from '../../contexts/AuthPromptContext'
import { SearchOverlayProvider } from '../../contexts/SearchOverlayContext'

export function MainLayout() {
  const headerShellRef = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    const el = headerShellRef.current
    if (!el) return
    const sync = () => {
      const h = el.getBoundingClientRect().height
      document.documentElement.style.setProperty('--app-header-height', `${Math.round(h)}px`)
    }
    sync()
    const ro = new ResizeObserver(sync)
    ro.observe(el)
    window.addEventListener('resize', sync)
    return () => {
      ro.disconnect()
      window.removeEventListener('resize', sync)
    }
  }, [])

  return (
    <AuthPromptProvider>
      <SearchOverlayProvider>
        <div ref={headerShellRef} className="relative z-50">
          <AppHeader />
        </div>
        <Outlet />
        <AppFooter />
        <NewDropFab />
      </SearchOverlayProvider>
    </AuthPromptProvider>
  )
}
