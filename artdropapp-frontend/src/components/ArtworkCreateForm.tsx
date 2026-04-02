import { type FormEvent, useState } from 'react'
import { createArtwork, type CreateArtworkPayload } from '../api/artworksApi'

export type ArtworkCreateFormProps = {
  onCreated: () => void
}

export function ArtworkCreateForm({ onCreated }: ArtworkCreateFormProps) {
  const [title, setTitle] = useState('')
  const [medium, setMedium] = useState('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setMessage(null)
    setSubmitting(true)
    const payload: CreateArtworkPayload = {
      title: title.trim(),
      medium: medium.trim(),
      description: description.trim(),
      imageUrl: imageUrl.trim(),
    }
    try {
      await createArtwork(payload)
      setTitle('')
      setMedium('')
      setDescription('')
      setImageUrl('')
      onCreated()
    } catch (err) {
      setMessage(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form className="artwork-form" onSubmit={(e) => void handleSubmit(e)}>
      <h3>New artwork</h3>
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
      <button type="submit" disabled={submitting}>
        {submitting ? 'Saving…' : 'Save'}
      </button>
      {message ? (
        <p className="artwork-form__message" role="alert">
          {message}
        </p>
      ) : null}
    </form>
  )
}
