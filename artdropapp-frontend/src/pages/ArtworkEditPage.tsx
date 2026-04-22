import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { fetchArtworkById, updateArtwork } from '../api/artworksApi'

export function ArtworkEditPage() {
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [medium, setMedium] = useState('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setError('Invalid ID')
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    setError(null)

    fetchArtworkById(id)
      .then((data) => {
        if (cancelled) return
        setTitle(data.title)
        setMedium(data.medium)
        setDescription(data.description ?? '')
        setImageUrl(data.imageUrl)
      })
      .catch((err: unknown) => {
        if (cancelled) return
        if (err instanceof Error && err.message === 'NOT_FOUND') {
          setError('Artwork not found.')
        } else {
          setError(err instanceof Error ? err.message : 'Failed to load artwork')
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [id])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!Number.isFinite(id)) return
    setSaving(true)
    setMessage(null)
    try {
      await updateArtwork(id, {
        title: title.trim(),
        medium: medium.trim(),
        description: description.trim(),
        imageUrl: imageUrl.trim(),
      })
      setMessage('Saved.')
      navigate(`/details/${id}`)
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  return (
    <main className="app-main">
      <h1>Edit artwork</h1>
      {loading ? <p role="status">Loading…</p> : null}
      {error ? (
        <p className="artwork-form__message" role="alert">
          {error}
        </p>
      ) : null}
      {!loading && !error ? (
        <form className="artwork-form" onSubmit={(e) => void handleSubmit(e)}>
          <label className="artwork-form__field">
            Title
            <input
              value={title}
              onChange={(ev) => setTitle(ev.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <label className="artwork-form__field">
            Medium
            <input
              value={medium}
              onChange={(ev) => setMedium(ev.target.value)}
              required
              autoComplete="off"
            />
          </label>
          <label className="artwork-form__field">
            Description
            <textarea
              value={description}
              onChange={(ev) => setDescription(ev.target.value)}
              rows={3}
              maxLength={2000}
            />
          </label>
          <label className="artwork-form__field">
            Image URL (https://…)
            <input
              type="url"
              value={imageUrl}
              onChange={(ev) => setImageUrl(ev.target.value)}
              required
              placeholder="https://example.com/image.jpg"
            />
          </label>
          <button
            type="submit"
            disabled={saving}
            className="w-full bg-on-surface text-surface font-semibold py-3 px-6 rounded-md hover:bg-on-surface/90 disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
          >
            {saving ? 'Saving…' : 'Save'}
          </button>
          {message ? (
            <p className="artwork-form__message" role="alert">
              {message}
            </p>
          ) : null}
        </form>
      ) : null}
    </main>
  )
}
