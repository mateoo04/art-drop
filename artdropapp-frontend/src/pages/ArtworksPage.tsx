import { useState } from 'react'
import { deleteArtworkByTitle } from '../api/artworksApi'
import { ArtworkCreateForm } from '../components/ArtworkCreateForm'
import { ArtworkListComponent } from '../components/ArtworkListComponent'
import { useArtworks } from '../hooks/useArtworks'

export function ArtworksPage() {
  const { data, loading, error, refetch, setData } = useArtworks()
  const [deleteError, setDeleteError] = useState<string | null>(null)

  async function handleDelete(title: string) {
    const previous = data
    setDeleteError(null)
    setData((curr) => curr?.filter((a) => a.title !== title) ?? null)
    try {
      await deleteArtworkByTitle(title)
    } catch (err) {
      setData(previous ?? null)
      setDeleteError(err instanceof Error ? err.message : 'Delete failed')
    }
  }

  return (
    <main className="app-main">
      <h1>ArtDrop – Artworks</h1>
      <section className="app-section" aria-labelledby="list-heading">
        <h2 id="list-heading">Artworks</h2>
        <ArtworkListComponent
          artworks={data ?? []}
          loading={loading}
          error={error}
          onDelete={(title) => void handleDelete(title)}
        />
        {deleteError ? (
          <p className="artwork-form__message" role="alert">
            {deleteError}
          </p>
        ) : null}
      </section>
      <section className="app-section" aria-labelledby="form-heading">
        <h2 id="form-heading" className="visually-hidden">
          New artwork
        </h2>
        <ArtworkCreateForm onCreated={() => void refetch()} />
      </section>
    </main>
  )
}
