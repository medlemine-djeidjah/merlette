import { Link, Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import { Loading } from '../common/Feedback'
export default function ProtectedRoute({ admin = false }: { admin?: boolean }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="page-heading"><Loading /></div>
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />
  if (admin && user.role !== 'ADMIN') return <section className="page-heading"><span className="eyebrow">Administration</span><h1>Un espace réservé</h1><p>Cette page est réservée à l’équipe d’administration.</p><Link className="button button-secondary" style={{ marginTop: 24 }} to="/me/observations">Retour à mon carnet</Link></section>
  return <Outlet />
}
