export interface CommentAuthor {
  id: number | null
  displayName: string | null
  slug: string | null
  avatarUrl: string | null
}

export interface Comment {
  id: number
  text: string
  createdAt: string
  author: CommentAuthor
  isAuthor: boolean
}
