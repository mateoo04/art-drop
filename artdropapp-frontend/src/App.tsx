import { useState } from 'react'
import { ArtworkDetailComponent } from './components/ArtworkDetailComponent'
import { ArtworkListComponent } from './components/ArtworkListComponent'
import { useArtworks } from './hooks/useArtworks'
import './App.css'

function App() {
  const [selectedArtworkId, setSelectedArtworkId] = useState<number | null>(null)
  const { data, loading, error } = useArtworks()

  return (
    <main className="app-main">
      <h1>ArtDrop</h1>
      <section className="app-section" aria-labelledby="list-heading">
        <h2 id="list-heading">Radovi</h2>
        <ArtworkListComponent
          artworks={data ?? []}
          loading={loading}
          error={error}
          selectedArtworkId={selectedArtworkId}
          onSelectArtwork={setSelectedArtworkId}
        />
      </section>
      <section className="app-section" aria-labelledby="detail-heading">
        <h2 id="detail-heading">Detalji</h2>
        <ArtworkDetailComponent selectedArtworkId={selectedArtworkId} artworks={data} />
      </section>
    </main>
  )
}

export default App
