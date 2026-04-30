import { useState } from 'react'
import { getToken } from '../../lib/auth'
import { useAuthPrompt } from '../../contexts/AuthPromptContext'
import { Button } from '../ui/Button'

type CommentComposerProps = {
  onSubmit: (text: string) => Promise<unknown>
}

export function CommentComposer({ onSubmit }: CommentComposerProps) {
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { promptToAuth } = useAuthPrompt()

  const isAuthed = Boolean(getToken())

  const handle = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isAuthed) {
      promptToAuth('leave a comment')
      return
    }
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

  const handleFocusGuard = () => {
    if (!isAuthed) promptToAuth('leave a comment')
  }

  return (
    <form onSubmit={handle} className="space-y-3">
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        onFocus={handleFocusGuard}
        rows={3}
        maxLength={2000}
        placeholder={isAuthed ? 'Share something thoughtful…' : 'Sign in to leave a comment…'}
        disabled={submitting}
        readOnly={!isAuthed}
        className="w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border border-outline-variant/15 focus:border-on-surface"
      />
      {error ? (
        <p role="alert" className="font-body text-xs text-error">
          {error}
        </p>
      ) : null}
      <div className="flex justify-end">
        <Button
          type="submit"
          variant="primary"
          loading={submitting}
          disabled={isAuthed && !text.trim()}
        >
          Post
        </Button>
      </div>
    </form>
  )
}
