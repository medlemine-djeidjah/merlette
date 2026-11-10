import { useEffect, useRef } from 'react'
import { Link, Outlet, useLocation } from 'react-router'
import Navbar from './Navbar'
import Icon from '../common/Icon'
export default function Layout() {
  const { pathname } = useLocation()
  const main = useRef<HTMLElement>(null)
  const previous = useRef(pathname)
  useEffect(() => {
    if (previous.current !== pathname) {
      window.scrollTo({ top: 0, behavior: 'instant' }); main.current?.focus({ preventScroll: true })
      previous.current = pathname
    }
  }, [pathname])
  return <><a className="skip-link" href="#main-content">Aller au contenu</a><Navbar />
    <main className="main-content" id="main-content" ref={main} tabIndex={-1}><Outlet /></main>
    <footer className="site-footer"><span className="footer-brand"><Icon name="leaf" size={17} />Merlette · Un carnet du vivant, écrit ensemble.</span><nav aria-label="Pied de page"><Link to="/comment-ca-marche">Le projet</Link><Link to="/observations/new">Contribuer</Link><span>Science participative</span></nav></footer>
  </>
}
