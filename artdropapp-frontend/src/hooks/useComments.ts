import { useCallback, useEffect, useState } from 'react'
import {
  deleteComment,
  fetchComments,
  fetchReplies,
  postComment,
} from '../api/commentsApi'
import type { Comment } from '../types/comment'

const PAGE_SIZE = 10

function removeFromTree(list: Comment[], commentId: number): Comment[] {
  const filtered = list.filter((c) => c.id !== commentId)
  return filtered.map((c) =>
    c.replies.length > 0
      ? {
          ...c,
          replies: removeFromTree(c.replies, commentId),
          replyCount: Math.max(0, c.replyCount - (c.replies.length - removeFromTree(c.replies, commentId).length)),
        }
      : c,
  )
}

export function useComments(artworkId: number | null) {
  const [data, setData] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loadingRepliesFor, setLoadingRepliesFor] = useState<number | null>(null)

  const load = useCallback(async () => {
    if (artworkId == null || Number.isNaN(artworkId)) return
    setLoading(true)
    setError(null)
    try {
      const list = await fetchComments(artworkId, PAGE_SIZE + 1, 0)
      const more = list.length > PAGE_SIZE
      setData(more ? list.slice(0, PAGE_SIZE) : list)
      setHasMore(more)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }, [artworkId])

  useEffect(() => {
    void load()
  }, [load])

  const loadMore = useCallback(async () => {
    if (artworkId == null || loadingMore || !hasMore) return
    setLoadingMore(true)
    try {
      const next = await fetchComments(artworkId, PAGE_SIZE + 1, data.length)
      const more = next.length > PAGE_SIZE
      setData((prev) => [...prev, ...(more ? next.slice(0, PAGE_SIZE) : next)])
      setHasMore(more)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoadingMore(false)
    }
  }, [artworkId, data.length, hasMore, loadingMore])

  const loadMoreReplies = useCallback(async (parentId: number) => {
    setLoadingRepliesFor(parentId)
    try {
      const parent = data.find((c) => c.id === parentId)
      const offset = parent?.replies.length ?? 0
      const more = await fetchReplies(parentId, 50, offset)
      setData((prev) =>
        prev.map((c) =>
          c.id === parentId ? { ...c, replies: [...c.replies, ...more] } : c,
        ),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoadingRepliesFor(null)
    }
  }, [data])

  const add = useCallback(
    async (text: string, parentId?: number | null) => {
      if (artworkId == null) throw new Error('No artwork')
      const created = await postComment(artworkId, text, parentId ?? null)
      if (parentId == null) {
        setData((prev) => [created, ...prev])
      } else {
        setData((prev) =>
          prev.map((c) =>
            c.id === parentId
              ? { ...c, replies: [...c.replies, created], replyCount: c.replyCount + 1 }
              : c,
          ),
        )
      }
      return created
    },
    [artworkId],
  )

  const remove = useCallback(async (commentId: number) => {
    const prev = data
    setData((list) => removeFromTree(list, commentId))
    try {
      await deleteComment(commentId)
    } catch (e) {
      setData(prev)
      throw e
    }
  }, [data])

  return {
    data,
    loading,
    loadingMore,
    hasMore,
    loadingRepliesFor,
    error,
    refetch: load,
    loadMore,
    loadMoreReplies,
    add,
    remove,
  }
}
