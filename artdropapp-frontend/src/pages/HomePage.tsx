import { Link } from 'react-router-dom'

export function HomePage() {
  return (
    <main className="app-main app-main--center">
      <h1>ArtDrop</h1>
      <p>
        <Link to="/artworks">Artworks</Link>
      </p>
      <p>
        <Link to="/collections">Collections</Link>
      </p>
    </main>
  )
}
