import Masonry from 'react-masonry-css'
import type { Artwork } from '../../types/artwork'
import { ArtworkCard } from './ArtworkCard'
import { MASONRY_BREAKPOINT_COLS } from './masonryFeedConfig'

type MasonryFeedProps = {
  artworks: Artwork[]
}

export function MasonryFeed({ artworks }: MasonryFeedProps) {
  return (
    <Masonry
      breakpointCols={MASONRY_BREAKPOINT_COLS}
      className="masonry-grid-mc"
      columnClassName="masonry-grid-mc__column"
    >
      {artworks.map((artwork) => (
        <ArtworkCard key={artwork.id} artwork={artwork} />
      ))}
    </Masonry>
  )
}
