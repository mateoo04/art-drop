import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AdminRoute } from './components/AdminRoute'
import { MainLayout } from './components/layout/MainLayout'
import { AccountPage } from './pages/AccountPage'
import { ProfilePage } from './pages/ProfilePage'
import { ArtworkDetailPage } from './pages/ArtworkDetailPage'
import { ArtworkDropPage } from './pages/ArtworkDropPage'
import { ArtworkEditPage } from './pages/ArtworkEditPage'
import { ChallengeDetailPage } from './pages/ChallengeDetailPage'
import { ChallengesPage } from './pages/ChallengesPage'
import { CollectionsPage } from './pages/CollectionsPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrdersPage } from './pages/OrdersPage'
import { OrderDetailPage } from './pages/OrderDetailPage'
import { SalesPage } from './pages/SalesPage'
import { SearchPage } from './pages/SearchPage'
import { SignupPage } from './pages/SignupPage'
import { AdminChallengeFormPage } from './pages/admin/AdminChallengeFormPage'
import { AdminChallengesPage } from './pages/admin/AdminChallengesPage'
import { AdminLayoutPage } from './pages/admin/AdminLayoutPage'
import { AdminUsersPage } from './pages/admin/AdminUsersPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route element={<MainLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/challenges" element={<ChallengesPage />} />
        <Route path="/challenges/:id" element={<ChallengeDetailPage />} />
        <Route path="/collections" element={<CollectionsPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/details/:id" element={<ArtworkDetailPage />} />
        <Route path="/edit/:id" element={<ArtworkEditPage />} />
        <Route
          path="/drop"
          element={
            <ProtectedRoute>
              <ArtworkDropPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/account"
          element={
            <ProtectedRoute>
              <AccountPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/checkout/:artworkId"
          element={
            <ProtectedRoute>
              <CheckoutPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <ProtectedRoute>
              <OrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/orders/:id"
          element={
            <ProtectedRoute>
              <OrderDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sales"
          element={
            <ProtectedRoute>
              <SalesPage />
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
          <Route index element={<Navigate to="users" replace />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="users/:id" element={<Navigate to="/admin/users" replace />} />
          <Route path="challenges" element={<AdminChallengesPage />} />
          <Route path="challenges/new" element={<AdminChallengeFormPage />} />
          <Route path="challenges/:id/edit" element={<AdminChallengeFormPage />} />
        </Route>
      </Route>
    </Routes>
  )
}

export default App
