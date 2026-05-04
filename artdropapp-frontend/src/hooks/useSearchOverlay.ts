import { useContext } from 'react'
import { SearchOverlayContext, type SearchOverlayContextValue } from '../contexts/SearchOverlayContext'

export function useSearchOverlay(): SearchOverlayContextValue {
  const ctx = useContext(SearchOverlayContext)
  if (!ctx) throw new Error('useSearchOverlay must be used within SearchOverlayProvider')
  return ctx
}
