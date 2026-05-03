import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import type { AdminChallengeRow } from '../../api/adminApi'
import { Button } from '../ui/Button'

const PANEL_BTN = '!py-2 !px-3 !text-[10px] !tracking-[0.12em]'

function formatRange(startsAt: string | null, endsAt: string | null) {
  const fmt = (s: string | null) => {
    if (!s) return '—'
    const d = new Date(s)
    return Number.isNaN(d.getTime()) ? s : d.toLocaleString()
  }
  return `${fmt(startsAt)} – ${fmt(endsAt)}`
}

type Props = {
  challenge: AdminChallengeRow
  onDeleteRequest: () => void
  onActivate: () => void
  onDeactivate: () => void
}

export function AdminChallengeExpandPanel({ challenge: c, onDeleteRequest, onActivate, onDeactivate }: Props) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const st = c.status
  const showActivate = st !== 'ACTIVE'
  const showDeactivate = st === 'ACTIVE'

  return (
    <div className="relative w-full border-t border-outline-variant/20 bg-surface-container-low px-4 py-5 space-y-4">
      <h4 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
        {t('admin.challenges.expand.details')}
      </h4>
      <dl className="grid gap-2 text-sm">
        <div>
          <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.fieldKind')}</dt>
          <dd className="text-on-surface">{c.kind ?? '—'}</dd>
        </div>
        {c.theme ? (
          <div>
            <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.fieldTheme')}</dt>
            <dd className="text-on-surface">{c.theme}</dd>
          </div>
        ) : null}
        {c.quote ? (
          <div>
            <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.fieldQuote')}</dt>
            <dd className="text-on-surface italic whitespace-pre-wrap">{c.quote}</dd>
          </div>
        ) : null}
        {c.description ? (
          <div>
            <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.fieldDescription')}</dt>
            <dd className="text-on-surface whitespace-pre-wrap">{c.description}</dd>
          </div>
        ) : null}
        <div>
          <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.expand.schedule')}</dt>
          <dd className="text-on-surface">{formatRange(c.startsAt, c.endsAt)}</dd>
        </div>
        {c.coverImageUrl ? (
          <div>
            <dt className="text-on-surface-variant text-xs uppercase tracking-wide">{t('admin.challenges.fieldCoverUrl')}</dt>
            <dd className="text-on-surface break-all">
              <a href={c.coverImageUrl} className="underline hover:no-underline" target="_blank" rel="noreferrer">
                {c.coverImageUrl}
              </a>
            </dd>
          </div>
        ) : null}
      </dl>

      <div>
        <h4 className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-3">
          {t('admin.challenges.expand.actions')}
        </h4>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" className={PANEL_BTN} onClick={() => navigate(`/admin/challenges/${c.id}/edit`)}>{t('admin.challenges.edit')}</Button>
          {showActivate ? (
            <Button variant="outline" className={PANEL_BTN} onClick={onActivate}>{t('admin.challenges.activate')}</Button>
          ) : null}
          {showDeactivate ? (
            <Button variant="outline" className={PANEL_BTN} onClick={onDeactivate}>{t('admin.challenges.deactivate')}</Button>
          ) : null}
          <Button variant="outline-destructive" className={PANEL_BTN} onClick={onDeleteRequest}>{t('admin.challenges.delete')}</Button>
        </div>
      </div>
    </div>
  )
}
