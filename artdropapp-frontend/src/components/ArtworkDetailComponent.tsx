import type { Artwork } from '../types/artwork'

export type ArtworkDetailComponentProps = {
  selectedArtworkId: number | null
  artworks: Artwork[] | null
}

function formatPublishedAt(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString()
}

export function ArtworkDetailComponent({
  selectedArtworkId,
  artworks,
}: ArtworkDetailComponentProps) {
  const list = artworks ?? []
  const selected =
    selectedArtworkId !== null &&
    selectedArtworkId >= 0 &&
    selectedArtworkId < list.length
      ? list[selectedArtworkId]
      : null

  if (!selected) {
    return (
      <div className="artwork-detail artwork-detail--empty" role="region" aria-label="Detalji">
        Odaberite rad iz liste.
      </div>
    )
  }

  return (
    <article className="artwork-detail" aria-label="Detalji odabranog rada">
      <h2>{selected.title}</h2>
      <dl className="artwork-detail__fields">
        <div>
          <dt>Medij</dt>
          <dd>{selected.medium}</dd>
        </div>
        <div>
          <dt>Oznake</dt>
          <dd>{selected.tags.join(', ')}</dd>
        </div>
        <div>
          <dt>Objavljeno</dt>
          <dd>{formatPublishedAt(selected.publishedAt)}</dd>
        </div>
        <div>
          <dt>Lajkovi</dt>
          <dd>{selected.likeCount}</dd>
        </div>
        <div>
          <dt>Komentari</dt>
          <dd>{selected.commentCount}</dd>
        </div>
      </dl>
    </article>
  )
}
