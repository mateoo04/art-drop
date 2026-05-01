import { Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { MainLayout } from './components/layout/MainLayout'
import { AccountPage } from './pages/AccountPage'
import { ProfilePage } from './pages/ProfilePage'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworkEditPage } from './pages/ArtworkEditPage'
import { ChallengesPage } from './pages/ChallengesPage'
import { CirclePage } from './pages/CirclePage'
import { CollectionsPage } from './pages/CollectionsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { SignupPage } from './pages/SignupPage'
import { AdminLayoutPage } from './pages/admin/AdminLayoutPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
import { AdminUserDetailPage } from './pages/admin/AdminUserDetailPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route
          path="/circle"
          element={
            <ProtectedRoute>
              <CirclePage />
            </ProtectedRoute>
          }
        />
        <Route path="/challenges" element={<ChallengesPage />} />
        <Route path="/collections" element={<CollectionsPage />} />
        <Route path="/details/:id" element={<ArtworkDetailPage />} />
        <Route path="/edit/:id" element={<ArtworkEditPage />} />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />
        <Route path="/u/:slug" element={<ProfilePage />} />
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminLayoutPage />
            </AdminRoute>
          }
        >
          <Route index element={<AdminUsersPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="users/:id" element={<AdminUserDetailPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
