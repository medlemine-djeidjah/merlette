import { Link } from 'react-router'
import ObservationFeed from '../components/observations/ObservationFeed'
import Botanical from '../components/common/Botanical'
import Icon from '../components/common/Icon'
export default function HomePage() {
  return <><section className="hero"><div className="hero-copy"><span className="eyebrow"><Icon name="leaf" size={15} />Observer · Partager · Comprendre</span><h1>Le vivant est partout.<br /><em>La curiosité aussi.</em></h1><p>Une rencontre au jardin, un détail sur le chemin. Partagez vos observations et mettons, ensemble, un nom sur ce qui nous entoure.</p><div className="hero-actions"><Link className="button" to="/observations/new"><Icon name="plus" size={17} />Partager une observation</Link><Link className="text-link" to="/comment-ca-marche">Comment ça marche <span aria-hidden="true">↗</span></Link></div></div><div className="hero-visual"><Botanical className="hero-art" /><p className="hero-note">UN CARNET DU VIVANT, ÉCRIT ENSEMBLE.</p></div></section>
    <ObservationFeed /><aside className="field-notes"><Icon name="leaf" size={30} /><div><h3>Pas besoin d’être spécialiste pour être curieux.</h3><p>Une photo nette, quelques indices, et la découverte commence.</p></div><Link className="button button-secondary button-small" to="/comment-ca-marche">Découvrir le principe <Icon name="arrow" size={15} /></Link></aside>
  </>
}
