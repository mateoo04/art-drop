import { ImagePlus } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button } from '../ui/Button'

type AvatarUploadPromptProps = {
  onUpload: () => void
}

export function AvatarUploadPrompt({ onUpload }: AvatarUploadPromptProps) {
  const { t } = useTranslation()
  return (
    <section className="mt-4 flex flex-col gap-4 px-4 py-4 bg-surface-container-lowest border border-outline-variant/15 sm:flex-row sm:items-center sm:px-6">
      <div className="flex min-w-0 items-center gap-4 sm:flex-1">
        <div className="w-12 h-12 rounded-full bg-surface-container-low flex items-center justify-center text-on-surface-variant shrink-0">
          <ImagePlus size={22} />
        </div>
        <div className="flex-1 min-w-0">
          <h2 className="font-headline text-base text-on-surface">
            {t('account.avatarPrompt.title')}
          </h2>
          <p className="font-body text-sm text-on-surface-variant mt-1">
            {t('account.avatarPrompt.body')}
          </p>
        </div>
      </div>
      <div className="flex justify-end sm:block">
        <Button type="button" variant="primary" onClick={onUpload}>
          {t('account.avatarPrompt.cta')}
        </Button>
      </div>
    </section>
  )
}
