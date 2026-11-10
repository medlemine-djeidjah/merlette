import { Link } from 'react-router'
import Botanical from '../components/common/Botanical'
import Icon from '../components/common/Icon'
export default function NotFoundPage() {
  return <section className="not-found"><div><span className="eyebrow">404 · Hors du sentier</span><h1>Cette page s’est<br />fait la belle.</h1><p>Le chemin a peut-être changé. Revenons là où les découvertes commencent.</p><Link className="button" to="/"><Icon name="back" size={17} />Retour aux observations</Link></div><Botanical className="not-found-art" /></section>
}
