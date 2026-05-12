import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <main className="max-w-[1440px] mx-auto px-8 py-24 flex flex-col items-center text-center gap-4">
      <h1 className="text-4xl font-semibold">404</h1>
      <p className="text-xl">{t('notFound.title')}</p>
      <p className="text-neutral-500">{t('notFound.description')}</p>
      <Link
        to="/"
        className="mt-4 inline-flex items-center px-4 py-2 rounded-full bg-neutral-900 text-white hover:bg-neutral-700 transition-colors"
      >
        {t('notFound.backHome')}
      </Link>
    </main>
  )
}
