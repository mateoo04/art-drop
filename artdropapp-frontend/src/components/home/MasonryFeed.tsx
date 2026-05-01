import Masonry from 'react-masonry-css'
import type { Artwork } from '../../types/artwork'
import { ArtworkCard } from './ArtworkCard'

const BREAKPOINTS = {
  default: 3,
  1024: 3,
  768: 2,
  0: 1,
}

type MasonryFeedProps = {
  artworks: Artwork[]
}

export function MasonryFeed({ artworks }: MasonryFeedProps) {
  return (
    <Masonry
      breakpointCols={BREAKPOINTS}
      className="masonry-grid-mc"
      columnClassName="masonry-grid-mc__column"
    >
      {artworks.map((artwork) => (
        <ArtworkCard key={artwork.id} artwork={artwork} />
      ))}
    </Masonry>
  )
}
