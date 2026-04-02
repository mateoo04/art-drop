import type { Artwork } from '../types/artwork'

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
    </article>
  )
}
