import { Link } from 'react-router'
import type { Observation } from '../../types'
import { formatDate } from '../../utils/format'
import Avatar from '../common/Avatar'
import Icon from '../common/Icon'
import Photo from '../common/Photo'
import StatusBadge from './StatusBadge'
export default function ObservationCard({ observation: o }: { observation: Observation }) {
  return <article className="observation-card"><Link to={`/observations/${o.id}`} aria-label={`Voir ${o.title || `l’observation ${o.id}`}`}>
    <div className="card-photo"><Photo path={o.coverUrl} alt={o.title || 'Observation naturaliste'} /><span className="card-category">{o.category.name}</span></div>
    <div className="card-content"><div className="card-title-row"><h3>{o.title || o.identifiedSpecies?.commonName || 'Une rencontre avec le vivant'}</h3><StatusBadge status={o.status} /></div>
      <div className="card-author"><Avatar name={o.author.username} /><span>@{o.author.username}</span><time dateTime={o.observedAt}>· {formatDate(o.observedAt, { day: 'numeric', month: 'short' })}</time></div>
      <div className="card-bottom"><span className="location-small"><Icon name="pin" size={12} />{o.locationName || 'Lieu non précisé'}</span><span className="card-metrics"><span aria-label={o.commentCount == null ? 'Nombre de commentaires indisponible' : `${o.commentCount} commentaires`} title="Commentaires"><Icon name="chat" size={12} />{o.commentCount ?? '—'}</span><span aria-label={o.proposalCount == null ? 'Nombre de propositions indisponible' : `${o.proposalCount} propositions`} title="Propositions"><Icon name="leaf" size={12} />{o.proposalCount ?? '—'}</span></span></div>
    </div>
  </Link></article>
}
