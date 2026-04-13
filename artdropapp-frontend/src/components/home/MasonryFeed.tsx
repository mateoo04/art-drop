import type { Artwork } from '../../types/artwork'
import { ArtworkCard } from './ArtworkCard'

type MasonryFeedProps = {
  artworks: Artwork[]
}

export function MasonryFeed({ artworks }: MasonryFeedProps) {
  return (
    <section className="masonry-grid" aria-label="Artwork feed">
      {artworks.map((artwork) => (
        <ArtworkCard key={artwork.id} artwork={artwork} />
      ))}
    </section>
  )
}
