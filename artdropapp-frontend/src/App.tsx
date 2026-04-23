import { Route, Routes } from 'react-router-dom'
import { MainLayout } from './components/layout/MainLayout'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworkEditPage } from './pages/ArtworkEditPage'
import { ArtworksPage } from './pages/ArtworksPage'
import { ChallengesPage } from './pages/ChallengesPage'
import { CirclePage } from './pages/CirclePage'
import { CollectionsPage } from './pages/CollectionsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/circle" element={<CirclePage />} />
        <Route path="/challenges" element={<ChallengesPage />} />
        <Route path="/artworks" element={<ArtworksPage />} />
        <Route path="/collections" element={<CollectionsPage />} />
        <Route path="/details/:id" element={<ArtworkDetailPage />} />
        <Route path="/edit/:id" element={<ArtworkEditPage />} />
      </Route>
    </Routes>
  )
}

export default App
