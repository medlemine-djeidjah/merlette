// Route : /observations/:id — stories #15, #17, #4, #5, #3
// TODO

import { Link, useParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { getObservation } from '../api/observations'
import { errorMessage, imageUrl } from '../api/client'
export default function ObservationDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({ queryKey: ['observation', id], queryFn: () => getObservation(id) })
  if (query.isPending) return <p role="status">Chargement de l’observation…</p>
  if (query.isError) return <p role="alert" className="alert alert-error">{errorMessage(query.error)}</p>
  const o = query.data
  return <article className="mx-auto max-w-4xl space-y-6 py-6">
    <Link className="link" to="/">← Toutes les observations</Link>
    <header className="space-y-3">
      <span className="badge badge-outline">{o.category.name}</span>
      <h1 className="text-3xl font-bold">{o.title || `Observation #${o.id}`}</h1>
      <p>Par {o.author.username} · Observée le {new Date(`${o.observedAt}T12:00:00`).toLocaleDateString('fr-FR')}</p>
      <span className="badge badge-soft">{o.status === 'NEEDS_ID' ? 'À identifier' : 'Identifiée'}</span>
    </header>
    <div className="grid gap-4 sm:grid-cols-2">{o.photos.map(p => <a key={p.id} href={imageUrl(p.url)} target="_blank" rel="noreferrer">
      <img className="max-h-96 w-full rounded-box object-contain bg-base-100" src={imageUrl(p.url)} alt={o.title || 'Photo de l’observation'} />
    </a>)}</div>
    <div className="rounded-box space-y-4 bg-base-100 p-6">
      {o.description && <p className="whitespace-pre-wrap">{o.description}</p>}
      {o.locationName && <p>Lieu : {o.locationName}</p>}
      {o.latitude !== null && o.longitude !== null && <p>Coordonnées : {o.latitude}, {o.longitude}</p>}
    </div>
  </article>
}
