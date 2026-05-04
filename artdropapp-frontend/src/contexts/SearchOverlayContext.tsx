/* eslint-disable react-refresh/only-export-components -- context + provider colocated */
import { createContext, useCallback, useMemo, useState, type ReactNode } from 'react'
import { SearchOverlay } from '../components/search/SearchOverlay'

export type SearchOverlayContextValue = {
  openSearch: () => void
  closeSearch: () => void
  isOpen: boolean
}

export const SearchOverlayContext = createContext<SearchOverlayContextValue | null>(null)

export function SearchOverlayProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false)

  const openSearch = useCallback(() => setOpen(true), [])
  const closeSearch = useCallback(() => setOpen(false), [])

  const value = useMemo<SearchOverlayContextValue>(
    () => ({ openSearch, closeSearch, isOpen: open }),
    [openSearch, closeSearch, open],
  )

  return (
    <SearchOverlayContext.Provider value={value}>
      {children}
      {open ? <SearchOverlay onClose={closeSearch} /> : null}
    </SearchOverlayContext.Provider>
  )
}
