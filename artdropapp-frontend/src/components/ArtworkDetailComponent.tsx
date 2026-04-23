import type { Artwork } from '../types/artwork'
import { useComments } from '../hooks/useComments'
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
          <dt>Likes</dt>
          <dd>{artwork.likeCount}</dd>
        </div>
        <div>
          <dt>Comments</dt>
          <dd>{artwork.commentCount}</dd>
        </div>
      </dl>

      <section className="mt-12 max-w-2xl">
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
