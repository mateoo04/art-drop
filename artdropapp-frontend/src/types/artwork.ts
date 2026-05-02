export type ProgressStatus = 'WIP' | 'FINISHED'
export type SaleStatus = 'ORIGINAL' | 'EDITION' | 'AVAILABLE' | 'SOLD'
export type DimensionUnit = 'CM' | 'MM' | 'IN' | 'PX'

export interface Artist {
  id: number
  displayName: string
  slug: string
  avatarUrl: string | null
}

export interface ArtworkImage {
  id: number | null
  imageUrl: string
  publicId: string
  sortOrder: number
  isCover: boolean
  caption: string | null
}

export interface Artwork {
  id: number
  title: string
  medium: string
  description: string | null
  imageUrl: string
  coverPublicId: string
  imageAlt: string
  aspectRatio: number
  images: ArtworkImage[]
  width: number | null
  height: number | null
  depth: number | null
  dimensionUnit: DimensionUnit | null
  price: number | null
  progressStatus: ProgressStatus | null
  saleStatus: SaleStatus | null
  artist: Artist | null
  tags: string[]
  publishedAt: string
  likeCount: number
  commentCount: number
  likedByMe: boolean
}
