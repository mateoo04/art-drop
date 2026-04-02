import { Link } from 'react-router-dom'
import type { Artwork } from '../types/artwork'

export type ArtworkListComponentProps = {
  artworks: Artwork[]
  loading: boolean
  error: string | null
  onDelete: (title: string) => void
}

export function ArtworkListComponent({
  artworks,
  loading,
  error,
  onDelete,
}: ArtworkListComponentProps) {
  if (loading) {
    return (
      <div className="artwork-list artwork-list--status" role="status">
        Loading artworks…
      </div>
    )
  }

  if (error) {
    return (
      <div className="artwork-list artwork-list--status artwork-list--error" role="alert">
        {error}
      </div>
    )
  }

  return (
    <ul className="artwork-list" aria-label="Artworks list">
      {artworks.map((artwork) => (
        <li key={artwork.id} className="artwork-list__li">
          <div className="artwork-list__row">
            <Link className="artwork-list__item" to={`/details/${artwork.id}`}>
              <span className="artwork-list__title">{artwork.title}</span>
              <span className="artwork-list__medium">{artwork.medium}</span>
              <span className="artwork-list__tags">{artwork.tags.join(', ')}</span>
            </Link>
            <button
              type="button"
              className="artwork-list__delete"
              onClick={() => onDelete(artwork.title)}
              aria-label={`Delete ${artwork.title}`}
            >
              Delete
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}
