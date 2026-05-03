import { useTranslation } from 'react-i18next'

export function AppFooter() {
  const { t } = useTranslation()
  const year = new Date().getFullYear()
  return (
    <footer
      id="app-footer"
      className="bg-surface-container-high w-full py-12 px-8 flex flex-col md:flex-row justify-between items-center mt-24"
    >
      <div className="font-headline text-sm text-on-surface mb-6 md:mb-0">ArtDrop</div>
      <p className="font-body text-xs tracking-widest uppercase text-primary">
        {t('footer.copyright', { year })}
      </p>
      <div className="flex gap-8 mt-6 md:mt-0">
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          {t('footer.privacyPolicy')}
        </a>
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          {t('footer.termsOfService')}
        </a>
        <a
          className="font-body text-xs tracking-widest uppercase text-primary hover:underline underline-offset-4 transition-all"
          href="#"
        >
          {t('footer.contact')}
        </a>
      </div>
    </footer>
  )
}
