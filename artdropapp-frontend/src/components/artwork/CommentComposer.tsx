import { useState } from 'react'
import { getToken } from '../../lib/auth'
import { useAuthPrompt } from '../../contexts/AuthPromptContext'
import { Button } from '../ui/Button'

type CommentComposerProps = {
  onSubmit: (text: string) => Promise<unknown>
  onCancel?: () => void
  compact?: boolean
  placeholder?: string
  submitLabel?: string
  authPromptReason?: string
  autoFocus?: boolean
}

export function CommentComposer({
  onSubmit,
  onCancel,
  compact = false,
  placeholder,
  submitLabel = 'Post',
  authPromptReason = 'leave a comment',
  autoFocus = false,
}: CommentComposerProps) {
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { promptToAuth } = useAuthPrompt()

  const isAuthed = Boolean(getToken())

  const handle = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isAuthed) {
      promptToAuth(authPromptReason)
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
    if (!isAuthed) promptToAuth(authPromptReason)
  }

  const placeholderText =
    placeholder ?? (isAuthed ? 'Share something thoughtful…' : `Sign in to ${authPromptReason}…`)

  return (
    <form onSubmit={handle} className={compact ? 'space-y-2' : 'space-y-3'}>
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        onFocus={handleFocusGuard}
        rows={compact ? 2 : 3}
        maxLength={2000}
        placeholder={placeholderText}
        disabled={submitting}
        readOnly={!isAuthed}
        autoFocus={autoFocus}
        className={`w-full bg-surface-container-lowest font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border border-outline-variant/15 focus:border-on-surface ${
          compact ? 'p-3' : 'p-4'
        }`}
      />
      {error ? (
        <p role="alert" className="font-body text-xs text-error">
          {error}
        </p>
      ) : null}
      <div className="flex justify-end gap-2">
        {onCancel ? (
          <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
        ) : null}
        <Button
          type="submit"
          variant="primary"
          loading={submitting}
          disabled={isAuthed && !text.trim()}
        >
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
