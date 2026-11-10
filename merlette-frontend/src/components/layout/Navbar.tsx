import { useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import Icon from '../common/Icon'
import Avatar from '../common/Avatar'
import NotificationBell from '../notifications/NotificationBell'
export default function Navbar() {
  const { user, logout } = useAuth()
  const [menuAt, setMenuAt] = useState<string | null>(null)
  const location = useLocation()
  const mobileOpen = menuAt === location.key
  function closeMenu() { setMenuAt(null) }
  return <header className="site-header"><div className="header-inner">
    <Link to="/" className="brand" aria-label="Merlette, accueil"><span className="brand-mark"><Icon name="leaf" size={29} /></span><span><span className="brand-name">merlette</span><span className="brand-tagline">Le vivant se reconnaît ensemble.</span></span></Link>
    <nav className="top-nav" aria-label="Navigation principale">
      <NavLink to="/" end>Explorer</NavLink><NavLink to="/comment-ca-marche">Comment ça marche</NavLink><NavLink className="nav-secondary" to="/especes">Les espèces</NavLink>
    </nav>
    <div className="header-actions">
      {user ? <><NotificationBell /><details className="profile-menu" key={location.key}><summary aria-label="Menu de mon compte"><Avatar name={user.username} url={user.avatarUrl} /><span className="username">{user.username}</span></summary><div className="profile-dropdown">
        <Link to="/me/observations"><Icon name="camera" size={17} />Mes observations</Link><Link to="/me/conversations"><Icon name="chat" size={17} />Mes discussions</Link><Link to={`/users/${user.id}`}><Icon name="user" size={17} />Mon profil</Link>
        {user.role === 'ADMIN' && <Link to="/admin"><Icon name="settings" size={17} />Administration</Link>}
        <button onClick={logout}><Icon name="logout" size={17} />Se déconnecter</button>
      </div></details></> : <Link to="/login" className="button button-quiet">Connexion</Link>}
      <Link to="/observations/new" className="button button-small"><Icon name="plus" size={16} />Observer</Link>
      <button className="icon-button mobile-menu-button" aria-label={mobileOpen ? 'Fermer le menu' : 'Ouvrir le menu'} aria-expanded={mobileOpen} aria-controls="mobile-menu" onClick={() => setMenuAt(mobileOpen ? null : location.key)}><Icon name={mobileOpen ? 'close' : 'menu'} /></button>
    </div>
  </div>{mobileOpen && <nav id="mobile-menu" className="mobile-menu" aria-label="Navigation mobile">
    <Link onClick={closeMenu} to="/">Explorer les observations</Link><Link onClick={closeMenu} to="/comment-ca-marche">Comment ça marche</Link><Link onClick={closeMenu} to="/especes">Rechercher une espèce</Link>
    {user && <><Link onClick={closeMenu} to="/me/observations">Mes observations</Link><Link onClick={closeMenu} to="/me/conversations">Mes discussions</Link></>}
    <Link onClick={closeMenu} className="button" to="/observations/new"><Icon name="plus" />Partager une observation</Link>
  </nav>}</header>
}
