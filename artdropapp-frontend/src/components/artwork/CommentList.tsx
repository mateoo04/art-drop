import { Link } from 'react-router-dom'
import type { Comment } from '../../types/comment'

type CommentListProps = {
  comments: Comment[]
  onDelete: (id: number) => void
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

function AuthorAvatar({ comment }: { comment: Comment }) {
  const { author } = comment
  if (author.avatarUrl) {
    return (
      <img
        src={author.avatarUrl}
        alt={author.displayName ?? ''}
        className="w-10 h-10 rounded-full object-cover bg-surface-container-low flex-shrink-0"
      />
    )
  }
  const initial = (author.displayName ?? '?').slice(0, 1).toUpperCase()
  return (
    <div className="w-10 h-10 rounded-full bg-surface-container-low flex items-center justify-center font-headline text-sm text-on-surface flex-shrink-0">
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

export function CommentList({ comments, onDelete }: CommentListProps) {
  if (comments.length === 0) {
    return (
      <p className="py-6 text-center text-on-surface-variant italic font-body text-sm">
        No comments yet.
      </p>
    )
  }

  return (
    <ul className="space-y-6" aria-label="Comments">
      {comments.map((c) => (
        <li key={c.id} className="flex gap-4">
          <AuthorAvatar comment={c} />
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3">
              <AuthorName comment={c} />
              <span className="font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
                {formatRelative(c.createdAt)}
              </span>
              {c.isAuthor ? (
                <button
                  type="button"
                  onClick={() => onDelete(c.id)}
                  className="ml-auto font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant hover:text-error transition-colors"
                >
                  Delete
                </button>
              ) : null}
            </div>
            <p className="mt-2 font-body text-sm text-on-surface whitespace-pre-line">
              {c.text}
            </p>
          </div>
        </li>
      ))}
    </ul>
  )
}
