import { useState } from 'react'
import { useTranslation } from 'react-i18next'
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
  submitLabel,
  authPromptReason,
  autoFocus = false,
}: CommentComposerProps) {
  const { t } = useTranslation()
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const { promptToAuth } = useAuthPrompt()

  const resolvedSubmitLabel = submitLabel ?? t('comments.composer.post')
  const resolvedAuthPromptReason = authPromptReason ?? t('comments.composer.defaultAuthReason')

  const isAuthed = Boolean(getToken())

  const handle = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isAuthed) {
      promptToAuth(resolvedAuthPromptReason)
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
      setError(err instanceof Error ? err.message : t('comments.composer.failedToPost'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleFocusGuard = () => {
    if (!isAuthed) promptToAuth(resolvedAuthPromptReason)
  }

  const placeholderText =
    placeholder ??
    (isAuthed
      ? t('comments.composer.placeholder')
      : t('comments.composer.placeholderSignedOut', { reason: resolvedAuthPromptReason }))

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
            {t('comments.composer.cancel')}
          </Button>
        ) : null}
        <Button
          type="submit"
          variant="primary"
          loading={submitting}
          disabled={isAuthed && !text.trim()}
        >
          {resolvedSubmitLabel}
        </Button>
      </div>
    </form>
  )
}
