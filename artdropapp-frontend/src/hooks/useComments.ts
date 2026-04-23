import { useCallback, useEffect, useState } from 'react'
import { deleteComment, fetchComments, postComment } from '../api/commentsApi'
import type { Comment } from '../types/comment'

export function useComments(artworkId: number | null) {
  const [data, setData] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (artworkId == null || Number.isNaN(artworkId)) return
    setLoading(true)
    setError(null)
    try {
      const list = await fetchComments(artworkId)
      setData(list)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unknown error')
    } finally {
      setLoading(false)
    }
  }, [artworkId])

  useEffect(() => {
    void load()
  }, [load])

  const add = useCallback(
    async (text: string) => {
      if (artworkId == null) throw new Error('No artwork')
      const created = await postComment(artworkId, text)
      setData((prev) => [created, ...prev])
      return created
    },
    [artworkId],
  )

  const remove = useCallback(async (commentId: number) => {
    const prev = data
    setData((list) => list.filter((c) => c.id !== commentId))
    try {
      await deleteComment(commentId)
    } catch (e) {
      setData(prev)
      throw e
    }
  }, [data])

  return { data, loading, error, refetch: load, add, remove }
}
