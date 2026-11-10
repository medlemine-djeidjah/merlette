import { useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { deleteObservation, getObservation } from '../api/observations'
import { useAuth } from '../hooks/useAuth'
import { features } from '../config/features'
import { formatDate } from '../utils/format'
import Icon from '../components/common/Icon'
import Avatar from '../components/common/Avatar'
import { EmptyState, ErrorState, Loading } from '../components/common/Feedback'
import ConfirmDialog from '../components/common/ConfirmDialog'
import PhotoGallery from '../components/observations/PhotoGallery'
import StatusBadge from '../components/observations/StatusBadge'
import ObservationEditor from '../components/observations/ObservationEditor'
import ProposalList from '../components/identification/ProposalList'
import CommentList from '../components/comments/CommentList'
export default function ObservationDetailPage() {
  const { id = '' } = useParams()
  const { user } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const cache = useQueryClient()
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [dismissed, setDismissed] = useState(false)
  const valid = /^\d+$/.test(id)
  const query = useQuery({ queryKey: ['observation', id], queryFn: () => getObservation(id), enabled: valid })
  const deletion = useMutation({ mutationFn: () => deleteObservation(Number(id)), onSuccess: async () => {
    await Promise.all(['observations', 'profile', 'profile-observations', 'profile-identified', 'conversations', 'notifications', 'notification-count', 'moderation-observations'].map(key => cache.invalidateQueries({ queryKey: [key] })))
    navigate('/me/observations', { replace: true })
  } })
  if (!valid) return <div className="page-heading"><EmptyState title="Une observation introuvable" action={<Link className="button" to="/">Retour au carnet</Link>}>Ce lien ne correspond pas à une observation.</EmptyState></div>
  if (query.isPending) return <div className="page-heading"><Loading /></div>
  if (query.isError) return <div className="page-heading"><Link className="back-link" to="/"><Icon name="back" size={15} />Retour au carnet</Link><ErrorState error={query.error} retry={() => query.refetch()} /></div>
  const o = query.data
  const owner = user?.id === o.author.id
  return <article className="detail-page"><Link className="back-link" to="/"><Icon name="back" size={15} />Retour aux observations</Link>
    {location.state?.published && !dismissed && <div role="status" className="feedback feedback-success success-banner"><Icon name="check" /><div><strong>Votre rencontre rejoint le carnet.</strong><p>L’observation a bien été publiée et est visible par la communauté.</p></div><button className="icon-button" style={{ marginLeft: 'auto' }} aria-label="Masquer la confirmation" onClick={() => setDismissed(true)}><Icon name="close" size={16} /></button></div>}
    <div className="detail-layout"><PhotoGallery key={o.id} observation={o} /><div><header className="detail-heading"><div className="tag-row"><span className="pill">{o.category.name}</span><StatusBadge status={o.status} /></div><h1>{o.title || o.identifiedSpecies?.commonName || 'Une rencontre avec le vivant'}</h1>{o.status === 'IDENTIFIED' && o.identifiedSpecies && <section className="soft-note" aria-label="Espèce retenue"><span className="eyebrow">Espèce retenue</span><h2 style={{ marginTop: 8 }}>{o.identifiedSpecies.commonName}</h2><p className="species-latin">{o.identifiedSpecies.scientificName}</p></section>}<p className="muted"><Icon name="calendar" size={15} />Observée le {formatDate(o.observedAt)}</p>{o.locationName && <p className="muted"><Icon name="pin" size={15} />{o.locationName}</p>}
      <Link className="author-line" to={`/users/${o.author.id}`}><Avatar name={o.author.username} /><span>{o.author.username}<small>@{o.author.username} · Publié le {formatDate(o.createdAt, { day: 'numeric', month: 'short' })}</small></span></Link></header>
      {o.description && <p className="observation-description">{o.description}</p>}
      {o.latitude != null && o.longitude != null && <p className="field-help" style={{ marginBottom: 20 }}>Coordonnées : {o.latitude}, {o.longitude}</p>}
      {(owner || user?.role === 'ADMIN') && (features.management || features.deletion) && <div className="management-actions">{features.management && owner && <button className="button button-secondary button-small" onClick={() => setEditing(true)}><Icon name="edit" size={14} />Modifier</button>}{features.deletion && <button className="button button-secondary button-small" onClick={() => setDeleting(true)}><Icon name="trash" size={14} />Supprimer</button>}</div>}
      <ProposalList key={`proposals-${o.id}`} observation={o} /><CommentList key={`comments-${o.id}`} observationId={o.id} />
    </div></div>
    {editing && <ObservationEditor observation={o} onClose={() => setEditing(false)} />}
    {deleting && <ConfirmDialog title="Supprimer cette observation ?" pending={deletion.isPending} onClose={() => { setDeleting(false); deletion.reset() }} onConfirm={() => deletion.mutate()}><p>Les photos, propositions et commentaires associés seront également supprimés. Cette action est définitive.</p>{deletion.isError && <ErrorState error={deletion.error} />}</ConfirmDialog>}
  </article>
}
