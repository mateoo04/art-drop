import { Trans, useTranslation } from 'react-i18next'

type Props = {
  existingTitle: string
  incomingTitle: string
  isPending: boolean
  onCancel: () => void
  onConfirm: () => void
}

export function ReservationSwapModal({ existingTitle, incomingTitle, isPending, onCancel, onConfirm }: Props) {
  const { t } = useTranslation()
  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-on-surface/40">
      <div className="bg-surface w-full max-w-md mx-4 p-6 border border-outline-variant/30">
        <h2 className="font-headline text-lg mb-3">{t('reservation.swap.title')}</h2>
        <p className="text-sm text-on-surface-variant mb-6">
          <Trans
            i18nKey="reservation.swap.body"
            values={{ existing: existingTitle, incoming: incomingTitle }}
            components={{ strong: <strong className="text-on-surface" /> }}
          />
        </p>
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="px-4 py-2 text-sm border border-outline-variant/40 text-on-surface hover:bg-surface-container-low"
          >
            {t('reservation.swap.cancel')}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="px-4 py-2 text-sm bg-on-surface text-surface hover:opacity-90 disabled:opacity-60"
          >
            {t('reservation.swap.confirm')}
          </button>
        </div>
      </div>
    </div>
  )
}
