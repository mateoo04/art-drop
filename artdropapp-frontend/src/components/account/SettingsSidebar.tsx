import { X } from 'lucide-react'
import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { SUPPORTED_LANGUAGES, type SupportedLanguage } from '../../lib/i18n'

type SettingsSidebarProps = {
  open: boolean
  onClose: () => void
}

export function SettingsSidebar({ open, onClose }: SettingsSidebarProps) {
  const { t, i18n } = useTranslation()

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

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
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={t('settings.title')}
        className={`absolute top-0 right-0 h-full w-full max-w-md bg-surface shadow-2xl transition-transform duration-300 ease-out overflow-y-auto ${
          open ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between px-8 py-6 border-b border-outline-variant/15">
          <h2 className="font-headline text-2xl text-on-surface">{t('settings.title')}</h2>
          <button
            type="button"
            aria-label={t('settings.close')}
            onClick={onClose}
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
        </div>
      </aside>
    </div>
  )
}
