import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getProfile, updateProfile } from '../api/users'
import { getObservations } from '../api/observations'
import { TOKEN_KEY } from '../api/client'
import { features } from '../config/features'
import { useAuth } from '../hooks/useAuth'
import { formatDate } from '../utils/format'
import Avatar from '../components/common/Avatar'
import Icon from '../components/common/Icon'
import ObservationCard from '../components/observations/ObservationCard'
import { ComingSoon, EmptyState, ErrorState, Loading } from '../components/common/Feedback'
export default function ProfilePage() {
  const { id = '' } = useParams()
  const { user, accept } = useAuth()
  const valid = /^\d+$/.test(id) && Number.isSafeInteger(Number(id)) && Number(id) > 0
  const own = user?.id === Number(id)
  const [editing, setEditing] = useState(false)
  const [saved, setSaved] = useState(false)
  const cache = useQueryClient()
  const profile = useQuery({ queryKey: ['profile', id], queryFn: () => getProfile(Number(id)), enabled: valid && features.profiles })
  const observations = useQuery({ queryKey: ['profile-observations', id], queryFn: () => getObservations({ userId: Number(id), limit: 6 }), enabled: valid })
  const identified = useQuery({ queryKey: ['profile-identified', id], queryFn: () => getObservations({ userId: Number(id), status: 'IDENTIFIED', limit: 1 }), enabled: valid && !features.profiles })
  const mutation = useMutation({ mutationFn: updateProfile, onSuccess: async updated => { if (own) { const token = localStorage.getItem(TOKEN_KEY); if (token) accept({ token, user: updated }) } setEditing(false); setSaved(true); await cache.invalidateQueries({ queryKey: ['profile', id] }) } })
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); const form = new FormData(event.currentTarget); mutation.mutate({ bio: String(form.get('bio')), avatarUrl: String(form.get('avatarUrl')) }) }
  if (!valid) return <div className="page-heading"><EmptyState title="Ce profil est introuvable">Vérifiez le lien ou revenez aux observations.</EmptyState></div>
  if (profile.isLoading) return <div className="page-heading"><Loading /></div>
  if (profile.isError) return <div className="page-heading"><ErrorState error={profile.error} retry={() => profile.refetch()} /></div>
  const data = profile.data
  const name = data?.username || (own ? user?.username : observations.data?.data[0]?.author.username) || 'Un regard sur le vivant'
  const bio = data?.bio || (own ? user?.bio : null)
  return <><header className="profile-header"><Avatar name={name} url={data?.avatarUrl || (own ? user?.avatarUrl : null)} large /><div><span className="eyebrow">{own ? 'Mon carnet de terrain' : 'Un membre de la communauté'}</span><h1>{name}</h1><p className="muted small">{data || own ? `@${name}` : 'Carnet public'}{(data?.createdAt || (own && user?.createdAt)) && ` · Membre depuis ${formatDate(data?.createdAt || user?.createdAt, { month: 'long', year: 'numeric' })}`}</p></div>{own && <button className="button button-secondary button-small" disabled={!features.profiles} title={!features.profiles ? 'La personnalisation du profil sera bientôt disponible.' : undefined} onClick={() => setEditing(!editing)}><Icon name="edit" size={15} />Modifier mon profil</button>}</header>
    <p className="profile-bio">{bio || 'Chaque observation est une nouvelle occasion d’apprendre à connaître le vivant.'}</p>
    {saved && <p role="status" className="feedback feedback-success" style={{ marginBottom: 25 }}>Votre profil a bien été mis à jour.</p>}
    {editing && <form className="panel form-stack settings-form" onSubmit={submit}>{mutation.isError && <ErrorState error={mutation.error} />}<label className="field">Quelques mots sur vous<textarea name="bio" defaultValue={bio || ''} maxLength={500} placeholder="Vos découvertes préférées, ce qui vous rend curieux…" /></label><label className="field">Photo de profil <span className="optional">Adresse HTTPS d’une image</span><input type="url" name="avatarUrl" pattern="https?://.*" defaultValue={data?.avatarUrl || ''} maxLength={500} placeholder="https://…" /></label><div className="page-actions"><button className="button" disabled={mutation.isPending}>{mutation.isPending ? 'Enregistrement…' : 'Enregistrer'}</button><button type="button" className="button button-secondary" onClick={() => setEditing(false)}>Annuler</button></div></form>}
    <div className="stats-row"><div className="stat"><strong>{data?.stats.observations ?? observations.data?.pagination.total ?? '—'}</strong><span>Observations</span></div><div className="stat"><strong>{data?.stats.proposals ?? '—'}</strong><span>Propositions</span></div><div className="stat"><strong>{data?.stats.identifiedObservations ?? identified.data?.pagination.total ?? '—'}</strong><span>Identifiées</span></div></div>
    <section><div className="section-heading" style={{ marginBottom: 25 }}><h2>Les dernières rencontres</h2>{own && <Link className="text-link" to="/me/observations">Voir mon carnet <span aria-hidden="true">↗</span></Link>}</div>
      {observations.isPending && <Loading cards />}{observations.isError && <ErrorState error={observations.error} retry={() => observations.refetch()} />}
      {observations.data && (observations.data.data.length ? <div className="observation-grid">{observations.data.data.map(o => <ObservationCard key={o.id} observation={o} />)}</div> : <EmptyState title="Les premières pages restent à écrire" action={own && <Link className="button" to="/observations/new">Partager une observation</Link>}>Aucune observation n’a encore été partagée dans ce carnet.</EmptyState>)}
      {!features.profiles && !own && !observations.data?.data.length && <div style={{ marginTop: 20 }}><ComingSoon title="Le profil se dessine">Les profils publics détaillés seront bientôt disponibles.</ComingSoon></div>}
    </section>
  </>
}
