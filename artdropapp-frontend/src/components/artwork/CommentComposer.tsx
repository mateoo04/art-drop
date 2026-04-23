import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getToken } from '../../lib/auth'
import { Button } from '../ui/Button'

type CommentComposerProps = {
  onSubmit: (text: string) => Promise<unknown>
}

export function CommentComposer({ onSubmit }: CommentComposerProps) {
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!getToken()) {
    return (
      <p className="font-body text-sm text-on-surface-variant italic py-4">
        <Link to="/login" className="underline underline-offset-4 hover:text-on-surface">
          Sign in
        </Link>{' '}
        to leave a comment.
      </p>
    )
  }

  const handle = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = text.trim()
    if (!trimmed) return
    setSubmitting(true)
    setError(null)
    try {
      await onSubmit(trimmed)
      setText('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to post')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handle} className="space-y-3">
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        rows={3}
        maxLength={2000}
        placeholder="Share something thoughtful…"
        disabled={submitting}
        className="w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border border-outline-variant/15 focus:border-on-surface"
      />
      {error ? (
        <p role="alert" className="font-body text-xs text-error">
          {error}
        </p>
      ) : null}
      <div className="flex justify-end">
        <Button type="submit" variant="primary" loading={submitting} disabled={!text.trim()}>
          Post
        </Button>
      </div>
    </form>
  )
}
