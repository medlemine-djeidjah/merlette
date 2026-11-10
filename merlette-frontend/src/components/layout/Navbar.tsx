// Logo, recherche, cloche, bouton + Observation, menu utilisateur
// TODO

import { Link } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
export default function Navbar() {
  const { user, logout } = useAuth()
  return (
    <nav aria-label="Navigation principale" className="navbar flex-wrap gap-2 bg-base-100 px-4 shadow-sm">
      <Link to="/" className="btn btn-ghost text-xl">Merlette</Link>
      <div className="ml-auto flex flex-wrap items-center gap-2">
        <Link to="/observations/new" className="btn btn-primary btn-sm">Publier une observation</Link>
        {user ? <>
          <Link to="/me/observations" className="btn btn-ghost btn-sm">Mes observations</Link>
          <span className="text-sm">{user.username}</span>
          <button className="btn btn-outline btn-sm" onClick={logout}>Déconnexion</button>
        </> : <>
          <Link to="/login" className="btn btn-ghost btn-sm">Connexion</Link>
          <Link to="/register" className="btn btn-outline btn-sm">Créer un compte</Link>
        </>}
      </div>
    </nav>
  )
}
