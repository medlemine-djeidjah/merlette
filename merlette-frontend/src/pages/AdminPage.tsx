import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createCategory, getCategories } from '../api/categories'
import { createSpecies } from '../api/species'
import { getObservations } from '../api/observations'
import { features } from '../config/features'
import SpeciesSearch from '../components/identification/SpeciesSearch'
import { ComingSoon, ErrorState, Loading } from '../components/common/Feedback'
import Icon from '../components/common/Icon'
import StatusBadge from '../components/observations/StatusBadge'
export default function AdminPage() {
  const [tab, setTab] = useState('reference')
  const [success, setSuccess] = useState('')
  const cache = useQueryClient()
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const recent = useQuery({ queryKey: ['moderation-observations'], queryFn: () => getObservations({ limit: 10 }), enabled: tab === 'moderation' && features.management })
  const categoryMutation = useMutation({ mutationFn: createCategory, onSuccess: async category => { setSuccess(`La catégorie « ${category.name} » a été ajoutée.`); await cache.invalidateQueries({ queryKey: ['categories'] }) } })
  const speciesMutation = useMutation({ mutationFn: createSpecies, onSuccess: async species => { setSuccess(`L’espèce « ${species.commonName} » a été ajoutée.`); await cache.invalidateQueries({ queryKey: ['species'] }) } })
  function addCategory(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSuccess(''); const form = new FormData(event.currentTarget); categoryMutation.mutate({ name: String(form.get('name')).trim(), slug: String(form.get('slug')).trim() }) }
  function addSpecies(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setSuccess(''); const form = new FormData(event.currentTarget); speciesMutation.mutate({ commonName: String(form.get('commonName')).trim(), scientificName: String(form.get('scientificName')).trim(), categoryId: Number(form.get('categoryId')) }) }
  return <><header className="page-heading"><span className="eyebrow">Administration</span><h1>Prendre soin du carnet.</h1><p>Entretenez le référentiel et veillez aux contenus partagés par la communauté.</p></header><div className="page-tabs"><button className={tab === 'reference' ? 'active' : ''} onClick={() => setTab('reference')} aria-pressed={tab === 'reference'}>Référentiel</button><button className={tab === 'moderation' ? 'active' : ''} onClick={() => setTab('moderation')} aria-pressed={tab === 'moderation'}>Modération</button></div>
    {success && <p className="feedback feedback-success" role="status" style={{ marginBottom: 20 }}><Icon name="check" size={18} />{success}</p>}
    {categories.isError && <ErrorState error={categories.error} retry={() => categories.refetch()} />}
    {tab === 'reference' ? <>
      {!features.administration && <p className="soft-note" style={{ marginBottom: 25 }}>La consultation des catégories est disponible. L’ajout de catégories et d’espèces sera bientôt ouvert.</p>}
      <div className="admin-grid"><section className="panel"><h2>Une nouvelle catégorie</h2><form className="form-stack" onSubmit={addCategory}>{categoryMutation.isError && <ErrorState error={categoryMutation.error} />}<label className="field">Nom<input name="name" required maxLength={50} placeholder="Ex. Lichens" disabled={!features.administration} /></label><label className="field">Identifiant<input name="slug" required maxLength={50} pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="Ex. lichens" disabled={!features.administration} /><small>Lettres minuscules sans accents, chiffres et tirets.</small></label><button className="button" disabled={!features.administration || categoryMutation.isPending}>{categoryMutation.isPending ? 'Ajout…' : 'Ajouter la catégorie'}<Icon name="plus" size={16} /></button></form></section>
        <section className="panel"><h2>Une nouvelle espèce</h2><form className="form-stack" onSubmit={addSpecies}>{speciesMutation.isError && <ErrorState error={speciesMutation.error} />}<label className="field">Nom commun<input name="commonName" required maxLength={150} placeholder="Ex. Sittelle torchepot" disabled={!features.administration} /></label><label className="field">Nom scientifique<input name="scientificName" required maxLength={150} placeholder="Ex. Sitta europaea" disabled={!features.administration} /></label><label className="field">Catégorie<select name="categoryId" required defaultValue="" disabled={!features.administration}><option value="" disabled>Choisir une catégorie</option>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><button className="button" disabled={!features.administration || speciesMutation.isPending || !categories.data}>{speciesMutation.isPending ? 'Ajout…' : 'Ajouter l’espèce'}<Icon name="plus" size={16} /></button></form></section></div>
      <section style={{ marginTop: 35 }}><h2>Catégories du référentiel{categories.data ? ` · ${categories.data.length}` : ''}</h2>{categories.isPending && <Loading />}<div className="table-wrap"><table><thead><tr><th>Catégorie</th><th>Identifiant</th></tr></thead><tbody>{categories.data?.map(c => <tr key={c.id}><td>{c.name}</td><td className="muted">{c.slug}</td></tr>)}</tbody></table></div></section>
      {features.identification && <section className="panel" style={{ marginTop: 25 }}><h2>Rechercher dans les espèces</h2><SpeciesSearch browse /></section>}
    </> : !features.management ? <ComingSoon title="Veiller sur la communauté" icon="settings">L’espace de modération sera bientôt disponible. Il permettra d’examiner les observations et de retirer les contenus inappropriés.</ComingSoon> : <>
      <p className="section-subtitle">Dernières observations publiées. Ouvrez une observation pour examiner son contenu et, si nécessaire, la supprimer.</p>{recent.isPending && <Loading />}{recent.isError && <ErrorState error={recent.error} retry={() => recent.refetch()} />}{recent.data && <div className="table-wrap"><table><thead><tr><th>Observation</th><th>Auteur</th><th>Statut</th><th>Action</th></tr></thead><tbody>{recent.data.data.map(o => <tr key={o.id}><td>{o.title || `Observation #${o.id}`}</td><td>{o.author.username}</td><td><StatusBadge status={o.status} /></td><td><Link className="text-link" to={`/observations/${o.id}`}>Examiner</Link></td></tr>)}</tbody></table></div>}
    </>}
  </>
}
