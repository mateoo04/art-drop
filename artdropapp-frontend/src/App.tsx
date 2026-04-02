import { Route, Routes } from 'react-router-dom'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworksPage } from './pages/ArtworksPage'
import { HomePage } from './pages/HomePage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/artworks" element={<ArtworksPage />} />
      <Route path="/details/:id" element={<ArtworkDetailPage />} />
    </Routes>
  )
}

export default App
