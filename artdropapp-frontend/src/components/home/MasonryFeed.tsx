import Masonry from 'react-masonry-css'
import type { HomeFeedItem } from '../../api/artworksApi'
import { ArtworkCard } from './ArtworkCard'
import { ChallengePromoCard } from './ChallengePromoCard'
import { MASONRY_BREAKPOINT_COLS } from './masonryFeedConfig'

type MasonryFeedProps = {
  items: HomeFeedItem[]
  onCardSeen?: (artworkId: number) => void
}

export function MasonryFeed({ items, onCardSeen }: MasonryFeedProps) {
  return (
    <Masonry
      breakpointCols={MASONRY_BREAKPOINT_COLS}
      className="masonry-grid-mc"
      columnClassName="masonry-grid-mc__column"
    >
      {items.map((item) =>
        item.kind === 'ARTWORK' ? (
          <ArtworkCard key={`a-${item.artwork.id}`} artwork={item.artwork} onSeen={onCardSeen} />
        ) : (
          <ChallengePromoCard key={`c-${item.challenge.id}`} challenge={item.challenge} />
        ),
      )}
    </Masonry>
  )
}
