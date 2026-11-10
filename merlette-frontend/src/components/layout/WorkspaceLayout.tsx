import { Link, NavLink, Outlet } from 'react-router'
import { useAuth } from '../../hooks/useAuth'
import Icon from '../common/Icon'
export default function WorkspaceLayout() {
  const { user } = useAuth()
  return <div className="workspace"><aside className="workspace-sidebar"><nav aria-label="Mon espace">
    <NavLink to="/me/observations"><Icon name="camera" size={18} />Mes observations</NavLink><NavLink to="/me/conversations"><Icon name="chat" size={18} />Mes discussions</NavLink><NavLink to="/notifications"><Icon name="bell" size={18} />Notifications</NavLink><NavLink to={`/users/${user?.id}`}><Icon name="user" size={18} />Mon profil</NavLink>
    {user?.role === 'ADMIN' && <NavLink to="/admin"><Icon name="settings" size={18} />Administration</NavLink>}
  </nav><div className="sidebar-note"><span className="eyebrow">Votre contribution</span><h3>Chaque regard<br />compte.</h3><p>Un oiseau, une feuille, une rencontre. Partagez ce qui vous émerveille.</p><Link className="button" to="/observations/new"><Icon name="plus" size={15} />Nouvelle observation</Link></div></aside><div className="workspace-main"><Outlet /></div></div>
}
