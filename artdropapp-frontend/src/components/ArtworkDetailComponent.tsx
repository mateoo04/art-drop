import { useState } from 'react'
import type { Artwork } from '../types/artwork'
import { useComments } from '../hooks/useComments'
import { useAuthPrompt } from '../contexts/AuthPromptContext'
import { getToken } from '../lib/auth'
import { CommentComposer } from './artwork/CommentComposer'
import { CommentList } from './artwork/CommentList'

export type ArtworkDetailComponentProps = {
  artwork: Artwork | null
  loading: boolean
  error: string | null
}

function formatPublishedAt(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

export function ArtworkDetailComponent({
  artwork,
  loading,
  error,
}: ArtworkDetailComponentProps) {
  const comments = useComments(artwork?.id ?? null)
  const { promptToAuth } = useAuthPrompt()
  const [liked, setLiked] = useState(false)
  const [likeBump, setLikeBump] = useState(0)

  const handleLike = () => {
    if (!getToken()) {
      promptToAuth('like this artwork')
      return
    }
    setLiked((v) => {
      setLikeBump((b) => b + (v ? -1 : 1))
      return !v
    })
  }

  if (loading) {
    return (
      <div className="artwork-detail artwork-detail--status" role="status">
        Loading details…
      </div>
    )
  }

  if (error) {
    return (
      <div className="artwork-detail artwork-detail--status artwork-list--error" role="alert">
        {error}
      </div>
    )
  }

  if (!artwork) {
    return (
      <div className="artwork-detail artwork-detail--empty" role="region" aria-label="Details">
        Artwork not found.
      </div>
    )
  }

  return (
    <article className="artwork-detail" aria-label="Selected artwork details">
      <h2>{artwork.title}</h2>
      {artwork.imageUrl ? (
        <img
          src={artwork.imageUrl}
          alt={artwork.imageAlt}
          className="artwork-detail__image"
          loading="lazy"
        />
      ) : null}
      <dl className="artwork-detail__fields">
        <div>
          <dt>Medium</dt>
          <dd>{artwork.medium}</dd>
        </div>
        <div>
          <dt>Tags</dt>
          <dd>{artwork.tags.join(', ')}</dd>
        </div>
        <div>
          <dt>Published</dt>
          <dd>{formatPublishedAt(artwork.publishedAt)}</dd>
        </div>
        <div>
          <dt>Comments</dt>
          <dd>{artwork.commentCount}</dd>
        </div>
      </dl>

      <div className="mt-6 flex items-center gap-3">
        <button
          type="button"
          onClick={handleLike}
          aria-pressed={liked}
          className={`inline-flex items-center gap-2 px-5 py-3 border font-label text-[11px] uppercase tracking-[0.2em] font-semibold transition-all duration-200 ${
            liked
              ? 'bg-on-surface text-surface border-on-surface'
              : 'bg-transparent text-on-surface border-on-surface hover:bg-on-surface hover:text-surface'
          }`}
        >
          <span className="material-symbols-outlined text-base" aria-hidden="true">
            favorite
          </span>
          <span>{liked ? 'Liked' : 'Like'}</span>
          <span className="ml-1 text-xs opacity-80">{artwork.likeCount + likeBump}</span>
        </button>
      </div>

      <section id="comments" className="mt-12 max-w-2xl scroll-mt-24">
        <h3 className="font-headline text-2xl text-on-surface mb-6">Comments</h3>
        <div className="mb-8">
          <CommentComposer
            onSubmit={async (text) => {
              await comments.add(text)
            }}
          />
        </div>
        {comments.loading ? (
          <p className="py-6 text-center text-on-surface-variant italic font-body text-sm" role="status">
            Loading comments…
          </p>
        ) : comments.error ? (
          <p
            className="py-6 text-center text-error border border-error-container/40 bg-error-container/10 font-body text-sm"
            role="alert"
          >
            {comments.error}
          </p>
        ) : (
          <CommentList
            comments={comments.data}
            onDelete={(id) => {
              void comments.remove(id)
            }}
          />
        )}
      </section>
    </article>
  )
}
