export interface UserProfile {
  id: number
  username: string
  slug: string
  displayName: string
  bio: string | null
  avatarUrl: string | null
  createdAt: string
  artworkCount: number
  circleSize: number | null
  followingCount: number | null
  isSelf: boolean
}
