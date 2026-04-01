import type { Artwork } from '../types/artwork'

export type ArtworkListComponentProps = {
  artworks: Artwork[]
  loading: boolean
  error: string | null
  selectedArtworkId: number | null
  onSelectArtwork: (id: number) => void
}

export function ArtworkListComponent({
  artworks,
  loading,
  error,
  selectedArtworkId,
  onSelectArtwork,
}: ArtworkListComponentProps) {
  if (loading) {
    return (
      <div className="artwork-list artwork-list--status" role="status">
        Učitavanje radova…
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
    <ul className="artwork-list" aria-label="Popis radova">
      {artworks.map((artwork, index) => (
        <li key={`${artwork.title}-${index}`}>
          <button
            type="button"
            className={`artwork-list__item${selectedArtworkId === index ? ' artwork-list__item--selected' : ''}`}
            onClick={() => onSelectArtwork(index)}
          >
            <span className="artwork-list__title">{artwork.title}</span>
            <span className="artwork-list__medium">{artwork.medium}</span>
            <span className="artwork-list__tags">{artwork.tags.join(', ')}</span>
          </button>
        </li>
      ))}
    </ul>
  )
}
