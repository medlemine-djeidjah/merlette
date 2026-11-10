// Carte du fil : photo, catégorie, statut, auteur, compteurs (#8)
// TODO

import { Link } from 'react-router'
import type { Observation } from '../../types'
import { imageUrl } from '../../api/client'
export default function ObservationCard({ observation: o }: { observation: Observation }) {
  return <article className="card overflow-hidden bg-base-100 shadow-sm">
    <Link to={`/observations/${o.id}`} className="block">
      {o.coverUrl && <img className="aspect-[4/3] w-full object-cover" src={imageUrl(o.coverUrl)} alt={o.title || 'Observation naturaliste'} loading="lazy" />}
      <div className="card-body gap-2">
        <span className="badge badge-outline">{o.category.name}</span>
        <h2 className="card-title">{o.title || `Observation #${o.id}`}</h2>
        <p className="text-sm">Par {o.author.username} · {new Date(`${o.observedAt}T12:00:00`).toLocaleDateString('fr-FR')}</p>
        <span className="badge badge-soft">{o.status === 'NEEDS_ID' ? 'À identifier' : 'Identifiée'}</span>
        {o.locationName && <p>{o.locationName}</p>}
      </div>
    </Link>
  </article>
}
