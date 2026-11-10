// Redirige vers /login si non connecté
// TODO

import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
export default function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <p role="status">Vérification de la session…</p>
  return user ? <Outlet /> : <Navigate to="/login" state={{ from: location.pathname }} replace />
}
