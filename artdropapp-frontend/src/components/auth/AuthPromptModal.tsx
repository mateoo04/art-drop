import { X } from 'lucide-react'
import { useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'

type AuthPromptModalProps = {
  open: boolean
  action: string
  onClose: () => void
}

export function AuthPromptModal({ open, onClose }: AuthPromptModalProps) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [open, onClose])

  if (!open) return null

  const go = (path: '/login' | '/signup') => {
    onClose()
    const from = location.pathname + location.search
    navigate(path, { state: { from } })
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-prompt-title"
      className="fixed inset-0 z-50 flex items-center justify-center"
    >
      <button
        type="button"
        aria-label={t('common.close')}
        onClick={onClose}
        className="absolute inset-0 bg-surface/60 backdrop-blur-[20px] cursor-default"
      />
      <main className="relative z-10 w-full max-w-sm mx-4 sm:mx-auto bg-surface-container-lowest shadow-[0_10px_40px_rgba(45,52,53,0.1)] rounded-none px-6 py-10 flex flex-col items-center">
        <button
          type="button"
          aria-label={t('auth.prompt.closeModal')}
          onClick={onClose}
          className="absolute top-4 right-4 text-on-surface hover:text-outline transition-colors p-2 flex items-center justify-center"
        >
          <X size={24} aria-hidden="true" />
        </button>
        <h1
          id="auth-prompt-title"
          className="font-headline text-3xl font-medium tracking-tight text-on-surface mb-4 mt-4 text-center w-full"
        >
          {t('auth.prompt.title')}
        </h1>
        <p className="font-body text-base text-on-surface-variant text-center leading-relaxed mb-10 w-full max-w-[280px]">
          {t('auth.prompt.body')}
        </p>
        <div className="w-full flex flex-col gap-4">
          <button
            type="button"
            onClick={() => go('/signup')}
            className="w-full bg-on-surface text-surface font-label text-sm uppercase tracking-[0.1em] py-4 rounded-none transition-colors hover:bg-on-surface/90 flex items-center justify-center"
          >
            {t('auth.prompt.signup')}
          </button>
          <button
            type="button"
            onClick={() => go('/login')}
            className="w-full bg-transparent border border-outline-variant/30 text-on-surface font-label text-sm uppercase tracking-[0.1em] py-4 rounded-none transition-colors hover:bg-surface-container-low flex items-center justify-center"
          >
            {t('auth.prompt.login')}
          </button>
        </div>
      </main>
    </div>
  )
}
