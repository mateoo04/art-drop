import { X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { getToken } from '../../lib/auth'

const STORAGE_KEY = 'artdrop_demo_hint_seen'
const AUTO_HIDE_MS = 3000

export function DemoLoginHint() {
  const { t } = useTranslation()
  const location = useLocation()
  const previousPath = useRef(location.pathname)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const navigated = previousPath.current !== location.pathname
    previousPath.current = location.pathname

    if (navigated) {
      setVisible(false)
      return
    }

    if (location.pathname === '/login' || getToken()) {
      setVisible(false)
      return
    }

    if (localStorage.getItem(STORAGE_KEY) === 'true') {
      return
    }

    localStorage.setItem(STORAGE_KEY, 'true')
    setVisible(true)
  }, [location.pathname])

  useEffect(() => {
    if (!visible) {
      return
    }
    const timeout = window.setTimeout(() => setVisible(false), AUTO_HIDE_MS)
    return () => window.clearTimeout(timeout)
  }, [visible])

  if (!visible) return null

  return (
    <aside
      className="fixed bottom-5 right-5 z-50 w-[min(calc(100vw-2.5rem),22rem)] border border-outline-variant/20 bg-surface-container-lowest p-4 shadow-[0_20px_50px_rgba(45,52,53,0.18)]"
      role="status"
      aria-live="polite"
    >
      <button
        type="button"
        className="absolute right-2 top-2 inline-flex h-8 w-8 items-center justify-center text-on-surface-variant transition-colors hover:text-on-surface focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        onClick={() => setVisible(false)}
        aria-label={t('auth.demoHint.dismiss')}
      >
        <X size={16} aria-hidden />
      </button>
      <p className="pr-8 font-label text-[10px] uppercase tracking-widest text-outline">
        {t('auth.demoHint.kicker')}
      </p>
      <p className="mt-2 font-body text-sm leading-relaxed text-on-surface">
        {t('auth.demoHint.body')}
      </p>
      <Link
        to="/login"
        className="mt-3 inline-flex font-label text-[10px] uppercase tracking-widest text-on-surface underline decoration-outline-variant/50 underline-offset-4 transition-colors hover:text-primary"
        onClick={() => setVisible(false)}
      >
        {t('auth.demoHint.cta')}
      </Link>
    </aside>
  )
}
