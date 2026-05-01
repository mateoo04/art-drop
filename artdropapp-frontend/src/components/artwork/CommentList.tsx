import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Comment } from '../../types/comment'
import { getToken } from '../../lib/auth'
import { useAuthPrompt } from '../../contexts/AuthPromptContext'
import { ConfirmModal } from '../ui/ConfirmModal'
import { CommentComposer } from './CommentComposer'

type CommentListProps = {
  comments: Comment[]
  onReply: (text: string, parentId: number) => Promise<unknown>
  onDelete: (id: number) => Promise<unknown> | void
  onLoadMoreReplies: (parentId: number) => Promise<unknown>
  loadingRepliesFor: number | null
}

function formatRelative(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const diffMs = Date.now() - d.getTime()
  const mins = Math.round(diffMs / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.round(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.round(hours / 24)
  if (days < 30) return `${days}d ago`
  return d.toLocaleDateString()
}

function AuthorAvatar({ comment, size = 'md' }: { comment: Comment; size?: 'md' | 'sm' }) {
  const dim = size === 'sm' ? 'w-8 h-8' : 'w-10 h-10'
  const fontSize = size === 'sm' ? 'text-xs' : 'text-sm'
  const { author } = comment
  if (author.avatarUrl) {
    return (
      <img
        src={author.avatarUrl}
        alt={author.displayName ?? ''}
        className={`${dim} rounded-full object-cover bg-surface-container-low flex-shrink-0`}
      />
    )
  }
  const initial = (author.displayName ?? '?').slice(0, 1).toUpperCase()
  return (
    <div
      className={`${dim} rounded-full bg-surface-container-low flex items-center justify-center font-headline ${fontSize} text-on-surface flex-shrink-0`}
    >
      {initial}
    </div>
  )
}

function AuthorName({ comment }: { comment: Comment }) {
  const { author } = comment
  const name = author.displayName ?? 'Unknown'
  if (author.slug) {
    return (
      <Link
        to={`/u/${author.slug}`}
        className="font-body text-sm text-on-surface hover:text-outline transition-colors"
      >
        {name}
      </Link>
    )
  }
  return <span className="font-body text-sm text-on-surface">{name}</span>
}

type CommentRowProps = {
  comment: Comment
  isReply?: boolean
  onReplyClick?: () => void
  onDeleteClick: () => void
}

function CommentRow({ comment, isReply = false, onReplyClick, onDeleteClick }: CommentRowProps) {
  return (
    <div className="flex gap-4">
      <AuthorAvatar comment={comment} size={isReply ? 'sm' : 'md'} />
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-3">
          <AuthorName comment={comment} />
          <span className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
            {formatRelative(comment.createdAt)}
          </span>
          {comment.isAuthor ? (
            <button
              type="button"
              onClick={onDeleteClick}
              className="ml-auto font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant hover:text-error transition-colors"
            >
              Delete
            </button>
          ) : null}
        </div>
        <p className="mt-2 font-body text-sm text-on-surface whitespace-pre-line">
          {comment.text}
        </p>
        {!isReply && onReplyClick ? (
          <button
            type="button"
            onClick={onReplyClick}
            className="mt-2 text-left font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant hover:text-on-surface transition-colors"
          >
            Reply
          </button>
        ) : null}
      </div>
    </div>
  )
}

export function CommentList({
  comments,
  onReply,
  onDelete,
  onLoadMoreReplies,
  loadingRepliesFor,
}: CommentListProps) {
  const [replyingTo, setReplyingTo] = useState<number | null>(null)
  const [pendingDelete, setPendingDelete] = useState<number | null>(null)
  const [deleting, setDeleting] = useState(false)
  const { promptToAuth } = useAuthPrompt()

  const handleReplyClick = (commentId: number) => {
    if (!getToken()) {
      promptToAuth('reply to a comment')
      return
    }
    setReplyingTo((current) => (current === commentId ? null : commentId))
  }

  const handleDeleteConfirm = async () => {
    if (pendingDelete == null) return
    setDeleting(true)
    try {
      await onDelete(pendingDelete)
      setPendingDelete(null)
    } finally {
      setDeleting(false)
    }
  }

  if (comments.length === 0) {
    return (
      <p className="py-6 text-center text-on-surface-variant italic font-body text-sm">
        No comments yet.
      </p>
    )
  }

  return (
    <>
      <ul className="space-y-8" aria-label="Comments">
        {comments.map((c) => {
          const remainingReplies = Math.max(0, c.replyCount - c.replies.length)
          const isLoadingMore = loadingRepliesFor === c.id
          return (
            <li key={c.id}>
              <CommentRow
                comment={c}
                onReplyClick={() => handleReplyClick(c.id)}
                onDeleteClick={() => setPendingDelete(c.id)}
              />

              {replyingTo === c.id ? (
                <div className="mt-4 ml-14">
                  <CommentComposer
                    compact
                    autoFocus
                    authPromptReason="reply to a comment"
                    placeholder={`Reply to ${c.author.displayName ?? 'this comment'}…`}
                    submitLabel="Reply"
                    onCancel={() => setReplyingTo(null)}
                    onSubmit={async (text) => {
                      await onReply(text, c.id)
                      setReplyingTo(null)
                    }}
                  />
                </div>
              ) : null}

              {c.replies.length > 0 ? (
                <ul
                  className="mt-6 ml-14 space-y-6 border-l border-outline-variant/15 pl-6"
                  aria-label="Replies"
                >
                  {c.replies.map((r) => (
                    <li key={r.id}>
                      <CommentRow
                        comment={r}
                        isReply
                        onDeleteClick={() => setPendingDelete(r.id)}
                      />
                    </li>
                  ))}
                </ul>
              ) : null}

              {remainingReplies > 0 ? (
                <button
                  type="button"
                  onClick={() => void onLoadMoreReplies(c.id)}
                  disabled={isLoadingMore}
                  className="mt-4 ml-14 font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant hover:text-on-surface transition-colors disabled:opacity-50"
                >
                  {isLoadingMore
                    ? 'Loading…'
                    : `View ${remainingReplies} more ${remainingReplies === 1 ? 'reply' : 'replies'}`}
                </button>
              ) : null}
            </li>
          )
        })}
      </ul>
      <ConfirmModal
        open={pendingDelete != null}
        title="Delete comment?"
        message="This will remove your comment from the discussion. You can't undo this."
        confirmLabel="Delete"
        destructive
        busy={deleting}
        onConfirm={() => void handleDeleteConfirm()}
        onCancel={() => {
          if (!deleting) setPendingDelete(null)
        }}
      />
    </>
  )
}
