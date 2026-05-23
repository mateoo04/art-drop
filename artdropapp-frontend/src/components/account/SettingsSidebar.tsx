import { useQueryClient } from '@tanstack/react-query'
import { LogOut, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { clearToken } from '../../lib/auth'
import { logout } from '../../api/authApi'
import { resetCurrentUser } from '../../hooks/useCurrentUser'
import { SUPPORTED_LANGUAGES, type SupportedLanguage } from '../../lib/i18n'

type SettingsSidebarProps = {
  open: boolean
  onClose: () => void
}

export function SettingsSidebar({ open, onClose }: SettingsSidebarProps) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirmingLogout, setConfirmingLogout] = useState(false)

  const handleClose = useCallback(() => {
    setConfirmingLogout(false)
    onClose()
  }, [onClose])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') handleClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, handleClose])

  const handleLogout = async () => {
    await logout()
    clearToken()
    resetCurrentUser()
    queryClient.clear()
    void navigate('/login', { replace: true })
  }

  const currentLang = (
    SUPPORTED_LANGUAGES.includes(i18n.resolvedLanguage as SupportedLanguage)
      ? i18n.resolvedLanguage
      : 'en'
  ) as SupportedLanguage

  const handleLanguageChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    void i18n.changeLanguage(e.target.value)
  }

  return (
    <div
      className={`fixed inset-0 z-[60] ${open ? '' : 'pointer-events-none'}`}
      aria-hidden={!open}
    >
      <div
        className={`absolute inset-0 bg-on-surface/30 transition-opacity duration-300 ${
          open ? 'opacity-100' : 'opacity-0'
        }`}
        onClick={handleClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={t('settings.title')}
        className={`absolute top-0 right-0 h-full w-full max-w-md bg-surface transition-[transform,box-shadow] duration-300 ease-out overflow-y-auto ${
          open ? 'translate-x-0' : 'translate-x-full'
        } ${open ? 'shadow-none sm:shadow-2xl' : 'shadow-none'}`}
      >
        <div className="flex items-center justify-between px-8 py-6 border-b border-outline-variant/15">
          <h2 className="font-headline text-2xl text-on-surface">{t('settings.title')}</h2>
          <button
            type="button"
            aria-label={t('settings.close')}
            onClick={handleClose}
            className="text-on-surface-variant hover:text-on-surface"
          >
            <X size={20} />
          </button>
        </div>

        <div className="px-8 py-8 space-y-6">
          <div className="space-y-1.5">
            <label
              htmlFor="settings-language"
              className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant"
            >
              {t('settings.language')}
            </label>
            <select
              id="settings-language"
              value={currentLang}
              onChange={handleLanguageChange}
              className="w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none border border-outline-variant/15 focus:outline-none focus:border-on-surface"
            >
              {SUPPORTED_LANGUAGES.map((lng) => (
                <option key={lng} value={lng}>
                  {t(`settings.languageOptions.${lng}`)}
                </option>
              ))}
            </select>
          </div>

          {confirmingLogout ? (
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={handleLogout}
                className="flex-1 flex items-center justify-center gap-2 p-4 font-label text-sm uppercase tracking-[0.15em] bg-error text-on-error hover:bg-error/90"
              >
                <LogOut size={16} />
                {t('settings.logoutConfirm')}
              </button>
              <button
                type="button"
                onClick={() => setConfirmingLogout(false)}
                className="p-4 font-label text-sm uppercase tracking-[0.15em] text-on-surface-variant hover:text-on-surface"
              >
                {t('settings.logoutCancel')}
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setConfirmingLogout(true)}
              className="w-full flex items-center justify-center gap-2 p-4 font-label text-sm uppercase tracking-[0.15em] text-on-surface-variant hover:text-on-surface border border-outline-variant/15"
            >
              <LogOut size={16} />
              {t('settings.logout')}
            </button>
          )}
        </div>
      </aside>
    </div>
  )
}
