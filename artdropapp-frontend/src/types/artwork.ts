export type ProgressStatus = 'WIP' | 'FINISHED'
export type SaleStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD'

export interface Artist {
  id: number
  displayName: string
  slug: string
  avatarUrl: string | null
}

export interface Artwork {
  id: number
  title: string
  medium: string
  description: string | null
  imageUrl: string
  imageAlt: string
  aspectRatio: number
  price: number | null
  progressStatus: ProgressStatus | null
  saleStatus: SaleStatus | null
  artist: Artist | null
  tags: string[]
  publishedAt: string
  likeCount: number
  commentCount: number
}
