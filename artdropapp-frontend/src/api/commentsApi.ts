import { authFetch } from '../lib/authFetch'
import type { Comment } from '../types/comment'

function normalizeCreatedAt(value: unknown): string {
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value ?? '')
}

function mapComment(raw: Record<string, unknown>): Comment {
  const repliesRaw = Array.isArray(raw.replies) ? raw.replies : []
  return {
    id: Number(raw.id),
    text: String(raw.text ?? ''),
    createdAt: normalizeCreatedAt(raw.createdAt),
    author: {
      id: raw.authorId == null ? null : Number(raw.authorId),
      displayName: raw.authorDisplayName == null ? null : String(raw.authorDisplayName),
      slug: raw.authorSlug == null ? null : String(raw.authorSlug),
      avatarUrl: raw.authorAvatarUrl == null ? null : String(raw.authorAvatarUrl),
    },
    isAuthor: Boolean(raw.isAuthor),
    parentId: raw.parentCommentId == null ? null : Number(raw.parentCommentId),
    replyCount: Number(raw.replyCount ?? 0),
    replies: repliesRaw.map((r) => mapComment(r as Record<string, unknown>)),
  }
}

export async function fetchComments(
  artworkId: number,
  limit = 10,
  offset = 0,
): Promise<Comment[]> {
  const res = await authFetch(
    `/api/artworks/${artworkId}/comments?limit=${limit}&offset=${offset}`,
  )
  if (!res.ok) {
    throw new Error(`Failed to load comments (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapComment(item as Record<string, unknown>))
}

export async function fetchReplies(
  parentId: number,
  limit = 20,
  offset = 0,
): Promise<Comment[]> {
  const res = await authFetch(
    `/api/comments/${parentId}/replies?limit=${limit}&offset=${offset}`,
  )
  if (!res.ok) {
    throw new Error(`Failed to load replies (${res.status})`)
  }
  const json: unknown = await res.json()
  if (!Array.isArray(json)) {
    throw new Error('Unexpected server response')
  }
  return json.map((item) => mapComment(item as Record<string, unknown>))
}

export async function postComment(
  artworkId: number,
  text: string,
  parentCommentId?: number | null,
): Promise<Comment> {
  const body: { text: string; parentCommentId?: number } = { text }
  if (parentCommentId != null) body.parentCommentId = parentCommentId
  const res = await authFetch(`/api/artworks/${artworkId}/comments`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    throw new Error(`Failed to post comment (${res.status})`)
  }
  const json = (await res.json()) as Record<string, unknown>
  return mapComment(json)
}

export async function deleteComment(commentId: number): Promise<void> {
  const res = await authFetch(`/api/comments/${commentId}`, { method: 'DELETE' })
  if (!res.ok && res.status !== 204) {
    throw new Error(`Failed to delete comment (${res.status})`)
  }
}
