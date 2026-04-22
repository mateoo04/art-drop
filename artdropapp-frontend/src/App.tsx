import { Route, Routes } from 'react-router-dom'
import { AppFooter } from './components/layout/AppFooter'
import { AppHeader } from './components/layout/AppHeader'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworkEditPage } from './pages/ArtworkEditPage'
import { ArtworksPage } from './pages/ArtworksPage'
import { ChallengesPage } from './pages/ChallengesPage'
import { CirclePage } from './pages/CirclePage'
import { CollectionsPage } from './pages/CollectionsPage'
import { HomePage } from './pages/HomePage'
import './App.css'

function App() {
  return (
    <>
      <AppHeader />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/circle" element={<CirclePage />} />
        <Route path="/challenges" element={<ChallengesPage />} />
        <Route path="/artworks" element={<ArtworksPage />} />
        <Route path="/collections" element={<CollectionsPage />} />
        <Route path="/details/:id" element={<ArtworkDetailPage />} />
        <Route path="/edit/:id" element={<ArtworkEditPage />} />
      </Routes>
      <AppFooter />
    </>
  )
}

export default App
