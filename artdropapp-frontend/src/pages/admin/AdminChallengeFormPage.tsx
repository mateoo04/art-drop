import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createAdminChallenge,
  fetchAdminChallenge,
  updateAdminChallenge,
  type AdminChallengeUpsert,
} from '../../api/adminApi'
import { Button } from '../../components/ui/Button'
import { Input } from '../../components/ui/Input'
import { Spinner } from '../../components/ui/Spinner'

function toDatetimeLocalValue(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

function fromDatetimeLocalValue(v: string): string | null {
  const t = v.trim()
  if (!t) return null
  return t.length === 16 ? `${t}:00` : t
}

const emptyForm: AdminChallengeUpsert = {
  title: '',
  description: '',
  quote: '',
  kind: 'OPEN',
  status: 'UPCOMING',
  theme: '',
  coverImageUrl: '',
  startsAt: null,
  endsAt: null,
}

export function AdminChallengeFormPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isNew = id == null
  const isEdit = !isNew && id != null
  const challengeId = isEdit ? Number.parseInt(id, 10) : Number.NaN

  const [loading, setLoading] = useState(!isNew)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState<AdminChallengeUpsert>(emptyForm)

  useEffect(() => {
    if (isNew || !Number.isFinite(challengeId)) return
    let cancelled = false
    setLoading(true)
    fetchAdminChallenge(challengeId)
      .then((row) => {
        if (cancelled) return
        setForm({
          title: row.title,
          description: row.description,
          quote: row.quote,
          kind: (row.kind === 'FEATURED' ? 'FEATURED' : 'OPEN'),
          status: (row.status === 'ACTIVE' || row.status === 'ENDED' || row.status === 'UPCOMING')
            ? row.status
            : 'UPCOMING',
          theme: row.theme,
          coverImageUrl: row.coverImageUrl,
          startsAt: row.startsAt,
          endsAt: row.endsAt,
        })
        setError(null)
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [isNew, challengeId])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!form.title.trim()) return
    setSaving(true)
    setError(null)
    const body: AdminChallengeUpsert = {
      ...form,
      title: form.title.trim(),
      description: form.description?.trim() || null,
      quote: form.quote?.trim() || null,
      theme: form.theme?.trim() || null,
      coverImageUrl: form.coverImageUrl?.trim() || null,
    }
    try {
      if (!isNew && Number.isFinite(challengeId)) {
        await updateAdminChallenge(challengeId, body)
      } else {
        await createAdminChallenge(body)
      }
      navigate('/admin/challenges')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center py-16">
        <Spinner />
      </div>
    )
  }

  return (
    <section>
      <Link to="/admin/challenges" className="text-sm text-on-surface-variant hover:underline">
        {t('admin.challenges.backToList')}
      </Link>
      <h2 className="font-headline text-2xl text-on-surface mt-3 mb-6">
        {isNew ? t('admin.challenges.newTitle') : t('admin.challenges.editTitle')}
      </h2>

      {error ? <p className="text-error mb-4" role="alert">{error}</p> : null}

      <form onSubmit={(e) => void handleSubmit(e)} className="max-w-xl space-y-4">
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldTitle')}</span>
          <Input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
        </label>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldDescription')}</span>
          <textarea
            value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            rows={4}
            className="w-full bg-surface-container-lowest p-4 font-body text-sm border border-outline-variant/15 focus:border-on-surface focus:outline-none"
          />
        </label>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldQuote')}</span>
          <textarea
            value={form.quote ?? ''}
            onChange={(e) => setForm({ ...form, quote: e.target.value })}
            rows={2}
            className="w-full bg-surface-container-lowest p-4 font-body text-sm border border-outline-variant/15 focus:border-on-surface focus:outline-none"
          />
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label className="block">
            <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldKind')}</span>
            <select
              value={form.kind}
              onChange={(e) => setForm({ ...form, kind: e.target.value as AdminChallengeUpsert['kind'] })}
              className="w-full bg-surface-container-lowest p-4 font-body text-sm border border-outline-variant/15"
            >
              <option value="OPEN">OPEN</option>
              <option value="FEATURED">FEATURED</option>
            </select>
          </label>
          <label className="block">
            <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldStatus')}</span>
            <select
              value={form.status}
              onChange={(e) => setForm({ ...form, status: e.target.value as AdminChallengeUpsert['status'] })}
              className="w-full bg-surface-container-lowest p-4 font-body text-sm border border-outline-variant/15"
            >
              <option value="UPCOMING">UPCOMING</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="ENDED">ENDED</option>
            </select>
          </label>
        </div>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldTheme')}</span>
          <Input value={form.theme ?? ''} onChange={(e) => setForm({ ...form, theme: e.target.value })} />
        </label>
        <label className="block">
          <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldCoverUrl')}</span>
          <Input value={form.coverImageUrl ?? ''} onChange={(e) => setForm({ ...form, coverImageUrl: e.target.value })} />
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label className="block">
            <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldStarts')}</span>
            <Input
              type="datetime-local"
              value={toDatetimeLocalValue(form.startsAt)}
              onChange={(e) => setForm({ ...form, startsAt: fromDatetimeLocalValue(e.target.value) })}
            />
          </label>
          <label className="block">
            <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">{t('admin.challenges.fieldEnds')}</span>
            <Input
              type="datetime-local"
              value={toDatetimeLocalValue(form.endsAt)}
              onChange={(e) => setForm({ ...form, endsAt: fromDatetimeLocalValue(e.target.value) })}
            />
          </label>
        </div>
        <div className="flex gap-3 pt-4">
          <Button type="submit" loading={saving}>{t('admin.challenges.save')}</Button>
          <Button type="button" variant="secondary" onClick={() => navigate('/admin/challenges')}>{t('admin.filters.cancel')}</Button>
        </div>
      </form>
    </section>
  )
}
